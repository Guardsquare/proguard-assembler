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
import proguard.classfile.attribute.*;
import proguard.classfile.editor.AttributesEditor;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;

import java.util.*;

/**
 * Prints ProgramClass members.
 *
 * @author Joachim Vandersmissen
 */
public class ClassMembersPrinter
implements   ClassVisitor,
             MemberVisitor
{
    private final Printer p;


    /**
     * Constructs a new ClassMembersPrinter that uses a Printer.
     *
     * @param p the Printer to use to print basic structures.
     */
    public ClassMembersPrinter(Printer p)
    {
        this.p = p;
    }


    // Implementations for ClassVisitor.

    public void visitAnyClass(Clazz clazz) {}


    public void visitProgramClass(ProgramClass programClass)
    {
        if (programClass.u2fieldsCount  > 0 ||
            programClass.u2methodsCount > 0)
        {
            p.printSpace();
            p.print(AssemblyConstants.BODY_OPEN);
            p.println();
            p.indent();
            for (int index = 0; index < programClass.u2fieldsCount; index++)
            {
                ProgramField field = programClass.fields[index];
                p.printIndent();
                if (printFieldAccessFlags(field.u2accessFlags))
                {
                    p.printSpace();
                }

                p.printType(field.getDescriptor(programClass));
                p.printSpace();
                p.printWord(field.getName(programClass));
                field.accept(programClass, this);
                p.println();
            }

            p.println();
            for (int index = 0; index < programClass.u2methodsCount; index++)
            {
                ProgramMethod method = programClass.methods[index];
                p.printIndent();
                boolean space = printMethodAccessFlags(method.u2accessFlags);
                if (!ClassConstants.METHOD_NAME_CLINIT.equals(method.getName(programClass)) ||
                    !ClassConstants.METHOD_TYPE_CLINIT.equals(method.getDescriptor(programClass)))
                {
                    if (space)
                    {
                        p.printSpace();
                    }

                    p.printMethodReturnType(method.getDescriptor(programClass));
                    p.printSpace();
                    p.printWord(method.getName(programClass));
                    printMethodArguments(programClass,
                                         method.getDescriptor(programClass),
                                         (MethodParametersAttribute) new AttributesEditor(programClass, method, false).findAttribute(Attribute.METHOD_PARAMETERS));
                }

                method.accept(programClass, this);
                p.println();
                p.println();
            }

            p.print(AssemblyConstants.BODY_CLOSE);
        }
        else
        {
            p.print(AssemblyConstants.STATEMENT_END);
        }
    }


    // Implementations for MemberVisitor.

    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        ConstantValueAttribute constantValueAttribute =
            (ConstantValueAttribute) new AttributesEditor(programClass,
                                                          programField,
                                                          false).findAttribute(Attribute.CONSTANT_VALUE);
        if (constantValueAttribute != null)
        {
            p.printSpace();
            p.print(AssemblyConstants.EQUALS);
            p.printSpace();
            programClass.constantPoolEntryAccept(constantValueAttribute.u2constantValueIndex, new ConstantPrinter(p, true));
        }

        programField.accept(programClass, new AttributesPrinter(p));
        p.print(AssemblyConstants.STATEMENT_END);
    }


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        ExceptionsAttribute exceptionsAttribute =
            (ExceptionsAttribute) new AttributesEditor(programClass,
                                                       programMethod,
                                                       false).findAttribute(Attribute.EXCEPTIONS);
        if (exceptionsAttribute != null &&
            exceptionsAttribute.u2exceptionIndexTableLength > 0)
        {
            p.printSpace();
            p.printWord(AssemblyConstants.THROWS);
            p.printSpace();
            for (int index = 0; index < exceptionsAttribute.u2exceptionIndexTableLength; index++)
            {
                p.printWord(ClassUtil.externalClassName(programClass.getClassName(exceptionsAttribute.u2exceptionIndexTable[index])));
                if (index < exceptionsAttribute.u2exceptionIndexTableLength - 1)
                {
                    p.print(JavaTypeConstants.METHOD_ARGUMENTS_SEPARATOR);
                    p.printSpace();
                }
            }
        }

        programMethod.accept(programClass, new AttributesPrinter(p));
        if (programMethod.processingInfo != null)
        {
            // Use shorthand notation.
            CodeAttribute codeAttribute = ((CodeAttribute) programMethod.processingInfo);
            Map<Integer, String> oldLabels = p.labels;
            p.labels = new HashMap<>();
            codeAttribute.accept(programClass, programMethod, new LabelsCollector(p.labels));
            codeAttribute.accept(programClass, programMethod, new InstructionsPrinter(p));
            p.labels = oldLabels;
        }
        else
        {
            p.print(AssemblyConstants.STATEMENT_END);
        }
    }


    // Small utility methods.

    /**
     * Prints ProgramMethod arguments.
     *
     * @param clazz                     the class being disassembled.
     * @param methodDescriptor          the internal method descriptor
     *                                  containing the method return type and
     *                                  the method arguments.
     * @param methodParametersAttribute the MethodParametersAttribute to read
     *                                  the parameters from.
     */
    private void printMethodArguments(Clazz clazz, String methodDescriptor, MethodParametersAttribute methodParametersAttribute)
    {
        p.print(JavaTypeConstants.METHOD_ARGUMENTS_OPEN);
        int index = 0;
        InternalTypeEnumeration internalTypeEnumeration =
            new InternalTypeEnumeration(methodDescriptor);
        while (internalTypeEnumeration.hasMoreTypes())
        {
            String type = internalTypeEnumeration.nextType();
            if (methodParametersAttribute != null &&
                index < methodParametersAttribute.u1parametersCount)
            {
                if (p.printAccessFlags(methodParametersAttribute.parameters[index].u2accessFlags))
                {
                    p.printSpace();
                }
            }

                p.printType(type);

            if (methodParametersAttribute != null &&
                index < methodParametersAttribute.u1parametersCount)
            {
                ParameterInfo parameterInfo =
                    methodParametersAttribute.parameters[index++];
                // Name index is optional.
                if (parameterInfo.u2nameIndex != 0)
                {
                    p.printSpace();
                    p.printWord(parameterInfo.getName(clazz));
                }
            }

            if (internalTypeEnumeration.hasMoreTypes())
            {
                p.print(JavaTypeConstants.METHOD_ARGUMENTS_SEPARATOR);
                p.printSpace();
            }
        }

        p.print(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE);
    }


    /**
     * Prints ProgramField access flags.
     *
     * @param accessFlags the access flags.
     * @return true if at least one access flag was written, false otherwise.
     */
    public boolean printFieldAccessFlags(int accessFlags)
    {
        if (accessFlags == 0)
        {
            return false;
        }

        StringBuilder stringBuilder = new StringBuilder();
        if ((accessFlags & AccessConstants.PUBLIC) != 0)    stringBuilder.append(JavaAccessConstants.PUBLIC).append(' ');
        if ((accessFlags & AccessConstants.PRIVATE) != 0)   stringBuilder.append(JavaAccessConstants.PRIVATE).append(' ');
        if ((accessFlags & AccessConstants.PROTECTED) != 0) stringBuilder.append(JavaAccessConstants.PROTECTED).append(' ');
        if ((accessFlags & AccessConstants.STATIC) != 0)    stringBuilder.append(JavaAccessConstants.STATIC).append(' ');
        if ((accessFlags & AccessConstants.FINAL) != 0)     stringBuilder.append(JavaAccessConstants.FINAL).append(' ');
        if ((accessFlags & AccessConstants.VOLATILE) != 0)  stringBuilder.append(JavaAccessConstants.VOLATILE).append(' ');
        if ((accessFlags & AccessConstants.TRANSIENT) != 0) stringBuilder.append(JavaAccessConstants.TRANSIENT).append(' ');
        if ((accessFlags & AccessConstants.SYNTHETIC) != 0) stringBuilder.append(JavaAccessConstants.SYNTHETIC).append(' ');
        if ((accessFlags & AccessConstants.ENUM) != 0)      stringBuilder.append(JavaAccessConstants.ENUM).append(' ');
        p.printWord(stringBuilder.toString().trim());
        return true;
    }


    /**
     * Prints ProgramMethod access flags.
     *
     * @param accessFlags the access flags.
     * @return true if at least one access flag was written, false otherwise.
     */
    public boolean printMethodAccessFlags(int accessFlags)
    {
        if (accessFlags == 0)
        {
            return false;
        }

        StringBuilder stringBuilder = new StringBuilder();
        if ((accessFlags & AccessConstants.PUBLIC) != 0)       stringBuilder.append(JavaAccessConstants.PUBLIC).append(' ');
        if ((accessFlags & AccessConstants.PRIVATE) != 0)      stringBuilder.append(JavaAccessConstants.PRIVATE).append(' ');
        if ((accessFlags & AccessConstants.PROTECTED) != 0)    stringBuilder.append(JavaAccessConstants.PROTECTED).append(' ');
        if ((accessFlags & AccessConstants.STATIC) != 0)       stringBuilder.append(JavaAccessConstants.STATIC).append(' ');
        if ((accessFlags & AccessConstants.FINAL) != 0)        stringBuilder.append(JavaAccessConstants.FINAL).append(' ');
        if ((accessFlags & AccessConstants.SYNCHRONIZED) != 0) stringBuilder.append(JavaAccessConstants.SYNCHRONIZED).append(' ');
        if ((accessFlags & AccessConstants.BRIDGE) != 0)       stringBuilder.append(JavaAccessConstants.BRIDGE).append(' ');
        if ((accessFlags & AccessConstants.VARARGS) != 0)      stringBuilder.append(JavaAccessConstants.VARARGS).append(' ');
        if ((accessFlags & AccessConstants.NATIVE) != 0)       stringBuilder.append(JavaAccessConstants.NATIVE).append(' ');
        if ((accessFlags & AccessConstants.ABSTRACT) != 0)     stringBuilder.append(JavaAccessConstants.ABSTRACT).append(' ');
        if ((accessFlags & AccessConstants.STRICT) != 0)       stringBuilder.append(JavaAccessConstants.STRICT).append(' ');
        if ((accessFlags & AccessConstants.SYNTHETIC) != 0)    stringBuilder.append(JavaAccessConstants.SYNTHETIC).append(' ');
        p.printWord(stringBuilder.toString().trim());
        return true;
    }
}
