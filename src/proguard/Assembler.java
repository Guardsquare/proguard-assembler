/*
 * ProGuard assembler/disassembler for Java bytecode.
 *
 * Copyright (c) 2019-2020 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package proguard;

import proguard.classfile.*;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.editor.ConstantPoolSorter;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.io.*;
import proguard.preverify.CodePreverifier;
import proguard.util.*;

import java.io.*;

/**
 * Main class for the ProGuard Disassembler and Assembler.
 *
 * @author Joachim Vandersmissen
 */
public class Assembler
{
    private static final byte[] JMOD_HEADER            = new byte[] { 'J', 'M', 1, 0 };
    private static final String JMOD_CLASS_FILE_PREFIX = "classes/";


    public static void main(String[] args) throws IOException
    {
        if (args.length < 2 || args.length > 3)
        {
            System.out.println("ProGuard Assembler: assembles and disassembles Java class files.");
            System.out.println("Usage:");
            System.out.println("  java proguard.Assembler [<classpath>] <input> <output>");
            System.out.println("The input and the output can be .class/.jbc/.jar/.jmod files or directories,");
            System.out.println("where .jbc files contain disassembled Java bytecode.");
            System.out.println("The classpath (with runtime classes and library classes) is only necessary for preverifying assembled code.");
            System.exit(1);
        }

        int index = 0;

        String[] libraryPath    = args.length >= 3 ?
                                  args[index++].split(System.getProperty("path.separator")) : null;
        String   inputFileName  = args[index++];
        String   outputFileName = args[index++];

        ClassPool libraryClassPool = new ClassPool();
        ClassPool programClassPool = new ClassPool();

        ClassPoolFiller libraryClassPoolFiller = new ClassPoolFiller(libraryClassPool);
        ClassPoolFiller programClassPoolFiller = new ClassPoolFiller(programClassPool);

        // Read the libraries.
        if (libraryPath != null)
        {
            for (String libraryFileName : libraryPath)
            {
                System.out.println("Reading library file ["+libraryFileName+"]...");

                // Any libraries go into the library class pool.
                DataEntryReader libraryClassReader =
                    new ClassReader(true, false, false, false, null, libraryClassPoolFiller);

                DirectoryPump libraryPump = new DirectoryPump(new File(libraryFileName));
                libraryPump.pumpDataEntries(reader(libraryClassReader,
                                                   null,
                                                   null));
            }
        }

        // Read the actual input.
        System.out.println("Reading input file ["+inputFileName+"]...");

        DirectoryPump inputPump =
            new DirectoryPump(new File(inputFileName));

        // Any class files go into the library class pool.
        DataEntryReader classReader =
            new ClassReader(false, false, false, false, null, libraryClassPoolFiller);

        // Any jbc files go into the program class pool.
        DataEntryReader jbcReader =
            new JbcReader(programClassPoolFiller);

        inputPump.pumpDataEntries(reader(classReader,
                                         jbcReader,
                                         null));

        // Preverify the program class pool.
        if (libraryPath != null && programClassPool.size() > 0)
        {
            System.out.println("Preverifying assembled class files...");

            programClassPool.classesAccept(new ClassReferenceInitializer(programClassPool, libraryClassPool));
            programClassPool.classesAccept(new ClassSuperHierarchyInitializer(programClassPool, libraryClassPool));
            libraryClassPool.classesAccept(new ClassReferenceInitializer(programClassPool, libraryClassPool));
            libraryClassPool.classesAccept(new ClassSuperHierarchyInitializer(programClassPool, libraryClassPool));

            programClassPool.classesAccept(
                new ClassVersionFilter(VersionConstants.CLASS_VERSION_1_6,
                new AllMethodVisitor(
                new AllAttributeVisitor(
                new CodePreverifier(false)))));
        }

        // Clean up the program classes.
        programClassPool.classesAccept(
            new ConstantPoolSorter());

        // Write out the output.
        System.out.println("Writing output file ["+outputFileName+"]...");

        DataEntryWriter writer = writer(outputFileName);

        // Write out class files as jbc files.
        DataEntryWriter classAsJbcWriter =
            new RenamedDataEntryWriter(new ConcatenatingStringFunction(
                                       new SuffixRemovingStringFunction(".class"),
                                       new ConstantStringFunction(".jbc")),
            new JbcDataEntryWriter(libraryClassPool, writer));

        // Write out jbc files as class files.
        DataEntryWriter jbcAsClassWriter =
            new RenamedDataEntryWriter(new ConcatenatingStringFunction(
                                       new SuffixRemovingStringFunction(".jbc"),
                                       new ConstantStringFunction(".class")),
            new ClassDataEntryWriter(programClassPool, writer));

        // Read the input again, writing out disassembled/assembled/preverified
        // files.
        inputPump.pumpDataEntries(reader(new IdleRewriter(classAsJbcWriter),
                                         new IdleRewriter(jbcAsClassWriter),
                                         new DataEntryCopier(writer)));

        writer.close();
    }


