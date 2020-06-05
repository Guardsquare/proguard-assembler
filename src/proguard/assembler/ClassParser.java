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
package proguard.assembler;

import proguard.AssemblyConstants;
import proguard.classfile.*;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.constant.Constant;
import proguard.classfile.editor.*;
import proguard.classfile.util.*;
import proguard.classfile.visitor.ClassVisitor;

/**
 * Parses a ProgramClass.
 *
 * @author Joachim Vandersmissen
 */
public class ClassParser
implements   ClassVisitor
{
    private final Parser p;


    /**
     * Constructs a new ClassParser that uses a Parser.
     *
     * @param p   the Parser to use to parse basic structures.
     */
    public ClassParser(Parser p)
    {
        this.p = p;
    }


    // Implementations for ClassVisitor.

    public void visitAnyClass(Clazz clazz) {}


    public void visitProgramClass(ProgramClass programClass)
    {
        programClass.u2constantPoolCount = 1;
        programClass.constantPool        = new Constant[1];
        programClass.u2interfaces        = new int[0];
        programClass.fields              = new ProgramField[0];
        programClass.methods             = new ProgramMethod[0];
        programClass.attributes          = new Attribute[0];

        while (p.nextTtypeEqualsWord())
        {
            String keyword = p.sval;
            if (AssemblyConstants.IMPORT.equals(keyword))
            {
                String className = p.expectWord("class name");
                p.expect(AssemblyConstants.STATEMENT_END, "import end");
                int index = className.lastIndexOf(JavaTypeConstants.PACKAGE_SEPARATOR);
                if (index > -1)
                {
                    p.imports.put(className.substring(index + 1), className);
                }
            }
            else if (AssemblyConstants.VERSION.equals(keyword))
            {
                programClass.u4version =
                    ClassUtil.internalClassVersion(AssemblyConstants.DOUBLE_TO_STRING.format(p.expectNumber("version number")));
                p.expect(AssemblyConstants.STATEMENT_END, "version end");
            }
            else
            {
                p.pushBack();
                break;
            }
        }

        ClassEditor classEditor = new ClassEditor(programClass);
        ConstantPoolEditor cpe  = new ConstantPoolEditor(programClass);

        programClass.u2accessFlags = p.expectClassAccessFlags();
        programClass.u2thisClass   =
            cpe.addClassConstant(ClassUtil.internalClassName(p.expectWord("this class")), null);

        // Syntactic sugar: extends in interfaces define bytecode interfaces.
        if ((programClass.u2accessFlags & AccessConstants.INTERFACE) != 0)
        {
            if (p.expectIfNextTtypeEqualsWord(AssemblyConstants.EXTENDS))
            {
                expectInterfaces(classEditor, cpe);
            }
        }
        else
        {
            if (p.nextTtypeEqualsWord()) {
                if (p.sval.equals(AssemblyConstants.EXTENDS))
                {
                    programClass.u2superClass =
                        cpe.addClassConstant(ClassUtil.internalClassName(p.expectWord("super class")), null);
                }
                else
                {
                    p.pushBack();
                }
            }

            while (p.expectIfNextTtypeEqualsWord(AssemblyConstants.IMPLEMENTS))
            {
                expectInterfaces(classEditor, cpe);
            }
        }

        // Syntactic sugar: default superclasses.
        if (programClass.u2superClass == 0)
        {
            if ((programClass.u2accessFlags & AccessConstants.ENUM) != 0)
            {
                // Syntactic sugar: default java.lang.Enum superclass for enums.
                programClass.u2superClass =
                    cpe.addClassConstant(ClassConstants.NAME_JAVA_LANG_ENUM, null);
            }
            else if ((programClass.u2accessFlags & AccessConstants.MODULE) == 0 &&
                     !ClassConstants.NAME_JAVA_LANG_OBJECT.equals(programClass.getName()))
            {
                // Syntactic sugar: default java.lang.Object superclass for any
                // other class type *except* modules and java.lang.Object itself.
                programClass.u2superClass =
                    cpe.addClassConstant(ClassConstants.NAME_JAVA_LANG_OBJECT, null);
            }
        }

        // Syntactic sugar: default interface for annotations.
        if ((programClass.u2accessFlags & AccessConstants.ANNOTATION) != 0 &&
            !containsInterface(programClass, ClassConstants.NAME_JAVA_LANG_ANNOTATION_ANNOTATION))
        {
            classEditor.addInterface(cpe.addClassConstant(ClassConstants.NAME_JAVA_LANG_ANNOTATION_ANNOTATION, null));
        }

        if (p.nextTtypeEquals(AssemblyConstants.ATTRIBUTES_OPEN))
        {
            programClass.accept(new AttributesParser(p, cpe));
        }

        if (p.nextTtypeEquals(AssemblyConstants.BODY_OPEN))
        {
            programClass.accept(new ClassMembersParser(p, cpe));
        }
        else
        {
            p.expect(AssemblyConstants.STATEMENT_END, "class end");
        }
    }


    // Small utility methods.

    /**
     * Parses interfaces.
     *
     * @param classEditor The ClassEditor to add the interfaces to.
     * @param cpe         The ConstantPoolEditor to use to add constants.
     */
    private void expectInterfaces(ClassEditor classEditor, ConstantPoolEditor cpe)
    {
        do
        {
            classEditor.addInterface(cpe.addClassConstant(ClassUtil.internalClassName(p.expectWord("interface class")), null));
        } while (p.nextTtypeEquals(','));
    }


    /**
     * Returns whether a given ProgramClass contains an interface.
     *
     * @param programClass the ProgramClass to check.
     * @param interf the internal name of the interface.
     * @return true if the ProgramClass contains the interface, false otherwise.
     */
    private boolean containsInterface(ProgramClass programClass, String interf)
    {
        for (int index = 0; index < programClass.u2interfacesCount; index++)
        {
            if (interf.equals(programClass.getInterfaceName(index)))
            {
                return true;
            }
        }

        return false;
    }
}
