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
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.util.*;

/**
 * Prints Constants.
 *
 * @author Joachim Vandersmissen
 */
public class ConstantPrinter
implements   ConstantVisitor
{
    private static final int INTEGER_FALSE = 0;
    private static final int INTEGER_TRUE  = 1;


    private final Printer p;
    private final char    intType;
    private final boolean printFullType;


    /**
     * Constructs a new ClassPrinter that uses a Printer and a boolean flag.
     *
     * @param p             the Printer to use to print basic structures.
     * @param printFullType if this is set to true, the full type of a constant
     *                      will be printed if that constant would not be
     *                      unambiguously parsed by the Assembler.
     */
    public ConstantPrinter(Printer p, boolean printFullType)
    {
        this.p             = p;
        this.intType       = 0;
        this.printFullType = printFullType;
    }


    /**
     * Constructs a new ClassPrinter that uses a Printer, the type of the
     * IntegerConstant, and a boolean flag.
     *
     * @param p       the Printer to use to print basic structures.
     * @param intType the internal type of the IntegerConstants being visited.
     */
    public ConstantPrinter(Printer p, char intType)
    {
        this.p             = p;
        this.intType       = intType;
        this.printFullType = false;
    }


    // Implementations for ConstantVisitor.

    public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant)
    {
        if (intType == TypeConstants.BOOLEAN)
        {
            if (integerConstant.u4value == INTEGER_TRUE)
            {
                p.printWord(AssemblyConstants.TRUE);
                return;
            }

            if (integerConstant.u4value == INTEGER_FALSE)
            {
                p.printWord(AssemblyConstants.FALSE);
                return;
            }
        }

        if (intType == TypeConstants.CHAR)
        {
            p.printChar((char) integerConstant.u4value);
            return;
        }

        if (intType != 0 && intType != TypeConstants.INT)
        {
            p.print(JavaTypeConstants.METHOD_ARGUMENTS_OPEN);
            p.printType(String.valueOf(intType));
            p.print(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE);
            p.printSpace();
        }

        p.printNumber(integerConstant.u4value);
    }


    public void visitLongConstant(Clazz clazz, LongConstant longConstant)
    {
        p.printNumber(longConstant.u8value);
        p.printWord(JavaTypeConstants.LONG);
    }


    public void visitFloatConstant(Clazz clazz, FloatConstant floatConstant)
    {
        p.printNumber(floatConstant.f4value);
        p.printWord(JavaTypeConstants.FLOAT);
    }


    public void visitDoubleConstant(Clazz clazz, DoubleConstant doubleConstant)
    {
        p.printNumber(doubleConstant.f8value);
        p.printWord(JavaTypeConstants.DOUBLE);
    }


    public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
    {
        p.printString(stringConstant.getString(clazz));
    }


    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant)
    {
        p.printString(utf8Constant.getString());
    }


    public void visitDynamicConstant(Clazz clazz, DynamicConstant dynamicConstant)
    {
        if (printFullType)
        {
            p.print(JavaTypeConstants.METHOD_ARGUMENTS_OPEN);
            p.printWord(AssemblyConstants.TYPE_DYNAMIC);
            p.print(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE);
            p.printSpace();
        }

        p.printNumber(dynamicConstant.u2bootstrapMethodAttributeIndex);
        p.printSpace();
        p.printType(dynamicConstant.getType(clazz));
        p.printSpace();
        p.printWord(dynamicConstant.getName(clazz));
    }


    public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant)
    {
        p.printNumber(invokeDynamicConstant.u2bootstrapMethodAttributeIndex);
        p.printSpace();
        p.printMethodReturnType(invokeDynamicConstant.getType(clazz));
        p.printSpace();
        p.printWord(invokeDynamicConstant.getName(clazz));
        p.printMethodArguments(invokeDynamicConstant.getType(clazz));
    }


    public void visitMethodHandleConstant(Clazz clazz, MethodHandleConstant methodHandleConstant)
    {
        if (printFullType)
        {
            p.print(JavaTypeConstants.METHOD_ARGUMENTS_OPEN);
            p.printWord(JavaConstants.TYPE_JAVA_LANG_INVOKE_METHODHANDLE);
            p.print(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE);
            p.printSpace();
        }

        printReferenceKind(methodHandleConstant.u1referenceKind);
        p.printSpace();
        clazz.constantPoolEntryAccept(methodHandleConstant.u2referenceIndex, this);
    }


    public void visitModuleConstant(Clazz clazz, ModuleConstant moduleConstant)
    {
        throw new PrintException("Unsupported operation");
    }


    public void visitPackageConstant(Clazz clazz, PackageConstant packageConstant)
    {
        throw new PrintException("Unsupported operation");
    }


    public void visitFieldrefConstant(Clazz clazz, FieldrefConstant fieldrefConstant)
    {
        if (fieldrefConstant.u2classIndex != ((ProgramClass) clazz).u2thisClass)
        {
            clazz.constantPoolEntryAccept(fieldrefConstant.u2classIndex, this);
        }

        p.print(AssemblyConstants.REFERENCE_SEPARATOR);
        p.printType(fieldrefConstant.getType(clazz));
        p.printSpace();
        p.printWord(fieldrefConstant.getName(clazz));
    }


    public void visitAnyMethodrefConstant(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant)
    {
        if (anyMethodrefConstant.u2classIndex != ((ProgramClass) clazz).u2thisClass)
        {
            clazz.constantPoolEntryAccept(anyMethodrefConstant.u2classIndex, this);
        }

        p.print(AssemblyConstants.REFERENCE_SEPARATOR);
        p.printMethodReturnType(anyMethodrefConstant.getType(clazz));
        p.printSpace();
        p.printWord(anyMethodrefConstant.getName(clazz));
        p.printMethodArguments(anyMethodrefConstant.getType(clazz));
    }


    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        p.printType(ClassUtil.internalTypeFromClassType(classConstant.getName(clazz)));
    }


    public void visitMethodTypeConstant(Clazz clazz, MethodTypeConstant methodTypeConstant)
    {
        if (printFullType)
        {
            p.print(JavaTypeConstants.METHOD_ARGUMENTS_OPEN);
            p.printWord(JavaConstants.TYPE_JAVA_LANG_INVOKE_METHODTYPE);
            p.print(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE);
            p.printSpace();
        }

        p.printMethodReturnType(methodTypeConstant.getType(clazz));
        p.printSpace();
        p.printMethodArguments(methodTypeConstant.getType(clazz));
    }


    public void visitNameAndTypeConstant(Clazz clazz, NameAndTypeConstant nameAndTypeConstant)
    {
        throw new PrintException("Unsupported operation.");
    }


    // Small utility methods.

    /**
     * Prints the representation of a reference kind.
     *
     * @param referenceKind the reference kind.
     * @throws PrintException if the reference kind is unknown
     */
    private void printReferenceKind(int referenceKind)
    {
        switch (referenceKind)
        {
            case MethodHandleConstant.REF_GET_FIELD:         p.printWord(AssemblyConstants.REF_GET_FIELD);          break;
            case MethodHandleConstant.REF_GET_STATIC:        p.printWord(AssemblyConstants.REF_GET_STATIC);         break;
            case MethodHandleConstant.REF_PUT_FIELD:         p.printWord(AssemblyConstants.REF_PUT_FIELD);          break;
            case MethodHandleConstant.REF_PUT_STATIC:        p.printWord(AssemblyConstants.REF_PUT_STATIC);         break;
            case MethodHandleConstant.REF_INVOKE_VIRTUAL:    p.printWord(AssemblyConstants.REF_INVOKE_VIRTUAL);     break;
            case MethodHandleConstant.REF_INVOKE_STATIC:     p.printWord(AssemblyConstants.REF_INVOKE_STATIC);      break;
            case MethodHandleConstant.REF_INVOKE_SPECIAL:    p.printWord(AssemblyConstants.REF_INVOKE_SPECIAL);     break;
            case MethodHandleConstant.REF_NEW_INVOKE_SPECIAL: p.printWord(AssemblyConstants.REF_NEW_INVOKE_SPECIAL); break;
            case MethodHandleConstant.REF_INVOKE_INTERFACE:  p.printWord(AssemblyConstants.REF_INVOKE_INTERFACE);   break;
            default:                                  throw new PrintException("Unknown reference kind " + referenceKind + ".");
        }
    }
}
