package com.guardsquare.proguard

import com.guardsquare.proguard.assembler.ClassParser
import com.guardsquare.proguard.assembler.Parser
import com.guardsquare.proguard.disassembler.ClassPrinter
import com.guardsquare.proguard.disassembler.Printer
import proguard.classfile.Clazz
import proguard.classfile.ProgramClass
import java.io.StringReader
import java.io.StringWriter

/**
 * Helper function to build and parse a java bytecode class from a string
 */
fun buildProgramClass(jbc: String) {
    val programClass = ProgramClass()
    programClass.accept(
        ClassParser(
            Parser(
                StringReader(jbc),
            ),
        ),
    )
}

/**
 * Helper function to disassemble a [Clazz] object.
 */
fun disassembleClass(clazz: Clazz): String {
    val writer = StringWriter()
        clazz.accept(
            ClassPrinter(
            Printer(writer)
        )
    )

    return writer.toString();
}
