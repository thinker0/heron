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

workspace(name = "org_apache_heron")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_file")

RULES_JVM_EXTERNAL_TAG = "5.3"

RULES_JVM_EXTERNAL_SHA = "6cc8444b20307113a62b676846c29ff018402fd4c7097fcd6d0a0fd5f2e86429"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")

rules_jvm_external_deps()

load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")

rules_jvm_external_setup()

load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@rules_jvm_external//:specs.bzl", "maven")

# versions shared across artifacts that should be upgraded together
aws_version = "1.11.58"

curator_version = "5.1.0"

google_client_version = "1.22.0"

jackson_version = "2.8.8"

powermock_version = "1.6.2"

reef_version = "0.14.0"

slf4j_version = "1.7.36"

logback_verison = "1.2.11"

distributedlog_version = "4.14.5"

http_client_version = "4.5.2"

# heron API server
jetty_version = "9.4.6.v20170531"

jersey_version = "2.25.1"

kubernetes_client_version = "14.0.0"

maven_install(
    name = "maven",
    artifacts = [
        "org.slf4j:slf4j-api:%s" % slf4j_version,
        "org.slf4j:log4j-over-slf4j:%s" % slf4j_version,
        "org.slf4j:jul-to-slf4j:%s" % slf4j_version,
        "org.slf4j:jcl-over-slf4j:%s" % slf4j_version,
        "ch.qos.logback:logback-classic:%s" % logback_verison,
        "antlr:antlr:2.7.7",
        "org.apache.zookeeper:zookeeper:3.8.3",
        "io.kubernetes:client-java:" + kubernetes_client_version,
        "io.kubernetes:client-java-api-fluent:" + kubernetes_client_version,
        "com.esotericsoftware:kryo:5.4.0",
        "org.apache.avro:avro:1.7.4",
        "org.apache.mesos:mesos:0.22.0",
        "com.hashicorp.nomad:nomad-sdk:0.7.0",
        "org.apache.hadoop:hadoop-core:0.20.2",
        "org.apache.pulsar:pulsar-client:jar:shaded:1.19.0-incubating",
        "org.apache.kafka:kafka-clients:2.2.0",
        "com.google.apis:google-api-services-storage:v1-rev108-" + google_client_version,
        "org.apache.reef:reef-runtime-yarn:" + reef_version,
        "org.apache.reef:reef-runtime-local:" + reef_version,
        "org.apache.httpcomponents:httpclient:" + http_client_version,
        "org.apache.httpcomponents:httpmime:" + http_client_version,
        "com.google.apis:google-api-services-storage:v1-rev108-1.22.0",
        "com.microsoft.dhalion:dhalion:0.2.6",
        "com.amazonaws:aws-java-sdk-s3:" + aws_version,
        "org.eclipse.jetty:jetty-server:" + jetty_version,
        "org.eclipse.jetty:jetty-http:" + jetty_version,
        "org.eclipse.jetty:jetty-security:" + jetty_version,
        "org.eclipse.jetty:jetty-continuation:" + jetty_version,
        "org.eclipse.jetty:jetty-servlets:" + jetty_version,
        "org.eclipse.jetty:jetty-servlet:" + jetty_version,
        "org.jvnet.mimepull:mimepull:1.9.7",
        "javax.servlet:javax.servlet-api:3.1.0",
        "org.glassfish.jersey.media:jersey-media-json-jackson:" + jersey_version,
        "org.glassfish.jersey.media:jersey-media-multipart:" + jersey_version,
        "org.glassfish.jersey.containers:jersey-container-servlet:" + jersey_version,
        "org.apache.distributedlog:distributedlog-core:" + distributedlog_version,
        "io.netty:netty-all:4.1.76.Final",
        "aopalliance:aopalliance:1.0",
        "org.roaringbitmap:RoaringBitmap:0.6.51",
        "com.google.inject:guice:5.1.0",
        "com.google.inject.extensions:guice-assistedinject:5.1.0",
        "com.google.guava:guava:23.6-jre",
        "com.google.protobuf:protobuf-java:3.19.6",
        "io.gsonfire:gson-fire:1.8.3",
        "org.apache.curator:curator-framework:" + curator_version,
        "org.apache.curator:curator-recipes:" + curator_version,
        "org.apache.curator:curator-client:" + curator_version,
        "tech.tablesaw:tablesaw-core:0.11.4",
        "org.glassfish.hk2.external:aopalliance-repackaged:2.5.0-b32",
        "org.apache.commons:commons-compress:1.14",
        "org.apache.commons:commons-lang3:3.12.0",
        "commons-io:commons-io:2.4",
        "commons-collections:commons-collections:3.2.1",
        "commons-cli:commons-cli:1.3.1",
        "org.apache.commons:commons-compress:1.14",
        "com.jayway.jsonpath:json-path:2.1.0",
        "com.fasterxml.jackson.core:jackson-core:%s" % jackson_version,
        "com.fasterxml.jackson.core:jackson-annotations:%s" % jackson_version,
        "com.fasterxml.jackson.core:jackson-databind:%s" % jackson_version,
        "com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:%s" % jackson_version,
        "com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:%s" % jackson_version,
        "javax.xml.bind:jaxb-api:2.3.1",
        "javax.activation:activation:1.1.1",
        "org.mockito:mockito-all:1.10.19",
        "org.powermock:powermock-api-mockito:" + powermock_version,
        "org.powermock:powermock-module-junit4:" + powermock_version,
        "com.puppycrawl.tools:checkstyle:6.17",
        "com.googlecode.json-simple:json-simple:1.1",
        maven.artifact(
            artifact = "httpclient",
            classifier = "tests",
            group = "org.apache.httpcomponents",
            packaging = "test-jar",
            version = http_client_version,
        ),
    ],
    excluded_artifacts = [
        "org.slf4j:slf4j-jdk14",
        "org.slf4j:slf4j-log4j12",
        "log4j:log4j",
        "commons-logging:commons-logging",
    ],
    fail_if_repin_required = True,
    fetch_sources = True,
    maven_install_json = "//:maven_install.json",
    repositories = [
        "https://jcenter.bintray.com",
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
    version_conflict_policy = "pinned",
)

# https://github.com/bazelbuild/rules_jvm_external#updating-maven_installjson
# To update `maven_install.json` run the following command:
# `REPIN=1 bazel run @unpinned_maven//:pin`
load("@maven//:defs.bzl", "pinned_maven_install")

pinned_maven_install()

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

git_repository(
    name = "com_github_johnynek_bazel_jar_jar",
    commit = "171f268569384c57c19474b04aebe574d85fde0d",  # Latest commit SHA as at 2019/02/13
    remote = "https://github.com/johnynek/bazel_jar_jar.git",
    shallow_since = "1594234634 -1000",
)

load(
    "@com_github_johnynek_bazel_jar_jar//:jar_jar.bzl",
    "jar_jar_repositories",
)

jar_jar_repositories()

http_archive(
    name = "bazel_skylib",
    sha256 = "74d544d96f4a5bb630d465ca8bbcfe231e3594e5aae57e1edbf17a6eb3ca2506",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/1.3.0/bazel-skylib-1.3.0.tar.gz",
        "https://github.com/bazelbuild/bazel-skylib/releases/download/1.3.0/bazel-skylib-1.3.0.tar.gz",
    ],
)

