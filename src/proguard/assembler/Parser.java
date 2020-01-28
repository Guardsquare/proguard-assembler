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
import proguard.classfile.attribute.annotation.ElementValue;
import proguard.classfile.constant.*;
import proguard.classfile.editor.ConstantPoolEditor;
import proguard.classfile.util.ClassUtil;

import java.io.*;
import java.util.*;

/**
 * General purpose parser.
 *
 * @author Joachim Vandersmissen
 */
public class Parser extends StreamTokenizer
{
    public final Map<String, String> imports = new HashMap<>();

    public Map<String, Integer> labels;

    /**
     * Creates a new Parser that reads from te given reader.
     * The Parser will be initialized to recognize slash slash comments, slash
     * star comments, and dollar sign and underscore words.
     *
     * @param reader the reader to read from.
     */
    public Parser(Reader reader)
    {
        super(reader);
        slashSlashComments(true);
        slashStarComments(true);
        // Only valid java identifiers
        wordChars(JavaTypeConstants.SPECIAL_MEMBER_SEPARATOR,
                  JavaTypeConstants.SPECIAL_MEMBER_SEPARATOR);
        wordChars('_', '_');
    }


    /**
     * Provides a human readable representation of a token type.
     *
     * @param ttype the token type.
     * @return the string representation.
     */
    private String toString(int ttype)
    {
        switch (ttype)
        {
            case TT_EOF:                         return "eof";
            case TT_EOL:                         return "eol";
            case TT_NUMBER:                      return "number";
            case TT_WORD:                        return "word";
            case AssemblyConstants.STRING_QUOTE: return "string";
            case AssemblyConstants.CHAR_QUOTE:   return "char";
            default:                             return String.valueOf((char) ttype);
        }
    }


    /**
     * Advances to the next token.
     *
     * @return the next token type.
     * @throws ParseException if an IOException occured while reading the token.
     */
    private int next()
    {
        try
        {
            return nextToken();
        }
        catch (IOException e)
        {
            throw new ParseException("An IOException occured while reading next token.", lineno(), e);
        }
    }


    /**
     * Throws a descriptive ParseException if the next token type does not equal
     * the provided token type.
     *
     * @param ttype    the token type to match.
     * @param typeName the name of the token type to match, used in the
     *                 ParseException message if needed.
     * @throws ParseException if the next token type does not equal the
     *                        provided token type.
     */
    private void expect(int ttype, String typeName)
    {
        if (next() != ttype)
        {
            throw new ParseException("Expected "          +
                                     typeName             +
                                     " but got "          +
                                     toString(this.ttype) +
                                     ".", lineno());
        }
    }


    /**
     * Constructs and throws a descriptive ParseException where one of multiple
     * keywords was expected.
     *
     * @param keywords the keywords that were expected.
     * @throws ParseException always, as described by the javadoc documentation.
     */
    public void throwKeywordError(String... keywords)
    {
        StringBuilder message = new StringBuilder();
        message.append("Expected one of: ");
        for (String keyword : keywords)
        {
            message.append('\'').append(keyword).append("', ");
        }

        message.append("but got: '").append(sval).append("'.");
        throw new ParseException(message.toString(), lineno());
    }


    /**
     * Throws a descriptive ParseException if the next token type does not equal
     * TT_NUMBER.
     *
     * @param typeName the name of the token type to match, used in the
     *                 ParseException message if needed.
     * @return the next token, if the next token type equals TT_NUMBER.
     * @throws ParseException if the next token type does not equal TT_NUMBER.
     */
    public double expectNumber(String typeName)
    {
        expect(TT_NUMBER, typeName);
        return nval;
    }


    /**
     * Throws a descriptive ParseException if the next token type does not equal
     * TT_WORD.
     *
     * @param typeName the name of the token type to match, used in the
     *                 ParseException message if needed.
     * @return the next token, if the next token type equals TT_WORD.
     * @throws ParseException if the next token type does not equal TT_WORD.
     */
    public String expectWord(String typeName)
    {
        expect(TT_WORD, typeName);
        return sval;
    }


    /**
     * Throws a descriptive ParseException if the next token is not a quoted
     * string.
     *
     * @param typeName the name of the token type to match, used in the
     *                 ParseException message if needed.
     * @return the next token, if the next token is a quoted string.
     * @throws ParseException if the next token is not a quoted string.
     */
    public String expectString(String typeName)
    {
        expect(AssemblyConstants.STRING_QUOTE, typeName);
        return sval;
    }


