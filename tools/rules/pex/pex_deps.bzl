load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file")

def _pex_deps_impl(ctx):
    http_file(
        name = "urllib3_pkg",
        downloaded_file_path = "urllib3-1.26.18-py2.py3-none-any.whl",
        sha256 = "34b97092d7e0a3a8cf7cd10e386f401b3737364026c45e622aa02903dffe0f07",
        urls = ["https://files.pythonhosted.org/packages/b0/53/aa91e163dcfd1e5b82d8a890ecf13314e3e149c05270cc644581f77f17fd/urllib3-1.26.18-py2.py3-none-any.whl"],
    )
    http_file(
        name = "certifi_pkg",
        downloaded_file_path = "certifi-2024.2.2-py3-none-any.whl",
        sha256 = "dc383c07b76109f368f6106eee2b593b04a011ea4d55f652c6ca24a754d1cdd1",
        urls = ["https://files.pythonhosted.org/packages/ba/06/a07f096c664aeb9f01624f858c3add0a4e913d6c96257acb4fce61e7de14/certifi-2024.2.2-py3-none-any.whl"],
    )
    http_file(
        name = "idna_pkg",
        downloaded_file_path = "idna-3.3-py2.py3-none-any.whl",
        sha256 = "84d9dd047ffa80596e0f246e2eab0b391788b0503584e8945f2368256d2735ff",
        urls = ["https://files.pythonhosted.org/packages/04/a2/d918dcd22354d8958fe113e1a3630137e0fc8b44859ade3063982eacd2a4/idna-3.3-py3-none-any.whl"],
    )
    http_file(
        name = "charset_pkg",
        downloaded_file_path = "charset_normalizer-3.3.2-py3-none-any.whl",
        sha256 = "3e4d1f6587322d2788836a99c69062fbb091331ec940e02d12d179c1d53e25fc",
        urls = ["https://files.pythonhosted.org/packages/28/76/e6222113b83e3622caa4bb41032d0b1bf785250607392e1b778aca0b8a7d/charset_normalizer-3.3.2-py3-none-any.whl"],
    )
    http_file(
        name = "pytest_pkg",
        downloaded_file_path = "pytest-6.2.5-py3-none-any.whl",
        sha256 = "7310f8d27bc79ced999e760ca304d69f6ba6c6649c0b60fb0e04a4a77cacc134",
        urls = ["https://files.pythonhosted.org/packages/40/76/86f886e750b81a4357b6ed606b2bcf0ce6d6c27ad3c09ebf63ed674fc86e/pytest-6.2.5-py3-none-any.whl"],
    )
    http_file(
        name = "wheel_pkg",
        downloaded_file_path = "wheel-0.37.1-py3-none-any.whl",
        sha256 = "4bdcd7d840138086126cd09254dc6195fb4fc6f01c050a1d7236f2630db1d22a",
        urls = ["https://files.pythonhosted.org/packages/27/d6/003e593296a85fd6ed616ed962795b2f87709c3eee2bca4f6d0fe55c6d00/wheel-0.37.1-py2.py3-none-any.whl"],
    )
    http_file(
        name = "pex_pkg",
        downloaded_file_path = "pex-2.1.164-py2.py3-none-any.whl",
        sha256 = "37d7d4cad605784dbf3494608fb5928c2c5e385d60dcc00bc816c4c70d6f0fae",
        urls = ["https://files.pythonhosted.org/packages/f5/75/df33045e065a49b6b39807343da8e3fa24d3e9665ce3650b9dfe59c3e97b/pex-2.1.164-py2.py3-none-any.whl"],
    )
    http_file(
        name = "requests_pkg",
        downloaded_file_path = "requests-2.29.0-py2.py3-none-any.whl",
        sha256 = "e8f3c9be120d3333921d213eef078af392fba3933ab7ed2d1cba3b56f2568c3b",
        urls = ["https://files.pythonhosted.org/packages/cf/e1/2aa539876d9ed0ddc95882451deb57cfd7aa8dbf0b8dbce68e045549ba56/requests-2.29.0-py3-none-any.whl"],
    )
    http_file(
        name = "setuptools_pkg",
        downloaded_file_path = "setuptools-69.5.1-py3-none-any.whl",
        sha256 = "c636ac361bc47580504644275c9ad802c50415c7522212252c033bd15f301f32",
        urls = ["https://files.pythonhosted.org/packages/f7/29/13965af254e3373bceae8fb9a0e6ea0d0e571171b80d6646932131d6439b/setuptools-69.5.1-py3-none-any.whl"],
    )

pex_deps = module_extension(
    implementation = _pex_deps_impl,
)
