package com.guardsquare.proguard

import com.guardsquare.proguard.assembler.ClassParser
import com.guardsquare.proguard.assembler.Parser
import com.guardsquare.proguard.disassembler.ClassPrinter
import com.guardsquare.proguard.disassembler.Printer
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.AccessConstants
import proguard.classfile.AccessConstants.ABSTRACT
import proguard.classfile.AccessConstants.ANNOTATION
import proguard.classfile.AccessConstants.FINAL
import proguard.classfile.AccessConstants.INTERFACE
import proguard.classfile.AccessConstants.PRIVATE
import proguard.classfile.AccessConstants.PROTECTED
import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.Clazz
import proguard.classfile.ProgramClass
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.InnerClassesAttribute
import proguard.classfile.attribute.visitor.AttributeVisitor
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter

/**
 * Tests for parsing inner class access flags in the InnerClasses attribute.
 */
class InnerClassAccessFlagsTest : FreeSpec({

    "public inner class" {
        val jbc = """
            public class Outer extends java.lang.Object [
                InnerClasses {
                    public class Outer${'$'}Inner as Inner in Outer;
                }
            ] {
            }
        """

        val programClass = jbc.parseJbcToProgramClass()
        val flags = programClass.getInnerClassFlags(0)

        flags shouldBe PUBLIC
    }

    "public static inner class" {
        val jbc = """
            public class Outer extends java.lang.Object [
                InnerClasses {
                    public static class Outer${'$'}StaticInner as StaticInner in Outer;
                }
            ] {
            }
        """

        val programClass = jbc.parseJbcToProgramClass()
        val flags = programClass.getInnerClassFlags(0)

        flags shouldBe (PUBLIC or AccessConstants.STATIC)
    }

    "private inner class" {
        val jbc = """
            public class Outer extends java.lang.Object [
                InnerClasses {
                    private class Outer${'$'}PrivateInner as PrivateInner in Outer;
                }
            ] {
            }
        """

        val programClass = jbc.parseJbcToProgramClass()
        val flags = programClass.getInnerClassFlags(0)

        flags shouldBe PRIVATE
    }

    "public inner interface" {
        val jbc = """
            public class Outer extends java.lang.Object [
                InnerClasses {
                    public interface Outer${'$'}InnerInterface as InnerInterface in Outer;
                }
            ] {
            }
        """

        val programClass = jbc.parseJbcToProgramClass()
        val flags = programClass.getInnerClassFlags(0)

        flags shouldBe (PUBLIC or INTERFACE or ABSTRACT)
    }

    "public inner enum" {
        val jbc = """
            public class Outer extends java.lang.Object [
                InnerClasses {
                    public enum Outer${'$'}InnerEnum as InnerEnum in Outer;
                }
            ] {
            }
        """

        val programClass = jbc.parseJbcToProgramClass()
        val flags = programClass.getInnerClassFlags(0)

        flags shouldBe (PUBLIC or AccessConstants.ENUM)
    }

    "public inner annotation" {
        val jbc = """
            public class Outer extends java.lang.Object [
                InnerClasses {
                    public @interface Outer${'$'}InnerAnnotation as InnerAnnotation in Outer;
                }
            ] {
            }
        """

        val programClass = jbc.parseJbcToProgramClass()
        val flags = programClass.getInnerClassFlags(0)

        flags shouldBe (PUBLIC or ANNOTATION or INTERFACE or ABSTRACT)
    }

    "round-trip preserves inner class flags" {
        val originalJbc = """
            public class Outer extends java.lang.Object [
                InnerClasses {
                    public class Outer${'$'}Inner as Inner in Outer;
                    public static class Outer${'$'}StaticInner as StaticInner in Outer;
                    private class Outer${'$'}PrivateInner as PrivateInner in Outer;
                }
            ] {
            }
        """.trimIndent()

        val programClass = originalJbc.parseJbcToProgramClass()
        val originalFlags = programClass.getAllInnerClassFlags()

        val stringWriter = StringWriter()
        val printer = Printer(PrintWriter(stringWriter))
        val classPrinter = ClassPrinter(printer)
        programClass.accept(classPrinter)

        val reparsedClass = stringWriter.toString().parseJbcToProgramClass()
        val reparsedFlags = reparsedClass.getAllInnerClassFlags()

        reparsedFlags shouldBe originalFlags
    }

    "multiple inner classes with different access flags" {
        val jbc = """
            public class Outer extends java.lang.Object [
                InnerClasses {
                    public class Outer${'$'}PublicInner as PublicInner in Outer;
                    protected class Outer${'$'}ProtectedInner as ProtectedInner in Outer;
                    class Outer${'$'}PackageInner as PackageInner in Outer;
                    private final class Outer${'$'}PrivateFinalInner as PrivateFinalInner in Outer;
                    public abstract class Outer${'$'}AbstractInner as AbstractInner in Outer;
                }
            ] {
            }
        """

        val programClass = jbc.parseJbcToProgramClass()

        programClass.getInnerClassFlags(0) shouldBe PUBLIC
        programClass.getInnerClassFlags(1) shouldBe PROTECTED
        programClass.getInnerClassFlags(2) shouldBe 0
        programClass.getInnerClassFlags(3) shouldBe (PRIVATE or FINAL)
        programClass.getInnerClassFlags(4) shouldBe (PUBLIC or ABSTRACT)
    }
})

private fun String.parseJbcToProgramClass(): ProgramClass {
    val programClass = ProgramClass()
    programClass.accept(
        ClassParser(
            Parser(
                StringReader(this),
            ),
        ),
    )
    return programClass
}

private fun ProgramClass.getInnerClassFlags(index: Int): Int {
    var flags = -1
    attributesAccept(object : AttributeVisitor {
        override fun visitAnyAttribute(clazz: Clazz, attribute: Attribute) {}
        override fun visitInnerClassesAttribute(clazz: Clazz, attr: InnerClassesAttribute) {
            flags = attr.classes[index].u2innerClassAccessFlags
        }
    })
    return flags
}

private fun ProgramClass.getAllInnerClassFlags(): List<Int> {
    val flagsList = mutableListOf<Int>()
    attributesAccept(object : AttributeVisitor {
        override fun visitAnyAttribute(clazz: Clazz, attribute: Attribute) {}
        override fun visitInnerClassesAttribute(clazz: Clazz, attr: InnerClassesAttribute) {
            for (i in 0 until attr.u2classesCount) {
                flagsList.add(attr.classes[i].u2innerClassAccessFlags)
            }
        }
    })
    return flagsList
}
