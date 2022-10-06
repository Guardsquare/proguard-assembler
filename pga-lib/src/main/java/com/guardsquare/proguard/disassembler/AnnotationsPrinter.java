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
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.annotation.target.*;
import proguard.classfile.attribute.annotation.target.visitor.*;
import proguard.classfile.attribute.annotation.visitor.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;

/**
 * Prints Annotations and TypeAnnotations.
 *
 * @author Joachim Vandersmissen
 */
public class AnnotationsPrinter
implements   AttributeVisitor,
             AnnotationVisitor,
             TypeAnnotationVisitor,
             TargetInfoVisitor,
             LocalVariableTargetElementVisitor,
             TypePathInfoVisitor,
             ElementValueVisitor
{
    private final Printer p;


    /**
     * Constructs a new AnnotationsPrinter that uses a Printer.
     *
     * @param p the Printer to use to print basic structures.
     */
    public AnnotationsPrinter(Printer p)
    {
        this.p = p;
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAnnotationsAttribute(Clazz clazz, AnnotationsAttribute annotationsAttribute)
    {
        p.printSpace();
        p.print(AssemblyConstants.BODY_OPEN);
        if (annotationsAttribute.u2annotationsCount > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < annotationsAttribute.u2annotationsCount; index++)
            {
                p.printIndent();
                visitAnnotation(clazz, annotationsAttribute.annotations[index]);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        p.print(AssemblyConstants.BODY_CLOSE);
    }


    public void visitAnyParameterAnnotationsAttribute(Clazz clazz, Method method, ParameterAnnotationsAttribute parameterAnnotationsAttribute)
    {
        p.printSpace();
        p.print(AssemblyConstants.BODY_OPEN);
        if (parameterAnnotationsAttribute.u1parametersCount > 0)
        {
            p.println();
            p.indent();
            for (int parameter = 0; parameter < parameterAnnotationsAttribute.u1parametersCount; parameter++)
            {
                p.printIndent();
                p.print(AssemblyConstants.BODY_OPEN);
                if (parameterAnnotationsAttribute.u2parameterAnnotationsCount[parameter] > 0)
                {
                    p.println();
                    p.indent();
                    for (int index = 0; index < parameterAnnotationsAttribute.u2parameterAnnotationsCount[parameter]; index++)
                    {
                        p.printIndent();
                        visitAnnotation(clazz, parameterAnnotationsAttribute.parameterAnnotations[parameter][index]);
                        p.println();
                    }

                    p.outdent();
                    p.printIndent();
                }

                p.print(AssemblyConstants.BODY_CLOSE);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        p.print(AssemblyConstants.BODY_CLOSE);
    }


    public void visitAnyTypeAnnotationsAttribute(Clazz clazz, TypeAnnotationsAttribute typeAnnotationsAttribute)
    {
        p.printSpace();
        p.print(AssemblyConstants.BODY_OPEN);
        if (typeAnnotationsAttribute.u2annotationsCount > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < typeAnnotationsAttribute.u2annotationsCount; index++)
            {
                p.printIndent();
                visitTypeAnnotation(clazz, (TypeAnnotation) typeAnnotationsAttribute.annotations[index]);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        p.print(AssemblyConstants.BODY_CLOSE);
    }


    public void visitAnnotationDefaultAttribute(Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute)
    {
        p.printSpace();
        annotationDefaultAttribute.defaultValue.accept(clazz, null, this);
    }


    // Implementations for AnnotationVisitor.

    public void visitAnnotation(Clazz clazz, Annotation annotation)
    {
        p.printType(annotation.getType(clazz));
        p.printSpace();
        p.print(AssemblyConstants.BODY_OPEN);
        if (annotation.u2elementValuesCount > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < annotation.u2elementValuesCount; index++)
            {
                ElementValue elementValue = annotation.elementValues[index];
                p.printIndent();
                p.printWord(elementValue.getMethodName(clazz));
                p.printSpace();
                p.print(AssemblyConstants.EQUALS);
                p.printSpace();
                elementValue.accept(clazz, annotation, this);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        p.print(AssemblyConstants.BODY_CLOSE);
    }


    // Implementations for TypeAnnotationVisitor.

    public void visitTypeAnnotation(Clazz clazz, TypeAnnotation typeAnnotation)
    {
        p.printType(typeAnnotation.getType(clazz));
        p.printSpace();
        p.print(AssemblyConstants.BODY_OPEN);
        if (typeAnnotation.u2elementValuesCount > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < typeAnnotation.u2elementValuesCount; index++)
            {
                ElementValue elementValue = typeAnnotation.elementValues[index];
                p.printIndent();
                p.printWord(elementValue.getMethodName(clazz));
                p.printSpace();
                p.print(AssemblyConstants.EQUALS);
                p.printSpace();
                elementValue.accept(clazz, typeAnnotation, this);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        p.print(AssemblyConstants.BODY_CLOSE);
        p.printSpace();
        printTargetInfoType(typeAnnotation.targetInfo.u1targetType);
        typeAnnotation.targetInfoAccept(clazz, this);
        p.printSpace();
        p.print(AssemblyConstants.BODY_OPEN);
        if (typeAnnotation.typePath.length > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < typeAnnotation.typePath.length; index++)
            {
                p.printIndent();
                visitTypePathInfo(clazz, typeAnnotation, typeAnnotation.typePath[index]);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        p.print(AssemblyConstants.BODY_CLOSE);
    }


    // Implementations for TargetInfoVisitor.

    public void visitTypeParameterTargetInfo(Clazz clazz, TypeAnnotation typeAnnotation, TypeParameterTargetInfo typeParameterTargetInfo)
    {
        p.printSpace();
        p.printNumber(typeParameterTargetInfo.u1typeParameterIndex);
    }


    public void visitSuperTypeTargetInfo(Clazz clazz, TypeAnnotation typeAnnotation, SuperTypeTargetInfo superTypeTargetInfo)
    {
        p.printSpace();
        p.printNumber(superTypeTargetInfo.u2superTypeIndex);
    }


    public void visitTypeParameterBoundTargetInfo(Clazz clazz, TypeAnnotation typeAnnotation, TypeParameterBoundTargetInfo typeParameterBoundTargetInfo)
    {
        p.printSpace();
        p.printNumber(typeParameterBoundTargetInfo.u1typeParameterIndex);
        p.printSpace();
        p.printNumber(typeParameterBoundTargetInfo.u1boundIndex);
    }


    public void visitEmptyTargetInfo(Clazz clazz, Member member, TypeAnnotation typeAnnotation, EmptyTargetInfo emptyTargetInfo)
    {
        // NOP.
    }


    public void visitFormalParameterTargetInfo(Clazz clazz, Method method, TypeAnnotation typeAnnotation, FormalParameterTargetInfo formalParameterTargetInfo)
    {
        p.printSpace();
        p.printNumber(formalParameterTargetInfo.u1formalParameterIndex);
    }


    public void visitThrowsTargetInfo(Clazz clazz, Method method, TypeAnnotation typeAnnotation, ThrowsTargetInfo throwsTargetInfo)
    {
        p.printSpace();
        p.printNumber(throwsTargetInfo.u2throwsTypeIndex);
    }


    public void visitLocalVariableTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, LocalVariableTargetInfo localVariableTargetInfo)
    {
        p.printSpace();
        p.print(AssemblyConstants.BODY_OPEN);
        if (localVariableTargetInfo.u2tableLength > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < localVariableTargetInfo.u2tableLength; index++)
            {
                p.printIndent();
                visitLocalVariableTargetElement(clazz, method, codeAttribute, typeAnnotation, localVariableTargetInfo, localVariableTargetInfo.table[index]);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        p.print(AssemblyConstants.BODY_CLOSE);
    }


    public void visitCatchTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, CatchTargetInfo catchTargetInfo)
    {
        p.printSpace();
        p.printNumber(catchTargetInfo.u2exceptionTableIndex);
    }


    public void visitOffsetTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, OffsetTargetInfo offsetTargetInfo)
    {
        p.printSpace();
        p.printOffset(offsetTargetInfo.u2offset);
    }


    public void visitTypeArgumentTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, TypeArgumentTargetInfo typeArgumentTargetInfo)
    {
        p.printSpace();
        p.printOffset(typeArgumentTargetInfo.u2offset);
        p.printSpace();
        p.printNumber(typeArgumentTargetInfo.u1typeArgumentIndex);
    }


    // Implementations for TypePathInfoVisitor.

    public void visitTypePathInfo(Clazz clazz, TypeAnnotation typeAnnotation, TypePathInfo typePathInfo)
    {
        printTypePathKind(typePathInfo.u1typePathKind);

        // Type argument index is optional.
        if (typePathInfo.u1typeArgumentIndex != 0)
        {
            p.printSpace();
            p.printNumber(typePathInfo.u1typeArgumentIndex);
        }

        p.print(AssemblyConstants.STATEMENT_END);
    }


    // Implementations for LocalVariableTargetElementVisitor.

    public void visitLocalVariableTargetElement(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, LocalVariableTargetInfo localVariableTargetInfo, LocalVariableTargetElement localVariableTargetElement)
    {
        p.printOffset(localVariableTargetElement.u2startPC);
        p.printSpace();
        p.printOffset(localVariableTargetElement.u2startPC + localVariableTargetElement.u2length);
        p.printSpace();
        p.printNumber(localVariableTargetElement.u2index);
        p.print(AssemblyConstants.STATEMENT_END);
    }


    // Implementations for ElementValueVisitor.

    public void visitConstantElementValue(Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue)
    {
        clazz.constantPoolEntryAccept(constantElementValue.u2constantValueIndex, new ConstantPrinter(p, constantElementValue.u1tag));
        p.print(AssemblyConstants.STATEMENT_END);
    }


    public void visitEnumConstantElementValue(Clazz clazz, Annotation annotation, EnumConstantElementValue enumConstantElementValue)
    {
        p.printType(enumConstantElementValue.getTypeName(clazz));
        p.print(AssemblyConstants.REFERENCE_SEPARATOR);
        p.printWord(enumConstantElementValue.getConstantName(clazz));
        p.print(AssemblyConstants.STATEMENT_END);
    }


    public void visitClassElementValue(Clazz clazz, Annotation annotation, ClassElementValue classElementValue)
    {
        clazz.constantPoolEntryAccept(classElementValue.u2classInfoIndex, new ConstantPrinter(p, false));
        p.print(AssemblyConstants.STATEMENT_END);
    }


    public void visitAnnotationElementValue(Clazz clazz, Annotation annotation, AnnotationElementValue annotationElementValue)
    {
        p.print(ElementValue.TAG_ANNOTATION);
        visitAnnotation(clazz, annotationElementValue.annotationValue);
    }


    public void visitArrayElementValue(Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue)
    {
        p.print(AssemblyConstants.BODY_OPEN);
        if (arrayElementValue.u2elementValuesCount > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < arrayElementValue.u2elementValuesCount; index++)
            {
                p.printIndent();
                arrayElementValue.elementValues[index].accept(clazz, annotation, this);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        p.print(AssemblyConstants.BODY_CLOSE);
    }


    // Small utility methods.

    /**
     * Prints the representation of a TargetInfo type.
     *
     * @param type the TargetInfo type.
     * @throws PrintException if the type is an unknown TargetInfo type.
     */
    private void printTargetInfoType(int type)
    {
        switch (type)
        {
            case TargetInfo.TARGET_TYPE_PARAMETER_GENERIC_CLASS:             p.printWord(AssemblyConstants.TARGET_TYPE_P_A_RAMETER_GENERIC_CLASS);               break;
            case TargetInfo.TARGET_TYPE_PARAMETER_GENERIC_METHOD:            p.printWord(AssemblyConstants.TARGET_TYPE_P_A_RAMETER_GENERIC_METHOD);              break;
            case TargetInfo.TARGET_TYPE_EXTENDS:                             p.printWord(AssemblyConstants.TARGET_TYPE_E_X_TENDS);                               break;
            case TargetInfo.TARGET_TYPE_BOUND_GENERIC_CLASS:                 p.printWord(AssemblyConstants.TARGET_TYPE_B_O_UND_GENERIC_CLASS);                   break;
            case TargetInfo.TARGET_TYPE_BOUND_GENERIC_METHOD:                p.printWord(AssemblyConstants.TARGET_TYPE_B_O_UND_GENERIC_METHOD);                  break;
            case TargetInfo.TARGET_TYPE_FIELD:                               p.printWord(AssemblyConstants.TARGET_TYPE_F_I_ELD);                                 break;
            case TargetInfo.TARGET_TYPE_RETURN:                              p.printWord(AssemblyConstants.TARGET_TYPE_R_E_TURN);                                break;
            case TargetInfo.TARGET_TYPE_RECEIVER:                            p.printWord(AssemblyConstants.TARGET_TYPE_R_E_CEIVER);                              break;
            case TargetInfo.TARGET_TYPE_PARAMETER:                           p.printWord(AssemblyConstants.TARGET_TYPE_P_A_RAMETER);                             break;
            case TargetInfo.TARGET_TYPE_THROWS:                              p.printWord(AssemblyConstants.TARGET_TYPE_T_H_ROWS);                                break;
            case TargetInfo.TARGET_TYPE_LOCAL_VARIABLE:                      p.printWord(AssemblyConstants.TARGET_TYPE_L_O_CAL_VARIABLE);                        break;
            case TargetInfo.TARGET_TYPE_RESOURCE_VARIABLE:                   p.printWord(AssemblyConstants.TARGET_TYPE_R_E_SOURCE_VARIABLE);                     break;
            case TargetInfo.TARGET_TYPE_CATCH:                               p.printWord(AssemblyConstants.TARGET_TYPE_C_A_TCH);                                 break;
            case TargetInfo.TARGET_TYPE_INSTANCE_OF:                         p.printWord(AssemblyConstants.TARGET_TYPE_I_N_STANCE_OF);                           break;
            case TargetInfo.TARGET_TYPE_NEW:                                 p.printWord(AssemblyConstants.TARGET_TYPE_N_E_W);                                   break;
            case TargetInfo.TARGET_TYPE_METHOD_REFERENCE_NEW:                p.printWord(AssemblyConstants.TARGET_TYPE_M_E_THOD_REFERENCE_NEW);                  break;
            case TargetInfo.TARGET_TYPE_METHOD_REFERENCE:                    p.printWord(AssemblyConstants.TARGET_TYPE_M_E_THOD_REFERENCE);                      break;
            case TargetInfo.TARGET_TYPE_CAST:                                p.printWord(AssemblyConstants.TARGET_TYPE_C_A_ST);                                  break;
            case TargetInfo.TARGET_TYPE_ARGUMENT_GENERIC_METHODNew:          p.printWord(AssemblyConstants.TARGET_TYPE_A_R_GUMENT_GENERIC_METHOD_NEW);           break;
            case TargetInfo.TARGET_TYPE_ARGUMENT_GENERIC_METHOD:             p.printWord(AssemblyConstants.TARGET_TYPE_A_R_GUMENT_GENERIC_METHOD);               break;
            case TargetInfo.TARGET_TYPE_ARGUMENT_GENERIC_METHODReferenceNew: p.printWord(AssemblyConstants.TARGET_TYPE_A_R_GUMENT_GENERIC_METHOD_REFERENCE_NEW); break;
            case TargetInfo.TARGET_TYPE_ARGUMENT_GENERIC_METHODReference:    p.printWord(AssemblyConstants.TARGET_TYPE_A_R_GUMENT_GENERIC_METHOD_REFERENCE);     break;
            default:                                                         throw new PrintException("Unknown target info type " + type + ".");
        }
    }


    /**
     * Prints the representation of a TypePathInfo kind.
     *
     * @param kind the TypePathInfo kind.
     * @throws PrintException if the kind is an unknown TypePathInfo kind.
     */
    private void printTypePathKind(int kind)
    {
        switch (kind)
        {
            case TypePathInfo.KIND_Array:             p.printWord(AssemblyConstants.TYPE_PATH_ARRAY);               break;
            case TypePathInfo.KIND_Nested:            p.printWord(AssemblyConstants.TYPE_PATH_NESTED);              break;
            case TypePathInfo.KIND_TypeArgumentBound: p.printWord(AssemblyConstants.TYPE_PATH_TYPE_ARGUMENT_BOUND); break;
            case TypePathInfo.KIND_TypeArgument:      p.printWord(AssemblyConstants.TYPE_PATH_TYPE_ARGUMENT);       break;
            default:                                  throw new PrintException("Unknown type path kind " + kind + ".");
        }
    }
}
