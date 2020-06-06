#!/bin/sh
#
# Start-up script for the ProGuard Assembler and Disassembler.
#
# Note: when passing file names containing spaces to this script,
#       you'll have to add escaped quotes around them, e.g.
#       "\"/My Directory/My File.txt\""

# Account for possibly missing/basic readlink.
# POSIX conformant (dash/ksh/zsh/bash).
ASSEMBLER=`readlink -f "$0" 2>/dev/null`
if test "$ASSEMBLER" = ''
then
  ASSEMBLER=`readlink "$0" 2>/dev/null`
  if test "$ASSEMBLER" = ''
  then
    ASSEMBLER="$0"
  fi
fi

ASSEMBLER_HOME=`dirname "$ASSEMBLER"`/..

# Find the compiled jar.
if   [ -f "$ASSEMBLER_HOME/lib/assembler.jar" ]
then
  ASSEMBLER_JAR=$ASSEMBLER_HOME/lib/assembler.jar
else
  echo 'Please build the project first'
  exit 1
fi

exec java -jar "$ASSEMBLER_JAR" "$@"
