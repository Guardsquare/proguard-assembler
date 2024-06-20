package com.guardsquare.proguard

import com.guardsquare.proguard.assembler.ClassParser
import com.guardsquare.proguard.assembler.ParseException
import com.guardsquare.proguard.assembler.Parser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import proguard.classfile.ProgramClass
import java.io.StringReader

/**
 * These tests check that various invalid code snippets are causing in an exceptions thrown from the
 * assembler.
 */
class AssemblerErrorsTest : FreeSpec({

    "Variable types on the stack do not match instruction using them" {
        shouldThrow<ParseException> {
            buildProgramClass(
                """
                        public class AssemblerErrorsTester extends java.lang.Object {
                            public long test() {
                                iconst_3
                                iconst_1
                                lsub
                                lreturn
                            }
                        }
                    """,
            )
        }
    }

    "Swap needs two elements on the stack" {
        shouldThrow<ParseException> {
            buildProgramClass(
                """
                    public class AssemblerErrorsTester extends java.lang.Object {
                        public void test() {
                            iconst_5
                            swap
                            return
                        }
                    }
                """,
            )
        }
    }

    "Duplicate top value of an empty stack" {
        shouldThrow<ParseException> {
            buildProgramClass(
                """
                    public class AssemblerErrorsTester extends java.lang.Object {
                        public void test() {
                            dup
                            return
                        }
                    }
                """,
            )
        }
    }

    "Illegal bytecode instruction" {
        // `apples` is not a valid bytecode instructions and this should be clearly indicated by the PGA
        // See: https://github.com/Guardsquare/proguard-assembler/issues/8
        shouldThrow<ParseException> {
            buildProgramClass(
                """
                    public class AssemblerErrorsTester extends java.lang.Object {
                        public java.lang.Object test() {
                            apples
                            aload_0
                            areturn
                        }
                    }
                """,
            )
        }
    }

    "`goto` to an invalid position" {
        shouldThrow<ParseException> {
            buildProgramClass(
                """
                    public class AssemblerErrorsTester extends java.lang.Object {
                        public void test() {
                            goto jafar
                            return
                        }
                    }
                """,
            )
        }
    }

    "bipush with invalid operand label" {
        // `bipush` expects a byte value but 300 exceeds the maximum byte value (>255)
        // If you want to fix this, you need to be in visitCodeAttribute:226 in the instructionParser (of the assembler)
        // Additionally you change the error to hande null for the clazz and method. (Just write something like "non-existing" or "null")
        shouldThrow<ParseException> {
            buildProgramClass(
                """
                    public class AssemblerErrorsTester extends java.lang.Object {
                        public java.lang.Object test() {
                            bipush 300
                            aload_0
                            areturn
                        }
                    }
                """,
            )
        }
    }
})