    /**
     * Throws a descriptive ParseException if the next token type does not equal
     * some special char.
     *
     * @param special  the special char.
     * @param typeName the name of the token type to match, used in the
     *                 ParseException message if needed.
     * @return the next token, if the next token type equals the char.
     * @throws ParseException if the next token type does not equal the char.
     */
    public void expect(char special, String typeName)
    {
        expect((int) special, typeName + " '" + special + "'");
    }


    /**
     * Throws a descriptive ParseException if the next token type does not equal
     * TT_WORD or the provided keywords do not contain the next token.
     *
     * @param keywords the possible keywords to match.
     * @throws ParseException if the next token type does not equal TT_WORD or
     *                        the provided keywords do not contain the next
     *                        token.
     * @return the keyword that matched.
     */
    public String expect(String... keywords)
    {
        StringBuilder typeName = new StringBuilder("one of: ");
        for (String keyword : keywords)
        {
            typeName.append('\'').append(keyword).append("', ");
        }

        String word = expectWord(typeName.toString().trim());
        for (int index = 0; index < keywords.length; index++)
        {
            if (Objects.equals(word, keywords[index]))
            {
                return word;
            }
        }

        throwKeywordError(keywords);
        return null;
    }


    /**
     * Returns whether the next token type equals the provided token type.
     * If next token type does not equal the provided token type, the Parser
     * does not advance, otherwise the Parser advances to the next token.
     *
     * @param ttype the token type to match.
     * @return true if the next token type equals the provided token type, false
     *         otherwise.
     */
    public boolean nextTtypeEquals(int ttype)
    {
        if (next() == ttype)
        {
            return true;
        }

        pushBack();
        return false;
    }


    /**
     * Returns whether the next token type equals TT_NUMBER.
     * If next token type does not equal TT_NUMBER, the Parser does not advance,
     * otherwise the Parser advances to the next token.
     *
     * @return true if the next token type equals TT_NUMBER, false otherwise.
     */
    public boolean nextTtypeEqualsNumber()
    {
        return nextTtypeEquals(TT_NUMBER);
    }


    /**
     * Returns whether the next token type equals TT_WORD.
     * If next token type does not equal TT_WORD, the Parser does not advance,
     * otherwise the Parser advances to the next token.
     *
     * @return true if the next token type equals TT_WORD, false otherwise.
     */
    public boolean nextTtypeEqualsWord()
    {
        return nextTtypeEquals(TT_WORD);
    }


    /**
     * Returns whether the next next token is a quoted string.
     * If next token is not a quoted string, the Parser does not advance,
     * otherwise the Parser advances to the next token.
     *
     * @return true if the next token is a quoted string, false otherwise.
     */
    public boolean nextTtypeEqualsString()
    {
        return nextTtypeEquals(AssemblyConstants.STRING_QUOTE);
    }


    /**
     * Returns whether the next next token is a quoted char.
     * If next token is not a char string, the Parser does not advance,
     * otherwise the Parser advances to the next token.
     *
     * @return true if the next token is a quoted char, false otherwise.
     */
    public boolean nextTtypeEqualsChar()
    {
        return nextTtypeEquals(AssemblyConstants.CHAR_QUOTE);
    }


    /**
     * Returns true if the next token type equals TT_WORD and the provided
     * keywords contain the next token.
     * If next token type does not equal TT_WORD, the Parser does not advance,
     * otherwise the Parser advances to the next token.
     * If the provided keywords do not contain the next token, a descriptive
     * ParseException is thrown.
     *
     * @param keywords the possible keywords to match.
     * @return true if the next token type equals TT_WORD and the provided
     *         keywords contain the next token, false if the next token type
     *         does not equal TT_WORD.
     * @throws ParseException if the next token type equals TT_WORD, but the
     *                        provided keywords do not contain the next token.
     */
    public boolean expectIfNextTtypeEqualsWord(String... keywords)
    {
        if (nextTtypeEqualsWord())
        {
            pushBack();
            expect(keywords);
            return true;
        }

        return false;
    }


    /**
     * Parses an internal field type.
     *
     * @param typeName the name of the internal field type to parse, used in the
     *                 ParseException message if needed.
     * @return the internal field type.
     * @throws ParseException if the internal field type is malformatted.
     */
    public String expectType(String typeName)
    {
        String className = expectWord(typeName);
        if (imports.containsKey(className))
        {
            className = imports.get(className);
        }

        StringBuilder type = new StringBuilder(className);
        while (nextTtypeEquals(JavaTypeConstants.ARRAY.charAt(0)))
        {
            expect(JavaTypeConstants.ARRAY.charAt(1), "array type end");
            type.append(JavaTypeConstants.ARRAY);
        }

        return ClassUtil.internalType(type.toString());
    }


