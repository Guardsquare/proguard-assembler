#!/bin/bash
#
# Support functions for building ProGuard.

SRC=src
OUT=out
LIB=lib

TARGET=1.8

set -o pipefail

function download {
  if [ ! -f "$2" ]; then
    echo "Downloading $2..."
    mkdir -p $(dirname "$2") && \
    if type wget > /dev/null 2>&1; then
      wget -O "$2" "$1"
    else
      curl -L -o "$2" "$1"
    fi
  fi
}

function compile {
  echo "Compiling $(basename $PWD) ($1)..."
  mkdir -p "$OUT" && \

  # Compile Java source files.
  find $SRC -name '_*.java' -o -path "$SRC/${1//.//}.java" \
  | xargs --no-run-if-empty \
    javac -nowarn -Xlint:none \
    -source $TARGET -target $TARGET \
    -sourcepath "$SRC" -d "$OUT" \
    ${2:+-classpath "$2"} 2>&1 \
  | sed -e 's|^|  |' || return 1

  # Compile Kotlin source files.
  #find $SRC -path "$SRC/${1//.//}.kotlin" \
  #| xargs --no-run-if-empty \
  #  kotlinc -nowarn -jvm-target $TARGET \
  #  -d "$OUT" \
  #  ${2:+-classpath "$2"} 2>&1 \
  #| sed -e 's|^|  |' || return 1

  # Compile Groovy source files.
  find $SRC -path "$SRC/${1//.//}.groovy" \
  | xargs --no-run-if-empty \
    groovyc \
    -sourcepath "$SRC" -d "$OUT" \
    ${2:+-classpath "$2"} 2>&1 \
  | sed -e 's|^|  |' || return 1

  # Copy resource files.
  (cd "$SRC" && \
   find proguard \
     \( -name \*.properties -o -name \*.png -o -name \*.gif -o -name \*.pro \) \
     -exec cp --parents {} "../$OUT" \; )
}

function createjar {
  echo "Creating $1..."
  mkdir -p $(dirname "$1") && \
  if [ -f "$SRC/META-INF/MANIFEST.MF" ]; then
    jar -cfm "$1" "$SRC/META-INF/MANIFEST.MF" -C "$OUT" proguard
  else
    jar -cf "$1" -C "$OUT" proguard
  fi
}

function updatejar {
  echo "Updating $1..."
  if [ -f "$SRC/META-INF/MANIFEST.MF" ]; then
    jar -ufm "$1" "$SRC/META-INF/MANIFEST.MF" -C "$OUT" proguard
  else
    jar -uf "$1" -C "$OUT" proguard
  fi
}
