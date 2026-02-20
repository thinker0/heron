package(default_visibility = ["//visibility:public"])

pex_library(
    name = "cli-py",
    srcs = glob(
        ["**/*.py"],
    ),
    reqs = [
        "PyYAML==6.0.1",
        "requests==2.29.0",
        "netifaces==0.10.6",
    ],
    deps = [
        "//heron/common/src/python:common-py",
        "//heron/proto:proto-py",
        "//heron/tools/common/src/python:common-py",
    ],
)

pex_binary(
    name = "heron",
    platforms = ["macosx-11.0-arm64-cp-39-cp39", "macosx-10.9-x86_64-cp-39-cp39"],
    srcs = [
        "main.py",
    ],
    deps = [":cli-py"],
)
