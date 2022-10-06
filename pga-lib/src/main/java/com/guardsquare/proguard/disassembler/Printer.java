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
package com.guardsquare.proguard.disassembler;

import com.guardsquare.proguard.assembler.AssemblyConstants;
import proguard.classfile.*;
import proguard.classfile.util.ClassUtil;

import java.io.*;
import java.util.Map;

/**
 * General purpose printer.
 *
 * @author Joachim Vandersmissen
 */
public class Printer
{
    private static final String INDENTATION = "    ";

    private final Writer w;

    private int indentation;

    public Map<Integer, String> labels;


    /**
     * Creates a new Printer that writes to the given writer.
     *
     * @param writer the writer to write to.
     */
    public Printer(Writer writer)
    {
        this.w = writer;
    }


    /**
     * Increases the level of indentation by one.
     */
    public void indent()
    {
        indentation++;
    }


    /**
     * Decreases the level of indentation by one.
     */
    public void outdent()
    {
        indentation--;
    }


    /**
     * Writes a character to the underlying writer.
     *
     * @param c the character to write.
     * @throws PrintException if an IOException occured while writing.
     */
    private void write(int c)
    {
        try
        {
            w.write(c);
        }
        catch (IOException e)
        {
            throw new PrintException("An IOException occured while writing.", e);
        }
    }


    /**
     * Writes a string to the underlying writer.
     *
     * @param s the string to write.
     * @throws PrintException if an IOException occured while writing.
     */
    private void write(String s)
    {
        try
        {
            w.write(s);
        }
        catch (IOException e)
        {
            throw new PrintException("An IOException occured while writing.", e);
        }
    }


    /**
     * Writes the system line separator.
     */
    public void println()
    {
        write(System.lineSeparator());
    }


    /**
     * Writes the indentation.
     */
    public void printIndent()
    {
        for (int index = 0; index < indentation; index++)
        {
            write(INDENTATION);
        }
    }


    /**
     * Writes a single character.
     *
     * @param c The character to write.
     */
    public void print(char c)
    {
        write(c);
    }


    /**
     * Writes a single space.
     */
    public void printSpace()
    {
        write(' ');
    }


    /**
     * Writes a number.
     * The number will be formatted according to the DecimalFormat field.
     *
     * @param number the number to write.
     */
    public void printNumber(double number)
    {
        write(AssemblyConstants.DOUBLE_TO_STRING.format(number));
    }


    /**
     * Writes a word.
     *
     * @param word the word to write.
     */
    public void printWord(String word)
    {
        write(word);
    }


    /**
     * Writes a string, surrounded by double quotes.
     *
     * @param string the string value.
     */
    public void printString(String string)
    {
        write(AssemblyConstants.STRING_QUOTE);
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : string.toCharArray())
        {
            if (c < ' ' || c > '~')
            {
                stringBuilder.append("\\" + Integer.toOctalString(c));
            }
            else if (c == '"')
            {
                stringBuilder.append("\\\"");
            }
            else if (c == '\\')
            {
                stringBuilder.append("\\\\");
            }
            else
            {
                stringBuilder.append(c);
            }
        }

