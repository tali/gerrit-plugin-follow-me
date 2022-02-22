load("@rules_java//java:defs.bzl", "java_library")
load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/bzl:plugin.bzl", "PLUGIN_DEPS", "PLUGIN_TEST_DEPS", "gerrit_plugin")


gerrit_plugin(
    name = "follow-me",
    srcs = glob(["server/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: follow-me",
        "Gerrit-Module: com.googlesource.gerrit.plugins.followme.PluginModule",
        "Implementation-Title: Post-Commit review by following a branch",
    ],
    resource_jars = ["//plugins/follow-me/ui:follow-me"],
    resources = glob(["Documentation/*.md"]),
)

junit_tests(
    name = "follow-me_tests",
    srcs = glob(["test-server/**/*.java"]),
    tags = ["follow-me"],
    deps = [
        ":follow-me__plugin_test_deps",
    ],
)

java_library(
    name = "follow-me__plugin_test_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":follow-me__plugin",
        "@commons-io//jar",
        "@mockito//jar",
    ],
)
