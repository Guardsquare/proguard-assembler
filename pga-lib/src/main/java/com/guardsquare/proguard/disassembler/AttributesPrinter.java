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
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.module.*;
import proguard.classfile.attribute.module.visitor.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.editor.AttributesEditor;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.util.ArrayUtil;

import java.util.*;

/**
 * Prints most Attributes.
 *
 * @author Joachim Vandersmissen
 */
public class AttributesPrinter
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
    private final Printer p;

    /**
     * Constructs a new AttributesPrinter that uses a Printer.
     *
     * @param p the Printer to use to print basic structures.
     */
    public AttributesPrinter(Printer p)
    {
        this.p = p;
    }


    // Implementations for ClassVisitor.

    public void visitAnyClass(Clazz clazz) {}


    public void visitProgramClass(ProgramClass programClass)
    {
        Attribute[] valid = getValidAttributes(programClass,
                                               programClass.u2attributesCount,
                                               programClass.attributes);
        if (valid.length > 0)
        {
            p.printSpace();
            p.print(AssemblyConstants.ATTRIBUTES_OPEN);
            p.println();
            p.indent();
            for (int index = 0; index < valid.length; index++)
            {
                Attribute attribute = valid[index];
                String name = attribute.getAttributeName(programClass);
                p.printIndent();
                p.printWord(name);
                attribute.accept(programClass, this);
                p.println();
            }

            p.outdent();
            p.printIndent();
            p.print(AssemblyConstants.ATTRIBUTES_CLOSE);
        }
    }


    // Implementations for MemberVisitor.

    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        Attribute[] valid = getValidAttributes(programClass,
                                               programField.u2attributesCount,
                                               programField.attributes);
        if (valid.length > 0)
        {
            p.printSpace();
            p.print(AssemblyConstants.ATTRIBUTES_OPEN);
            p.println();
            p.indent();
            for (int index = 0; index < valid.length; index++)
            {
                Attribute attribute = valid[index];
                String name = attribute.getAttributeName(programClass);
                p.printIndent();
                p.printWord(name);
                attribute.accept(programClass, programField, this);
                p.println();
            }

            p.outdent();
            p.printIndent();
            p.print(AssemblyConstants.ATTRIBUTES_CLOSE);
        }
    }


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        // Processing info indicates that the method contains a code attribute
        // that can be printed using shorthand notation. Processing info will be
        // set to null if the code attribute is already printed here.
        programMethod.processingInfo =
            new AttributesEditor(programClass, programMethod, false).findAttribute(Attribute.CODE);

        Attribute[] valid = getValidAttributes(programClass,
                                               programMethod.u2attributesCount,
                                               programMethod.attributes);
        if (valid.length > 0)
        {
            p.printSpace();
            p.print(AssemblyConstants.ATTRIBUTES_OPEN);
            p.println();
            p.indent();
            for (int index = 0; index < valid.length; index++)
            {
                Attribute attribute = valid[index];
                String name = attribute.getAttributeName(programClass);
                p.printIndent();
                p.printWord(name);
                attribute.accept(programClass, programMethod, this);
                p.println();
            }

            p.outdent();
            p.printIndent();
            p.print(AssemblyConstants.ATTRIBUTES_CLOSE);
        }
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitBootstrapMethodsAttribute(Clazz clazz, BootstrapMethodsAttribute bootstrapMethodsAttribute)
    {
        p.printSpace();
        p.print(AssemblyConstants.BODY_OPEN);
        if (bootstrapMethodsAttribute.u2bootstrapMethodsCount > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < bootstrapMethodsAttribute.u2bootstrapMethodsCount; index++)
            {
                p.printIndent();
                visitBootstrapMethodInfo(clazz, bootstrapMethodsAttribute.bootstrapMethods[index]);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        p.print(AssemblyConstants.BODY_CLOSE);
    }


    public void visitSourceFileAttribute(Clazz clazz, SourceFileAttribute sourceFileAttribute)
    {
        p.printSpace();
        p.printString(clazz.getString(sourceFileAttribute.u2sourceFileIndex));
        p.print(AssemblyConstants.STATEMENT_END);
    }


    public void visitSourceDirAttribute(Clazz clazz, SourceDirAttribute sourceDirAttribute)
    {
        p.printSpace();
        p.printString(clazz.getString(sourceDirAttribute.u2sourceDirIndex));
        p.print(AssemblyConstants.STATEMENT_END);
    }


    public void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute)
    {
        p.printSpace();
        p.print(AssemblyConstants.BODY_OPEN);
        if (innerClassesAttribute.u2classesCount > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < innerClassesAttribute.u2classesCount; index++)
            {
                p.printIndent();
                visitInnerClassesInfo(clazz, innerClassesAttribute.classes[index]);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        p.print(AssemblyConstants.BODY_CLOSE);
    }


    public void visitEnclosingMethodAttribute(Clazz clazz, EnclosingMethodAttribute enclosingMethodAttribute)
    {
        p.printSpace();
        p.printType(ClassUtil.internalTypeFromClassType(enclosingMethodAttribute.getClassName(clazz)));

        // Enclosing method is optional.
        if (enclosingMethodAttribute.u2nameAndTypeIndex != 0)
        {
            p.print(AssemblyConstants.REFERENCE_SEPARATOR);
            p.printMethodReturnType(enclosingMethodAttribute.getType(clazz));
            p.printSpace();
            p.printWord(enclosingMethodAttribute.getName(clazz));
            p.printMethodArguments(enclosingMethodAttribute.getType(clazz));
        }

        p.print(AssemblyConstants.STATEMENT_END);
    }


    public void visitNestHostAttribute(Clazz clazz, NestHostAttribute nestHostAttribute)
    {
        p.printSpace();
        p.printType(ClassUtil.internalTypeFromClassType(nestHostAttribute.getHostClassName(clazz)));
        p.print(AssemblyConstants.STATEMENT_END);
    }


    public void visitNestMembersAttribute(Clazz clazz, NestMembersAttribute nestMembersAttribute)
    {
        p.printSpace();
        p.print(AssemblyConstants.BODY_OPEN);
        if (nestMembersAttribute.u2classesCount > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < nestMembersAttribute.u2classesCount; index++)
            {
                p.printIndent();
                p.printType(ClassUtil.internalTypeFromClassType(clazz.getClassName(nestMembersAttribute.u2classes[index])));
                p.print(AssemblyConstants.STATEMENT_END);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        p.print(AssemblyConstants.BODY_CLOSE);
    }


    public void visitModuleAttribute(Clazz clazz, ModuleAttribute moduleAttribute)
    {
        p.printSpace();
        if (printModuleAccessFlags(moduleAttribute.u2moduleFlags))
        {
            p.printSpace();
        }

        // Modules are not encoded in internal form like class and interface
        // names...
        p.printWord(clazz.getModuleName(moduleAttribute.u2moduleNameIndex));
        p.printSpace();

        // Module version is optional.
        if (moduleAttribute.u2moduleVersionIndex != 0)
        {
            p.printWord(clazz.getString(moduleAttribute.u2moduleVersionIndex));
            p.printSpace();
        }

        p.print(AssemblyConstants.BODY_OPEN);
        if (moduleAttribute.u2requiresCount > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < moduleAttribute.u2requiresCount; index++)
            {
                p.printIndent();
                p.printWord(AssemblyConstants.REQUIRES);
                p.printSpace();
                visitRequiresInfo(clazz, moduleAttribute.requires[index]);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        if (moduleAttribute.u2exportsCount > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < moduleAttribute.u2exportsCount; index++)
            {
                p.printIndent();
                p.printWord(AssemblyConstants.EXPORTS);
                p.printSpace();
                visitExportsInfo(clazz, moduleAttribute.exports[index]);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        if (moduleAttribute.u2opensCount > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < moduleAttribute.u2opensCount; index++)
            {
                p.printIndent();
                p.printWord(AssemblyConstants.OPENS);
                p.printSpace();
                visitOpensInfo(clazz, moduleAttribute.opens[index]);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        if (moduleAttribute.u2usesCount > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < moduleAttribute.u2usesCount; index++)
            {
                p.printIndent();
                p.printWord(AssemblyConstants.USES);
                p.printSpace();
                p.printType(ClassUtil.internalTypeFromClassType(clazz.getClassName(moduleAttribute.u2uses[index])));
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        if (moduleAttribute.u2providesCount > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < moduleAttribute.u2providesCount; index++)
            {
                p.printIndent();
                p.printWord(AssemblyConstants.PROVIDES);
                p.printSpace();
                visitProvidesInfo(clazz, moduleAttribute.provides[index]);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        p.print(AssemblyConstants.BODY_CLOSE);
    }


    public void visitModuleMainClassAttribute(Clazz clazz, ModuleMainClassAttribute moduleMainClassAttribute)
    {
        p.printSpace();
        p.printType(ClassUtil.internalTypeFromClassType(moduleMainClassAttribute.getMainClassName(clazz)));
        p.print(AssemblyConstants.STATEMENT_END);
    }


    public void visitModulePackagesAttribute(Clazz clazz, ModulePackagesAttribute modulePackagesAttribute)
    {
        p.printSpace();
        p.print(AssemblyConstants.BODY_OPEN);
        if (modulePackagesAttribute.u2packagesCount > 0)
        {
            p.println();
            p.indent();
            for (int index = 0; index < modulePackagesAttribute.u2packagesCount; index++)
            {
                p.printIndent();
                p.printWord(clazz.getPackageName(modulePackagesAttribute.u2packages[index]));
                p.print(AssemblyConstants.STATEMENT_END);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        p.print(AssemblyConstants.BODY_CLOSE);
    }


    public void visitDeprecatedAttribute(Clazz clazz, DeprecatedAttribute deprecatedAttribute)
    {
        p.print(AssemblyConstants.STATEMENT_END);
    }


    public void visitSyntheticAttribute(Clazz clazz, SyntheticAttribute syntheticAttribute)
    {
        p.print(AssemblyConstants.STATEMENT_END);
    }


    public void visitSignatureAttribute(Clazz clazz, SignatureAttribute signatureAttribute)
    {
        p.printSpace();
        p.printString(signatureAttribute.getSignature(clazz));
        p.print(AssemblyConstants.STATEMENT_END);
    }


    public void visitConstantValueAttribute(Clazz clazz, Field field, ConstantValueAttribute constantValueAttribute)
    {
        p.printSpace();
        clazz.constantPoolEntryAccept(constantValueAttribute.u2constantValueIndex, new ConstantPrinter(p, true));
        p.print(AssemblyConstants.STATEMENT_END);
    }


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        method.setProcessingInfo(null);

        Map<Integer, String> oldLabels = p.labels;
        p.labels = new HashMap<>();
        codeAttribute.accept(clazz, method, new LabelsCollector(p.labels));

        Attribute[] valid = getValidAttributes(clazz,
                                               codeAttribute.u2attributesCount,
                                               codeAttribute.attributes);
        if (valid.length > 0)
        {
            p.printSpace();
            p.print(AssemblyConstants.ATTRIBUTES_OPEN);
            p.println();
            p.indent();
            for (int index = 0; index < valid.length; index++)
            {
                Attribute attribute = valid[index];
                String name = attribute.getAttributeName(clazz);
                p.printIndent();
                p.printWord(name);
                attribute.accept(clazz, method, codeAttribute, this);
                p.println();
            }

            p.outdent();
            p.printIndent();
            p.print(AssemblyConstants.ATTRIBUTES_CLOSE);
        }

        codeAttribute.accept(clazz, method, new InstructionsPrinter(p));
        p.labels = oldLabels;
    }


    public void visitAnyAnnotationsAttribute(Clazz clazz, AnnotationsAttribute annotationsAttribute)
    {
        annotationsAttribute.accept(clazz, new AnnotationsPrinter(p));
    }


    public void visitAnyParameterAnnotationsAttribute(Clazz clazz, Method method, ParameterAnnotationsAttribute parameterAnnotationsAttribute)
    {
        parameterAnnotationsAttribute.accept(clazz, method, new AnnotationsPrinter(p));
    }


    public void visitAnnotationDefaultAttribute(Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute)
    {
        annotationDefaultAttribute.accept(clazz, method, new AnnotationsPrinter(p));
    }


    // Implementations for BootstrapMethodInfoVisitor.

    public void visitBootstrapMethodInfo(Clazz clazz, BootstrapMethodInfo bootstrapMethodInfo)
    {
        clazz.constantPoolEntryAccept(bootstrapMethodInfo.u2methodHandleIndex, new ConstantPrinter(p, false));
        p.print(AssemblyConstants.BODY_OPEN);
        if (bootstrapMethodInfo.u2methodArgumentCount > 0)
        {
            ConstantPrinter constantPrinter = new ConstantPrinter(p, true);
            p.println();
            p.indent();
            for (int index = 0; index < bootstrapMethodInfo.u2methodArgumentCount; index++)
            {
                p.printIndent();
                clazz.constantPoolEntryAccept(bootstrapMethodInfo.u2methodArguments[index], constantPrinter);
                p.print(AssemblyConstants.STATEMENT_END);
                p.println();
            }

            p.outdent();
            p.printIndent();
        }

        p.print(AssemblyConstants.BODY_CLOSE);
    }


    // Implementations for InnerClassesInfoVisitor.

    public void visitInnerClassesInfo(Clazz clazz, InnerClassesInfo innerClassesInfo)
    {
        if (p.printClassAccessFlags(innerClassesInfo.u2innerClassAccessFlags))
        {
            p.printSpace();
        }

        p.printWord(ClassUtil.externalClassName(clazz.getClassName(innerClassesInfo.u2innerClassIndex)));

        // Inner name is optional.
        if (innerClassesInfo.u2innerNameIndex != 0)
        {
            p.printSpace();
            p.printWord(AssemblyConstants.AS);
            p.printSpace();
            p.printWord(clazz.getString(innerClassesInfo.u2innerNameIndex));
        }

        // Outer class is optional.
        if (innerClassesInfo.u2outerClassIndex != 0)
        {
            p.printSpace();
            p.printWord(AssemblyConstants.IN);
            p.printSpace();
            p.printType(ClassUtil.internalTypeFromClassType(clazz.getClassName(innerClassesInfo.u2outerClassIndex)));
        }

        p.print(AssemblyConstants.STATEMENT_END);
    }


    // Implementations for ExportsInfoVisitor.

    public void visitExportsInfo(Clazz clazz, ExportsInfo exportsInfo)
    {
        if (p.printAccessFlags(exportsInfo.u2exportsFlags))
        {
            p.printSpace();
        }

        p.printWord(clazz.getPackageName(exportsInfo.u2exportsIndex));
        if (exportsInfo.u2exportsToCount > 0)
        {
            p.printSpace();
            p.printWord(AssemblyConstants.TO);
            p.printSpace();
            for (int index = 0; index < exportsInfo.u2exportsToCount; index++)
            {
                // Modules are not encoded in internal form like class and
                // interface names...
                p.printWord(clazz.getModuleName(exportsInfo.u2exportsToIndex[index]));
                if (index < exportsInfo.u2exportsToCount - 1)
                {
                    p.print(JavaTypeConstants.METHOD_ARGUMENTS_SEPARATOR);
                    p.printSpace();
                }
            }
        }

        p.print(AssemblyConstants.STATEMENT_END);
    }


    // Implementations for OpensInfoVisitor.

    public void visitOpensInfo(Clazz clazz, OpensInfo opensInfo)
    {
        if (p.printAccessFlags(opensInfo.u2opensFlags))
        {
            p.printSpace();
        }

        p.printWord(clazz.getPackageName(opensInfo.u2opensIndex));
        if (opensInfo.u2opensToCount > 0)
        {
            p.printSpace();
            p.printWord(AssemblyConstants.TO);
            p.printSpace();
            for (int index = 0; index < opensInfo.u2opensToCount; index++)
            {
                // Modules are not encoded in internal form like class and
                // interface names...
                p.printWord(clazz.getModuleName(opensInfo.u2opensToIndex[index]));
                if (index < opensInfo.u2opensToCount - 1)
                {
                    p.print(JavaTypeConstants.METHOD_ARGUMENTS_SEPARATOR);
                    p.printSpace();
                }
            }
        }

        p.print(AssemblyConstants.STATEMENT_END);
    }


    // Implementations for ProvidesInfoVisitor.

    public void visitProvidesInfo(Clazz clazz, ProvidesInfo providesInfo)
    {
        p.printType(ClassUtil.internalTypeFromClassType(clazz.getClassName(providesInfo.u2providesIndex)));
        if (providesInfo.u2providesWithCount > 0)
        {
            p.printSpace();
            p.printWord(AssemblyConstants.WITH);
            p.printSpace();
            for (int index = 0; index < providesInfo.u2providesWithCount; index++)
            {
                p.printType(ClassUtil.internalTypeFromClassType(clazz.getClassName(providesInfo.u2providesWithIndex[index])));
                if (index < providesInfo.u2providesWithCount - 1)
                {
                    p.print(JavaTypeConstants.METHOD_ARGUMENTS_SEPARATOR);
                    p.printSpace();
                }
            }
        }

        p.print(AssemblyConstants.STATEMENT_END);
    }


    // Implementations for RequiresInfoVisitor.

    public void visitRequiresInfo(Clazz clazz, RequiresInfo requiresInfo)
    {
        if (p.printAccessFlags(requiresInfo.u2requiresFlags))
        {
            p.printSpace();
        }

        // Modules are not encoded in internal form like class and interface
        // names...
        p.printWord(clazz.getModuleName(requiresInfo.u2requiresIndex));

        // Requires info version is optional.
        if (requiresInfo.u2requiresVersionIndex != 0)
        {
            p.printSpace();
            p.printWord(clazz.getString(requiresInfo.u2requiresVersionIndex));
        }

        p.print(AssemblyConstants.STATEMENT_END);
    }


    // Small utility methods.

    /**
     * Returns the valid attributes from the provided attributes.
     * An attribute is valid if the name of the attribute is valid, and, if the
     * attribute is a CodeAttribute, the amount of valid attributes of the
     * CodeAttribute is greater than 0
     *
     * @param clazz           the class being disassembled.
     * @param attributesCount the amount of original attributes.
     * @param attributes      the original attributes.
     * @return The valid attributes.
     */
    private static Attribute[] getValidAttributes(Clazz clazz, int attributesCount, Attribute[] attributes)
    {
        Attribute[] valid = new Attribute[0];
        for (int index = 0; index < attributesCount; index++)
        {
            Attribute attribute = attributes[index];
            if (isValidAttributeName(attribute.getAttributeName(clazz)))
            {
                if (attribute instanceof CodeAttribute)
                {
                    CodeAttribute codeAttribute = (CodeAttribute) attribute;
                    if (getValidAttributes(clazz, codeAttribute.u2attributesCount, codeAttribute.attributes).length == 0)
                    {
                        // "invalid" CodeAttributes are handled using shorthand notation.
                        continue;
                    }
                }

                valid = ArrayUtil.add(valid, valid.length, attribute);
            }
        }

        return valid;
    }


    /**
     * Returns whether an attribute name is valid (i.e. would be parsed by the
     * Assembler)
     *
     * @param name the attribute name to check.
     * @return true if the attribute name would be parsed by the Assembler,
     *         false otherwise.
     */
    private static boolean isValidAttributeName(String name)
    {
        switch (name)
        {
            // Commented out attributes are handled in a special way.
            case Attribute.BOOTSTRAP_METHODS:
            case Attribute.SOURCE_FILE:
            case Attribute.SOURCE_DIR:
            case Attribute.INNER_CLASSES:
            case Attribute.ENCLOSING_METHOD:
            case Attribute.NEST_HOST:
            case Attribute.NEST_MEMBERS:
            case Attribute.DEPRECATED:
            case Attribute.SYNTHETIC:
            case Attribute.SIGNATURE:
//            case Attribute.CONSTANT_VALUE:
//            case Attribute.METHOD_PARAMETERS:
//            case Attribute.EXCEPTIONS:
            case Attribute.CODE:
//            case Attribute.STACK_MAP:
//            case Attribute.STACK_MAP_TABLE:
//            case Attribute.LINE_NUMBER_TABLE:
//            case Attribute.LOCAL_VARIABLE_TABLE:
//            case Attribute.LOCAL_VARIABLE_TYPE_TABLE:
            case Attribute.RUNTIME_VISIBLE_ANNOTATIONS:
            case Attribute.RUNTIME_INVISIBLE_ANNOTATIONS:
            case Attribute.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS:
            case Attribute.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS:
            case Attribute.RUNTIME_VISIBLE_TYPE_ANNOTATIONS:
            case Attribute.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS:
            case Attribute.ANNOTATION_DEFAULT:
            case Attribute.MODULE:
            case Attribute.MODULE_MAIN_CLASS:
            case Attribute.MODULE_PACKAGES:                       return true;
            default:                                                       return false;
        }
    }


    /**
     * Prints ModuleAttribute access flags.
     *
     * @param accessFlags the access flags.
     * @return true if at least one access flag was written, false otherwise.
     */
    public boolean printModuleAccessFlags(int accessFlags)
    {
        if (accessFlags == 0)
        {
            return false;
        }

        StringBuilder stringBuilder = new StringBuilder();
        if ((accessFlags & AccessConstants.OPEN) != 0)      stringBuilder.append(JavaAccessConstants.OPEN).append(' ');
        if ((accessFlags & AccessConstants.SYNTHETIC) != 0) stringBuilder.append(JavaAccessConstants.SYNTHETIC).append(' ');
        if ((accessFlags & AccessConstants.MANDATED) != 0)  stringBuilder.append(JavaAccessConstants.MANDATED).append(' ');
        p.printWord(stringBuilder.toString().trim());
        return true;
    }
}
