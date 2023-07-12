package com.guardsquare.proguard

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FreeSpec
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder

/**
 * These tests check that various invalid code snippets are causing in an exceptions thrown from the
 * assembler.
 */
class AssemblerErrorsTest : FreeSpec({

    "Throws from assembler but should be a formatted message" - {
        "Variable types do not match instruction but negative stack size exception is thrown first" {
            // The same test has been built using the `CodeBuilder` in the previous section
            // The assembler uses the same methods and throws the same error, but it is wrapped in a `IOException`
            shouldThrowAny {
                fastBuild(
                    """
                        public long test() {
                            iconst_3
                            iconst_1
                            lsub
                            lreturn
                        }
                    """.trimIndent(),
                )
            }
        }

        "Swap needs two elements on the stack" {
            // The same test has been built using the `CodeBuilder` in the previous section
            // The assembler uses the same methods and throws the same error, but it is wrapped in a `IOException`
            shouldThrowAny {
                fastBuild(
                    """
                        public void test() {
                            iconst_5
                            swap
                            return
                        }
                    """.trimIndent(),
                )
            }
        }

        "Duplicate top value of an empty stack" {
            // The same test has been built using the `CodeBuilder` in the previous section
            // The assembler uses the same methods and throws the same error, but it is wrapped in a `IOException`
            shouldThrowAny {
                fastBuild(
                    """
                        public void test() {
                            dup
                            return
                        }
                    """.trimIndent(),
                )
            }
        }

        "Illegal bytecode instruction" {
            // `apples` is not a valid bytecode instructions and this should be clearly indicated by the PGA
            // This is an issue for the assembler and not for the partial evaluator
            // See: https://github.com/Guardsquare/proguard-assembler/issues/8
            shouldThrowAny {
                fastBuild(
                    """
                        public java.lang.Object test() {
                            apples
                            aload_0
                            areturn
                        }
                    """.trimIndent(),
                )
            }
        }

        "`goto` to an invalid position" {
            // Jumping to an invalid label is caught by the PGA
            // The `PartialEvaluator` should do the same, see this test built with the ClassBuilder
            // in "Throws from partial evaluator but should be formatted"
            shouldThrowAny {
                fastBuild(
                    """
                        public void test() {
                            goto jafar
                            return
                        }
                    """.trimIndent(),
                )
            }
        }

        "bipush with invalid operand label" {
            // `bipush` expects a byte value but 300 exceeds the maximum byte value (>255)

            // If you want to fix this, you need to be in visitCodeAttribute:226 in the instructionParser (of the assambler)
            // Additionally you change the error to hande null for the clazz and method. (Just write something like "non-existing" or "null")
            shouldThrowAny {
                fastBuild(
                    """
                        public java.lang.Object test() {
                            bipush 300
                            aload_0
                            areturn
                        }
                    """.trimIndent(),
                )
            }
        }
    }
})

val fastBuild = { impl: String ->
    ClassPoolBuilder.fromSource(
        AssemblerSource(
            "PartialEvaluatorDummy.jbc",
            """
                public class PartialEvaluatorDummy extends java.lang.Object {
                    $impl
                }
            """.trimIndent(),
        ),
    )
}
