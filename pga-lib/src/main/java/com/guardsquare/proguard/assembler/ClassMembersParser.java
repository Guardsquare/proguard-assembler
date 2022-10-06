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
package com.guardsquare.proguard.assembler;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.editor.*;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.util.ArrayUtil;

/**
 * Parses ProgramClass members.
 *
 * @author Joachim Vandersmissen
 */
public class ClassMembersParser
implements   ClassVisitor,
             MemberVisitor
{
    private final Parser             p;
    private final ConstantPoolEditor cpe;


    /**
     * Constructs a new ClassMembersParser that uses a Parser and a
     * ConstantPoolEditor.
     *
     * @param p   the Parser to use to parse basic structures.
     * @param cpe the ConstantPoolEditor to use to add Constants to the constant
     *            pool.
     */
    public ClassMembersParser(Parser p, ConstantPoolEditor cpe)
    {
        this.p   = p;
        this.cpe = cpe;
    }


    // Implementations for ClassVisitor.

    public void visitAnyClass(Clazz clazz) {}


    public void visitProgramClass(ProgramClass programClass)
    {
        ClassEditor classEditor = new ClassEditor(programClass);
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            // We need to parse a lot of information before we know which
            // class member we're declaring.
            int accessFlags = p.expectAccessFlags();
            if (p.nextTtypeEquals(AssemblyConstants.ATTRIBUTES_OPEN) ||
                p.nextTtypeEquals(AssemblyConstants.BODY_OPEN))
            {
                // We know we're declaring a <clinit> method now.
                p.pushBack();

                ProgramMethod method =
                    new ProgramMethod(accessFlags,
                                      cpe.addUtf8Constant(ClassConstants.METHOD_NAME_CLINIT),
                                      cpe.addUtf8Constant(ClassConstants.METHOD_TYPE_CLINIT),
                                      null);
                method.accept(programClass, this);

                classEditor.addMethod(method);
            }
            else
            {
                String type = p.expectType("class member type");
                String name = p.expectMethodName("class member name");
                if (p.nextTtypeEquals(JavaTypeConstants.METHOD_ARGUMENTS_OPEN))
                {
                    // We know we're declaring a method now.
                    MethodParametersAttribute methodParametersAttribute =
                        new MethodParametersAttribute();
                    methodParametersAttribute.u2attributeNameIndex      =
                        cpe.addUtf8Constant(Attribute.METHOD_PARAMETERS);
                    methodParametersAttribute.parameters                =
                        new ParameterInfo[0];

                    ProgramMethod method =
                        new ProgramMethod(accessFlags,
                                          cpe.addUtf8Constant(name),
                                          cpe.addUtf8Constant(expectMethodArguments(methodParametersAttribute) + type),
                                          null);
                    method.accept(programClass, this);

                    if (methodParametersAttribute.u1parametersCount > 0)
                    {
                        new AttributesEditor(programClass, method, false).addAttribute(methodParametersAttribute);
                    }

                    classEditor.addMethod(method);
                }
                else
                {
                    // We know we're declaring a field now.
                    ProgramField field  =
                        new ProgramField(accessFlags,
                                         cpe.addUtf8Constant(name),
                                         cpe.addUtf8Constant(type),
                                         null);
                    field.accept(programClass, this);

                    classEditor.addField(field);
                }
            }
        }
    }


    // Implementations for MemberVisitor.

    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        if (p.nextTtypeEquals(AssemblyConstants.EQUALS))
        {
            ConstantValueAttribute constantValueAttribute =
                new ConstantValueAttribute(cpe.addUtf8Constant(Attribute.CONSTANT_VALUE),
                                           p.expectLoadableConstant(programClass, cpe, new ConstantParser(p, cpe)));

            new AttributesEditor(programClass, programField, false).addAttribute(constantValueAttribute);
        }

        if (p.nextTtypeEquals(AssemblyConstants.ATTRIBUTES_OPEN))
        {
            programField.accept(programClass, new AttributesParser(p, cpe));
        }

        p.expect(AssemblyConstants.STATEMENT_END, "field end");
    }


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {

        if (p.expectIfNextTtypeEqualsWord(AssemblyConstants.THROWS))
        {
            ExceptionsAttribute exceptionsAttribute   =
                new ExceptionsAttribute();
            exceptionsAttribute.u2attributeNameIndex  =
                cpe.addUtf8Constant(Attribute.EXCEPTIONS);
            exceptionsAttribute.u2exceptionIndexTable = new int[0];

            do
            {
                String exceptionType =
                    ClassUtil.internalClassName(p.expectWord("exception class"));

                exceptionsAttribute.u2exceptionIndexTable =
                    ArrayUtil.add(exceptionsAttribute.u2exceptionIndexTable,
                                  exceptionsAttribute.u2exceptionIndexTableLength++,
                                  cpe.addClassConstant(exceptionType, null));
            } while (p.nextTtypeEquals(JavaTypeConstants.METHOD_ARGUMENTS_SEPARATOR));

            new AttributesEditor(programClass, programMethod, false).addAttribute(exceptionsAttribute);
        }

        if (p.nextTtypeEquals(AssemblyConstants.ATTRIBUTES_OPEN))
        {
            programMethod.accept(programClass, new AttributesParser(p, cpe));
        }

        if (p.nextTtypeEquals(AssemblyConstants.BODY_OPEN))
        {
            programMethod.accept(programClass, new InstructionsParser(p, cpe, new CodeAttributeComposer(true, true, false)));
        }
        else
        {
            p.expect(AssemblyConstants.STATEMENT_END, "method end");
        }
    }


    // Small utility methods.

    /**
     * Parses ProgramMethod arguments. This method advances the Parser.
     *
     * @param methodParametersAttribute the MethodParametersAttribute to add the
     *                                  method parameters to.
     * @return the method arguments descriptor.
     */
    private String expectMethodArguments(MethodParametersAttribute methodParametersAttribute)
    {
        StringBuilder methodArguments = new StringBuilder();
        methodArguments.append(JavaTypeConstants.METHOD_ARGUMENTS_OPEN);
        if (p.nextTtypeEquals(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE))
        {
            return methodArguments.append(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE).toString();
        }

        while (true)
        {
            // Syntactic sugar: parameters access/names in method descriptor.
            ParameterInfo parameterInfo = new ParameterInfo();
            parameterInfo.u2accessFlags = p.expectAccessFlags();
            methodArguments.append(p.expectType("method parameter type"));

            // Name index is optional.
            if (p.nextTtypeEqualsWord())
            {
                parameterInfo.u2nameIndex = cpe.addUtf8Constant(p.sval);
            }

            methodParametersAttribute.parameters =
                ArrayUtil.add(methodParametersAttribute.parameters, methodParametersAttribute.u1parametersCount++, parameterInfo);

            if (p.nextTtypeEquals(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE))
            {
                break;
            }

            p.expect(JavaTypeConstants.METHOD_ARGUMENTS_SEPARATOR, "method arguments separator");
        }

        // Check if any of the parameters actually has a name or access flags.
        for (int index = 0; index < methodParametersAttribute.u1parametersCount; index++)
        {
            ParameterInfo parameterInfo = methodParametersAttribute.parameters[index];
            if (parameterInfo.u2accessFlags != 0 ||
                parameterInfo.u2nameIndex   != 0)
            {
                return methodArguments.append(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE).toString();
            }
        }

        // No parameters with name or access flag so we set the count to 0.
        methodParametersAttribute.u1parametersCount = 0;
        return methodArguments.append(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE).toString();
    }
}
