If you've downloaded the source code of the the **ProGuard Assembler and
Disassembler**, you can build it in a number of ways:

- build.gradle : a Gradle build file for all platforms

        gradle clean assemble

- pom.xml: a Maven POM for all platforms

        mvn clean package

- build.sh: a simple and fast shell script for GNU/Linux

        ./build.sh

Once built, you can [run the assembler and disassembler](index.md) with the
scripts in the `bin` directory.
