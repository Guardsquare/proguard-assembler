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
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.editor.*;
import proguard.classfile.util.*;

/**
 * Parses Constants, adds them to the constant pool and sets the index field.
 *
 * @author Joachim Vandersmissen
 */
public class ConstantParser
implements   ConstantVisitor
{
    private static final int INTEGER_FALSE = 1;
    private static final int INTEGER_TRUE  = 1;


    private final Parser             p;
    private final ConstantPoolEditor cpe;

    private int index;


    /**
     * Constructs a new ConstantParser that uses a Parser and a
     * ConstantPoolEditor.
     *
     * @param p   the Parser to use to parse basic structures.
     * @param cpe the ConstantPoolEditor to use to add Constants to the constant
     *            pool.
     */
    public ConstantParser(Parser p, ConstantPoolEditor cpe)
    {
        this.p = p;
        this.cpe = cpe;
    }


    public int getIndex()
    {
        return index;
    }


    // Implementations for ConstantVisitor.

    public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant)
    {
        if (p.nextTtypeEqualsChar())
        {
            if (p.sval.length() > 1)
            {
                throw new ParseException("Char value contains multiple characters.", p.lineno());
            }

            index = cpe.addIntegerConstant((int) p.sval.charAt(0));
            return;
        }

        if (p.nextTtypeEqualsNumber())
        {
            index = cpe.addIntegerConstant((int) p.nval);
            return;
        }

        if (AssemblyConstants.TRUE.equals(p.expect(AssemblyConstants.TRUE, AssemblyConstants.FALSE)))
        {
            index = cpe.addIntegerConstant(INTEGER_TRUE);
            return;
        }

        // Keyword is FALSE
        index = cpe.addIntegerConstant(INTEGER_FALSE);
    }


    public void visitLongConstant(Clazz clazz, LongConstant longConstant)
    {
        index = cpe.addLongConstant((long) p.expectNumber("long value"));
    }


    public void visitFloatConstant(Clazz clazz, FloatConstant floatConstant)
    {
        index = cpe.addFloatConstant((float) p.expectNumber("float value"));
    }


    public void visitDoubleConstant(Clazz clazz, DoubleConstant doubleConstant)
    {
        index = cpe.addDoubleConstant(p.expectNumber("double value"));
    }


    public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
    {
        index = cpe.addStringConstant(p.expectString("string value"), null, null);
    }


    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant)
    {
        // Should only be used when an actual "string" (surrounded by quotes) is
        // expected.
        index = cpe.addUtf8Constant(p.expectString("string value"));
    }


    public void visitDynamicConstant(Clazz clazz, DynamicConstant dynamicConstant)
    {
        int bootstrapMethodAttributeIndex =
            (int) p.expectNumber("dynamic bootstrap method index");
        String type = p.expectType("dynamic type");
        String name = p.expectWord("dynamic name");
        index = cpe.addDynamicConstant(bootstrapMethodAttributeIndex,
                                       cpe.addNameAndTypeConstant(name, type),
                                       null);
    }


    public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant)
    {
        int bootstrapMethodAttributeIndex =
            (int) p.expectNumber("invokedynamic bootstrap method index");
        String returnType = p.expectType("invokedynamic return type");
        String name       = p.expectMethodName("invokedynamic name");
        String methodArgs = p.expectMethodArguments("invokedynamic arguments");
        index = cpe.addInvokeDynamicConstant(bootstrapMethodAttributeIndex,
                                             cpe.addNameAndTypeConstant(name, methodArgs + returnType),
                                             null);
    }


    public void visitMethodHandleConstant(Clazz clazz, MethodHandleConstant methodHandleConstant)
    {
        String referenceKind = p.expectWord("reference kind");
        int refKind;
        RefConstant refConstant;
        switch (referenceKind)
        {
            case AssemblyConstants.REF_GET_FIELD:          refKind = MethodHandleConstant.REF_GET_FIELD;         refConstant = new FieldrefConstant();           break;
            case AssemblyConstants.REF_GET_STATIC:         refKind = MethodHandleConstant.REF_GET_STATIC;        refConstant = new FieldrefConstant();           break;
            case AssemblyConstants.REF_PUT_FIELD:          refKind = MethodHandleConstant.REF_PUT_FIELD;         refConstant = new FieldrefConstant();           break;
            case AssemblyConstants.REF_PUT_STATIC:         refKind = MethodHandleConstant.REF_PUT_STATIC;        refConstant = new FieldrefConstant();           break;
            case AssemblyConstants.REF_INVOKE_VIRTUAL:     refKind = MethodHandleConstant.REF_INVOKE_VIRTUAL;    refConstant = new MethodrefConstant();          break;
            case AssemblyConstants.REF_INVOKE_STATIC:      refKind = MethodHandleConstant.REF_INVOKE_STATIC;     refConstant = new MethodrefConstant();          break;
            case AssemblyConstants.REF_INVOKE_SPECIAL:     refKind = MethodHandleConstant.REF_INVOKE_SPECIAL;    refConstant = new MethodrefConstant();          break;
            case AssemblyConstants.REF_NEW_INVOKE_SPECIAL: refKind = MethodHandleConstant.REF_NEW_INVOKE_SPECIAL; refConstant = new MethodrefConstant();          break;
            case AssemblyConstants.REF_INVOKE_INTERFACE:   refKind = MethodHandleConstant.REF_INVOKE_INTERFACE;  refConstant = new InterfaceMethodrefConstant(); break;
            default:                                       throw new ParseException("Unknown reference kind " + referenceKind + ".", p.lineno());
        }

        refConstant.accept(clazz, this);
        index = cpe.addMethodHandleConstant(refKind, index);
    }


    public void visitModuleConstant(Clazz clazz, ModuleConstant moduleConstant)
    {
        // Modules are not encoded in internal form like class and interface
        // names...
        index = cpe.addModuleConstant(p.expectWord("module name"));
    }


    public void visitPackageConstant(Clazz clazz, PackageConstant packageConstant)
    {
        index = cpe.addPackageConstant(ClassUtil.externalPackageName(p.expectWord("package name")));
    }


    public void visitFieldrefConstant(Clazz clazz, FieldrefConstant fieldrefConstant)
    {
        int classIndex;
        if (p.nextTtypeEquals(AssemblyConstants.REFERENCE_SEPARATOR))
        {
            classIndex = ((ProgramClass) clazz).u2thisClass;
        }
        else
        {
            new ClassConstant().accept(clazz, this);
            classIndex = index;
            p.expect(AssemblyConstants.REFERENCE_SEPARATOR, "fieldref separator");
        }

        String type = p.expectType("fieldref type");
        String name = p.expectWord("fieldref name");
        index = cpe.addFieldrefConstant(classIndex, name, type, null, null);
    }


    public void visitMethodrefConstant(Clazz clazz, MethodrefConstant methodrefConstant)
    {
        int classIndex;
        if (p.nextTtypeEquals(AssemblyConstants.REFERENCE_SEPARATOR))
        {
            classIndex = ((ProgramClass) clazz).u2thisClass;
        }
        else
        {
            new ClassConstant().accept(clazz, this);
            classIndex = index;
            p.expect(AssemblyConstants.REFERENCE_SEPARATOR, "methodref separator");
        }

        String returnType = p.expectType("methodref return type");
        String name       = p.expectMethodName("methodref name");
        String methodArgs = p.expectMethodArguments("methodref arguments");
        index = cpe.addMethodrefConstant(classIndex, name, methodArgs + returnType, null, null);
    }


    public void visitInterfaceMethodrefConstant(Clazz clazz, InterfaceMethodrefConstant interfaceMethodrefConstant)
    {
        int classIndex;
        if (p.nextTtypeEquals(AssemblyConstants.REFERENCE_SEPARATOR))
        {
            classIndex = ((ProgramClass) clazz).u2thisClass;
        }
        else
        {
            new ClassConstant().accept(clazz, this);
            classIndex = index;
            p.expect(AssemblyConstants.REFERENCE_SEPARATOR, "interfacemethodref separator");
        }

        String returnType = p.expectType("interfacemethodref return type");
        String name       = p.expectMethodName("interfacemethodref name");
        String methodArgs = p.expectMethodArguments("interfacemethodref arguments");
        index = cpe.addInterfaceMethodrefConstant(classIndex, name, methodArgs + returnType, null, null);
    }


    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        index = cpe.addClassConstant(ClassUtil.internalClassTypeFromType(p.expectType("class name")), null);
    }


    public void visitMethodTypeConstant(Clazz clazz, MethodTypeConstant methodTypeConstant)
    {
        String returnType = p.expectType("method return type");
        String methodArgs = p.expectMethodArguments("method arguments");
        index = cpe.addMethodTypeConstant(methodArgs + returnType, null);
    }


    public void visitNameAndTypeConstant(Clazz clazz, NameAndTypeConstant nameAndTypeConstant)
    {
        throw new ParseException("Unsupported operation", p.lineno());
    }
}
