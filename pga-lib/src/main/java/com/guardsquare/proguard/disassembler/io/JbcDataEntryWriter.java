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
package com.guardsquare.proguard.disassembler.io;

import com.guardsquare.proguard.assembler.AssemblyConstants;
import com.guardsquare.proguard.disassembler.ClassPrinter;
import com.guardsquare.proguard.disassembler.Printer;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.io.DataEntry;
import proguard.io.DataEntryWriter;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * This DataEntryWriter finds received class entries in the given class pool
 * and disassembles them as jbc files to the given data entry writer. For
 * resource entries, it returns valid output streams. For jbc entries, it
 * returns output streams that must not be used.
 *
 * @author Joachim Vandersmissen
 */
public class JbcDataEntryWriter
implements DataEntryWriter
{
    private final ClassPool       classPool;
    private final DataEntryWriter dataEntryWriter;


    /**
     * Creates a new JbcDataEntryWriter.
     * @param classPool       the class pool in which classes are found.
     * @param dataEntryWriter the writer to which the jbc file is disassembled.
     */
    public JbcDataEntryWriter(ClassPool       classPool,
                              DataEntryWriter dataEntryWriter)
    {
        this.classPool       = classPool;
        this.dataEntryWriter = dataEntryWriter;
    }


    // Implementations for DataEntryWriter.

    public boolean createDirectory(DataEntry dataEntry) throws IOException
    {
        return dataEntryWriter.createDirectory(dataEntry);
    }


    public boolean sameOutputStream(DataEntry dataEntry1,
                                    DataEntry dataEntry2)
    throws IOException
    {
        return dataEntryWriter.sameOutputStream(dataEntry1, dataEntry2);
    }


    public OutputStream createOutputStream(DataEntry dataEntry) throws IOException
    {
        // Is it a class entry?
        String name = dataEntry.getName();
        if (name.endsWith(AssemblyConstants.JBC_EXTENSION))
        {
            // Does it still have a corresponding class?
            String className = name.substring(0, name.length() - AssemblyConstants.JBC_EXTENSION.length());
            Clazz clazz = classPool.getClass(className);
            if (clazz != null)
            {
                // Get the output stream for this input entry.
                OutputStream outputStream = dataEntryWriter.createOutputStream(dataEntry);
                if (outputStream != null)
                {
                    // Disassemble the class to the output stream.
                    try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
                    {
                        clazz.accept(new ClassPrinter(new Printer(outputStreamWriter)));
                    }
                    catch (Exception e)
                    {
                        throw new IOException("An exception occured while disassembling " + dataEntry.getName(), e);
                    }
                }
            }

            // Return a dummy, non-null output stream (to work with cascading
            // output writers).
            return new FilterOutputStream(null);
        }

        // Delegate for resource entries.
        return dataEntryWriter.createOutputStream(dataEntry);
    }


    public void close() throws IOException
    {
        // Close the delegate writer.
        dataEntryWriter.close();
    }


    public void println(PrintWriter pw, String prefix)
    {
        pw.println(prefix + "JbcDataEntryWriter");
        dataEntryWriter.println(pw, prefix + "  ");
    }
}
