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
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.module.*;
import proguard.classfile.attribute.module.visitor.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.editor.*;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.util.ArrayUtil;

import java.util.*;

/**
 * Parses most Attributes.
 *
 * @author Joachim Vandersmissen
 */
public class AttributesParser
implements   ClassVisitor,
             MemberVisitor,
             AttributeVisitor,
             BootstrapMethodInfoVisitor,
             InnerClassesInfoVisitor,
             RequiresInfoVisitor,
             ExportsInfoVisitor,
             OpensInfoVisitor,
             ProvidesInfoVisitor
{
    private final Parser             p;
    private final ConstantPoolEditor cpe;
    private final ConstantParser     cp;


    /**
     * Constructs a new AttributesParser that uses a Parser and a
     * ConstantPoolEditor.
     *
     * @param p      the Parser to use to parse basic structures.
     * @param cpe    the ConstantPoolEditor to use to add Constants to the
     *               constant pool.
     */
    public AttributesParser(Parser p, ConstantPoolEditor cpe)
    {
        this.p   = p;
        this.cpe = cpe;
        this.cp  = new ConstantParser(p, cpe);
    }


    // Implementations for ClassVisitor.

    public void visitAnyClass(Clazz clazz) {}


    public void visitProgramClass(ProgramClass programClass)
    {
        AttributesEditor attributesEditor =
            new AttributesEditor(programClass, false);
        while (!p.nextTtypeEquals(AssemblyConstants.ATTRIBUTES_CLOSE))
        {
            Attribute attribute = matchAttribute();
            attribute.accept(programClass, this);
            attributesEditor.addAttribute(attribute);
        }
    }


    // Implementations for MemberVisitor.

    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        AttributesEditor attributesEditor =
            new AttributesEditor(programClass, programField, false);
        while (!p.nextTtypeEquals(AssemblyConstants.ATTRIBUTES_CLOSE))
        {
            Attribute attribute = matchAttribute();
            attribute.accept(programClass, programField, this);
            attributesEditor.addAttribute(attribute);
        }
    }


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        AttributesEditor attributesEditor =
            new AttributesEditor(programClass, programMethod, false);
        while (!p.nextTtypeEquals(AssemblyConstants.ATTRIBUTES_CLOSE))
        {
            Attribute attribute = matchAttribute();
            attribute.accept(programClass, programMethod, this);
            attributesEditor.addAttribute(attribute);
        }
    }


    // Implementations for AttributeVisitor.

    public void visitBootstrapMethodsAttribute(Clazz clazz, BootstrapMethodsAttribute bootstrapMethodsAttribute)
    {
        bootstrapMethodsAttribute.bootstrapMethods = new BootstrapMethodInfo[0];

        p.expect(AssemblyConstants.BODY_OPEN, "bootstrap methods open");
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            BootstrapMethodInfo bootstrapMethod = new BootstrapMethodInfo();
            visitBootstrapMethodInfo(clazz, bootstrapMethod);

            bootstrapMethodsAttribute.bootstrapMethods =
                ArrayUtil.add(bootstrapMethodsAttribute.bootstrapMethods,
                              bootstrapMethodsAttribute.u2bootstrapMethodsCount++,
                              bootstrapMethod);
        }
    }


    public void visitSourceFileAttribute(Clazz clazz, SourceFileAttribute sourceFileAttribute)
    {
        sourceFileAttribute.u2sourceFileIndex =
            cpe.addUtf8Constant(p.expectString("source file"));
        p.expect(AssemblyConstants.STATEMENT_END, "source file end");
    }


    public void visitSourceDirAttribute(Clazz clazz, SourceDirAttribute sourceDirAttribute)
    {
        sourceDirAttribute.u2sourceDirIndex =
            cpe.addUtf8Constant(p.expectString("source dir"));
        p.expect(AssemblyConstants.STATEMENT_END, "source dir end");
    }


    public void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute)
    {
        innerClassesAttribute.classes = new InnerClassesInfo[0];

        p.expect(AssemblyConstants.BODY_OPEN, "inner classes open");
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            InnerClassesInfo innerClass = new InnerClassesInfo();
            visitInnerClassesInfo(clazz, innerClass);

            innerClassesAttribute.classes =
                ArrayUtil.add(innerClassesAttribute.classes,
                              innerClassesAttribute.u2classesCount++,
                              innerClass);
        }
    }


    public void visitEnclosingMethodAttribute(Clazz clazz, EnclosingMethodAttribute enclosingMethodAttribute)
    {
        cp.visitClassConstant(clazz, null);
        enclosingMethodAttribute.u2classIndex = cp.getIndex();

        // Enclosing method is optional.
        if (p.nextTtypeEquals(AssemblyConstants.REFERENCE_SEPARATOR))
        {
            String returnType = p.expectType("enclosing method return type");
            String name       = p.expectMethodName("enclosing method name");
            String methodArgs = p.expectMethodArguments("enclosing method arguments");
            enclosingMethodAttribute.u2nameAndTypeIndex =
                cpe.addNameAndTypeConstant(name, methodArgs + returnType);
        }

        p.expect(AssemblyConstants.STATEMENT_END, "enclosing method end");
    }


    public void visitNestHostAttribute(Clazz clazz, NestHostAttribute nestHostAttribute)
    {
        cp.visitClassConstant(clazz, null);
        nestHostAttribute.u2hostClassIndex = cp.getIndex();
        p.expect(AssemblyConstants.STATEMENT_END, "nest host end");
    }


    public void visitNestMembersAttribute(Clazz clazz, NestMembersAttribute nestMembersAttribute)
    {
        nestMembersAttribute.u2classes = new int[0];

        p.expect(AssemblyConstants.BODY_OPEN, "nest members open");
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            cp.visitClassConstant(clazz, null);
            p.expect(AssemblyConstants.STATEMENT_END, "nest member end");

            nestMembersAttribute.u2classes =
                ArrayUtil.add(nestMembersAttribute.u2classes,
                              nestMembersAttribute.u2classesCount++,
                              cp.getIndex());
        }
    }


    public void visitModuleAttribute(Clazz clazz, ModuleAttribute moduleAttribute)
    {
        moduleAttribute.requires = new RequiresInfo[0];
        moduleAttribute.exports  = new ExportsInfo[0];
        moduleAttribute.opens    = new OpensInfo[0];
        moduleAttribute.u2uses   = new int[0];
        moduleAttribute.provides = new ProvidesInfo[0];

        moduleAttribute.u2moduleFlags = p.expectAccessFlags();

        cp.visitModuleConstant(clazz, null);
        moduleAttribute.u2moduleNameIndex = cp.getIndex();

        // Module version is optional.
        if (p.nextTtypeEqualsWord())
        {
            moduleAttribute.u2moduleVersionIndex = cpe.addUtf8Constant(p.sval);
        }

        p.expect(AssemblyConstants.BODY_OPEN, "module open");
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            String directive = p.expectWord("module directive");
            if (AssemblyConstants.REQUIRES.equals(directive))
            {
                RequiresInfo requiresInfo = new RequiresInfo();
                visitRequiresInfo(clazz, requiresInfo);

                moduleAttribute.requires =
                    ArrayUtil.add(moduleAttribute.requires,
                                  moduleAttribute.u2requiresCount++,
                                  requiresInfo);
                continue;
            }

            if (AssemblyConstants.EXPORTS.equals(directive))
            {
                ExportsInfo exportsInfo = new ExportsInfo();
                visitExportsInfo(clazz, exportsInfo);

                moduleAttribute.exports =
                    ArrayUtil.add(moduleAttribute.exports,
                                  moduleAttribute.u2exportsCount++,
                                  exportsInfo);
                continue;
            }

            if (AssemblyConstants.OPENS.equals(directive))
            {
                OpensInfo opensInfo = new OpensInfo();
                visitOpensInfo(clazz, opensInfo);

                moduleAttribute.opens =
                    ArrayUtil.add(moduleAttribute.opens,
                                  moduleAttribute.u2opensCount++,
                                  opensInfo);
                continue;
            }

            if (AssemblyConstants.USES.equals(directive))
            {
                cp.visitClassConstant(clazz, null);

                moduleAttribute.u2uses =
                    ArrayUtil.add(moduleAttribute.u2uses,
                                  moduleAttribute.u2usesCount++,
                                  cp.getIndex());
                p.expect(AssemblyConstants.STATEMENT_END, "uses end");
                continue;
            }

            if (AssemblyConstants.PROVIDES.equals(directive))
            {
                ProvidesInfo providesInfo = new ProvidesInfo();
                visitProvidesInfo(clazz, providesInfo);

                moduleAttribute.provides =
                    ArrayUtil.add(moduleAttribute.provides,
                                  moduleAttribute.u2providesCount++,
                                  providesInfo);
                continue;
            }

            p.throwKeywordError(AssemblyConstants.REQUIRES,
                                AssemblyConstants.EXPORTS,
                                AssemblyConstants.OPENS,
                                AssemblyConstants.USES,
                                AssemblyConstants.PROVIDES);
        }
    }


    public void visitModuleMainClassAttribute(Clazz clazz, ModuleMainClassAttribute moduleMainClassAttribute)
    {
        cp.visitClassConstant(clazz, null);
        moduleMainClassAttribute.u2mainClass = cp.getIndex();
        p.expect(AssemblyConstants.STATEMENT_END, "module main class end");
    }


    public void visitModulePackagesAttribute(Clazz clazz, ModulePackagesAttribute modulePackagesAttribute)
    {
        modulePackagesAttribute.u2packages = new int[0];

        p.expect(AssemblyConstants.BODY_OPEN, "module packages open");
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE))
        {
            cp.visitPackageConstant(clazz, null);
            p.expect(AssemblyConstants.STATEMENT_END, "module package end");

            modulePackagesAttribute.u2packages =
                ArrayUtil.add(modulePackagesAttribute.u2packages,
                              modulePackagesAttribute.u2packagesCount++,
                              cp.getIndex());
        }
    }


    public void visitDeprecatedAttribute(Clazz clazz, DeprecatedAttribute deprecatedAttribute)
    {
        p.expect(AssemblyConstants.STATEMENT_END, "deprecated end");
    }


    public void visitSyntheticAttribute(Clazz clazz, SyntheticAttribute syntheticAttribute)
    {
        p.expect(AssemblyConstants.STATEMENT_END, "synthetic end");
    }


    public void visitSignatureAttribute(Clazz clazz, SignatureAttribute signatureAttribute)
    {
        signatureAttribute.u2signatureIndex =
            cpe.addUtf8Constant(p.expectString("signature"));
        p.expect(AssemblyConstants.STATEMENT_END, "signature end");
    }


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        codeAttribute.attributes = new Attribute[0];

        Map<String, Integer> oldLabels = p.labels;
        p.labels = new HashMap<>();
        CodeAttributeComposer cac = new CodeAttributeComposer(true, true, false);

        if (p.nextTtypeEquals(AssemblyConstants.ATTRIBUTES_OPEN))
        {
            AttributesEditor attributesEditor =
                new AttributesEditor((ProgramClass) clazz,
                                     (ProgramMethod) method,
                                     codeAttribute,
                                     false);
            while (!p.nextTtypeEquals(AssemblyConstants.ATTRIBUTES_CLOSE))
            {
                Attribute attribute = matchAttribute();
                attribute.accept(clazz, method, codeAttribute, this);
                attributesEditor.addAttribute(attribute);
            }
        }

        p.expect(AssemblyConstants.BODY_OPEN, "code open");
        try
        {
            codeAttribute.accept(clazz, method, new InstructionsParser(p, cpe, cac));
            codeAttribute.accept(clazz, method, cac);
        }
        catch (Exception e)
        {
            throw new ParseException("An exception occured while parsing " +
                                     method.getName(clazz) +
                                     '(' +
                                     ClassUtil.externalMethodArguments(method.getDescriptor(clazz)) +
                                     ')',
                                     p.lineno(),
                                     e);
        }

        p.labels = oldLabels;
    }


    public void visitAnyAnnotationsAttribute(Clazz clazz, AnnotationsAttribute annotationsAttribute)
    {
        annotationsAttribute.accept(clazz, new AnnotationsParser(p, cpe));
    }


    public void visitAnyParameterAnnotationsAttribute(Clazz clazz, Method method, ParameterAnnotationsAttribute parameterAnnotationsAttribute)
    {
        parameterAnnotationsAttribute.accept(clazz, method, new AnnotationsParser(p, cpe));
    }


    public void visitAnnotationDefaultAttribute(Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute)
    {
        annotationDefaultAttribute.accept(clazz, method, new AnnotationsParser(p, cpe));
    }


    // Implementations for BootstrapMethodInfoVisitor.

    public void visitBootstrapMethodInfo(Clazz clazz, BootstrapMethodInfo bootstrapMethodInfo)
    {
        bootstrapMethodInfo.u2methodArguments = new int[0];

        cp.visitMethodHandleConstant(clazz, null);
        bootstrapMethodInfo.u2methodHandleIndex = cp.getIndex();
        p.expect(AssemblyConstants.BODY_OPEN, "bootstrap method arguments open");
        while (!p.nextTtypeEquals(AssemblyConstants.BODY_CLOSE)) {
            int argumentIndex = p.expectLoadableConstant(clazz, cpe, cp);
            p.expect(AssemblyConstants.STATEMENT_END, "bootstrap method argument end");

            bootstrapMethodInfo.u2methodArguments =
                ArrayUtil.add(bootstrapMethodInfo.u2methodArguments,
                              bootstrapMethodInfo.u2methodArgumentCount++,
                              argumentIndex);
        }
    }


    // Implementations for InnerClassesInfoVisitor.

    public void visitInnerClassesInfo(Clazz clazz, InnerClassesInfo innerClassesInfo)
    {
        innerClassesInfo.u2innerClassAccessFlags = p.expectClassAccessFlags();
        innerClassesInfo.u2innerClassIndex       =
            cpe.addClassConstant(ClassUtil.internalClassName(p.expectWord("inner class")), null);
        while (p.expectIfNextTtypeEqualsWord(AssemblyConstants.AS,
                                             AssemblyConstants.IN))
        {
            // Inner name is optional.
            if (AssemblyConstants.AS.equals(p.sval))
            {
                innerClassesInfo.u2innerNameIndex =
                    cpe.addUtf8Constant(p.expectWord("inner name"));
            }

            // Outer class is optional.
            if (AssemblyConstants.IN.equals(p.sval))
            {
                cp.visitClassConstant(clazz, null);
                innerClassesInfo.u2outerClassIndex = cp.getIndex();
            }
        }

        p.expect(AssemblyConstants.STATEMENT_END, "inner class end");
    }


    // Implementations for ExportsInfoVisitor.

    public void visitExportsInfo(Clazz clazz, ExportsInfo exportsInfo)
    {
        exportsInfo.u2exportsToIndex = new int[0];

        exportsInfo.u2exportsFlags = p.expectAccessFlags();
        cp.visitPackageConstant(clazz, null);
        exportsInfo.u2exportsIndex = cp.getIndex();
        if (!p.nextTtypeEquals(AssemblyConstants.STATEMENT_END))
        {
            p.expect(AssemblyConstants.TO);
            while (true)
            {
                cp.visitModuleConstant(clazz, null);

                exportsInfo.u2exportsToIndex =
                    ArrayUtil.add(exportsInfo.u2exportsToIndex,
                                  exportsInfo.u2exportsToCount++,
                                  cp.getIndex());
                if (p.nextTtypeEquals(AssemblyConstants.STATEMENT_END))
                {
                    return;
                }

                p.expect(JavaTypeConstants.METHOD_ARGUMENTS_SEPARATOR, "exports to separator");
            }
        }
    }


    // Implementations for OpensInfoVisitor.

    public void visitOpensInfo(Clazz clazz, OpensInfo opensInfo)
    {
        opensInfo.u2opensToIndex = new int[0];

        opensInfo.u2opensFlags = p.expectAccessFlags();
        cp.visitPackageConstant(clazz, null);
        opensInfo.u2opensIndex = cp.getIndex();
        if (!p.nextTtypeEquals(AssemblyConstants.STATEMENT_END))
        {
            p.expect(AssemblyConstants.TO);
            while (true)
            {
                cp.visitModuleConstant(clazz, null);

                opensInfo.u2opensToIndex =
                    ArrayUtil.add(opensInfo.u2opensToIndex,
                                  opensInfo.u2opensToCount++,
                                  cp.getIndex());
                if (p.nextTtypeEquals(AssemblyConstants.STATEMENT_END))
                {
                    return;
                }

                p.expect(JavaTypeConstants.METHOD_ARGUMENTS_SEPARATOR, "opens to separator");
            }
        }
    }


    // Implementations for ProvidesInfoVisitor.

    public void visitProvidesInfo(Clazz clazz, ProvidesInfo providesInfo)
    {
        providesInfo.u2providesWithIndex = new int[0];

        cp.visitClassConstant(clazz, null);
        providesInfo.u2providesIndex = cp.getIndex();
        // Even though the specification says the provides_with array should not
        // be empty, we could allow it...
        if (!p.nextTtypeEquals(AssemblyConstants.STATEMENT_END))
        {
            p.expect(AssemblyConstants.WITH);
            while (true)
            {
                cp.visitClassConstant(clazz, null);

                providesInfo.u2providesWithIndex =
                    ArrayUtil.add(providesInfo.u2providesWithIndex,
                                  providesInfo.u2providesWithCount++,
                                  cp.getIndex());
                if (p.nextTtypeEquals(AssemblyConstants.STATEMENT_END))
                {
                    return;
                }

                p.expect(JavaTypeConstants.METHOD_ARGUMENTS_SEPARATOR, "provides with separator");
            }
        }
    }


    // Implementations for RequiresInfoVisitor.

    public void visitRequiresInfo(Clazz clazz, RequiresInfo requiresInfo)
    {
        requiresInfo.u2requiresFlags = p.expectAccessFlags();

        cp.visitModuleConstant(clazz, null);
        requiresInfo.u2requiresIndex = cp.getIndex();

        // Requires info version is optional.
        if (!p.nextTtypeEquals(AssemblyConstants.STATEMENT_END))
        {
            requiresInfo.u2requiresVersionIndex =
                cpe.addUtf8Constant(p.expectWord("requires module version"));
            p.expect(AssemblyConstants.STATEMENT_END, "requires info end");
        }
    }


    /**
     * Parses an Attribute. This method advances the Parser.
     *
     * @return the Attribute.
     * @throws ParseException if an unknown Attribute name is read.
     */
    private Attribute matchAttribute()
    {
        String name = p.expectWord("attribute name");
        Attribute attribute;
        switch (name)
        {
            // Commented out attributes are handled in a special way.
            case Attribute.BOOTSTRAP_METHODS:                       attribute = new BootstrapMethodsAttribute();                     break;
            case Attribute.SOURCE_FILE:                             attribute = new SourceFileAttribute();                           break;
            case Attribute.SOURCE_DIR:                              attribute = new SourceDirAttribute();                            break;
            case Attribute.INNER_CLASSES:                           attribute = new InnerClassesAttribute();                         break;
            case Attribute.ENCLOSING_METHOD:                        attribute = new EnclosingMethodAttribute();                      break;
            case Attribute.NEST_HOST:                               attribute = new NestHostAttribute();                             break;
            case Attribute.NEST_MEMBERS:                            attribute = new NestMembersAttribute();                          break;
            case Attribute.DEPRECATED:                              attribute = new DeprecatedAttribute();                           break;
            case Attribute.SYNTHETIC:                               attribute = new SyntheticAttribute();                            break;
            case Attribute.SIGNATURE:                               attribute = new SignatureAttribute();                            break;
//            case Attribute.CONSTANT_VALUE:                          attribute = new ConstantValueAttribute();                        break;
//            case Attribute.METHOD_PARAMETERS:                       attribute = new MethodParametersAttribute();                     break;
//            case Attribute.EXCEPTIONS:                              attribute = new ExceptionsAttribute();                           break;
            case Attribute.CODE:                                    attribute = new CodeAttribute();                                 break;
//            case Attribute.STACK_MAP:                               attribute = new StackMapAttribute();                             break;
//            case Attribute.STACK_MAP_TABLE:                         attribute = new StackMapTableAttribute();                        break;
//            case Attribute.LINE_NUMBER_TABLE:                       attribute = new LineNumberTableAttribute();                      break;
//            case Attribute.LOCAL_VARIABLE_TABLE:                    attribute = new LocalVariableTableAttribute();                   break;
//            case Attribute.LOCAL_VARIABLE_TYPE_TABLE:               attribute = new LocalVariableTypeTableAttribute();               break;
            case Attribute.RUNTIME_VISIBLE_ANNOTATIONS:             attribute = new RuntimeVisibleAnnotationsAttribute();            break;
            case Attribute.RUNTIME_INVISIBLE_ANNOTATIONS:           attribute = new RuntimeInvisibleAnnotationsAttribute();          break;
            case Attribute.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS:   attribute = new RuntimeVisibleParameterAnnotationsAttribute();   break;
            case Attribute.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS: attribute = new RuntimeInvisibleParameterAnnotationsAttribute(); break;
            case Attribute.RUNTIME_VISIBLE_TYPE_ANNOTATIONS:        attribute = new RuntimeVisibleTypeAnnotationsAttribute();        break;
            case Attribute.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS:      attribute = new RuntimeInvisibleTypeAnnotationsAttribute();      break;
            case Attribute.ANNOTATION_DEFAULT:                      attribute = new AnnotationDefaultAttribute();                    break;
            case Attribute.MODULE:                                  attribute = new ModuleAttribute();                               break;
            case Attribute.MODULE_MAIN_CLASS:                       attribute = new ModuleMainClassAttribute();                      break;
            case Attribute.MODULE_PACKAGES:                         attribute = new ModulePackagesAttribute();                       break;
            default: throw new ParseException("Unknown attribute name " + name + ".", p.lineno());
        }

        attribute.u2attributeNameIndex = cpe.addUtf8Constant(name);
        return attribute;
    }
}
