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

/**
 * General purpose printing exception.
 *
 * @author Joachim Vandersmissen
 */
public class PrintException extends RuntimeException
{
    /**
     * Constructs a new PrintException with a message.
     *
     * @param message the message to add to the exception.
     */
    public PrintException(String message)
    {
        super(message);
    }


    /**
     * Constructs a new PrintException with a message and the cause of the
     * exception.
     *
     * @param message the message to add to the exception.
     * @param cause   the cause of the exception.
     */
    public PrintException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
