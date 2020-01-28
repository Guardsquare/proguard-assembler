#!/bin/bash
#
# GNU/Linux build script for the ProGuard Assembler and Disassembler.

cd $(dirname "$0")

source functions.sh

ASSEMBLER_JAR=lib/assembler.jar

MAIN_CLASS=proguard.Assembler

PROGUARD_CORE_VERSION=7.0.0
PROGUARD_CORE_URL=https://jcenter.bintray.com/net/sf/proguard/proguard-core/${PROGUARD_CORE_VERSION}/proguard-core-${PROGUARD_CORE_VERSION}.jar
PROGUARD_CORE_JAR=$LIB/proguard-core-${PROGUARD_CORE_VERSION}.jar

KOTLIN_VERSION=1.3.31
KOTLIN_STDLIB_URL=https://jcenter.bintray.com/org/jetbrains/kotlin/kotlin-stdlib/${KOTLIN_VERSION}/kotlin-stdlib-${KOTLIN_VERSION}.jar
KOTLIN_STDLIB_JAR=$LIB/kotlin-stdlib-${KOTLIN_VERSION}.jar

KOTLIN_STDLIB_COMMON_URL=https://jcenter.bintray.com/org/jetbrains/kotlin/kotlin-stdlib-common/${KOTLIN_VERSION}/kotlin-stdlib-common-${KOTLIN_VERSION}.jar
KOTLIN_STDLIB_COMMON_JAR=$LIB/kotlin-stdlib-common-${KOTLIN_VERSION}.jar

KOTLINX_METADATA_VERSION=0.1.0
KOTLINX_METADATA_JVM_URL=https://jcenter.bintray.com/org/jetbrains/kotlinx/kotlinx-metadata-jvm/${KOTLINX_METADATA_VERSION}/kotlinx-metadata-jvm-${KOTLINX_METADATA_VERSION}.jar
KOTLINX_METADATA_JVM_JAR=$LIB/kotlinx-metadata-jvm-${KOTLINX_METADATA_VERSION}.jar

download  "$PROGUARD_CORE_URL"        "$PROGUARD_CORE_JAR"        && \
download  "$KOTLIN_STDLIB_URL"        "$KOTLIN_STDLIB_JAR"        && \
download  "$KOTLIN_STDLIB_COMMON_URL" "$KOTLIN_STDLIB_COMMON_JAR" && \
download  "$KOTLINX_METADATA_JVM_URL" "$KOTLINX_METADATA_JVM_JAR" && \

# Compile and package.
compile   $MAIN_CLASS "$PROGUARD_CORE_JAR" && \
createjar "$ASSEMBLER_JAR" || exit 1
