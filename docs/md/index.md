The **ProGuard Assembler and Disassembler** can assemble and disassemble
Java class files.

The disassembler takes `class` files and converts them to readable `jbc`
(Java ByteCode) files, following the [ProGuard Assembly Language
specification](specification.md).

The assembler does the opposite; it takes readable `jbc` files and
converts them to `class` files.

# Usage

The program is distributed as a single executable jar file that serves both as
an assembler and as a disassembler. When the program detects a `jbc` file as
input, it assembles it. Vice versa, when it detects a `class` file, it
disassembles it. It can handle a combination of both; a jar file containing
both `jbc` files and `class` files will have its `jbc` files assembled in the
output and its `class` files disassembled in the output.

    bin/assembler [<classpath>] <input> <output>
    
The _input_ and the _output_ can be .class/.jbc/.jar/.jmod files or
directories, where  _.jbc files_ contain disassembled Java bytecode.

The _classpath_ (with runtime classes and library classes) is only necessary
for preverifying assembled code.

# Example

As a small test, you can let the program disassemble itself:

    bin/assembler lib/assembler.jar disassembled

The program will create a `disassembled` directory with readable files
disassembled from the input jar. You can then let the program assemble the
assembly files again:

    bin/assembler /usr/lib/jvm/java-12-oracle/jmods/java.base.jmod disassembled assembled.jar

The program will now create an `assembled.jar` file with reassembled class
files. It will use the `jmod` file as a run-time library to properly preverify
the code.

# Preverification

The ProGuard Assembly Language contains some syntactic sugar, notably for the
`StackMap` and `StackMapTable` attributes. As of Java version 7, each `Code`
attribute must contain a `StackMapTable` attribute. Since it would be very
cumbersome to write these attributes by hand, the assembler automatically
preverifies every method using the ProGuard preverifier, and creates the
`StackMapTable` using the preverification info. As a consequence, the
assembler needs to know the location of any library jars when assembling
`class` files.