    /**
     * Creates a data entry reader that unpacks archives if necessary and sends
     * data entries to given relevant readers.
     */
    private static DataEntryReader reader(DataEntryReader classReader,
                                          DataEntryReader jbcReader,
                                          DataEntryReader resourceReader)
    {
        // Handle class files and resource files.
        DataEntryReader reader =
             new FilteredDataEntryReader(new DataEntryNameFilter(new ExtensionMatcher(".class")),
                 classReader,
                 resourceReader);

        // Handle jbc files.
        reader =
            new FilteredDataEntryReader(new DataEntryNameFilter(new ExtensionMatcher(".jbc")),
                 jbcReader,
                 reader);

        // Strip the "classes/" prefix from class/jbc files inside jmod files.
        DataEntryReader prefixStrippingReader =
            new FilteredDataEntryReader(new DataEntryNameFilter(
                                        new OrMatcher(
                                        new ExtensionMatcher(".class"),
                                        new ExtensionMatcher(".jbc"))),
                new PrefixStrippingDataEntryReader(JMOD_CLASS_FILE_PREFIX, reader),
                reader);

        // Unpack jmod files.
        reader =
            new FilteredDataEntryReader(new DataEntryNameFilter(new ExtensionMatcher(".jmod")),
                new JarReader(true, prefixStrippingReader),
                reader);

        // Unpack jar files.
        reader =
            new FilteredDataEntryReader(new DataEntryNameFilter(new ExtensionMatcher(".jar")),
                new JarReader(reader),
                reader);

        return reader;
    }


    /**
     * Creates a data entry writer for the given file name that creates archives
     * if necessary.
     */
    private static DataEntryWriter writer(String outputFileName)
    {
        boolean outIsJar   = outputFileName.endsWith(".jar");
        boolean outIsJmod  = outputFileName.endsWith(".jmod");
        boolean outIsJbc   = outputFileName.endsWith(".jbc");
        boolean outIsClass = outputFileName.endsWith(".class");

        DataEntryWriter writer =
            outIsJar   ||
            outIsJmod  ||
            outIsJbc   ||
            outIsClass ?
                new FixedFileWriter(new File(outputFileName)) :
                new DirectoryWriter(new File(outputFileName));

        // Pack jar files.
        if (outIsJar)
        {
            writer =
                new JarWriter(
                new ZipWriter(writer));
        }

        // Pack jmod files.
        else if (outIsJmod)
        {
            writer =
                new JarWriter(
                new ZipWriter(null, 1, 0, JMOD_HEADER,
                    writer));

            // Add the "classes/" prefix to class/jbc files inside jmod files.
            writer =
                new FilteredDataEntryWriter(new DataEntryNameFilter(
                                            new OrMatcher(
                                            new ExtensionMatcher(".class"),
                                            new ExtensionMatcher(".jbc"))),
                    new PrefixAddingDataEntryWriter(JMOD_CLASS_FILE_PREFIX, writer),
                    writer);
        }

        return writer;
    }
}
