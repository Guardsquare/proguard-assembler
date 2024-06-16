# Assembler and Disassembler for Java class files

The **ProGuard Assembler and Disassembler** can assemble and disassemble
Java class files.

The disassembler takes `class` files and converts them to readable `jbc`
(Java ByteCode) files, following the [ProGuard Assembly Language
specification](docs/md/specification.md).

The assembler does the opposite; it takes readable `jbc` files and
converts them to `class` files.

## Usage

The program is distributed as a single executable jar file that serves both as
an assembler and as a disassembler. When the program detects a `jbc` file as
input, it assembles it. Vice versa, when it detects a `class` file, it
disassembles it. It can handle any combination of both; a jar file containing
both `jbc` files and `class` files will have its `jbc` files assembled in the
output and its `class` files disassembled in the output.

    bin/assembler [<classpath>] <input> <output>

The _input_ and the _output_ can be .class/.jbc/.jar/.jmod files or
directories, where  _.jbc files_ contain disassembled Java bytecode.

The _classpath_ (with runtime classes and library classes) is only necessary
for preverifying assembled code.

## Example

As a small test, you can let the program disassemble itself:

    bin/assembler lib/assembler.jar disassembled

The program will create a `disassembled` directory with readable files
disassembled from the input jar. You can then let the program assemble the
assembly files again:

    bin/assembler /usr/lib/jvm/java-12-oracle/jmods/java.base.jmod disassembled assembled.jar

The program will now create an `assembled.jar` file with reassembled class
files. It will use the `jmod` file as a run-time library to properly preverify
the code.

## Preverification

The ProGuard Assembly Language contains some syntactic sugar, notably for the
`StackMap` and `StackMapTable` attributes. As of Java version 7, each `Code`
attribute must contain a `StackMapTable` attribute. Since it would be very
cumbersome to write these attributes by hand, the assembler automatically
preverifies every method using the ProGuard preverifier, and creates the
`StackMapTable` using the preverification info. As a consequence, the
assembler needs to know the location of any library jars when assembling
`class` files.

## Downloads

The code is written in Java, so it requires a Java Runtime Environment
(JRE 1.8 or higher).

You can download the assembler and disassembler in various forms:

- [Pre-built artifacts](https://search.maven.org/search?q=g:com.guardsquare) at Maven Central
- A [Git repository of the source code](https://github.com/Guardsquare/proguard-assembler) at Github

## Building

If you've downloaded the source code, you can build it with Gradle:

    ./gradlew clean build

Once built, you can [run the assembler and disassembler](index.md) with the
script `bin/assembler.sh` or `bin/assembler.bat`.

## Using as a library

You can use the ProGuard assembler or disassembler from your own code by
adding a dependency on the ProGuard assembler library. For a more complete
example, see the CLI project (`pga-cli`).

```kotlin
import com.guardsquare.proguard.assembler.io.JbcReader
import proguard.classfile.ClassPool
import proguard.classfile.VersionConstants.CLASS_VERSION_1_6
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.util.ClassReferenceInitializer
import proguard.classfile.util.ClassSuperHierarchyInitializer
import proguard.classfile.visitor.AllMethodVisitor
import proguard.classfile.visitor.ClassPoolFiller
import proguard.classfile.visitor.ClassVersionFilter
import proguard.io.ClassDataEntryWriter
import proguard.io.DataEntryNameFilter
import proguard.io.DataEntryReader
import proguard.io.DataEntryWriter
import proguard.io.FileSource
import proguard.io.FilteredDataEntryReader
import proguard.io.FixedFileWriter
import proguard.io.IdleRewriter
import proguard.io.RenamedDataEntryWriter
import proguard.io.util.IOUtil
import proguard.preverify.CodePreverifier
import proguard.util.ConcatenatingStringFunction
import proguard.util.ConstantStringFunction
import proguard.util.ExtensionMatcher
import proguard.util.SuffixRemovingStringFunction
import java.io.File


fun main(args: Array<String>) {
    // Read a JBC file and write a class file
 
    val inputSource = FileSource(File(args[0]))

    val programClassPool = ClassPool()
    val libraryClassPool = if (args.size > 2) IOUtil.read(args[2], true) else ClassPool()

    val jbcReader: DataEntryReader = JbcReader(
        ClassPoolFiller(programClassPool)
    )

    val reader: DataEntryReader = FilteredDataEntryReader(
        DataEntryNameFilter(ExtensionMatcher(".jbc")), jbcReader
    )

    inputSource.pumpDataEntries(reader)

    // Preverify before writing the class
    preverify(programClassPool, libraryClassPool)

    val outputFile = File(args[1])
    val writer = FixedFileWriter(outputFile)

    val jbcAsClassWriter: DataEntryWriter = RenamedDataEntryWriter(
        ConcatenatingStringFunction(
            SuffixRemovingStringFunction(".jbc"),
            ConstantStringFunction(".class")
        ),
        ClassDataEntryWriter(programClassPool, writer)
    )

    // Write the class file by reading in the jbc file and 
    // replacing it using the `IdleRewriter`.
    inputSource.pumpDataEntries(
        FilteredDataEntryReader(
            DataEntryNameFilter(ExtensionMatcher(".jbc")),
            IdleRewriter(jbcAsClassWriter)
        )
    )

    writer.close()
}

fun preverify(libraryClassPool: ClassPool, programClassPool: ClassPool) {
    programClassPool.classesAccept(ClassReferenceInitializer(programClassPool, libraryClassPool))
    programClassPool.classesAccept(ClassSuperHierarchyInitializer(programClassPool, libraryClassPool))
    libraryClassPool.classesAccept(ClassReferenceInitializer(programClassPool, libraryClassPool))
    libraryClassPool.classesAccept(ClassSuperHierarchyInitializer(programClassPool, libraryClassPool))
    programClassPool.classesAccept(
        ClassVersionFilter(
            CLASS_VERSION_1_6,
            AllMethodVisitor(
                AllAttributeVisitor(
                    CodePreverifier(false)
                )
            )
        )
    )
}
```


## Contributing

The **ProGuard Assembler and Disassembler** are build on the
[ProGuardCORE](https://github.com/Guardsquare/proguard-core) library.

Contributions, issues and feature requests are welcome in both projects.
Feel free to check the [issues](issues) page and the [contributing
guide](CONTRIBUTING.md) if you would like to contribute.

## License

The **ProGuard Assembler and Disassembler** are distributed under the terms of
the [Apache License Version 2.0](LICENSE).

Enjoy!

Copyright (c) 2002-2022 [Guardsquare NV](https://www.guardsquare.com/)
