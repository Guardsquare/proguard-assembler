<h4 align="center">Assembler and Disassembler for Java class files</h4>

<!-- Badges -->
<p align="center">
  <!-- CI -->
  <a href="https://github.com/Guardsquare/proguard-assembler/actions?query=workflow%3A%22Continuous+Integration%22">
    <img src="https://github.com/Guardsquare/proguard-assembler/workflows/Continuous%20Integration/badge.svg?branch=github-workflow">
  </a>

  <!-- Github version -->
  <a href="releases">
    <img src="https://img.shields.io/github/v/release/guardsquare/proguard-assembler">
  </a>

  <!-- Maven -->
  <a href="https://search.maven.org/search?q=g:com.guardsquare">
    <img src="https://img.shields.io/maven-central/v/com.guardsquare/proguard-parent">
  </a>

  <!-- License -->
  <a href="LICENSE">
    <img src="https://img.shields.io/github/license/guardsquare/proguard-assembler">
  </a>

  <!-- Twitter -->
  <a href="https://twitter.com/Guardsquare">
    <img src="https://img.shields.io/twitter/follow/guardsquare?style=social">
  </a>
</p>

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

- [Pre-built artifacts](https://bintray.com/guardsquare/proguard) at JCenter
- [Pre-built artifacts](https://search.maven.org/search?q=g:net.sf.proguard) at Maven Central
- A [Git repository of the source code](https://github.com/Guardsquare/proguard-assembler) at Github

## Building

If you've downloaded the source code, you can build it in a number of ways:

- build.gradle : a Gradle build file for all platforms

        gradle clean assemble

- pom.xml: a Maven POM for all platforms

        mvn clean package

- build.sh: a simple and fast shell script for GNU/Linux

        ./build.sh

Once built, you can [run the assembler and disassembler](index.md) with the
scripts in the `bin` directory.

## Contributing

The **ProGuard Assembler and Disassembler** are build on the
[ProGuard Core](https://github.com/Guardsquare/proguard-core) library.

Contributions, issues and feature requests are welcome in both projects.
Feel free to check the [issues](issues) page and the [contributing
guide](CONTRIBUTING.md) if you would like to contribute.

## License

The **ProGuard Assembler and Disassembler** are distributed under the terms of
the [Apache License Version 2.0](LICENSE).

Enjoy!

Copyright (c) 2002-2020 [Guardsquare NV](https://www.guardsquare.com/)