load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")

bazel_skylib_workspace()

http_archive(
    name = "rules_python",
    sha256 = "497ca47374f48c8b067d786b512ac10a276211810f4a580178ee9b9ad139323a",
    strip_prefix = "rules_python-0.16.1",
    url = "https://github.com/bazelbuild/rules_python/archive/refs/tags/0.16.1.tar.gz",
)

http_archive(
    name = "platforms",
    sha256 = "5308fc1d8865406a49427ba24a9ab53087f17f5266a7aabbfc28823f3916e1ca",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/platforms/releases/download/0.0.6/platforms-0.0.6.tar.gz",
        "https://github.com/bazelbuild/platforms/releases/download/0.0.6/platforms-0.0.6.tar.gz",
    ],
)

http_archive(
    name = "rules_pkg",
    sha256 = "eea0f59c28a9241156a47d7a8e32db9122f3d50b505fae0f33de6ce4d9b61834",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/rules_pkg/releases/download/0.8.0/rules_pkg-0.8.0.tar.gz",
        "https://github.com/bazelbuild/rules_pkg/releases/download/0.8.0/rules_pkg-0.8.0.tar.gz",
    ],
)

load("@rules_pkg//pkg:deps.bzl", "rules_pkg_dependencies")

rules_pkg_dependencies()

# for pex repos
PEX_PKG = "https://files.pythonhosted.org/packages/f5/75/df33045e065a49b6b39807343da8e3fa24d3e9665ce3650b9dfe59c3e97b/pex-2.1.164-py2.py3-none-any.whl"

