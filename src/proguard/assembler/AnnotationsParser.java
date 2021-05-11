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
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.annotation.target.*;
import proguard.classfile.attribute.annotation.target.visitor.*;
import proguard.classfile.attribute.annotation.visitor.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.editor.ConstantPoolEditor;
import proguard.util.ArrayUtil;

/**
 * Parses Annotations and TypeAnnotations.
 *
 * @author Joachim Vandersmissen
 */
public class AnnotationsParser
implements   AttributeVisitor,
             AnnotationVisitor,
             TypeAnnotationVisitor,
             TargetInfoVisitor,
             LocalVariableTargetElementVisitor,
             TypePathInfoVisitor,
             ElementValueVisitor

{
    private final Parser             p;
    private final ConstantPoolEditor cpe;
    private final ConstantParser     cp;


    /**
     * Constructs a new AnnotationsParser that uses a Parser and a
     * ConstantPoolEditor.
     *
     * @param p   the Parser to use to parse basic structures.
     * @param cpe the ConstantPoolEditor to use to add Constants to the constant
     *            pool.
     */
    public AnnotationsParser(Parser p, ConstantPoolEditor cpe)
    {
        this.p   = p;
        this.cpe = cpe;
        this.cp  = new ConstantParser(p, cpe);
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAnnotationsAttribute(Clazz clazz, AnnotationsAttribute annotationsAttribute)
    {
        annotationsAttribute.annotations = new Annotation[0];

        p.expect(AssemblyConstants.BODY_OPEN, "annotations open");
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            Annotation annotation = new Annotation();
            visitAnnotation(clazz, annotation);

            annotationsAttribute.annotations =
                ArrayUtil.add(annotationsAttribute.annotations,
                              annotationsAttribute.u2annotationsCount++,
                              annotation);
        }
    }


    public void visitAnyParameterAnnotationsAttribute(Clazz clazz, Method method, ParameterAnnotationsAttribute parameterAnnotationsAttribute)
    {
        parameterAnnotationsAttribute.parameterAnnotations        = new Annotation[0][0];
        parameterAnnotationsAttribute.u2parameterAnnotationsCount = new int[0];

        p.expect(AssemblyConstants.BODY_OPEN, "parameter annotations open");
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            Annotation[] annotations = new Annotation[0];
            int annotationsCount     = 0;

            p.expect(AssemblyConstants.BODY_OPEN, "annotations open");
            while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
            {
                Annotation annotation = new Annotation();
                visitAnnotation(clazz, annotation);

                annotations =
                    ArrayUtil.add(annotations, annotationsCount++, annotation);
            }

            parameterAnnotationsAttribute.parameterAnnotations        =
                ArrayUtil.add(parameterAnnotationsAttribute.parameterAnnotations,
                              parameterAnnotationsAttribute.u1parametersCount,
                              annotations);
            parameterAnnotationsAttribute.u2parameterAnnotationsCount =
                ArrayUtil.add(parameterAnnotationsAttribute.u2parameterAnnotationsCount,
                              parameterAnnotationsAttribute.u1parametersCount++,
                              annotationsCount);
        }
    }


    public void visitAnyTypeAnnotationsAttribute(Clazz clazz, TypeAnnotationsAttribute typeAnnotationsAttribute)
    {
        typeAnnotationsAttribute.annotations = new TypeAnnotation[0];

        p.expect(AssemblyConstants.BODY_OPEN, "type annotations open");
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            TypeAnnotation typeAnnotation = new TypeAnnotation();
            visitTypeAnnotation(clazz, typeAnnotation);

            typeAnnotationsAttribute.annotations =
                ArrayUtil.add(typeAnnotationsAttribute.annotations,
                              typeAnnotationsAttribute.u2annotationsCount++,
                              typeAnnotation);
        }
    }


    public void visitAnnotationDefaultAttribute(Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute)
    {
        annotationDefaultAttribute.defaultValue = expectElementValue(clazz, null);
    }


    // Implementations for AnnotationVisitor.

    public void visitAnnotation(Clazz clazz, Annotation annotation)
    {
        annotation.elementValues = new ElementValue[0];

        annotation.u2typeIndex =
            cpe.addUtf8Constant(p.expectType("annotation type"));

        p.expect(AssemblyConstants.BODY_OPEN, "annotation open");
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            String elementName = p.expectWord("element name");
            p.expect(AssemblyConstants.EQUALS, "element value equals");
            ElementValue elementValue = expectElementValue(clazz, annotation);
            elementValue.u2elementNameIndex = cpe.addUtf8Constant(elementName);

            annotation.elementValues =
                ArrayUtil.add(annotation.elementValues,
                              annotation.u2elementValuesCount++,
                              elementValue);
        }
    }


    // Implementations for TypeAnnotationVisitor.

    public void visitTypeAnnotation(Clazz clazz, TypeAnnotation typeAnnotation)
    {
        typeAnnotation.typePath      = new TypePathInfo[0];

        visitAnnotation(clazz, typeAnnotation);

        typeAnnotation.targetInfo = expectTargetInfo();
        typeAnnotation.targetInfo.accept(clazz, typeAnnotation, this);

        p.expect(AssemblyConstants.BODY_OPEN, "type path open");
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            TypePathInfo typePathInfo = new TypePathInfo();
            visitTypePathInfo(clazz, typeAnnotation, typePathInfo);

            typeAnnotation.typePath =
                ArrayUtil.add(typeAnnotation.typePath,
                              typeAnnotation.typePath.length,
                              typePathInfo);
        }
    }


    // Implementations for TargetInfoVisitor.

    public void visitTypeParameterTargetInfo(Clazz clazz, TypeAnnotation typeAnnotation, TypeParameterTargetInfo typeParameterTargetInfo)
    {
        typeParameterTargetInfo.u1typeParameterIndex =
            (int) p.expectNumber("type parameter index");
    }


    public void visitSuperTypeTargetInfo(Clazz clazz, TypeAnnotation typeAnnotation, SuperTypeTargetInfo superTypeTargetInfo)
    {
        superTypeTargetInfo.u2superTypeIndex =
            (int) p.expectNumber("super type index");
    }


    public void visitTypeParameterBoundTargetInfo(Clazz clazz, TypeAnnotation typeAnnotation, TypeParameterBoundTargetInfo typeParameterBoundTargetInfo)
    {
        typeParameterBoundTargetInfo.u1typeParameterIndex =
            (int) p.expectNumber("type parameter index");
        typeParameterBoundTargetInfo.u1boundIndex         =
            (int) p.expectNumber("bound index");
    }


    public void visitEmptyTargetInfo(Clazz clazz, Member member, TypeAnnotation typeAnnotation, EmptyTargetInfo emptyTargetInfo)
    {
        // NOP.
    }


    public void visitFormalParameterTargetInfo(Clazz clazz, Method method, TypeAnnotation typeAnnotation, FormalParameterTargetInfo formalParameterTargetInfo)
    {
        formalParameterTargetInfo.u1formalParameterIndex =
            (int) p.expectNumber("formal parameter index");
    }


    public void visitThrowsTargetInfo(Clazz clazz, Method method, TypeAnnotation typeAnnotation, ThrowsTargetInfo throwsTargetInfo)
    {
        throwsTargetInfo.u2throwsTypeIndex =
            (int) p.expectNumber("exceptions index");
    }


    public void visitLocalVariableTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, LocalVariableTargetInfo localVariableTargetInfo)
    {
        localVariableTargetInfo.table = new LocalVariableTargetElement[0];

        p.expect(AssemblyConstants.BODY_OPEN, "local variable target open");
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            LocalVariableTargetElement localVariableTargetElement =
                new LocalVariableTargetElement();
            visitLocalVariableTargetElement(clazz, method, codeAttribute, typeAnnotation, localVariableTargetInfo, localVariableTargetElement);

            localVariableTargetInfo.table =
                ArrayUtil.add(localVariableTargetInfo.table,
                              localVariableTargetInfo.u2tableLength++,
                              localVariableTargetElement);
        }
    }


    public void visitCatchTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, CatchTargetInfo catchTargetInfo)
    {
        catchTargetInfo.u2exceptionTableIndex =
            (int) p.expectNumber("exception table index");
    }


    public void visitOffsetTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, OffsetTargetInfo offsetTargetInfo)
    {
        offsetTargetInfo.u2offset = p.expectOffset("offset");
    }


    public void visitTypeArgumentTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, TypeArgumentTargetInfo typeArgumentTargetInfo)
    {
        typeArgumentTargetInfo.u2offset            = p.expectOffset("offset");
        typeArgumentTargetInfo.u1typeArgumentIndex =
            (int) p.expectNumber("type argument index");
    }


    // Implementations for TypePathInfoVisitor.

    public void visitTypePathInfo(Clazz clazz, TypeAnnotation typeAnnotation, TypePathInfo typePathInfo)
    {
        typePathInfo.u1typePathKind = expectTypePathInfoKind();

        // Type argument index is optional.
        if (!p.nextTtypeEquals(AssemblyConstants.STATEMENT_END))
        {
            typePathInfo.u1typeArgumentIndex =
                (int) p.expectNumber("type argument index");
            p.expect(AssemblyConstants.STATEMENT_END, "type path info end");
        }
    }


    // Implementations for LocalVariableTargetElementVisitor.

    public void visitLocalVariableTargetElement(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, LocalVariableTargetInfo localVariableTargetInfo, LocalVariableTargetElement localVariableTargetElement)
    {
        localVariableTargetElement.u2startPC =
            p.expectOffset("local variable target element start");
        localVariableTargetElement.u2length  =
            p.expectOffset("local variable target element end") - localVariableTargetElement.u2startPC;
        localVariableTargetElement.u2index   =
            (int) p.expectNumber("local variable target element index");
        p.expect(AssemblyConstants.STATEMENT_END, "local variable target element end");
    }


    // Implementations for ElementValueVisitor.

    public void visitConstantElementValue(Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue)
    {
        switch (constantElementValue.u1tag)
        {
            case TypeConstants.BOOLEAN:
            case TypeConstants.BYTE:
            case TypeConstants.CHAR:
            case TypeConstants.INT:
            case TypeConstants.SHORT:                    cp.visitIntegerConstant(clazz, null); break;
            case TypeConstants.DOUBLE:                   cp.visitDoubleConstant(clazz, null);  break;
            case TypeConstants.FLOAT:                    cp.visitFloatConstant(clazz, null);   break;
            case TypeConstants.LONG:                     cp.visitLongConstant(clazz, null);    break;
            case ElementValue.TAG_STRING_CONSTANT: cp.visitUtf8Constant(clazz, null);    break;
            default:                                           throw  new ParseException("Unknown element value type " + constantElementValue.u1tag + ".", p.lineno());
        }

        constantElementValue.u2constantValueIndex = cp.getIndex();
        p.expect(AssemblyConstants.STATEMENT_END, "constant element value end");
    }


    public void visitEnumConstantElementValue(Clazz clazz, Annotation annotation, EnumConstantElementValue enumConstantElementValue)
    {
        enumConstantElementValue.u2typeNameIndex =
            cpe.addUtf8Constant(p.expectType("enum constant element value type"));
        p.expect(AssemblyConstants.REFERENCE_SEPARATOR, "enum constant element value separator");
        enumConstantElementValue.u2constantNameIndex =
            cpe.addUtf8Constant(p.expectWord("enum constant element value constant"));
        p.expect(AssemblyConstants.STATEMENT_END, "enum constant element value end");
    }


    public void visitClassElementValue(Clazz clazz, Annotation annotation, ClassElementValue classElementValue)
    {
        classElementValue.u2classInfoIndex = cpe.addUtf8Constant(p.expectType("class type"));
        p.expect(AssemblyConstants.STATEMENT_END, "class element value end");
    }


    public void visitAnnotationElementValue(Clazz clazz, Annotation annotation, AnnotationElementValue annotationElementValue)
    {
        annotationElementValue.annotationValue = new Annotation();
        visitAnnotation(clazz, annotationElementValue.annotationValue);
    }


    public void visitArrayElementValue(Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue)
    {
        arrayElementValue.elementValues = new ElementValue[0];

        p.expect(AssemblyConstants.BODY_OPEN, "array element value open");
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            ElementValue elementValue = expectElementValue(clazz, annotation);

            arrayElementValue.elementValues =
                ArrayUtil.add(arrayElementValue.elementValues,
                              arrayElementValue.u2elementValuesCount++,
                              elementValue);
        }
    }


    // Small utility methods.

    /**
     * Parses an ElementValue. This method advances the Parser. The ElementValue
     * is fully parsed, no additional visiting or parsing is necessary.
     *
     * @param clazz      the Class for which to parse the ElementValue.
     * @param annotation the Annotation for which to parse the ElementValue.
     * @return the ElementValue.
     * @throws ParseException if an unknown ElementValue type is read.
     */
    private ElementValue expectElementValue(Clazz clazz, Annotation annotation)
    {
        // Simple type detection: java cast format
        if (p.nextTtypeEquals(JavaTypeConstants.METHOD_ARGUMENTS_OPEN))
        {
            String type = p.expectWord("element value type");
            p.expect(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE, "element value type close");
            ElementValue elementValue;
            switch (type)
            {
                case JavaTypeConstants.BOOLEAN:         elementValue = new ConstantElementValue(TypeConstants.BOOLEAN);                  break;
                case JavaTypeConstants.BYTE:            elementValue = new ConstantElementValue(TypeConstants.BYTE);                     break;
                case JavaTypeConstants.CHAR:            elementValue = new ConstantElementValue(TypeConstants.CHAR);                     break;
                case JavaTypeConstants.DOUBLE:          elementValue = new ConstantElementValue(TypeConstants.DOUBLE);                   break;
                case JavaTypeConstants.FLOAT:           elementValue = new ConstantElementValue(TypeConstants.FLOAT);                    break;
                case JavaTypeConstants.INT:             elementValue = new ConstantElementValue(TypeConstants.INT);                      break;
                case JavaTypeConstants.LONG:            elementValue = new ConstantElementValue(TypeConstants.LONG);                     break;
                case JavaTypeConstants.SHORT:           elementValue = new ConstantElementValue(TypeConstants.SHORT);                    break;
                case AssemblyConstants.TYPE_STRING:     elementValue = new ConstantElementValue(ElementValue.TAG_STRING_CONSTANT); break;
                case AssemblyConstants.TYPE_CLASS:      elementValue = new ClassElementValue();                                                break;
                case AssemblyConstants.TYPE_ANNOTATION: elementValue = new AnnotationElementValue();                                           break;
                case AssemblyConstants.TYPE_ENUM:       elementValue = new EnumConstantElementValue();                                         break;
                case AssemblyConstants.TYPE_ARRAY:      elementValue = new ArrayElementValue();                                                break;
                default:                                throw new ParseException("Unknown element value type " + type + ".", p.lineno());
            }

            elementValue.accept(clazz, annotation, this);
            return elementValue;
        }

        // Difficult type detection: inferring from format.
        if (p.nextTtypeEquals(ElementValue.TAG_ANNOTATION))
        {
            ElementValue elementValue = new AnnotationElementValue();
            elementValue.accept(clazz, annotation, this);
            return elementValue;
        }

        if (p.nextTtypeEquals(AssemblyConstants.BODY_OPEN))
        {
            p.pushBack();
            ElementValue elementValue = new ArrayElementValue();
            elementValue.accept(clazz, annotation, this);
            return elementValue;
        }

        if (p.nextTtypeEqualsChar())
        {
            p.pushBack();
            ElementValue elementValue =
                new ConstantElementValue(TypeConstants.CHAR);
            elementValue.accept(clazz, annotation, this);
            return elementValue;
        }

        if (p.nextTtypeEqualsString())
        {
            p.pushBack();
            ElementValue elementValue =
                new ConstantElementValue(ElementValue.TAG_STRING_CONSTANT);
            elementValue.accept(clazz, annotation, this);
            return elementValue;
        }

        if (p.nextTtypeEqualsNumber())
        {
            double number = p.nval;
            ElementValue elementValue = null;
            if (p.nextTtypeEqualsWord())
            {
                String literalType = p.sval.toUpperCase();
                switch (literalType)
                {
                    // Java doubles, floats, longs can end with D (or d),
                    // F (or f), L (or l) respectively.
                    case AssemblyConstants.TYPE_DOUBLE: elementValue = new ConstantElementValue(TypeConstants.DOUBLE, 0, cpe.addDoubleConstant(number));       break;
                    case AssemblyConstants.TYPE_FLOAT:  elementValue = new ConstantElementValue(TypeConstants.FLOAT, 0, cpe.addFloatConstant((float) number)); break;
                    case AssemblyConstants.TYPE_LONG:   elementValue = new ConstantElementValue(TypeConstants.LONG, 0, cpe.addLongConstant((long) number));    break;
                    default:                            p.pushBack();
                }
            }

            if (elementValue == null)
            {
                elementValue =
                    new ConstantElementValue(TypeConstants.INT, 0, cpe.addIntegerConstant((int) number));
            }

            p.expect(AssemblyConstants.STATEMENT_END, "element value end");
            return elementValue;
        }
        else if (p.nextTtypeEqualsWord())
        {
            if (AssemblyConstants.TRUE.equals(p.sval) ||
                AssemblyConstants.FALSE.equals(p.sval))
            {
                p.pushBack();
                ElementValue elementValue =
                    new ConstantElementValue(TypeConstants.BOOLEAN);
                elementValue.accept(clazz, annotation, this);
                return elementValue;
            }
            else
            {
                p.pushBack();
                String classType = p.expectType("class type");
                ElementValue elementValue;
                if (p.nextTtypeEquals(AssemblyConstants.REFERENCE_SEPARATOR))
                {
                    elementValue =
                        new EnumConstantElementValue(0, cpe.addUtf8Constant(classType), cpe.addUtf8Constant(p.expectWord("enum constant")));
                }
                else
                {
                    elementValue =
                        new ClassElementValue(0, cpe.addUtf8Constant(classType));
                }

                p.expect(AssemblyConstants.STATEMENT_END, "element value end");
                return elementValue;
            }
        }

        throw new ParseException("Unknown element value type.", p.lineno());
    }


    /**
     * Parses a TargetInfo. This method advances the Parser.
     *
     * @return the TargetInfo.
     * @throws ParseException if an unknown TargetInfo is read.
     */
    private TargetInfo expectTargetInfo()
    {
        String type = p.expectWord("target info type");
        switch (type)
        {
            case AssemblyConstants.TARGET_TYPE_P_A_RAMETER_GENERIC_CLASS:               return new TypeParameterTargetInfo((byte) TargetInfo.TARGET_TYPE_PARAMETER_GENERIC_CLASS);
            case AssemblyConstants.TARGET_TYPE_P_A_RAMETER_GENERIC_METHOD:              return new TypeParameterTargetInfo((byte) TargetInfo.TARGET_TYPE_PARAMETER_GENERIC_METHOD);
            case AssemblyConstants.TARGET_TYPE_E_X_TENDS:                               return new SuperTypeTargetInfo((byte) TargetInfo.TARGET_TYPE_EXTENDS);
            case AssemblyConstants.TARGET_TYPE_B_O_UND_GENERIC_CLASS:                   return new TypeParameterBoundTargetInfo((byte) TargetInfo.TARGET_TYPE_BOUND_GENERIC_CLASS);
            case AssemblyConstants.TARGET_TYPE_B_O_UND_GENERIC_METHOD:                  return new TypeParameterBoundTargetInfo((byte) TargetInfo.TARGET_TYPE_BOUND_GENERIC_METHOD);
            case AssemblyConstants.TARGET_TYPE_F_I_ELD:                                 return new EmptyTargetInfo((byte) TargetInfo.TARGET_TYPE_FIELD);
            case AssemblyConstants.TARGET_TYPE_R_E_TURN:                                return new EmptyTargetInfo((byte) TargetInfo.TARGET_TYPE_RETURN);
            case AssemblyConstants.TARGET_TYPE_R_E_CEIVER:                              return new EmptyTargetInfo((byte) TargetInfo.TARGET_TYPE_RECEIVER);
            case AssemblyConstants.TARGET_TYPE_P_A_RAMETER:                             return new FormalParameterTargetInfo((byte) TargetInfo.TARGET_TYPE_PARAMETER);
            case AssemblyConstants.TARGET_TYPE_T_H_ROWS:                                return new ThrowsTargetInfo((byte) TargetInfo.TARGET_TYPE_THROWS);
            case AssemblyConstants.TARGET_TYPE_L_O_CAL_VARIABLE:                        return new LocalVariableTargetInfo((byte) TargetInfo.TARGET_TYPE_LOCAL_VARIABLE);
            case AssemblyConstants.TARGET_TYPE_R_E_SOURCE_VARIABLE:                     return new LocalVariableTargetInfo((byte) TargetInfo.TARGET_TYPE_RESOURCE_VARIABLE);
            case AssemblyConstants.TARGET_TYPE_C_A_TCH:                                 return new CatchTargetInfo((byte) TargetInfo.TARGET_TYPE_CATCH);
            case AssemblyConstants.TARGET_TYPE_I_N_STANCE_OF:                           return new OffsetTargetInfo((byte) TargetInfo.TARGET_TYPE_INSTANCE_OF);
            case AssemblyConstants.TARGET_TYPE_N_E_W:                                   return new OffsetTargetInfo((byte) TargetInfo.TARGET_TYPE_NEW);
            case AssemblyConstants.TARGET_TYPE_M_E_THOD_REFERENCE_NEW:                  return new OffsetTargetInfo((byte) TargetInfo.TARGET_TYPE_METHOD_REFERENCE_NEW);
            case AssemblyConstants.TARGET_TYPE_M_E_THOD_REFERENCE:                      return new OffsetTargetInfo((byte) TargetInfo.TARGET_TYPE_METHOD_REFERENCE);
            case AssemblyConstants.TARGET_TYPE_C_A_ST:                                  return new TypeArgumentTargetInfo((byte) TargetInfo.TARGET_TYPE_CAST);
            case AssemblyConstants.TARGET_TYPE_A_R_GUMENT_GENERIC_METHOD_NEW:           return new TypeArgumentTargetInfo((byte) TargetInfo.TARGET_TYPE_ARGUMENT_GENERIC_METHODNew);
            case AssemblyConstants.TARGET_TYPE_A_R_GUMENT_GENERIC_METHOD:               return new TypeArgumentTargetInfo((byte) TargetInfo.TARGET_TYPE_ARGUMENT_GENERIC_METHOD);
            case AssemblyConstants.TARGET_TYPE_A_R_GUMENT_GENERIC_METHOD_REFERENCE_NEW: return new TypeArgumentTargetInfo((byte) TargetInfo.TARGET_TYPE_ARGUMENT_GENERIC_METHODReferenceNew);
            case AssemblyConstants.TARGET_TYPE_A_R_GUMENT_GENERIC_METHOD_REFERENCE:     return new TypeArgumentTargetInfo((byte) TargetInfo.TARGET_TYPE_ARGUMENT_GENERIC_METHODReference);
            default:                                                                        throw  new ParseException("Unknown target info type " + type + ".", p.lineno());
        }
    }


    /**
     * Parses a type path kind. This method advances the Parser.
     *
     * @return the type path kind.
     * @throws ParseException if an unknown type path kind is read.
     */
    private int expectTypePathInfoKind()
    {
        String kind = p.expectWord("type path kind");
        switch (kind)
        {
            case AssemblyConstants.TYPE_PATH_ARRAY:               return TypePathInfo.KIND_Array;
            case AssemblyConstants.TYPE_PATH_NESTED:              return TypePathInfo.KIND_Nested;
            case AssemblyConstants.TYPE_PATH_TYPE_ARGUMENT_BOUND: return TypePathInfo.KIND_TypeArgumentBound;
            case AssemblyConstants.TYPE_PATH_TYPE_ARGUMENT:       return TypePathInfo.KIND_TypeArgument;
            default:                                              throw  new ParseException("Unknown type path kind " + kind + ".", p.lineno());
        }
    }
}
