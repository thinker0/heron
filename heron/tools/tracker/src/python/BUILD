package(default_visibility = ["//visibility:public"])

pex_library(
    name = "tracker-py",
    srcs = glob(
        ["**/*.py"],
        exclude = ["main.py"],
    ),
    reqs = [
        "click==8.1.8",
        "fastapi==0.124.4",
        "httpx==0.28.1",
        "javaobj-py3==0.4.4",
        "networkx==3.1",
        "protobuf==5.29.5",
        "uvicorn==0.33.0",
    ],
    deps = [
        "//heron/common/src/python:common-py",
        "//heron/proto:proto-py",
        "//heron/statemgrs/src/python:statemgr-py",
        "//heron/tools/common/src/python:common-py",
        "//heronpy/api:heron-python-py",
    ],
)

pex_binary(
    name = "heron-tracker",
    srcs = ["main.py"],
    deps = [
        ":tracker-py",
    ],
)