PYTEST_PKG = "https://files.pythonhosted.org/packages/40/76/86f886e750b81a4357b6ed606b2bcf0ce6d6c27ad3c09ebf63ed674fc86e/pytest-6.2.5-py3-none-any.whl"

REQUESTS_PKG = "https://files.pythonhosted.org/packages/70/8e/0e2d847013cb52cd35b38c009bb167a1a26b2ce6cd6965bf26b47bc0bf44/requests-2.31.0-py3-none-any.whl"

SETUPTOOLS_PKG = "https://files.pythonhosted.org/packages/f7/29/13965af254e3373bceae8fb9a0e6ea0d0e571171b80d6646932131d6439b/setuptools-69.5.1-py3-none-any.whl"

WHEEL_PKG = "https://files.pythonhosted.org/packages/27/d6/003e593296a85fd6ed616ed962795b2f87709c3eee2bca4f6d0fe55c6d00/wheel-0.37.1-py2.py3-none-any.whl"

CHARSET_PKG = "https://files.pythonhosted.org/packages/68/2b/02e9d6a98ddb73fa238d559a9edcc30b247b8dc4ee848b6184c936e99dc0/charset_normalizer-3.0.1-py3-none-any.whl"

IDNA_PKG = "https://files.pythonhosted.org/packages/04/a2/d918dcd22354d8958fe113e1a3630137e0fc8b44859ade3063982eacd2a4/idna-3.3-py3-none-any.whl"

CERTIFI_PKG = "https://files.pythonhosted.org/packages/ba/06/a07f096c664aeb9f01624f858c3add0a4e913d6c96257acb4fce61e7de14/certifi-2024.2.2-py3-none-any.whl"

URLLIB3_PKG = "https://files.pythonhosted.org/packages/a2/73/a68704750a7679d0b6d3ad7aa8d4da8e14e151ae82e6fee774e6e0d05ec8/urllib3-2.2.1-py3-none-any.whl"

http_file(
    name = "urllib3_pkg",
    downloaded_file_path = "urllib3-2.2.1-py3-none-any.whl",
    sha256 = "450b20ec296a467077128bff42b73080516e71b56ff59a60a02bef2232c4fa9d",
    urls = [URLLIB3_PKG],
)

http_file(
    name = "certifi_pkg",
    downloaded_file_path = "certifi-2024.2.2-py3-none-any.whl",
    sha256 = "dc383c07b76109f368f6106eee2b593b04a011ea4d55f652c6ca24a754d1cdd1",
    urls = [CERTIFI_PKG],
)

http_file(
    name = "idna_pkg",
    downloaded_file_path = "idna-3.3-py2.py3-none-any.whl",
    sha256 = "84d9dd047ffa80596e0f246e2eab0b391788b0503584e8945f2368256d2735ff",
    urls = [IDNA_PKG],
)

http_file(
    name = "charset_pkg",
    downloaded_file_path = "charset_normalizer-3.0.1-py3-none-any.whl",
    sha256 = "7e189e2e1d3ed2f4aebabd2d5b0f931e883676e51c7624826e0a4e5fe8a0bf24",
    urls = [CHARSET_PKG],
)

http_file(
    name = "pytest_pkg",
    downloaded_file_path = "pytest-6.2.5-py3-none-any.whl",
    sha256 = "7310f8d27bc79ced999e760ca304d69f6ba6c6649c0b60fb0e04a4a77cacc134",
    urls = [PYTEST_PKG],
)

http_file(
    name = "wheel_pkg",
    downloaded_file_path = "wheel-0.37.1-py3-none-any.whl",
    sha256 = "4bdcd7d840138086126cd09254dc6195fb4fc6f01c050a1d7236f2630db1d22a",
    urls = [WHEEL_PKG],
)

http_file(
    name = "pex_pkg",
    downloaded_file_path = "pex-2.1.164-py2.py3-none-any.whl",
    sha256 = "37d7d4cad605784dbf3494608fb5928c2c5e385d60dcc00bc816c4c70d6f0fae",
    urls = [PEX_PKG],
)

http_file(
    name = "requests_pkg",
    downloaded_file_path = "requests-2.31.0-py3-none-any.whl",
    sha256 = "58cd2187c01e70e6e26505bca751777aa9f2ee0b7f4300988b709f44e013003f",
    urls = [REQUESTS_PKG],
)

http_file(
    name = "setuptools_pkg",
    downloaded_file_path = "setuptools-69.5.1-py3-none-any.whl",
    sha256 = "c636ac361bc47580504644275c9ad802c50415c7522212252c033bd15f301f32",
    urls = [SETUPTOOLS_PKG],
)

