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

/**
 * General purpose parsing exception.
 *
 * @author Joachim Vandersmissen
 */
public class ParseException extends RuntimeException
{
    /**
     * Constructs a new ParseException with a message and the line number at
     * which the exception occured. The provided line number will be prepended
     * to the message.
     *
     * @param message the message to add to the exception.
     * @param line    the line number at which the exception occured.
     */
    public ParseException(String message, int line)
    {
        super("Line " + line + ": " + message);
    }


    /**
     * Constructs a new ParseException with a message, the line number at which
     * the exception occured, and the cause of the exception. The provided line
     * number will be prepended to the message.
     *
     * @param message the message to add to the exception.
     * @param line    the line number at which the exception occured.
     * @param cause   the cause of the exception.
     */
    public ParseException(String message, int line, Throwable cause)
    {
        super("Line " + line + ": " + message, cause);
    }
}
