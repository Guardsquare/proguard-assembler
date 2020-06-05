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
package proguard.disassembler;

import proguard.AssemblyConstants;
import proguard.classfile.*;
import proguard.classfile.util.*;
import proguard.classfile.visitor.ClassVisitor;

/**
 * Prints a ProgramClass.
 *
 * @author Joachim Vandersmissen
 */
public class ClassPrinter
implements   ClassVisitor
{
    private final Printer p;


    /**
     * Constructs a new ClassPrinter that uses a Printer.
     *
     * @param p the Printer to use to print basic structures.
     */
    public ClassPrinter(Printer p)
    {
        this.p = p;
    }


    // Implementations for ClassVisitor.

    public void visitAnyClass(Clazz clazz) {}


    public void visitProgramClass(ProgramClass programClass)
    {
        p.printWord(AssemblyConstants.VERSION);
        p.printSpace();
        p.printWord(ClassUtil.externalClassVersion(programClass.u4version));
        p.print(AssemblyConstants.STATEMENT_END);
        p.println();
        if (p.printClassAccessFlags(programClass.u2accessFlags))
        {
            p.printSpace();
        }

        p.printWord(ClassUtil.externalClassName(programClass.getName()));
        // Syntactic sugar: extends in interfaces define bytecode interfaces.
        if ((programClass.u2accessFlags & AccessConstants.INTERFACE) != 0)
        {
            if (programClass.u2interfacesCount > 0)
            {
                p.printSpace();
                p.printWord(AssemblyConstants.EXTENDS);
                p.printSpace();
                printInterfaces(programClass);
            }
        }
        else
        {
            if (programClass.u2superClass != 0)
            {
                p.printSpace();
                p.printWord(AssemblyConstants.EXTENDS);
                p.printSpace();
                p.printWord(ClassUtil.externalClassName(programClass.getSuperName()));
            }

            if (programClass.u2interfacesCount > 0)
            {
                p.printSpace();
                p.printWord(AssemblyConstants.IMPLEMENTS);
                p.printSpace();
                printInterfaces(programClass);
            }
        }

        programClass.accept(new AttributesPrinter(p));
        programClass.accept(new ClassMembersPrinter(p));

        p.flush();
    }


    // Small utility methods.

    /**
     * Prints the interfaces of a class.
     *
     * @param programClass the class to print the interfaces of.
     */
    private void printInterfaces(ProgramClass programClass)
    {
        for (int index = 0; index < programClass.u2interfacesCount; index++)
        {
            p.printWord(ClassUtil.externalClassName(programClass.getInterfaceName(index)));
            if (index < programClass.u2interfacesCount - 1)
            {
                p.print(JavaTypeConstants.METHOD_ARGUMENTS_SEPARATOR);
                p.printSpace();
            }
        }
    }
}
