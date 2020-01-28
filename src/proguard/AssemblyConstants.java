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
package proguard;

import java.text.*;
import java.util.Locale;

public final class AssemblyConstants
{
    public static final String JBC_EXTENSION = ".jbc";

    public static final char ATTRIBUTES_OPEN       = '[';
    public static final char ATTRIBUTES_CLOSE      = ']';
    public static final char EQUALS                = '=';
    public static final char COLON                 = ':';
    public static final char REFERENCE_SEPARATOR   = '#';
    public static final char SPECIAL_METHOD_PREFIX = '<';
    public static final char SPECIAL_METHOD_SUFFIX = '>';
    public static final char STRING_QUOTE          = '"';
    public static final char CHAR_QUOTE            = '\'';

    public static final String ACC_ANNOTATION = "annotation";

    public static final String TYPE_STRING        = "String";
    public static final String TYPE_CLASS         = "Class";
    public static final String TYPE_METHOD_HANDLE = "MethodHandle";
    public static final String TYPE_METHOD_TYPE   = "MethodType";
    public static final String TYPE_DYNAMIC       = "Dynamic";
    public static final String TYPE_ANNOTATION    = "Annotation";
    public static final String TYPE_ENUM          = "Enum";
    public static final String TYPE_ARRAY         = "Array";
    public static final String TYPE_DOUBLE        = "D";
    public static final String TYPE_FLOAT         = "F";
    public static final String TYPE_LONG          = "L";

    public static final String REF_GET_FIELD          = "getfield";
    public static final String REF_GET_STATIC         = "getstatic";
    public static final String REF_PUT_FIELD          = "putfield";
    public static final String REF_PUT_STATIC         = "putstatic";
    public static final String REF_INVOKE_VIRTUAL     = "invokevirtual";
    public static final String REF_INVOKE_STATIC      = "invokestatic";
    public static final String REF_INVOKE_SPECIAL     = "invokespecial";
    public static final String REF_NEW_INVOKE_SPECIAL = "newinvokespecial";
    public static final String REF_INVOKE_INTERFACE   = "invokeinterface";

    public static final String TARGET_TYPE_P_A_RAMETER_GENERIC_CLASS               = "parameter_generic_class";
    public static final String TARGET_TYPE_P_A_RAMETER_GENERIC_METHOD              = "parameter_generic_method";
    public static final String TARGET_TYPE_E_X_TENDS                               = "extends";
    public static final String TARGET_TYPE_B_O_UND_GENERIC_CLASS                   = "bound_generic_class";
    public static final String TARGET_TYPE_B_O_UND_GENERIC_METHOD                  = "bound_generic_method";
    public static final String TARGET_TYPE_F_I_ELD                                 = "field";
    public static final String TARGET_TYPE_R_E_TURN                                = "return";
    public static final String TARGET_TYPE_R_E_CEIVER                              = "receiver";
    public static final String TARGET_TYPE_P_A_RAMETER                             = "parameter";
    public static final String TARGET_TYPE_T_H_ROWS                                = "throws";
    public static final String TARGET_TYPE_L_O_CAL_VARIABLE                        = "local_variable";
    public static final String TARGET_TYPE_R_E_SOURCE_VARIABLE                     = "resource_variable";
    public static final String TARGET_TYPE_C_A_TCH                                 = "catch";
    public static final String TARGET_TYPE_I_N_STANCE_OF                           = "instance_of";
    public static final String TARGET_TYPE_N_E_W                                   = "new";
    public static final String TARGET_TYPE_M_E_THOD_REFERENCE_NEW                  = "method_reference_new";
    public static final String TARGET_TYPE_M_E_THOD_REFERENCE                      = "method_reference";
    public static final String TARGET_TYPE_C_A_ST                                  = "cast";
    public static final String TARGET_TYPE_A_R_GUMENT_GENERIC_METHOD_NEW           = "argument_generic_method_new";
    public static final String TARGET_TYPE_A_R_GUMENT_GENERIC_METHOD               = "argument_generic_method";
    public static final String TARGET_TYPE_A_R_GUMENT_GENERIC_METHOD_REFERENCE_NEW = "argument_generic_method_reference_new";
    public static final String TARGET_TYPE_A_R_GUMENT_GENERIC_METHOD_REFERENCE     = "argument_generic_method_reference";

    public static final String TYPE_PATH_ARRAY               = "array";
    public static final String TYPE_PATH_NESTED              = "inner_type";
    public static final String TYPE_PATH_TYPE_ARGUMENT_BOUND = "wildcard";
    public static final String TYPE_PATH_TYPE_ARGUMENT       = "type_argument";

    public static final String IMPORT     = "import";
    public static final String CLASS      = "class";
    public static final String EXTENDS    = "extends";
    public static final String IMPLEMENTS = "implements";
    public static final String THROWS     = "throws";
    public static final String CATCH      = "catch";
    public static final String CASE       = "case";
    public static final String DEFAULT    = "default";
    public static final String REQUIRES   = "requires";
    public static final String EXPORTS    = "exports";
    public static final String OPENS      = "opens";
    public static final String USES       = "uses";
    public static final String PROVIDES   = "provides";
    public static final String TRUE       = "true";
    public static final String FALSE      = "false";

    public static final char BODY_OPEN     = '{';
    public static final char BODY_CLOSE    = '}';
    public static final char STATEMENT_END = ';';

    public static final String VERSION              = "version";
    public static final String INIT                 = "init";
    public static final String CLINIT               = "clinit";
    public static final String LINE                 = "line";
    public static final String ANY                  = "any";
    public static final String TO                   = "to";
    public static final String WITH                 = "with";
    public static final String AS                   = "as";
    public static final String IN                   = "in";
    public static final String LABEL                = "label";
    public static final String LOCAL_VAR_START      = "startlocalvar";
    public static final String LOCAL_VAR_END        = "endlocalvar";
    public static final String LOCAL_VAR_TYPE_START = "startlocalvartype";
    public static final String LOCAL_VAR_TYPE_END   = "endlocalvartype";

    public static final DecimalFormat DOUBLE_TO_STRING = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    static {
        DOUBLE_TO_STRING.setMaximumFractionDigits(340);
    }
}
