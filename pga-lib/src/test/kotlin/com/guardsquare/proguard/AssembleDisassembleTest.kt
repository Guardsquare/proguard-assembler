package com.guardsquare.proguard

import io.kotest.core.spec.style.FreeSpec
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class AssembleDisassembleTest : FreeSpec({
    "Test TYPE_METHOD_TYPE constant" {
        val classBPools = ClassPoolBuilder.fromSource(
            JavaSource(
                "Main.java",
                """
                import java.util.function.Consumer;                    
                
                public class Main {
                    public static void test(Consumer<String> lambda) {
                        lambda.accept("Hello world!");
                    }
                    
                    public static void main(String[] args) {
                        test(System.out::println);
                    }
                }
            """.trimIndent())
        )

        buildProgramClass(disassembleClass(classBPools.programClassPool.getClass("Main")))
    }
})
