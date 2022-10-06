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
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.editor.AttributesEditor;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.*;

import java.util.*;

/**
 * Prints the Instructions, ExceptionInfos and LineNumberInfos of a Code
 * attribute.
 *
 * @author Joachim Vandersmissen
 */
public class InstructionsPrinter
implements   AttributeVisitor,
             ExceptionInfoVisitor,
             LineNumberInfoVisitor,
             InstructionVisitor
{
    private final Printer p;


    /**
     * Constructs a new InstructionsPrinter that uses a Printer.
     *
     * @param p the Printer to use to print basic structures.
     */
    public InstructionsPrinter(Printer p)
    {
        this.p = p;
    }


    // Implementations for AttributeVisitor.

    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        AttributesEditor attributesEditor =
            new AttributesEditor((ProgramClass) clazz,
                                 (ProgramMethod) method,
                                 codeAttribute,
                                 false);

        LineNumberTableAttribute lineNumberTableAttribute               =
            (LineNumberTableAttribute) attributesEditor.findAttribute(Attribute.LINE_NUMBER_TABLE);
        LocalVariableTableAttribute localVariableTableAttribute         =
            (LocalVariableTableAttribute) attributesEditor.findAttribute(Attribute.LOCAL_VARIABLE_TABLE);
        LocalVariableTypeTableAttribute localVariableTypeTableAttribute =
            (LocalVariableTypeTableAttribute) attributesEditor.findAttribute(Attribute.LOCAL_VARIABLE_TYPE_TABLE);

        p.printSpace();
        p.print(AssemblyConstants.BODY_OPEN);
        if (codeAttribute.u4codeLength > 0)
        {
            p.println();
            p.indent();
            p.indent();

            int offset = 0;

            // Inclusive because we want LocalVariableInfo and
            // LocalVariableTypeInfo lengths to work properly.
            while (offset <= codeAttribute.u4codeLength)
            {
                if (p.labels.containsKey(offset))
                {
                    p.outdent();
                    p.printIndent();
                    p.printOffset(offset);
                    p.print(AssemblyConstants.COLON);
                    p.println();
                    p.indent();
                }

                if (lineNumberTableAttribute != null)
                {
                    List<LineNumberInfo> lineNumberInfos =
                        findAtOffset(lineNumberTableAttribute, offset);
                    for (int index = 0; index < lineNumberInfos.size(); index++)
                    {
                        p.outdent();
                        visitLineNumberInfo(clazz, method, codeAttribute, lineNumberInfos.get(index));
                        p.indent();
                    }
                }

                if (localVariableTableAttribute != null)
                {
                    List<LocalVariableInfo> startingAt =
                        findStartingAtOffset(localVariableTableAttribute, offset);
                    for (int index = 0; index < startingAt.size(); index++)
                    {
                        LocalVariableInfo localVariableInfo =
                            startingAt.get(index);
                        p.outdent();
                        p.printIndent();
                        p.printWord(AssemblyConstants.LOCAL_VAR_START);
                        p.printSpace();
                        p.printNumber(localVariableInfo.u2index);
                        p.printSpace();
                        p.printType(localVariableInfo.getDescriptor(clazz));
                        p.printSpace();
                        p.printWord(localVariableInfo.getName(clazz));
                        p.println();
                        p.indent();
                    }

                    List<LocalVariableInfo> endingAt =
                        findEndingAtOffset(localVariableTableAttribute, offset);
                    for (int index = 0; index < endingAt.size(); index++)
                    {
                        LocalVariableInfo localVariableInfo =
                            endingAt.get(index);
                        p.outdent();
                        p.printIndent();
                        p.printWord(AssemblyConstants.LOCAL_VAR_END);
                        p.printSpace();
                        p.printNumber(localVariableInfo.u2index);
                        p.println();
                        p.indent();
                    }
                }

                if (localVariableTypeTableAttribute != null)
                {
                    List<LocalVariableTypeInfo> startingAt =
                        findStartingAtOffset(localVariableTypeTableAttribute, offset);
                    for (int index = 0; index < startingAt.size(); index++)
                    {
                        LocalVariableTypeInfo localVariableTypeInfo =
                            startingAt.get(index);
                        p.outdent();
                        p.printIndent();
                        p.printWord(AssemblyConstants.LOCAL_VAR_TYPE_START);
                        p.printSpace();
                        p.printNumber(localVariableTypeInfo.u2index);
                        p.printSpace();
                        p.printString(localVariableTypeInfo.getSignature(clazz));
                        p.printSpace();
                        p.printWord(localVariableTypeInfo.getName(clazz));
                        p.println();
                        p.indent();
                    }

                    List<LocalVariableTypeInfo> endingAt =
                        findEndingAtOffset(localVariableTypeTableAttribute, offset);
                    for (int index = 0; index < endingAt.size(); index++)
                    {
                        LocalVariableTypeInfo localVariableTypeInfo =
                            endingAt.get(index);
                        p.outdent();
                        p.printIndent();
                        p.printWord(AssemblyConstants.LOCAL_VAR_TYPE_END);
                        p.printSpace();
                        p.printNumber(localVariableTypeInfo.u2index);
                        p.println();
                        p.indent();
                    }
                }

                List<ExceptionInfo> handlerAt =
                    findHandlerAtOffset(codeAttribute, offset);
                for (int index = 0; index < handlerAt.size(); index++)
                {
                    p.outdent();
                    p.printIndent();
                    p.printWord(AssemblyConstants.CATCH);
                    visitExceptionInfo(clazz, method, codeAttribute, handlerAt.get(index));
                    p.println();
                    p.indent();
                }

                if (offset >= codeAttribute.u4codeLength)
                {
                    break;
                }

                Instruction instruction =
                    InstructionFactory.create(codeAttribute.code, offset);
                p.printIndent();
                p.printWord(instruction.getName());
                instruction.accept(clazz, method, codeAttribute, offset, this);
                offset += instruction.length(offset);
                p.println();
            }

            p.outdent();
            p.outdent();
            p.printIndent();
        }

        p.print(AssemblyConstants.BODY_CLOSE);
    }


    // Implementations for InstructionVisitor.

    public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
    {
        if (simpleInstruction.opcode == Instruction.OP_BIPUSH ||
            simpleInstruction.opcode == Instruction.OP_SIPUSH)
        {
            p.printSpace();
            p.printNumber(simpleInstruction.constant);
        }

        if (simpleInstruction.opcode == Instruction.OP_NEWARRAY)
        {
            p.printSpace();
            p.printType(String.valueOf(InstructionUtil.internalTypeFromArrayType((byte) simpleInstruction.constant)));
        }
    }


    public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
    {
        if (variableInstruction.wide)
        {
            p.printWord("_w");
        }

        if (variableInstruction.opcode >= Instruction.OP_ILOAD  &&
            variableInstruction.opcode <= Instruction.OP_ALOAD  ||
            variableInstruction.opcode >= Instruction.OP_ISTORE &&
            variableInstruction.opcode <= Instruction.OP_ASTORE ||
            variableInstruction.opcode == Instruction.OP_IINC   ||
            variableInstruction.opcode == Instruction.OP_RET)
        {
            p.printSpace();
            p.printNumber(variableInstruction.variableIndex);
        }

        if (variableInstruction.opcode == Instruction.OP_IINC)
        {
            p.printSpace();
            p.printNumber(variableInstruction.constant);
        }
    }


    public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
    {
        p.printSpace();
        clazz.constantPoolEntryAccept(constantInstruction.constantIndex, new ConstantPrinter(p, true));
        if (constantInstruction.opcode == Instruction.OP_MULTIANEWARRAY)
        {
            p.printSpace();
            p.printNumber(constantInstruction.constant);
        }
    }


    public void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction)
    {
        p.printSpace();
        p.printOffset(offset + branchInstruction.branchOffset);
    }


    public void visitTableSwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, TableSwitchInstruction tableSwitchInstruction)
    {
        p.printSpace();
        p.print(AssemblyConstants.BODY_OPEN);
        p.println();
        p.indent();
        for (int index = 0; index < tableSwitchInstruction.highCase - tableSwitchInstruction.lowCase + 1; index++)
        {
            p.printIndent();
            p.printWord(AssemblyConstants.CASE);
            p.printSpace();
            p.printNumber(tableSwitchInstruction.lowCase + index);
            p.print(AssemblyConstants.COLON);
            p.printSpace();
            p.printOffset(offset + tableSwitchInstruction.jumpOffsets[index]);
            p.println();
        }

        p.printIndent();
        p.printWord(AssemblyConstants.DEFAULT);
        p.print(AssemblyConstants.COLON);
        p.printSpace();
        p.printOffset(offset + tableSwitchInstruction.defaultOffset);
        p.println();
        p.outdent();
        p.printIndent();
        p.print(AssemblyConstants.BODY_CLOSE);
    }


    public void visitLookUpSwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, LookUpSwitchInstruction lookUpSwitchInstruction)
    {
        p.printSpace();
        p.print(AssemblyConstants.BODY_OPEN);
        p.println();
        p.indent();
        for (int index = 0; index < lookUpSwitchInstruction.cases.length; index++)
        {
            p.printIndent();
            p.printWord(AssemblyConstants.CASE);
            p.printSpace();
            p.printNumber(lookUpSwitchInstruction.cases[index]);
            p.print(AssemblyConstants.COLON);
            p.printSpace();
            p.printOffset(offset + lookUpSwitchInstruction.jumpOffsets[index]);
            p.println();
        }

        p.printIndent();
        p.printWord(AssemblyConstants.DEFAULT);
        p.print(AssemblyConstants.COLON);
        p.printSpace();
        p.printOffset(offset + lookUpSwitchInstruction.defaultOffset);
        p.println();
        p.outdent();
        p.printIndent();
        p.print(AssemblyConstants.BODY_CLOSE);
    }


    // Implementations for ExceptionInfoVisitor.

    public void visitExceptionInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, ExceptionInfo exceptionInfo)
    {
        p.printSpace();

        // If u2catchType = 0, the catch block is a finally block.
        if (exceptionInfo.u2catchType == 0)
        {
            p.printWord(AssemblyConstants.ANY);
        }
        else
        {
            p.printType(ClassUtil.internalTypeFromClassType(clazz.getClassName(exceptionInfo.u2catchType)));
        }

        p.printSpace();
        p.printOffset(exceptionInfo.u2startPC);
        p.printSpace();
        p.printOffset(exceptionInfo.u2endPC);
    }


    // Implementations for LineNumberInfoVisitor.

    public void visitLineNumberInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberInfo lineNumberInfo)
    {
        p.printIndent();
        p.printWord(AssemblyConstants.LINE);
        p.printSpace();
        p.printNumber(lineNumberInfo.u2lineNumber);
        p.println();
    }


    // Small utility methods.

    /**
     * Returns all LineNumberInfos at an offset in the bytecode.
     *
     * @param lineNumberTableAttribute the LineNumberTableAttribute to search in.
     * @param offset                   the bytecode offset to search for.
     * @return the LineNumberInfos at the offset.
     */
    private List<LineNumberInfo> findAtOffset(LineNumberTableAttribute lineNumberTableAttribute, int offset)
    {
        List<LineNumberInfo> lineNumberInfos =
            new ArrayList<>(lineNumberTableAttribute.u2lineNumberTableLength);
        for (int index = 0; index < lineNumberTableAttribute.u2lineNumberTableLength; index++)
        {
            LineNumberInfo lineNumberInfo =
                lineNumberTableAttribute.lineNumberTable[index];
            if (lineNumberInfo.u2startPC == offset)
            {
                lineNumberInfos.add(lineNumberInfo);
            }
        }

        return lineNumberInfos;
    }


    /**
     * Returns the ExceptionInfos whose handler are at an offset in the bytecode.
     *
     * @param codeAttribute the code attribute to search in.
     * @param offset        the bytecode offset to search for.
     * @return the ExceptionInfos whose handler are at the offset.
     */
    private List<ExceptionInfo> findHandlerAtOffset(CodeAttribute codeAttribute, int offset)
    {
        List<ExceptionInfo> handlerAt =
            new ArrayList<>(codeAttribute.u2exceptionTableLength);
        for (int index = 0; index < codeAttribute.u2exceptionTableLength; index++)
        {
            ExceptionInfo exceptionInfo =
                codeAttribute.exceptionTable[index];
            if (exceptionInfo.u2handlerPC == offset)
            {
                handlerAt.add(exceptionInfo);
            }
        }

        return handlerAt;
    }


    /**
     * Returns all LocalVariableInfos starting at an offset in the bytecode.
     *
     * @param localVariableTableAttribute the LocalVariableTableAttribute to
     *                                    search in.
     * @param offset                      the bytecode offset to search for.
     * @return the LocalVariableInfos starting at the offset.
     */
    private List<LocalVariableInfo> findStartingAtOffset(LocalVariableTableAttribute localVariableTableAttribute, int offset)
    {
        List<LocalVariableInfo> startingAt =
            new ArrayList<>(localVariableTableAttribute.u2localVariableTableLength);
        for (int index = 0; index < localVariableTableAttribute.u2localVariableTableLength; index++)
        {
            LocalVariableInfo localVariableInfo =
                localVariableTableAttribute.localVariableTable[index];
            if (localVariableInfo.u2startPC == offset)
            {
                startingAt.add(localVariableInfo);
            }
        }

        return startingAt;
    }


    /**
     * Returns all LocalVariableInfos ending at an offset in the bytecode.
     *
     * @param localVariableTableAttribute the LocalVariableTableAttribute to
     *                                    search in.
     * @param offset                      the bytecode offset to search for.
     * @return the LocalVariableInfos ending at the offset.
     */
    private List<LocalVariableInfo> findEndingAtOffset(LocalVariableTableAttribute localVariableTableAttribute, int offset)
    {
        List<LocalVariableInfo> endingAt =
            new ArrayList<>(localVariableTableAttribute.u2localVariableTableLength);
        for (int index = 0; index < localVariableTableAttribute.u2localVariableTableLength; index++)
        {
            LocalVariableInfo localVariableInfo =
                localVariableTableAttribute.localVariableTable[index];
            if (localVariableInfo.u2startPC + localVariableInfo.u2length == offset)
            {
                endingAt.add(localVariableInfo);
            }
        }

        return endingAt;
    }


    /**
     * Returns all LocalVariableTypeInfos starting at an offset in the bytecode.
     *
     * @param localVariableTypeTableAttribute the LocalVariableTypeTableAttribute
     *                                        to search in.
     * @param offset                          the bytecode offset to search for.
     * @return the LocalVariableTypeInfos starting at the offset.
     */
    private List<LocalVariableTypeInfo> findStartingAtOffset(LocalVariableTypeTableAttribute localVariableTypeTableAttribute, int offset)
    {
        List<LocalVariableTypeInfo> startingAt =
            new ArrayList<>(localVariableTypeTableAttribute.u2localVariableTypeTableLength);
        for (int index = 0; index < localVariableTypeTableAttribute.u2localVariableTypeTableLength; index++)
        {
            LocalVariableTypeInfo localVariableTypeInfo =
                localVariableTypeTableAttribute.localVariableTypeTable[index];
            if (localVariableTypeInfo.u2startPC == offset)
            {
                startingAt.add(localVariableTypeInfo);
            }
        }

        return startingAt;
    }


    /**
     * Returns all LocalVariableTypeInfos ending at an offset in the bytecode.
     *
     * @param localVariableTypeTableAttribute the LocalVariableTypeTableAttribute
     *                                        to search in.
     * @param offset                          the bytecode offset to search for.
     * @return the LocalVariableTypeInfos ending at the offset.
     */
    private List<LocalVariableTypeInfo> findEndingAtOffset(LocalVariableTypeTableAttribute localVariableTypeTableAttribute, int offset)
    {
        List<LocalVariableTypeInfo> endingAt =
            new ArrayList<>(localVariableTypeTableAttribute.u2localVariableTypeTableLength);
        for (int index = 0; index < localVariableTypeTableAttribute.u2localVariableTypeTableLength; index++)
        {
            LocalVariableTypeInfo localVariableTypeInfo =
                localVariableTypeTableAttribute.localVariableTypeTable[index];
            if (localVariableTypeInfo.u2startPC + localVariableTypeInfo.u2length == offset)
            {
                endingAt.add(localVariableTypeInfo);
            }
        }

        return endingAt;
    }
}