    /**
     * Parses internal method arguments.
     *
     * @param typeName the name of the internal method arguments to parse, used
     *                 in the ParseException message if needed.
     * @return the internal method arguments.
     * @throws ParseException if the internal method arguments are malformatted.
     */
    public String expectMethodArguments(String typeName)
    {
        StringBuilder methodArguments = new StringBuilder();
        expect(JavaTypeConstants.METHOD_ARGUMENTS_OPEN, "method arguments open");
        methodArguments.append(JavaTypeConstants.METHOD_ARGUMENTS_OPEN);
        if (nextTtypeEquals(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE))
        {
            return methodArguments.append(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE).toString();
        }

        while (true)
        {
            methodArguments.append(expectType(typeName));
            if (nextTtypeEquals(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE))
            {
                return methodArguments.append(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE).toString();
            }

            expect(JavaTypeConstants.METHOD_ARGUMENTS_SEPARATOR, "method arguments separator");
        }
    }


    /**
     * Parses an internal method name.
     *
     * @param typeName the name of the internal method name to parse, used in
     *                 the ParseException message if needed.
     * @return the internal method name.
     * @throws ParseException if the internal method name is malformatted.
     */
    public String expectMethodName(String typeName)
    {
        if (nextTtypeEquals(AssemblyConstants.SPECIAL_METHOD_PREFIX))
        {
            String name = expect(AssemblyConstants.INIT,
                                 AssemblyConstants.CLINIT);
            expect(AssemblyConstants.SPECIAL_METHOD_SUFFIX, "special method suffix");
            return AssemblyConstants.SPECIAL_METHOD_PREFIX +
                   name                                    +
                   AssemblyConstants.SPECIAL_METHOD_SUFFIX;
        }

        return expectWord(typeName);
    }


    /**
     * Parses one or more access flags. This method will stop parsing if the
     * next token type does not equal a word, or the next token is not
     * recognized as an access flag.
     * Note that no compatibility checking for incompatible access flags is
     * performed.
     *
     * @return the access flags, in bit vector format.
     */
    public int expectAccessFlags()
    {
        int accessFlags = 0;
        while (nextTtypeEqualsWord())
        {
            switch (sval)
            {
                case JavaAccessConstants.PUBLIC:         accessFlags |= AccessConstants.PUBLIC;       break;
                case JavaAccessConstants.PRIVATE:        accessFlags |= AccessConstants.PRIVATE;      break;
                case JavaAccessConstants.PROTECTED:      accessFlags |= AccessConstants.PROTECTED;    break;
                case JavaAccessConstants.STATIC:         accessFlags |= AccessConstants.STATIC;       break;
                case JavaAccessConstants.FINAL:          accessFlags |= AccessConstants.FINAL;        break;
                case JavaAccessConstants.SUPER:          accessFlags |= AccessConstants.SUPER;        break;
                case JavaAccessConstants.SYNCHRONIZED:   accessFlags |= AccessConstants.SYNCHRONIZED; break;
                case JavaAccessConstants.VOLATILE:       accessFlags |= AccessConstants.VOLATILE;     break;
                case JavaAccessConstants.TRANSIENT:      accessFlags |= AccessConstants.TRANSIENT;    break;
                case JavaAccessConstants.BRIDGE:         accessFlags |= AccessConstants.BRIDGE;       break;
                case JavaAccessConstants.VARARGS:        accessFlags |= AccessConstants.VARARGS;      break;
                case JavaAccessConstants.NATIVE:         accessFlags |= AccessConstants.NATIVE;       break;
                case JavaAccessConstants.INTERFACE:      accessFlags |= AccessConstants.INTERFACE;    break;
                case JavaAccessConstants.ABSTRACT:       accessFlags |= AccessConstants.ABSTRACT;     break;
                case JavaAccessConstants.STRICT:         accessFlags |= AccessConstants.STRICT;       break;
                case JavaAccessConstants.SYNTHETIC:      accessFlags |= AccessConstants.SYNTHETIC;    break;
                case AssemblyConstants.ACC_ANNOTATION: accessFlags |= AccessConstants.ANNOTATION;   break;
                case JavaAccessConstants.ENUM:           accessFlags |= AccessConstants.ENUM;         break;
                case JavaAccessConstants.MANDATED:       accessFlags |= AccessConstants.MANDATED;     break;
                case JavaAccessConstants.MODULE:         accessFlags |= AccessConstants.MODULE;       break;
                case JavaAccessConstants.OPEN:           accessFlags |= AccessConstants.OPEN;         break;
                case JavaAccessConstants.TRANSITIVE:     accessFlags |= AccessConstants.TRANSITIVE;   break;
                case JavaAccessConstants.STATIC_PHASE:   accessFlags |= AccessConstants.STATIC_PHASE; break;
                default:                               pushBack();                                     return accessFlags;
            }
        }

        return accessFlags;
    }