# end pex repos

# protobuf dependencies for C++ and Java
http_archive(
    name = "com_google_protobuf",
    sha256 = "9a301cf94a8ddcb380b901e7aac852780b826595075577bb967004050c835056",
    strip_prefix = "protobuf-3.19.6",
    urls = ["https://github.com/protocolbuffers/protobuf/archive/v3.19.6.tar.gz"],
)
# end protobuf dependencies for C++ and Java

# 3rdparty C++ dependencies
http_archive(
    name = "com_github_gflags_gflags",
    sha256 = "34af2f15cf7367513b352bdcd2493ab14ce43692d2dcd9dfc499492966c64dcf",
    strip_prefix = "gflags-2.2.2",
    urls = ["https://github.com/gflags/gflags/archive/v2.2.2.tar.gz"],
)

http_archive(
    name = "org_libevent_libevent",
    build_file = "@//:third_party/libevent/libevent.BUILD",
    sha256 = "e864af41a336bb11dab1a23f32993afe963c1f69618bd9292b89ecf6904845b0",
    strip_prefix = "libevent-2.1.10-stable",
    urls = ["https://github.com/libevent/libevent/releases/download/release-2.1.10-stable/libevent-2.1.10-stable.tar.gz"],
)

http_archive(
    name = "org_nongnu_libunwind",
    build_file = "@//:third_party/libunwind/libunwind.BUILD",
    sha256 = "90337653d92d4a13de590781371c604f9031cdb50520366aa1e3a91e1efb1017",
    strip_prefix = "libunwind-1.5.0",
    urls = ["https://github.com/libunwind/libunwind/releases/download/v1.5/libunwind-1.5.0.tar.gz"],
)

http_archive(
    name = "org_apache_zookeeper",
    build_file = "@//:third_party/zookeeper/BUILD",
    patch_args = ["-p1"],
    patches = ["//third_party/zookeeper:zookeeper_jute.patch"],
    sha256 = "b0c5684640bea2d8bd6610b47ff41be2aefd6c910ba48fcad5949bd2bf2fa1ac",
    strip_prefix = "apache-zookeeper-3.8.0",
    urls = ["https://archive.apache.org/dist/zookeeper/zookeeper-3.8.0/apache-zookeeper-3.8.0.tar.gz"],
)

http_archive(
    name = "com_github_gperftools_gperftools",
    build_file = "@//:third_party/gperftools/gperftools.BUILD",
    sha256 = "83e3bfdd28b8bcf53222c3798d4d395d52dadbbae59e8730c4a6d31a9c3732d8",
    strip_prefix = "gperftools-2.10",
    urls = ["https://github.com/gperftools/gperftools/releases/download/gperftools-2.10/gperftools-2.10.tar.gz"],
)

http_archive(
    name = "com_github_google_glog",
    sha256 = "21bc744fb7f2fa701ee8db339ded7dce4f975d0d55837a97be7d46e8382dea5a",
    strip_prefix = "glog-0.5.0",
    urls = ["https://github.com/google/glog/archive/v0.5.0.zip"],
)

http_archive(
    name = "com_google_googletest",
    sha256 = "b4870bf121ff7795ba20d20bcdd8627b8e088f2d1dab299a031c1034eddc93d5",
    strip_prefix = "googletest-release-1.11.0",
    urls = ["https://github.com/google/googletest/archive/release-1.11.0.tar.gz"],
)

http_archive(
    name = "com_github_cereal",
    build_file = "@//:third_party/cereal/cereal.BUILD",
    sha256 = "1921f26d2e1daf9132da3c432e2fd02093ecaedf846e65d7679ddf868c7289c4",
    strip_prefix = "cereal-1.2.2",
    urls = ["https://github.com/USCiLab/cereal/archive/v1.2.2.tar.gz"],
)

http_archive(
    name = "com_github_jbeder_yaml_cpp",
    build_file = "@//:third_party/yaml-cpp/yaml.BUILD",
    sha256 = "e4d8560e163c3d875fd5d9e5542b5fd5bec810febdcba61481fe5fc4e6b1fd05",
    strip_prefix = "yaml-cpp-yaml-cpp-0.6.2",
    urls = ["https://github.com/jbeder/yaml-cpp/archive/yaml-cpp-0.6.2.tar.gz"],
)

