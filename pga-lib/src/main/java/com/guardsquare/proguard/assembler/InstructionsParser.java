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
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.editor.*;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.*;
import proguard.classfile.visitor.MemberVisitor;
import proguard.util.ArrayUtil;

import java.util.*;


/**
 * Parses the Instructions, ExceptionInfos and LineNumberInfos of a Code
 * attribute.
 *
 * @author Joachim Vandersmissen
 */
public class InstructionsParser
implements   MemberVisitor,
             AttributeVisitor,
             ExceptionInfoVisitor,
             InstructionVisitor
{
    private final Parser                p;
    private final ConstantPoolEditor    cpe;
    private final CodeAttributeComposer cac;
    private final ConstantParser        cp;


    /**
     * Constructs a new InstructionsParser that uses a Parser, a
     * ConstantPoolEditor, a CodeAttributeComposer, and program and library
     * ClassPools.
     *
     * @param p   the Parser to use to parse basic structures.
     * @param cpe the ConstantPoolEditor to use to add Constants to the constant
     *            pool.
     * @param cac the CodeAttributeComposer to use to add Instructions and
     *            ExceptionInfos to the Code attribute.
     */
    public InstructionsParser(Parser                p,
                              ConstantPoolEditor    cpe,
                              CodeAttributeComposer cac)
    {
        this.p   = p;
        this.cpe = cpe;
        this.cac = cac;
        this.cp  = new ConstantParser(p, cpe);
    }


    // Implementations for MemberVisitor.

    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        Map<String, Integer> oldLabels = p.labels;
        p.labels = new HashMap<>();

        CodeAttribute codeAttribute =
            new CodeAttribute(cpe.addUtf8Constant(Attribute.CODE));
        try
        {
            codeAttribute.accept(programClass, programMethod, this);
            codeAttribute.accept(programClass, programMethod, cac);
        }
        catch (Exception e)
        {
            throw new ParseException("An exception occured while parsing " +
                                     programMethod.getName(programClass) +
                                     '(' +
                                     ClassUtil.externalMethodArguments(programMethod.getDescriptor(programClass)) +
                                     ')',
                                     p.lineno(),
                                     e);
        }

        new AttributesEditor(programClass, programMethod, false).addAttribute(codeAttribute);

        p.labels = oldLabels;
    }


    // Implementations for AttributeVisitor.

    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        codeAttribute.code           = new byte[0];
        codeAttribute.exceptionTable = new ExceptionInfo[0];

        LocalVariableTableAttribute localVariableTableAttribute         =
            new LocalVariableTableAttribute();
        LocalVariableTypeTableAttribute localVariableTypeTableAttribute =
            new LocalVariableTypeTableAttribute();

        localVariableTableAttribute.u2attributeNameIndex       =
            cpe.addUtf8Constant(Attribute.LOCAL_VARIABLE_TABLE);
        localVariableTableAttribute.localVariableTable         =
            new LocalVariableInfo[0];
        localVariableTypeTableAttribute.u2attributeNameIndex   =
            cpe.addUtf8Constant(Attribute.LOCAL_VARIABLE_TYPE_TABLE);
        localVariableTypeTableAttribute.localVariableTypeTable =
            new LocalVariableTypeInfo[0];

        Map<Integer, LocalVariableInfo> localVariableInfoByIndex         =
            new HashMap<>();
        Map<Integer, LocalVariableTypeInfo> localVariableTypeInfoByIndex =
            new HashMap<>();

        cac.beginCodeFragment(65534);
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            Instruction instruction = expectInstruction();
            if (instruction == null)
            {
                if (AssemblyConstants.LOCAL_VAR_START.equals(p.sval))
                {
                    int index = (int) p.expectNumber("local variable index");
                    LocalVariableInfo localVariableInfo =
                        new LocalVariableInfo();

                    localVariableInfo.u2startPC         = createSyntheticLabel();
                    localVariableInfo.u2descriptorIndex =
                        cpe.addUtf8Constant(p.expectType("local variable descriptor"));
                    localVariableInfo.u2nameIndex       =
                        cpe.addUtf8Constant(p.expectWord("local variable name"));
                    localVariableInfo.u2index           = index;

                    localVariableInfoByIndex.put(index, localVariableInfo);
                    localVariableTableAttribute.localVariableTable =
                        ArrayUtil.add(localVariableTableAttribute.localVariableTable,
                                      localVariableTableAttribute.u2localVariableTableLength++,
                                      localVariableInfo);
                }
                else if (AssemblyConstants.LOCAL_VAR_END.equals(p.sval))
                {
                    int index = (int) p.expectNumber("local variable index");
                    if (!localVariableInfoByIndex.containsKey(index))
                    {
                        throw new ParseException("Local var end without associated start (" + index + ").", p.lineno());
                    }

                    LocalVariableInfo localVariableInfo =
                        localVariableInfoByIndex.get(index);

                    localVariableInfo.u2length =
                        createSyntheticLabel() - localVariableInfo.u2startPC;
                }
                else if (AssemblyConstants.LOCAL_VAR_TYPE_START.equals(p.sval))
                {
                    int index = (int) p.expectNumber("local variable type index");
                    LocalVariableTypeInfo localVariableTypeInfo =
                        new LocalVariableTypeInfo();

                    localVariableTypeInfo.u2startPC        = createSyntheticLabel();
                    localVariableTypeInfo.u2signatureIndex =
                        cpe.addUtf8Constant(p.expectString("local variable type signature"));
                    localVariableTypeInfo.u2nameIndex      =
                        cpe.addUtf8Constant(p.expectWord("local variable type name"));
                    localVariableTypeInfo.u2index          = index;

                    localVariableTypeInfoByIndex.put(index, localVariableTypeInfo);
                    localVariableTypeTableAttribute.localVariableTypeTable =
                        ArrayUtil.add(localVariableTypeTableAttribute.localVariableTypeTable,
                                      localVariableTypeTableAttribute.u2localVariableTypeTableLength++,
                                      localVariableTypeInfo);
                }
                else if (AssemblyConstants.LOCAL_VAR_TYPE_END.equals(p.sval))
                {
                    int index = (int) p.expectNumber("local variable type index");
                    if (!localVariableTypeInfoByIndex.containsKey(index))
                    {
                        throw new ParseException("Local var type end without associated start (" + index + ").", p.lineno());
                    }

                    LocalVariableTypeInfo localVariableTypeInfo =
                        localVariableTypeInfoByIndex.get(index);

                    localVariableTypeInfo.u2length =
                        createSyntheticLabel() - localVariableTypeInfo.u2startPC;
                }
                else if (AssemblyConstants.CATCH.equals(p.sval))
                {
                    ExceptionInfo exceptionInfo = new ExceptionInfo();
                    visitExceptionInfo(clazz, method, codeAttribute, exceptionInfo);
                    exceptionInfo.u2handlerPC = createSyntheticLabel();
                    cac.appendException(exceptionInfo);
                }
                else if (AssemblyConstants.LINE.equals(p.sval))
                {
                    LineNumberInfo lineNumberInfo = new LineNumberInfo();
                    lineNumberInfo.u2startPC    = createSyntheticLabel();
                    lineNumberInfo.u2lineNumber = (int) p.expectNumber("line number");
                    cac.insertLineNumber(lineNumberInfo);
                }
                else
                {
                    p.pushBack();
                    cac.appendLabel(p.expectOffset("label"));
                    p.expect(AssemblyConstants.COLON, "label colon");
                }
            }
            else
            {
                instruction.accept(clazz, method, codeAttribute, 0, this);
                cac.appendInstruction(0, instruction);
            }
        }

        cac.endCodeFragment();

        AttributesEditor attributesEditor =
            new AttributesEditor((ProgramClass) clazz,
                                 (ProgramMethod) method,
                                 codeAttribute,
                                 false);
        if (localVariableTableAttribute.u2localVariableTableLength > 0)
        {
            attributesEditor.addAttribute(localVariableTableAttribute);
        }

        if (localVariableTypeTableAttribute.u2localVariableTypeTableLength > 0)
        {
            attributesEditor.addAttribute(localVariableTypeTableAttribute);
        }
    }


    // Implementations for InstructionVisitor.

    public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
    {
        if (simpleInstruction.opcode == Instruction.OP_BIPUSH ||
            simpleInstruction.opcode == Instruction.OP_SIPUSH)
        {
            simpleInstruction.constant = (int) p.expectNumber("push constant");
        }

        if (simpleInstruction.opcode == Instruction.OP_NEWARRAY)
        {
            simpleInstruction.constant =
                InstructionUtil.arrayTypeFromInternalType(p.expectType("newarray type").charAt(0));
        }
    }


    public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
    {
        if (variableInstruction.opcode >= Instruction.OP_ILOAD  &&
            variableInstruction.opcode <= Instruction.OP_ALOAD  ||
            variableInstruction.opcode >= Instruction.OP_ISTORE &&
            variableInstruction.opcode <= Instruction.OP_ASTORE ||
            variableInstruction.opcode == Instruction.OP_IINC   ||
            variableInstruction.opcode == Instruction.OP_RET)
        {
            variableInstruction.variableIndex =
                (int) p.expectNumber("variable index");
        }

        if (variableInstruction.opcode == Instruction.OP_IINC)
        {
            variableInstruction.constant =
                (int) p.expectNumber("iinc constant");
        }
    }


    public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
    {
        if (constantInstruction.opcode == Instruction.OP_LDC   ||
            constantInstruction.opcode == Instruction.OP_LDC_W ||
            constantInstruction.opcode == Instruction.OP_LDC2_W)
        {
            constantInstruction.constantIndex =
                p.expectLoadableConstant((ProgramClass) clazz, cpe, cp);
        }

        if (constantInstruction.opcode == Instruction.OP_GETSTATIC ||
            constantInstruction.opcode == Instruction.OP_PUTSTATIC ||
            constantInstruction.opcode == Instruction.OP_GETFIELD  ||
            constantInstruction.opcode == Instruction.OP_PUTFIELD)
        {
            cp.visitFieldrefConstant(clazz, null);
            constantInstruction.constantIndex = cp.getIndex();
        }

        if (constantInstruction.opcode == Instruction.OP_INVOKEVIRTUAL ||
            constantInstruction.opcode == Instruction.OP_INVOKESPECIAL ||
            constantInstruction.opcode == Instruction.OP_INVOKESTATIC)
        {
            cp.visitMethodrefConstant(clazz, null);
            constantInstruction.constantIndex = cp.getIndex();
        }

        if (constantInstruction.opcode == Instruction.OP_INVOKEINTERFACE)
        {
            cp.visitInterfaceMethodrefConstant(clazz, null);
            constantInstruction.constantIndex = cp.getIndex();
            constantInstruction.constant =
                (ClassUtil.internalMethodParameterSize(clazz.getRefType(cp.getIndex())) + 1) << 8;
        }

        if (constantInstruction.opcode == Instruction.OP_INVOKEDYNAMIC)
        {
            cp.visitInvokeDynamicConstant(clazz, null);
            constantInstruction.constantIndex = cp.getIndex();
        }

        if (constantInstruction.opcode == Instruction.OP_NEW       ||
            constantInstruction.opcode == Instruction.OP_ANEWARRAY ||
            constantInstruction.opcode == Instruction.OP_CHECKCAST ||
            constantInstruction.opcode == Instruction.OP_INSTANCEOF)
        {
            cp.visitClassConstant(clazz, null);
            constantInstruction.constantIndex = cp.getIndex();
        }

        if (constantInstruction.opcode == Instruction.OP_MULTIANEWARRAY)
        {
            cp.visitClassConstant(clazz, null);
            constantInstruction.constantIndex = cp.getIndex();
            constantInstruction.constant =
                (int) p.expectNumber("array dimensions");
        }
    }


    public void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction)
    {
        branchInstruction.branchOffset = p.expectOffset("branch label");
    }


    public void visitTableSwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, TableSwitchInstruction tableSwitchInstruction)
    {
        tableSwitchInstruction.lowCase = Integer.MAX_VALUE;
        tableSwitchInstruction.highCase = Integer.MIN_VALUE;
        tableSwitchInstruction.jumpOffsets = new int[0];

        p.expect(AssemblyConstants.BODY_OPEN, "tableswitch open");
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            String keyword = p.expect(AssemblyConstants.CASE,
                                      AssemblyConstants.DEFAULT);
            if (AssemblyConstants.CASE.equals(keyword))
            {
                int number = (int) p.expectNumber("case number");
                if (tableSwitchInstruction.lowCase  == Integer.MAX_VALUE &&
                    tableSwitchInstruction.highCase == Integer.MIN_VALUE)
                {
                    tableSwitchInstruction.lowCase = number;
                }
                else if (number - tableSwitchInstruction.highCase != 1)
                {
                    throw new ParseException("Tableswitch cases should be incremental.", p.lineno());
                }

                tableSwitchInstruction.highCase = number;

                p.expect(AssemblyConstants.COLON, "table switch case colon");
                int label = p.expectOffset("table switch case label");

                tableSwitchInstruction.jumpOffsets =
                    ArrayUtil.add(tableSwitchInstruction.jumpOffsets,
                                  tableSwitchInstruction.jumpOffsets.length,
                                  label);
            }
            else if (AssemblyConstants.DEFAULT.equals(keyword))
            {
                p.expect(AssemblyConstants.COLON, "table switch default colon");
                tableSwitchInstruction.defaultOffset =
                    p.expectOffset("table switch default label");
            }
        }
    }


    public void visitLookUpSwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, LookUpSwitchInstruction lookUpSwitchInstruction)
    {
        lookUpSwitchInstruction.cases       = new int[0];
        lookUpSwitchInstruction.jumpOffsets = new int[0];

        int previousNumber = Integer.MIN_VALUE;
        p.expect(AssemblyConstants.BODY_OPEN, "lookupswitch open");
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            String keyword = p.expect(AssemblyConstants.CASE,
                                      AssemblyConstants.DEFAULT);
            if (AssemblyConstants.CASE.equals(keyword))
            {
                int number = (int) p.expectNumber("case number");
                if (number <= previousNumber)
                {
                    throw new ParseException("Lookupswitch cases should be strictly increasing.", p.lineno());
                }

                previousNumber = number;

                p.expect(AssemblyConstants.COLON, "lookup switch case colon");
                int label = p.expectOffset("lookup switch case label");

                lookUpSwitchInstruction.cases       =
                    ArrayUtil.add(lookUpSwitchInstruction.cases,
                                  lookUpSwitchInstruction.cases.length,
                                  number);
                lookUpSwitchInstruction.jumpOffsets =
                    ArrayUtil.add(lookUpSwitchInstruction.jumpOffsets,
                                  lookUpSwitchInstruction.jumpOffsets.length,
                                  label);
            }
            else if (AssemblyConstants.DEFAULT.equals(keyword))
            {
                p.expect(AssemblyConstants.COLON, "lookup switch default colon");
                lookUpSwitchInstruction.defaultOffset =
                    p.expectOffset("lookupswitch switch default label");
            }
        }
    }


    // Implementations for ExceptionInfoVisitor.

    public void visitExceptionInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, ExceptionInfo exceptionInfo)
    {
        String classType = ClassUtil.internalClassTypeFromType(p.expectType("catch type"));

        // If u2catchType = 0, the catch block is a finally block.
        if (!AssemblyConstants.ANY.equals(classType))
        {
            exceptionInfo.u2catchType = cpe.addClassConstant(classType, null);
        }

        exceptionInfo.u2startPC = p.expectOffset("exception from label");
        exceptionInfo.u2endPC   = p.expectOffset("exception to label");
    }


    // Small utility methods.

    /**
     * Creates a synthetic label.
     *
     * @return The offset of the synthetic label.
     */
    private int createSyntheticLabel()
    {
        int offset = p.labels.size() + 1;
        p.labels.put("$" + offset, offset);
        cac.appendLabel(offset);
        return offset;
    }


    /**
     * Parses an Instruction. This method advances the Parser.
     *
     * @return the Instruction, or null if no Instruction was matched.
     */
    private Instruction expectInstruction() throws ParseException
    {
        String opcode = p.expectWord("instruction opcode");
        for (int index = 0; index < Instruction.NAMES.length; index++)
        {
            if (Instruction.NAMES[index].equals(opcode))
            {
                Instruction instruction = InstructionFactory.create((byte) index, false);
                instruction.opcode = (byte) index;
                return instruction;
            }
        }

        // Syntactic sugar: _w format for wide opcodes.
        if (opcode.endsWith("_w"))
        {
            opcode = opcode.substring(0, opcode.length() - 2);
            for (int index = 0; index < Instruction.NAMES.length; index++)
            {
                if (Instruction.NAMES[index].equals(opcode))
                {
                    Instruction instruction = InstructionFactory.create((byte)index, true);
                    instruction.opcode = (byte)index;
                    return instruction;
                }

            }
        }

        return null;
    }
}