    /**
     * Parses one or more class access flags. This method provides syntactic
     * sugar for enums, modules, interfaces, and annotations.
     *
     * @return the access flags, in bit vector format.
     */
    public int expectClassAccessFlags()
    {
        int accessFlags = 0;
        while (nextTtypeEqualsWord())
        {
            switch (sval)
            {
                case JavaAccessConstants.PUBLIC:         accessFlags |= AccessConstants.PUBLIC;       break;
                case JavaAccessConstants.PRIVATE:        accessFlags |= AccessConstants.PRIVATE;      break;
                case JavaAccessConstants.PROTECTED:      accessFlags |= AccessConstants.PROTECTED;    break;
                case JavaAccessConstants.STATIC:         accessFlags |= AccessConstants.STATIC;       break;
                case JavaAccessConstants.FINAL:          accessFlags |= AccessConstants.FINAL;        break;
                case JavaAccessConstants.SUPER:          accessFlags |= AccessConstants.SUPER;        break;
                case JavaAccessConstants.SYNCHRONIZED:   accessFlags |= AccessConstants.SYNCHRONIZED; break;
                case JavaAccessConstants.VOLATILE:       accessFlags |= AccessConstants.VOLATILE;     break;
                case JavaAccessConstants.TRANSIENT:      accessFlags |= AccessConstants.TRANSIENT;    break;
                case JavaAccessConstants.BRIDGE:         accessFlags |= AccessConstants.BRIDGE;       break;
                case JavaAccessConstants.VARARGS:        accessFlags |= AccessConstants.VARARGS;      break;
                case JavaAccessConstants.NATIVE:         accessFlags |= AccessConstants.NATIVE;       break;
                case JavaAccessConstants.ABSTRACT:       accessFlags |= AccessConstants.ABSTRACT;     break;
                case JavaAccessConstants.STRICT:         accessFlags |= AccessConstants.STRICT;       break;
                case JavaAccessConstants.SYNTHETIC:      accessFlags |= AccessConstants.SYNTHETIC;    break;
                case JavaAccessConstants.MANDATED:       accessFlags |= AccessConstants.MANDATED;     break;
                case JavaAccessConstants.OPEN:           accessFlags |= AccessConstants.OPEN;         break;
                case JavaAccessConstants.TRANSITIVE:     accessFlags |= AccessConstants.TRANSITIVE;   break;
                case JavaAccessConstants.STATIC_PHASE:   accessFlags |= AccessConstants.STATIC_PHASE; break;
                default:
                {
                    // Syntactic sugar: module notation for modules.
                    if (JavaAccessConstants.MODULE.equals(sval))
                    {
                        return accessFlags |
                               AccessConstants.MODULE;
                    }

                    // Syntactic sugar: enum notation for enums.
                    if (JavaAccessConstants.ENUM.equals(sval))
                    {
                        // Syntactic sugar: adding ACC_SUPER to enums automatically.
                        return accessFlags              |
                               AccessConstants.SUPER |
                               AccessConstants.ENUM;
                    }

                    if (JavaAccessConstants.INTERFACE.equals(sval))
                    {
                        // Syntactic sugar: adding ACC_ABSTRACT to interfaces automatically.
                        return accessFlags                  |
                               AccessConstants.INTERFACE |
                               AccessConstants.ABSTRACT;
                    }

                    if (AssemblyConstants.CLASS.equals(sval))
                    {
                        // Syntactic sugar: adding ACC_SUPER to classes automatically.
                        return accessFlags |
                               AccessConstants.SUPER;
                    }

                    throwKeywordError(JavaAccessConstants.ENUM,
                                      JavaAccessConstants.INTERFACE,
                                      JavaAccessConstants.MODULE,
                                      AssemblyConstants.CLASS);
                }
            }
        }

        // Syntactic sugar: @interface notation for annotations.
        expect(ElementValue.TAG_ANNOTATION, "annotation class");
        expectWord(JavaAccessConstants.INTERFACE);
        // Syntactic sugar: adding ACC_ABSTRACT to interfaces automatically.
        return accessFlags                  |
               AccessConstants.INTERFACE |
               AccessConstants.ABSTRACT  |
               AccessConstants.ANNOTATION;
    }