        write(stringBuilder.toString());
        write(AssemblyConstants.STRING_QUOTE);
    }


    /**
     * Writes a char, surrounded by single quotes.
     *
     * @param c the char value.
     */
    public void printChar(char c)
    {
        write(AssemblyConstants.CHAR_QUOTE);
        if (c < ' ' || c > '~')
        {
            write("\\" + Integer.toOctalString(c));
        }
        else if (c == '\'')
        {
            write("\\'");
        }
        else if (c == '\\')
        {
            write("\\\\");
        }
        else
        {
            write(c);
        }

        write(AssemblyConstants.CHAR_QUOTE);
    }


    /**
     * Writes an internal field type.
     *
     * @param type the internal field type.
     */
    public void printType(String type)
    {
        printWord(ClassUtil.externalType(type));
    }


    /**
     * Writes a method return type.
     *
     * @param methodDescriptor The internal method descriptor containing the
     *                         method return type and the method arguments.
     */
    public void printMethodReturnType(String methodDescriptor)
    {
        printType(ClassUtil.internalMethodReturnType(methodDescriptor));
    }


    /**
     * Writes method arguments.
     *
     * @param methodDescriptor The internal method descriptor containing the
     *                         method return type and the method arguments.
     */
    public void printMethodArguments(String methodDescriptor)
    {
        print(JavaTypeConstants.METHOD_ARGUMENTS_OPEN);
        printWord(ClassUtil.externalMethodArguments(methodDescriptor));
        print(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE);
    }


    /**
     * Writes MethodParameters, RequiresInfo, ExportsInfo, and OpensInfo access
     * flags.
     *
     * @param accessFlags the access flags.
     * @return true if at least one access flag was written, false otherwise.
     */
    public boolean printAccessFlags(int accessFlags)
    {
        if (accessFlags == 0)
        {
            return false;
        }

        StringBuilder stringBuilder = new StringBuilder();
        if ((accessFlags & AccessConstants.FINAL) != 0)        stringBuilder.append(JavaAccessConstants.FINAL).append(' ');
        if ((accessFlags & AccessConstants.TRANSITIVE) != 0)   stringBuilder.append(JavaAccessConstants.TRANSITIVE).append(' ');
        if ((accessFlags & AccessConstants.STATIC_PHASE) != 0) stringBuilder.append(JavaAccessConstants.STATIC_PHASE).append(' ');
        if ((accessFlags & AccessConstants.SYNTHETIC) != 0)    stringBuilder.append(JavaAccessConstants.SYNTHETIC).append(' ');
        if ((accessFlags & AccessConstants.MANDATED) != 0)     stringBuilder.append(JavaAccessConstants.MANDATED).append(' ');
        printWord(stringBuilder.toString().trim());
        return true;
    }


    /**
     * Writes ProgramClass and InnerClassInfo access flags and class types.
     *
     * @param accessFlags the access flags.
     * @return true if at least one access flag or class type was written, false
     *         otherwise.
     */
    public boolean printClassAccessFlags(int accessFlags)
    {
        StringBuilder stringBuilder = new StringBuilder();
        if ((accessFlags & AccessConstants.PUBLIC) != 0)    stringBuilder.append(JavaAccessConstants.PUBLIC).append(' ');
        if ((accessFlags & AccessConstants.PRIVATE) != 0)   stringBuilder.append(JavaAccessConstants.PRIVATE).append(' ');
        if ((accessFlags & AccessConstants.PROTECTED) != 0) stringBuilder.append(JavaAccessConstants.PROTECTED).append(' ');
        if ((accessFlags & AccessConstants.STATIC) != 0)    stringBuilder.append(JavaAccessConstants.STATIC).append(' ');
        if ((accessFlags & AccessConstants.FINAL) != 0)     stringBuilder.append(JavaAccessConstants.FINAL).append(' ');
        if ((accessFlags & AccessConstants.ABSTRACT) != 0)  stringBuilder.append(JavaAccessConstants.ABSTRACT).append(' ');
        if ((accessFlags & AccessConstants.SYNTHETIC) != 0) stringBuilder.append(JavaAccessConstants.SYNTHETIC).append(' ');

        if ((accessFlags & AccessConstants.MODULE) != 0)
        {
            stringBuilder.append(JavaAccessConstants.MODULE);
            printWord(stringBuilder.toString());
            return true;
        }

        if ((accessFlags & AccessConstants.ENUM) != 0)
        {
            stringBuilder.append(JavaAccessConstants.ENUM);
            printWord(stringBuilder.toString());
            return true;
        }

        if ((accessFlags & AccessConstants.ANNOTATION) != 0)
        {
            stringBuilder.append(JavaAccessConstants.ANNOTATION).append(JavaAccessConstants.INTERFACE);
            printWord(stringBuilder.toString());
            return true;
        }

        if ((accessFlags & AccessConstants.INTERFACE) != 0)
        {
            stringBuilder.append(JavaAccessConstants.INTERFACE);
            printWord(stringBuilder.toString());
            return true;
        }

        stringBuilder.append(AssemblyConstants.CLASS);
        printWord(stringBuilder.toString());
        return true;
    }


    /**
     * Writes a bytecode offset.
     *
     * @param offset the bytecode offset.
     * @throws PrintException if the offset was not found in the labels map.
     */
    public void printOffset(int offset)
    {
        if (!labels.containsKey(offset))
        {
            throw new PrintException("Offset " + offset + " not found in labels.");
        }

        printWord(labels.get(offset));
    }


    /**
     * Flushes the writer.
     */
    public void flush()
    {
        try
        {
            w.flush();
        }
        catch (IOException ignore) {}
    }
}
