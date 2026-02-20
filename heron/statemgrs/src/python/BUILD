package(default_visibility = ["//visibility:public"])

pex_library(
    name = "statemgr-py",
    srcs = glob(["**/*.py"]),
    reqs = [
        "PyYAML==6.0.1",
        "kazoo==2.10.0",
        "zope.interface==7.2",
    ],
    deps = [
        "//heron/proto:proto-py",
    ],
)
