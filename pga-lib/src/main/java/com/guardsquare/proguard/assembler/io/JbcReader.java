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
package com.guardsquare.proguard.assembler.io;

import com.guardsquare.proguard.assembler.ClassParser;
import com.guardsquare.proguard.assembler.Parser;
import proguard.classfile.Clazz;
import proguard.classfile.ProgramClass;
import proguard.classfile.visitor.ClassVisitor;
import proguard.io.DataEntry;
import proguard.io.DataEntryReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * This DataEntryReader parses jbc files using the ClassParser, and then applies
 * a given ClassVisitor to the parsed ProgramClass objects.
 *
 * @author Joachim Vandersmissen
 */
public class JbcReader implements DataEntryReader
{
    private final ClassVisitor classVisitor;


    /**
     * Creates a new JbcReader.
     *
     * @param classVisitor The class visitor to apply after conversion from JBC to Clazz.
     */
    public JbcReader(ClassVisitor classVisitor)
    {
        this.classVisitor = classVisitor;
    }


    // Implementations for DataEntryReader.

    public void read(DataEntry dataEntry) throws IOException
    {
        try
        {
            // Get the input stream.
            InputStream inputStream = dataEntry.getInputStream();

            // Wrap it into an input stream reader.
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

            // Create a Clazz representation.
            Clazz clazz = new ProgramClass();
            clazz.accept(new ClassParser(new Parser(inputStreamReader)));

            // Apply the visitor.
            clazz.accept(classVisitor);

            dataEntry.closeInputStream();
        }
        catch (Exception e)
        {
            throw new IOException("An exception occured while assembling " + dataEntry.getName(), e);
        }
    }
}
