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
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.annotation.target.*;
import proguard.classfile.attribute.annotation.target.visitor.*;
import proguard.classfile.attribute.annotation.visitor.TypeAnnotationVisitor;
import proguard.classfile.attribute.preverification.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;

import java.util.Map;

/**
 * Visits relevant offsets in a CodeAttribute and transforms them to labels in
 * a map.
 *
 * @author Joachim Vandersmissen
 */
public class LabelsCollector
implements   AttributeVisitor,
             ExceptionInfoVisitor,
             TypeAnnotationVisitor,
             TargetInfoVisitor,
             LocalVariableTargetElementVisitor,
             InstructionVisitor
{
    private final Map<Integer, String> labels;


    /**
     * Constructs a new LabelsCollector.
     *
     * @param labels the map to collect the labels in.
     */
    public LabelsCollector(Map<Integer, String> labels)
    {
        this.labels = labels;
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        codeAttribute.instructionsAccept(clazz, method, this);
        codeAttribute.exceptionsAccept(clazz, method, this);
        codeAttribute.attributesAccept(clazz, method, this);
    }


    public void visitStackMapAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, StackMapAttribute stackMapAttribute)
    {
        // For now we don't look at StackMap attributes, as they aren't written
        // to the output anyway.
    }


    public void visitStackMapTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, StackMapTableAttribute stackMapTableAttribute)
    {
        // For now we don't look at StackMapTable attributes, as they aren't written
        // to the output anyway.
    }


    public void visitLineNumberTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberTableAttribute lineNumberTableAttribute)
    {
        // Line numbers are handled inline.
    }


    public void visitLocalVariableTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTableAttribute localVariableTableAttribute)
    {
        // Local variables are handled inline.
    }


    public void visitLocalVariableTypeTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeTableAttribute localVariableTypeTableAttribute)
    {
        // Local variable types are handled inline.
    }


    public void visitAnyTypeAnnotationsAttribute(Clazz clazz, TypeAnnotationsAttribute typeAnnotationsAttribute)
    {
        typeAnnotationsAttribute.typeAnnotationsAccept(clazz, this);
    }


    // Implementations for InstructionsVisitor.

    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction) {}


    public void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction)
    {
        collectOffset(offset + branchInstruction.branchOffset);
    }


    public void visitAnySwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SwitchInstruction switchInstruction)
    {
        collectOffset(offset + switchInstruction.defaultOffset);
        for (int index = 0; index < switchInstruction.jumpOffsets.length; index++)
        {
            collectOffset(offset + switchInstruction.jumpOffsets[index]);
        }
    }


    // Implementations for TypeAnnotationVisitor.

    public void visitTypeAnnotation(Clazz clazz, TypeAnnotation typeAnnotation)
    {
        typeAnnotation.targetInfoAccept(clazz, this);
    }


    // Implementations for TargetInfoVisitor.

    public void visitAnyTargetInfo(Clazz clazz, TypeAnnotation typeAnnotation, TargetInfo targetInfo) {}


    public void visitLocalVariableTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, LocalVariableTargetInfo localVariableTargetInfo)
    {
        localVariableTargetInfo.targetElementsAccept(clazz, method, codeAttribute, typeAnnotation, this);
    }


    public void visitOffsetTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, OffsetTargetInfo offsetTargetInfo)
    {
        collectOffset(offsetTargetInfo.u2offset);
    }


    public void visitTypeArgumentTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, TypeArgumentTargetInfo typeArgumentTargetInfo)
    {
        collectOffset(typeArgumentTargetInfo.u2offset);
    }


    // Implementations for LocalVariableTargetElementVisitor.

    public void visitLocalVariableTargetElement(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, LocalVariableTargetInfo localVariableTargetInfo, LocalVariableTargetElement localVariableTargetElement)
    {
        collectOffset(localVariableTargetElement.u2startPC);
        collectOffset(localVariableTargetElement.u2startPC + localVariableTargetElement.u2length);
    }


    // Implementations for ExceptionInfoVisitor.

    public void visitExceptionInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, ExceptionInfo exceptionInfo)
    {
        collectOffset(exceptionInfo.u2startPC);
        collectOffset(exceptionInfo.u2endPC);
        // Exception handlers are handled inline.
    }


    // Small utility methods.

    /**
     * Adds an offset to the labels map.
     *
     * @param offset the offset to add.
     */
    private void collectOffset(int offset)
    {
        if (!this.labels.containsKey(offset))
        {
            this.labels.put(offset, AssemblyConstants.LABEL + (labels.size() + 1));
        }
    }
}
