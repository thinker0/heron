#!/usr/bin/env python3
# -*- encoding: utf-8 -*-

#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.

import json
import logging
import hashlib
from base64 import urlsafe_b64encode

import jwt
import jwt.api_jwt
import jwt.algorithms

from functools import wraps
from json.decoder import JSONDecodeError
from typing import Dict, Union
from urllib.parse import urlencode

import requests
from expiringdict import ExpiringDict
from fastapi import Request

from jwt.algorithms import RSAAlgorithm
from jwt.api_jws import get_unverified_header, get_algorithm_by_name
from jwt.jwks_client import PyJWKClient
from jwt.exceptions import DecodeError
from jwt.exceptions import InvalidTokenError
from starlette.responses import RedirectResponse
from rediscluster import RedisCluster

from heron.common.src.python.utils import log

Log = log.Log
Log.setLevel(logging.DEBUG)

AUTH_KEY = "auth_key"
USER_INFO = "user_info"

class OpenIDConnectException(Exception):
  """Raised when OpenID login flow fails in various places."""

  def __init__(self, message: str, *args: object) -> None:
    super().__init__(*args)
    self._message = message
    self.args = args

  def __str__(self) -> str:
    return f"OpenIDConnectException: {self._message}, {self.args}"


class OpenIDConnect:
  well_known_pattern = "{}/.well-known/openid-configuration"
  _redis_cluster: RedisCluster = None
  _cache: ExpiringDict = None
  _prefix_key = "/auth/user_info/"
  _cache_expiration = 1800

  def __init__(self, verify: bool = True) -> None:
    self._authorization_server_uri = None
    self._client_id = None
    self._client_secret = None
    self._scope = "openid email profile"
    self._signing_algos = None
    self._inspection_endpoint = None
    self._device_authorization_endpoint = None
    self._jwks_uri = None
    self._token_endpoint = None
    self._authorization_endpoint = None
    self._userinfo_endpoint = None
    self._issuer = None
    self._jwks_client = None
    self._verify = verify
    self._is_initialized = False

  def initialize_oidc(self,
                      base_authorization_server_uri: str,
                      client_id: str,
                      client_secret: str,
                      scope: str = "openid email profile",
                      ):
    self._authorization_server_uri = base_authorization_server_uri
    self._scope = scope
    self._client_id = client_id
    self._client_secret = client_secret

    # Part 1: setup
    # get the OIDC config and JWKs to use
    # in OIDC, you must know your client_id (this is the OAuth 2.0 client_id)

    # example of fetching data from your OIDC server
    # see: https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfig
    endpoints = self.to_dict_or_raise(
      requests.get(f"{base_authorization_server_uri}/.well-known/openid-configuration", verify=self._verify)
    )
    self._issuer = endpoints.get("issuer")
    self._authorization_endpoint = endpoints.get("authorization_endpoint")
    self._token_endpoint = endpoints.get("token_endpoint")
    self._userinfo_endpoint = endpoints.get("userinfo_endpoint")
    self._jwks_uri = endpoints.get("jwks_uri")
    self._device_authorization_endpoint = endpoints.get("device_authorization_endpoint")
    self._inspection_endpoint = endpoints.get("introspection_endpoint")
    self._signing_algos = endpoints.get("id_token_signing_alg_values_supported")
    # setup a PyJWKClient to get the appropriate signing key
    # Update Certification: pip install --upgrade certifi
    self._jwks_client = PyJWKClient(uri=self._jwks_uri, cache_keys=True)
    self._jwks_client.fetch_data()
    self._is_initialized = True

  def get_user(self, hash_key: str) -> Union[Dict, None]:
    if self._cache.__contains__(hash_key):
      Log.debug('cache hit: %s' % hash_key)
      val = self._cache.get(hash_key, None)
      return val
    try:
      val = self._redis_cluster.get(self._prefix_key + '%s' % hash_key)
      if hash_key is not None and val is not None:
        Log.debug('redis get: %s=%s' % (hash_key, str(val, 'utf-8')))
        res = jwt.api_jwt.decode(val, self._client_secret, algorithms=["HS256"])
        # Set cache
        self._cache[hash_key] = res
        return res
    except Exception as e:
      Log.error('redis get: %s' % e)
    return None

  def set_cache(self, hash_key: str, user: Dict):
    if user is not None and hash_key is not None:
      self._cache[hash_key] = user
      Log.debug('cache set: %s=%s' % (hash_key, user))
      enc = jwt.encode(user, self._client_secret, algorithm="HS256")
      self._redis_cluster.set(self._prefix_key + '%s' % hash_key, enc, ex=self._cache_expiration)
      Log.debug('redis set: %s=%s' % (hash_key, enc))

  # self redirect uri
  def get_self_redirect_uri(self, request: Request) -> str:
    scheme = request.url.scheme
    if request.headers.get("X-Forwarded-Proto"):
      scheme = request.headers.get("X-Forwarded-Proto")
    url = "{scheme}://{netloc}{path}".format(
      scheme=scheme,
      netloc=request.url.netloc,
      path=request.url.path,
    )
    params = []
    for k, v in request.query_params.items():
      if k == "code" or k == "state":
        continue
      params.append(f"{k}={v}")
    if 0 != len(params):
      url += "?" + "&".join(params)
    return url

  # 'device_code' of Device Authorization Code
  def device_authorization_code(self) -> Dict:
    response = requests.post(
      self._device_authorization_endpoint,
      data={
        "scope": self._scope,
        "client_id": self._client_id,
        "client_secret": self._client_secret,
      },
      verify=self._verify
    )
    device_code = self.to_dict_or_raise(response)
    Log.debug(
      f"Device Authorization response: {self._device_authorization_endpoint} {response.status_code}: {device_code}")
    return device_code

  # 'refresh_token' of Device Authorization Token
  def get_device_authorization_token(self, device_code: str) -> Dict:
    response = requests.post(
      self._token_endpoint,
      data={
        "grant_type": "urn:ietf:params:oauth:grant-type:device_code",
        "scope": self._scope,  # "openid email profile
        "client_id": self._client_id,
        "client_secret": self._client_secret,
        "device_code": device_code,
      },
      verify=self._verify
    )
    refresh_token = self.to_dict_or_raise(response)
    Log.debug(f"Token response: {self._token_endpoint} {response.status_code}: {refresh_token}")
    return refresh_token

  # 'access_token' of Refresh Token
  def get_refresh_token(self, refresh_token: str) -> Dict:
    response = requests.post(
      self._token_endpoint,
      data={
        "grant_type": "refresh_token",
        "client_id": self._client_id,
        "client_secret": self._client_secret,
        "refresh_token": refresh_token,
      },
      verify=self._verify
    )
    refresh_status = self.to_dict_or_raise(response)
    # TODO refresh before expiration (`expires_in` seconds)
    Log.debug(f"Token response: {self._token_endpoint} {response.status_code}: {refresh_status}")
    return refresh_status

  # 'access_token' of Introspection API Endpoint
  def get_introspection_token(self, access_token: str) -> Dict:
    response = requests.post(
      self._inspection_endpoint,
      data={
        "client_id": self._client_id,
        "client_secret": self._client_secret,
        "token": access_token,
        "token_type_hit": "access_token",
      },
      verify=self._verify
    )
    introspection = self.to_dict_or_raise(response)
    # TODO refresh before expiration (`expires_in` seconds)
    Log.debug(f"Token response: {self._inspection_endpoint} {response.status_code}: {introspection}")
    return introspection

  # 'user_info' of Access Token
  def get_user_info(self, access_token: str) -> Dict:
    bearer = "Bearer {}".format(access_token)
    headers = {"Authorization": bearer}
    response = requests.get(self._userinfo_endpoint, headers=headers, verify=self._verify)
    user_info = self.to_dict_or_raise(response)
    Log.debug(f"User info response: {self._userinfo_endpoint} {response.status_code}: {user_info}")
    return user_info

  def authenticate(
      self, code: str, callback_uri: str, get_user_info: bool = False
  ) -> (str, Dict):
    """
    https://pyjwt.readthedocs.io/en/stable/usage.html#oidc-login-flow

    :param code:
    :param callback_uri:
    :param get_user_info:
    :return:
    """
    # Part 2: login / authorization
    # when a user completes an OIDC login flow, there will be a well-formed
    # response object to parse/handle

    # data from the login flow
    # see: https://openid.net/specs/openid-connect-core-1_0.html#TokenResponse
    auth_token = self.get_auth_token(code, callback_uri)
    id_token = auth_token.get("id_token")
    access_token = auth_token.get("access_token")

    # Part 3: decode and validate at_hash
    # after the login is complete, the id_token needs to be decoded
    # this is the stage at which an OIDC client must verify the at_hash

    # get signing_key from id_token
    signing_key = self._jwks_client.get_signing_key_from_jwt(id_token)

    # now, jwt.api_jwt.decode to get payload
    payload = jwt.api_jwt.decode(jwt=id_token,
                         key=signing_key.key,
                         algorithms=self._signing_algos,
                         audience=self._client_id)
    user_info = self.get_user_info(auth_token.get("access_token"))
    if not payload.get("at_hash"):
      Log.warning("at_hash not found in id_token.")
    else:
      try:
        # get the algorithm used to sign the id_token
        alg = get_unverified_header(id_token).get("alg")
        # get the pyjwt algorithm object
        alg_obj = get_algorithm_by_name(alg)
      except DecodeError:
        Log.warning("Error getting unverified header in jwt.")
        raise OpenIDConnectException
      # compute at_hash, then validate / assert
      digest = alg_obj.compute_hash_digest(access_token.encode("utf-8"))
      at_hash = urlsafe_b64encode(digest[: (len(digest) // 2)]).decode('utf-8').rstrip("=")
      # validate the at_hash of the access_token
      if at_hash != payload.get("at_hash"):
        message=f"at_hash mismatch error. {at_hash} != {payload.get('at_hash')}"
        Log.warning(message)
        raise OpenIDConnectException(message)
      validated_token = self.obtain_validated_token(alg_obj, id_token)
      if not get_user_info:
        return validated_token
      self.validate_sub_matching(validated_token, user_info)

    Log.debug(f"User info: {user_info}")
    return auth_token, user_info

  def get_auth_redirect_uri(self, callback_url: str, state: Dict) -> str:
    enc_state = jwt.encode(state, self._client_secret, algorithm="HS256")
    params = {
      "response_type": "code",
      "scope": self._scope,
      "client_id": self._client_id,
      "redirect_uri": callback_url,
      "state": enc_state,
    }
    Log.debug(f"Redirecting to: {self._authorization_endpoint}: {params}")
    return self._authorization_endpoint + "?" + urlencode(params)

  def get_auth_token(self, code: str, callback_uri: str) -> Dict:
    headers = {}
    data = {
      "grant_type": "authorization_code",
      "scope": self._scope,
      "redirect_uri": callback_uri,
      "client_id": self._client_id,
      "client_secret": self._client_secret,
      "code": code,
    }
    Log.debug(f"Requesting token with: {self._token_endpoint}: {data}")
    response = requests.post(
      self._token_endpoint, data=data, headers=headers, verify=self._verify
    )
    token_data = self.to_dict_or_raise(response)
    # TODO refresh before expiration (`expires_in` seconds)
    Log.debug(f"Token response: {self._token_endpoint} {response.status_code}: {token_data}")
    return token_data

  def obtain_validated_token(self, alg: str, id_token: str) -> Dict:
    if alg == "HS256":
      try:
        return jwt.api_jwt.decode(
          id_token,
          self._client_secret,
          algorithms=["HS256"],
          audience=self._client_id,
        )
      except InvalidTokenError:
        Log.error("An error occurred while decoding the id_token")
        raise OpenIDConnectException(
          "An error occurred while decoding the id_token"
        )
    elif alg == "RS256":
      response = requests.get(self._jwks_uri, verify=self._verify)
      web_key_sets = self.to_dict_or_raise(response)
      Log.debug(f"JWKS: {self._jwks_uri} {response.status_code}: {web_key_sets}")
      jwks = web_key_sets.get("keys")
      public_key = self.extract_token_key(jwks, id_token)
      try:
        return jwt.api_jwt.decode(
          id_token,
          key=public_key,
          algorithms=["RS256"],
          audience=self._client_id,
        )
      except InvalidTokenError as e:
        Log.error(f"An error occurred while decoding the id_token {e}")
        raise OpenIDConnectException(
          "An error occurred while decoding the id_token"
        )
    else:
      raise OpenIDConnectException("Unsupported jwt algorithm found.")

  def extract_token_key(self, jwks: Dict, id_token: str) -> str:
    public_keys = {}
    for jwk in jwks:
      kid = jwk.get("kid")
      if not kid:
        continue
      public_keys[kid] = RSAAlgorithm.from_jwk(
        json.dumps(jwk)
      )
    try:
      kid = get_unverified_header(id_token).get("kid")
    except DecodeError:
      Log.warning("kid could not be extracted.")
      raise OpenIDConnectException("kid could not be extracted.")
    return public_keys.get(kid)

  @staticmethod
  def validate_sub_matching(token: Dict, user_info: Dict) -> None:
    token_sub = ""  # nosec
    if token:
      token_sub = token.get("sub")
    if token_sub != user_info.get("sub") or not token_sub:
      Log.warning("Subject mismatch error.")
      raise OpenIDConnectException("Subject mismatch error.")

  @staticmethod
  def to_dict_or_raise(response: requests.Response) -> Dict:
    if response.status_code != 200:
      Log.error(f"Returned with status {response.raise_for_status()}.")
      raise OpenIDConnectException(
        f"Status {response.raise_for_status()}."
      )
    try:
      return response.json()
    except JSONDecodeError:
      Log.error("Unable to decode json.")
      raise OpenIDConnectException(
        "Was not able to retrieve data from the response."
      )

  def require_login(self, view_func):
    @wraps(view_func)
    def decorated(
        request: Request, get_user_info: bool = False, *args, **kwargs
    ):
      if not self._is_initialized:
        Log.debug("OpenID Connect not initialized.")
        return view_func(request, *args, **kwargs)

      netloc = request.url.netloc
      cookies = request.cookies
      if cookies.get(AUTH_KEY):
        c_user_info = self.get_user(cookies.get(AUTH_KEY))
        if c_user_info:
          if hashlib.sha256(f"{c_user_info.get('email')}/{netloc}".encode('utf-8')).hexdigest() == cookies.get(
              AUTH_KEY):
            request.__setattr__(USER_INFO, c_user_info)
            # Authenticated user
            return view_func(request, *args, **kwargs)
          else:
            Log.error("User info invalidate.")
      scheme = request.url.scheme
      if request.headers.get("X-Forwarded-Proto"):
        scheme = request.headers.get("X-Forwarded-Proto")
      redirect_uri = "{scheme}://{netloc}{path}".format(
        scheme=scheme,
        netloc=request.url.netloc,
        path=request.url.path,
      )
      if 0 != len(request.query_params):
        redirect_uri += "?" + request.url.query
      state = {
        # This is a simple way to pass the redirect_uri to the callback
        "redirect_uri": redirect_uri,
      }
      code = request.query_params.get("code")
      if not code:
        # Redirect to the authorization server
        return RedirectResponse(self.get_auth_redirect_uri(redirect_uri, state))
      try:
        state_query = request.query_params.get("state")
        state_decode = jwt.api_jwt.decode(
          state_query, self._client_secret, algorithms=["HS256"]
        )
        auth_token, user_info = self.authenticate(
          code, state_decode["redirect_uri"], get_user_info=get_user_info
        )
        cookie_domain = netloc
        s_cache_key = hashlib.sha256(f"{user_info.get('email')}/{cookie_domain}".encode('utf-8')).hexdigest()
        self.set_cache(s_cache_key, user_info)
        if cookie_domain.startswith("localhost"):
          cookie_domain = "localhost"
        if cookie_domain.__contains__(":"):
          cookie_domain = cookie_domain.split(":")[0]
        auth_headers = {
          'Set-Cookie': f"auth_key={s_cache_key}; Domain={cookie_domain}; Path=/; Max-Age={auth_token.get('expires_in')}; HttpOnly"}
        self_redirect_uri = self.get_self_redirect_uri(request)
        return RedirectResponse(self_redirect_uri, headers=auth_headers)
      except OpenIDConnectException as e:
        Log.error(f"An error occurred during the authentication process. {e}")
        return RedirectResponse(self.get_auth_redirect_uri(redirect_uri, state))

    return decorated

  def set_session_store(self, cache: ExpiringDict, redis_cluster: RedisCluster):
    self._cache = cache
    self._redis_cluster = redis_cluster