    /**
     * Parses a loadable constant and returns its index in the constant pool.
     *
     * @param clazz the Class for which to parse the constant.
     * @param cpe   the ConstantPoolEditor to use to add constants to the Class,
     *              if necessary.
     * @param cp    the ConstantParser to use to parse constants, if necessary.
     * @return the index of the loadable constant.
     * @throws ParseException if the loadable constant is malformatted.
     */
    public int expectLoadableConstant(Clazz clazz, ConstantPoolEditor cpe, ConstantParser cp)
    {
        // Simple type detection: java cast format
        if (nextTtypeEquals(JavaTypeConstants.METHOD_ARGUMENTS_OPEN))
        {
            String type = expectWord("loadable constant type");
            expect(JavaTypeConstants.METHOD_ARGUMENTS_CLOSE, "loadable constant type close");
            Constant loadableConstant;
            switch (type)
            {
                case JavaTypeConstants.BOOLEAN:
                case JavaTypeConstants.BYTE:
                case JavaTypeConstants.CHAR:
                case JavaTypeConstants.INT:
                case JavaTypeConstants.SHORT:              cp.visitIntegerConstant(clazz, null);      break;
                case JavaTypeConstants.DOUBLE:             cp.visitDoubleConstant(clazz, null);       break;
                case JavaTypeConstants.FLOAT:              cp.visitFloatConstant(clazz, null);        break;
                case JavaTypeConstants.LONG:               cp.visitLongConstant(clazz, null);         break;
                case AssemblyConstants.TYPE_STRING:        cp.visitStringConstant(clazz, null);       break;
                case AssemblyConstants.TYPE_CLASS:         cp.visitClassConstant(clazz, null);        break;
                case AssemblyConstants.TYPE_METHOD_HANDLE: cp.visitMethodHandleConstant(clazz, null); break;
                case AssemblyConstants.TYPE_METHOD_TYPE:   cp.visitMethodTypeConstant(clazz, null);   break;
                case AssemblyConstants.TYPE_DYNAMIC:       cp.visitDynamicConstant(clazz, null);      break;
                default:                                   throw new ParseException("Unknown loadable constant type " + type + ".", lineno());
            }

            return cp.getIndex();
        }

        // Difficult type detection: inferring from format.
        if (nextTtypeEqualsChar())
        {
            pushBack();
            cp.visitIntegerConstant(clazz, null);
            return cp.getIndex();
        }

        if (nextTtypeEqualsString())
        {
            pushBack();
            cp.visitStringConstant(clazz, null);
            return cp.getIndex();
        }

        if (nextTtypeEqualsNumber())
        {
            double number = nval;
            if (nextTtypeEqualsWord())
            {
                String literalType = sval.toUpperCase();
                switch (literalType)
                {
                    // Java doubles, floats, longs can end with D (or d),
                    // F (or f), L (or l) respectively.
                    case JavaTypeConstants.DOUBLE: return cpe.addDoubleConstant(number);
                    case JavaTypeConstants.FLOAT:  return cpe.addFloatConstant((float) number);
                    case JavaTypeConstants.LONG:   return cpe.addLongConstant((long) number);
                    default:                       pushBack();
                }
            }

            return cpe.addIntegerConstant((int) number);
        }

        if (nextTtypeEqualsWord())
        {
            if (AssemblyConstants.TRUE.equals(sval) ||
                AssemblyConstants.FALSE.equals(sval))
            {
                pushBack();
                cp.visitIntegerConstant(clazz, null);
                return cp.getIndex();
            }

            pushBack();
            cp.visitClassConstant(clazz, null);
            return cp.getIndex();
        }

        throw new ParseException("Unknown loadable constant type.", lineno());
    }


    /**
     * Parses a label as an offset.
     * If the label is not present in the labels map, a new value (the old
     * labels map size + 1) is added to the labels map.
     *
     * @param typeName the name of the label to parse, used in the
     *                 ParseException message if needed.
     * @return the offset associated with the label.
     * @throws ParseException if the label is malformatted.
     */
    public int expectOffset(String typeName)
    {
        String label = expectWord("label");
        if (!labels.containsKey(label)) {
            labels.put(label, labels.size() + 1);
        }

        return labels.get(label);
    }
}