http_archive(
    name = "com_github_corvusoft_kashmir_cpp",
    build_file = "@//:third_party/kashmir/kashmir.BUILD",
    patch_args = ["-p1"],
    patches = ["//third_party/kashmir:kashmir-random-fix.patch"],
    sha256 = "c3515d6c7a470663f06b79bb23cbb2ff2f3feab4c2a333f783edc0a802f1d062",
    strip_prefix = "kashmir-dependency-19fb1d5c14866bd5202c2458baf50263001a9cb0",
    urls = ["https://github.com/Corvusoft/kashmir-dependency/archive/19fb1d5c14866bd5202c2458baf50263001a9cb0.zip"],
)

http_archive(
    name = "com_github_danmar_cppcheck",
    build_file = "@//:third_party/cppcheck/cppcheck.BUILD",
    sha256 = "9285bf64af22a07fb24a7431510cc34fba118cf6950190abc2a08c9f7a7084c8",
    strip_prefix = "cppcheck-2.7",
    urls = ["https://github.com/danmar/cppcheck/archive/refs/tags/2.7.zip"],
)

http_archive(
    name = "com_github_hopscotch_hashmap",
    build_file = "@//:third_party/hopscotch-hashmap/hopscotch.BUILD",
    sha256 = "73e301925e1418c5ed930ef37ebdcab2c395a6d1bdaf5a012034bb75307d33f1",
    strip_prefix = "hopscotch-map-2.2.1",
    urls = ["https://github.com/Tessil/hopscotch-map/archive/v2.2.1.tar.gz"],
)
# end 3rdparty C++ dependencies

# for helm
http_archive(
    name = "helm_mac",
    build_file = "@//:third_party/helm/helm.BUILD",
    sha256 = "5a0738afb1e194853aab00258453be8624e0a1d34fcc3c779989ac8dbcd59436",
    strip_prefix = "darwin-amd64",
    urls = ["https://get.helm.sh/helm-v3.7.2-darwin-amd64.tar.gz"],
)

http_archive(
    name = "helm_linux",
    build_file = "@//:third_party/helm/helm.BUILD",
    sha256 = "4ae30e48966aba5f807a4e140dad6736ee1a392940101e4d79ffb4ee86200a9e",
    strip_prefix = "linux-amd64",
    urls = ["https://get.helm.sh/helm-v3.7.2-linux-amd64.tar.gz"],
)
# end helm

# for docker image building

http_archive(
    name = "io_bazel_rules_docker",
    sha256 = "27d53c1d646fc9537a70427ad7b034734d08a9c38924cc6357cc973fed300820",
    strip_prefix = "rules_docker-0.24.0",
    urls = ["https://github.com/bazelbuild/rules_docker/releases/download/v0.24.0/rules_docker-v0.24.0.tar.gz"],
)

load(
    "@io_bazel_rules_docker//repositories:repositories.bzl",
    container_repositories = "repositories",
)

container_repositories()

load("@io_bazel_rules_docker//repositories:deps.bzl", container_deps = "deps")

container_deps()

load("@io_bazel_rules_docker//repositories:py_repositories.bzl", "py_deps")

py_deps()

load(
    "@io_bazel_rules_docker//container:container.bzl",
    "container_pull",
)

container_pull(
    name = "heron-base",
    digest = "sha256:495800e9eb001dfd2fb41d1941155203bb9be06b716b0f8b1b0133eb12ea813c",
    registry = "index.docker.io",
    repository = "heron/base",
    tag = "0.5.0",
)
# end docker image building

# scala integration
http_archive(
    name = "io_bazel_rules_scala",
    sha256 = "77a3b9308a8780fff3f10cdbbe36d55164b85a48123033f5e970fdae262e8eb2",
    strip_prefix = "rules_scala-20220201",
    type = "zip",
    url = "https://github.com/bazelbuild/rules_scala/releases/download/20220201/rules_scala-20220201.zip",
)

skylib_version = "1.0.3"

http_archive(
    name = "bazel_skylib",
    sha256 = "1c531376ac7e5a180e0237938a2536de0c54d93f5c278634818e0efc952dd56c",
    type = "tar.gz",
    url = "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/{}/bazel-skylib-{}.tar.gz".format(skylib_version, skylib_version),
)

load("@io_bazel_rules_scala//:scala_config.bzl", "scala_config")

scala_config(scala_version = "2.12.15")

load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")

scala_repositories()

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")

rules_proto_dependencies()

rules_proto_toolchains()

load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")

scala_register_toolchains()

# optional: setup ScalaTest toolchain and dependencies
load("@io_bazel_rules_scala//testing:scalatest.bzl", "scalatest_repositories", "scalatest_toolchain")

scalatest_repositories()

scalatest_toolchain()
