If you've downloaded the source code of the the **ProGuard Assembler and
Disassembler**, you can build it yourself with Gradle:

- Build the artifacts:

        ./gradlew assemble

- Build the artifacts when you also have a personal copy of the [ProGuard
  Core](https://github.com/Guardsquare/proguard-core) library:

        ./gradlew --include-build <path_to_proguard_core> assemble

- Publish the artifacts to your local Maven cache (something like `~/.m2/`):

        ./gradlew publishToMavenLocal

- Build tar and zip archives with the binaries and documentation:

        ./gradlew distTar distZip

Once built, you can [run the assembler and disassembler](index.md) with the
script `bin/assembler.sh` or `bin/assembler.bat`.
