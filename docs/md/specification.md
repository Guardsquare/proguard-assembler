## Preliminaries
### File type
The assembler will automatically recognize files with the extension `jbc` (Java ByteCode) as files to parse and assemble.
Files with the extension `class` will be disassembled to `jbc` files.

### Notation
Tokens are defined in this document using the `token := ...` notation.
Tokens are written in _italic_, literals use the normal formatting.

Regex-like operations, such as `(` and `)` for groups, `*` for 0 or more, and `?` for 0 or 1 are also be used in the documentation.

### Comments
Standard Java syntax comments are possible: `//` for single-line comments and `/* */` for multi-line comments.

### Basic token types
In essence, the Assembler can distinguish 4 different token types (based on the `StreamTokenizer`) tokens:
* `number`: any sequence of `0-9`, starting with `-` for negative numbers, and containing a single `.` for non-integer numbers.
* `word`: any sequence of `-`, `.`, `0-9`, `A-Z`, `a-z`, and all characters with a value greater than, or equal to 240 but less than, or equal to 255. A `word` must not start with a `number`.
* `string`: any sequence of characters surrounded by double quotes (`"`)
    * `string` can contain escaped characters:
        * `\a` for the bell character
        * `\b` for the backspace character
        * `\f` for the new page character
        * `\n` for the new line character
        * `\r` for the carriage return character
        * `\t` for the horizontal tab character
        * `\v` for the vertical tab character
    * Additionally `string` can contain octal-escaped characters: `\xxx` where x is a `0-7` digit (up to `\377`).
* `character`: a single character, surrounded by single quotes (`'`)
    * `character` follows the same escape rules as `string`, e.g. `'\n'` and `'\177'` are valid characters.


### Types
Types generally follow the Java syntax, albeit less restrictive: any word can be a type, and any type can be succeeded by `[]` to denote an array.
Method arguments also follow Java, with the important distinction that no argument names are specified.

<pre>
<i>type</i> := <i>word</i>
<i>type</i> := <i>type</i> []
<i>methodArguments</i> := ( )
<i>methodArguments</i> := ( <i>(type</i> ,<i>)* type</i> )
</pre>

### Access Flags
In most cases, every Java bytecode access flag can be combined, even if these combinations would be meaningless, or illegal for the JVM.
An exception to this are some class access flags, which are conveniently expressed as class types rather than access flags.

<pre>
<i>classAccessFlag</i> := public
<i>classAccessFlag</i> := private
<i>classAccessFlag</i> := protected
<i>classAccessFlag</i> := static
<i>classAccessFlag</i> := final
<i>classAccessFlag</i> := super
<i>classAccessFlag</i> := synchronized
<i>classAccessFlag</i> := volatile
<i>classAccessFlag</i> := transient
<i>classAccessFlag</i> := bridge
<i>classAccessFlag</i> := varargs
<i>classAccessFlag</i> := native
<i>classAccessFlag</i> := abstract
<i>classAccessFlag</i> := strictfp
<i>classAccessFlag</i> := synthetic
<i>classAccessFlag</i> := mandated
<i>classAccessFlag</i> := open
<i>classAccessFlag</i> := transitive
<i>classAccessFlag</i> := static_phase
</pre>

<pre>
<i>accessFlag</i> := <i>classAccessFlag</i>
<i>accessFlag</i> := module
<i>accessFlag</i> := enum
<i>accessFlag</i> := interface
<i>accessFlag</i> := annotation
</pre>

## Class files
<pre>
<i>classFile</i> := <i>import* version class</i>
<i>import</i> := import <i>type</i> ;
<i>version</i> := version <i>number</i> ;
</pre>

As in Java, it's possible to import classes at the top of the file.
Only fully qualified class names are allowed, no wildcard or static imports are supported.

Every file should also declare the Java version to assemble for.
Valid versions are: 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 5. 1.6, 6, 1.7, 7, 1.8, 8, 1.9, 9, 10, 11, 12, and 13.

Example:
```
import java.lang.String;
import java.lang.System;
import java.io.PrintStream;
version 12;
public class MyClass {
    public static void main(final String[] args) {
        getstatic System#PrintStream out
        ldc "Hello World!"
        invokevirtual PrintStream#void println(String)
        return
    }
}
```

## Classes
<pre>
<i>class</i> := <i>classAccessFlag* classType word superClassSpecifier classInterfacesSpecifier attributes? classBody</i>
<i>class</i> := <i>classAccessFlag* interfaceType word interfaceInterfacesSpecifier attributes? classBody</i>

<i>classType</i> := class
<i>classType</i> := enum
<i>classType</i> := module
<i>interfaceType</i> := interface
<i>interfaceType</i> := @interface

<i>superClassSpecifier</i> := extends <i>word</i>
<i>classInterfacesSpecifier</i> := implements <i>(word</i> ,<i>)* word</i>
<i>interfaceInterfacesSpecifier</i> := extends <i>(word</i> ,<i>)* word</i>
<i>classBody</i> := ;
<i>classBody</i> := { <i>classMember*</i> }
</pre>

Classes are defined analogously to Java: classes and enums can extend superclasses and implement interfaces.
Even though the syntax also allows modules to extend a superclass and implement interfaces, this is illegal for the JVM.
Interfaces and annotations (`@interface`) use the `extends` keywords as syntactic sugar to implement interfaces.
Additionally, classes extend `java.lang.Object` by default, emums extend `java.lang.Enum` by default, and annotations implement `java.lang.annotation.Annotation` by default.

Classes can optionally declare attributes and class members.

Examples:
```
public class MyException extends RuntimeException { // No attributes
    // No fields
    // No methods
}

public enum MyEnum; // Extends java.lang.Enum by default, no attributes, no fields, no methods

public @interface MyAnnotatation [ // Implements java.lang.annotation.Annotation by default
    Synthetic;
    Deprecated;
]; // No fields, no methods

public module module-info [ // Does not extend java.lang.Object by default
    // No attributes
] {
    // No fields, no methods
}

public class MyClass; // Extends java.lang.Object by default, no attributes, no fields, no methods
```

## Class members
<pre>
<i>classMember</i> := <i>field</i>
<i>classMember</i> := <i>method</i>

<i>field</i> := <i>accessFlag* type word (</i>= <i>fieldConstant)? attributes?</i> ;

<i>method</i> := <i>accessFlag* attributes? methodBody</i>
<i>method</i> := <i>accessFlag* type methodName methodArgumentsDefinition methodThrows? attributes? methodBody</i>
<i>methodBody</i> := ;
<i>methodBody</i> := { <i>instruction*</i> }
<i>methodName</i> := <i>word</i>
<i>methodName</i> := &lt;init&gt;
<i>methodName</i> := &lt;clinit&gt;
<i>methodThrows</i> := throws <i>(type</i> ,<i>)* type</i>
<i>methodArgumentsDefinition</i> := ( )
<i>methodArgumentsDefinition</i> := ( <i>(methodArgumentDefinition</i> ,<i>)* methodArgumentDefinition</i> )
<i>methodArgumentDefinition</i> := <i>accessFlag* type word?</i>
</pre>

Fields and methods are also defined analogously to Java.

Fields can be initialized using the equals sign (`=`), which will set the ConstantValue attribute.
Although this syntax is always valid, this initialization is only legal for the JVM if the field has `static` and `final` access flags.
The loadable constant type does not have to match the field type, indeed, some combinations are perfectly valid: a `boolean` field can be initialized using an `intConstant`.
Fields can also optionally declare attributes.

Examples:
```
public static final int INT_FIELD = 0; // No attributes

public static final boolean BOOLEAN_FIELD = 1; // The loadable constant type does not have to match the field type.
java.lang.String myStringField [
    Deprecated;
    Synthetic;
];

protected transient char someChar = 'a' []; // This is illegal for the JVM, as the field is not static and final.
```

A method can be declared in _class initializer_ format or regular Java method format.
Method arguments can contain access flags and method names, and the method can have a `throws` clause.
Methods can also optionally have a method attribute, which will set the Code attribute, and declare other attributes.

Examples:
```
static { // No attributes
    return
} // Class initializer

public static final void main(synthetic final java.lang.String[] args) throws java.lang.Throwable [
    Deprecated;
    Synthetic;
] {
    new java.lang.Exception
    dup
    invokespecial java.lang.Exception#void <init>()
    athrow
}

public void <init>() [
    Code {
        aload_0
        invokespecial java.lang.Object#void <init>()
        return
    } // Explicit code attribute
]; // No method body
``` 

## Constants
<pre>
<i>fieldReference</i> := <i>type</i> # type word
<i>fieldReference</i> := # <i>type word</i>
<i>methodReference</i> := <i>type</i> # <i>type word methodArguments</i>
<i>methodReference</i> := # <i>type word methodArguments</i>
<i>invokedynamicReference</i> := <i>number type word methodArguments</i>

<i>methodHandle</i> := getstatic <i>fieldReference</i>
<i>methodHandle</i> := putstatic <i>fieldReference</i>
<i>methodHandle</i> := getfield <i>fieldReference</i>
<i>methodHandle</i> := putfield <i>fieldReference</i>
<i>methodHandle</i> := invokevirtual <i>methodReference</i>
<i>methodHandle</i> := invokestatic <i>methodReference</i>
<i>methodHandle</i> := invokespecial <i>methodReference</i>
<i>methodHandle</i> := newinvokespecial <i>methodReference</i>
<i>methodHandle</i> := invokeinterface <i>methodReference</i>
</pre>

If the first `type` of `fieldReference` or `methodReference` is not supplied, the type will be the type of the current class being assembled.
In other words, these notations are shorthand for accessing fields or invoking methods of the current class.

`invokedynamicReference` has 4 arguments: the index in the BootstrapMethods attribute, the return type of the method, the name of the method, and the method arguments.


Because the Assembler has to know which constant to assign a value to, there are multiple notations for most constants.
Some constants have a defining format, for example in the case of `booleanConstant`, however it's always possible to explicitly provide the type of the constant, in Java 'cast' format.

<pre>
<i>boolean</i> := true
<i>boolean</i> := false
<i>doubleLiteralSuffix</i> := D
<i>doubleLiteralSuffix</i> := d
<i>floatLiteralSuffix</i> := F
<i>floatLiteralSuffix</i> := f
<i>longLiteralSuffix</i> := L
<i>longLiteralSuffix</i> := l

<i>booleanConstant</i> := <i>boolean</i>
<i>booleanConstant</i> := (boolean) <i>boolean</i>
<i>booleanConstant</i> := (boolean) <i>number</i>
<i>byteConstant</i> := (byte) <i>number</i>
<i>charConstant</i> := <i>character</i>
<i>charConstant</i> := (char) <i>character</i>
<i>charConstant</i> := (char) <i>number</i>
<i>doubleConstant</i> := <i>number doubleLiteralSuffix</i>
<i>doubleConstant</i> := (double) <i>number doubleLiteralSuffix</i>
<i>doubleConstant</i> := (double) <i>number</i>
<i>floatConstant</i> := <i>number floatLiteralSuffix</i>
<i>floatConstant</i> := (float) <i>number floatLiteralSuffix</i>
<i>floatConstant</i> := (float) <i>number</i>
<i>intConstant</i> := <i>number</i>
<i>intConstant</i> := (int) <i>number</i>
<i>longConstant</i> := <i>number longLiteralSuffix</i>
<i>longConstant</i> := (long) <i>number longLiteralSuffix</i>
<i>longConstant</i> := (long) <i>number</i>
<i>shortConstant</i> := (short) <i>number</i>

<i>stringConstant</i> := <i>string</i>
<i>stringConstant</i> := (String) <i>string</i>
<i>classConstant</i> := <i>type</i>
<i>classConstant</i> := (Class) <i>type</i>
<i>methodHandleConstant</i> := (MethodHandle) </i>methodHandle</i>
<i>methodTypeConstant</i> := (MethodType) <i>type methodArguments</i>
<i>dynamicConstant</i> := (Dynamic) <i>number type word</i>

<i>fieldConstant</i> := <i>booleanConstant</i>
<i>fieldConstant</i> := <i>byteConstant</i>
<i>fieldConstant</i> := <i>charConstant</i>
<i>fieldConstant</i> := <i>doubleConstant</i>
<i>fieldConstant</i> := <i>floatConstant</i>
<i>fieldConstant</i> := <i>intConstant</i>
<i>fieldConstant</i> := <i>longConstant</i>
<i>fieldConstant</i> := <i>shortConstant</i>
<i>fieldConstant</i> := <i>stringConstant</i>

<i>loadableConstant</i> := <i>fieldConstant</i>
<i>loadableConstant</i> := <i>classConstant</i>
<i>loadableConstant</i> := <i>methodHandleConstant</i>
<i>loadableConstant</i> := <i>methodTypeConstant</i>
<i>loadableConstant</i> := <i>dynamicConstant</i>
</pre>

`booleanConstant`, `byteConstant`, `charConstant`, `intConstant`, and `shortConstant` are all converted to integer constants by the Assembler.
This means that, in most cases, those constants are indistinguishable in the compiled class file.

`dynamicConstant` has 3 arguments: the index in the BootstrapMethods attribute, the type of the constant and the name of the constant.

## Attributes
<pre>
attributes</i> := <i>[</i> attribute* <i>]</i>
</pre>

Some attributes are not explicitly parsed by the Assembler, but handled in a special way:
* ConstantValue: assignment similar to Java (see section _Fields_)
* MethodParameters: parameter access flags and names similar to Java (see section _Methods_)
* Exceptions: methods throw exceptions similar to Java (see section _Methods_)
* StackMap and StackMapTable: code is preverified by the ProGuard preverifier and these attributes are generated automatically.
* LineNumberTable, LocalVariableTable, and LocalVariableTypeTable: using pseudo-instructions in the code (see subsection _Code attribute_)

These attributes can not be defined explicitly, and will not be printed explicitly by the Disassembler

### BootstrapMethods attribute
<pre>
<i>attribute</i> := BootstrapMethods { <i>bootstrapMethod*</i> }
<i>bootstrapMethod</i> := <i>methodHandle</i> { <i>bootstrapMethodArgument*</i> }
<i>bootstrapMethodArgument</i> := <i>loadableConstant</i> ;
</pre>

Example:
```
BootstrapMethods {
    invokestatic java.lang.invoke.StringConcatFactory#java.lang.invoke.CallSite makeConcatWithConstants(java.lang.invoke.MethodHandles$Lookup, java.lang.String, java.lang.invoke.MethodType, java.lang.String, java.lang.Object[]) {
        "abc \001 def";
    }
}
```

### SourceFile attribute
<pre>
<i>attribute</i> := SourceFile <i>string</i> ;
</pre>

Example: `SourceFile "Assembler.java";`

### SourceDir attribute
<pre>
<i>attribute</i> := SourceDir <i>string</i> ;
</pre>

Example: `SourceDir "My Source Directory";`

### InnerClasses attribute
<pre>
<i>attribute</i> := InnerClasses { <i>innerClass*</i> }
<i>innerClass</i> := <i>classAccessFlag* innerClassType innerName? outerClass?</i> ;
<i>innerClassType</i> := <i>classType</i>
<i>innerClassType</i> := <i>interfaceType</i>
<i>innerName</i> := as <i>word</i>
<i>outerClass</i> := in <i>type</i>
</pre>

Both `innerName` and `outerClass` are optional. Note that even though _module_ is a valid class type, it has no valid meaning in inner classes in Java bytecode.

Example:
```
InnerClasses {
    public class InnerClass as InnerName in OuterClass;
    public static @interface InnerAnnotation as Annotation;
    public enum InnerEnum in EnclosingClass;
    private module InnerModule;
}
```

### EnclosingMethod attribute
<pre>
<i>attribute</i> := EnclosingMethod <i>enclosingClass enclosingMethod?</i> ;
<i>enclosingClass</i> := <i>type</i>
<i>enclosingMethod</i> := # <i>type word methodArguments</i>
</pre>

Although the enclosing class always has to be specified, `enclosingMethod` is optional.

Example:
```
EnclosingMethod EnclosingClass # void enclosingMethod(java.lang.String, java.lang.Object);
EnclosingMethod AnotherEnclosingClass;
```

### NestHost attribute
<pre>
<i>attribute</i> := NestHost <i>type</i> ;
</pre>

Example:
```
NestHost java.lang.Class;
```

### NestMembers attribute
<pre>
<i>attribute</i> := NestMembers { <i>nestMember*</i> }
<i>nestMember</i> := <i>type</i> ;
</pre>

Example:
```
NestMembers {
    java.lang.Class;
    java.lang.String;
}
```

### Deprecated attribute
<pre>
<i>attribute</i> := Deprecated ;
</pre>

### Synthetic attribute
<pre>
<i>attribute</i> := Synthetic ;
</pre>

### Signature attribute
<pre>
<i>attribute</i> := Signature <i>string</i> ;
</pre>

Example:
```
Signature "Ljava/lang/Enum<LType;>;";
```

### Code attribute
<pre>
<i>attribute</i> := Code { <i>instruction*</i> } <i>attributes?</i>
</pre>

#### Instructions
<pre>
<i>instruction</i> := nop
<i>instruction</i> := aconst_null
<i>instruction</i> := iconst_m1
<i>instruction</i> := iconst_0
<i>instruction</i> := iconst_1
<i>instruction</i> := iconst_2
<i>instruction</i> := iconst_3
<i>instruction</i> := iconst_4
<i>instruction</i> := iconst_5
<i>instruction</i> := lconst_0
<i>instruction</i> := lconst_1
<i>instruction</i> := fconst_0
<i>instruction</i> := fconst_1
<i>instruction</i> := fconst_2
<i>instruction</i> := dconst_0
<i>instruction</i> := dconst_1
<i>instruction</i> := bipush <i>number</i>
<i>instruction</i> := sipush <i>number</i>
<i>instruction</i> := ldc <i>loadableConstant</i>
<i>instruction</i> := ldc_w <i>loadableConstant</i>
<i>instruction</i> := ldc2_w <i>loadableConstant</i>
<i>instruction</i> := iload <i>number</i>
<i>instruction</i> := lload <i>number</i>
<i>instruction</i> := fload <i>number</i>
<i>instruction</i> := dload <i>number</i>
<i>instruction</i> := aload <i>number</i>
<i>instruction</i> := iload_0
<i>instruction</i> := iload_1
<i>instruction</i> := iload_2
<i>instruction</i> := iload_3
<i>instruction</i> := lload_0
<i>instruction</i> := lload_1
<i>instruction</i> := lload_2
<i>instruction</i> := lload_3
<i>instruction</i> := fload_0
<i>instruction</i> := fload_1
<i>instruction</i> := fload_2
<i>instruction</i> := fload_3
<i>instruction</i> := dload_0
<i>instruction</i> := dload_1
<i>instruction</i> := dload_2
<i>instruction</i> := dload_3
<i>instruction</i> := aload_0
<i>instruction</i> := aload_1
<i>instruction</i> := aload_2
<i>instruction</i> := aload_3
<i>instruction</i> := iaload
<i>instruction</i> := laload
<i>instruction</i> := faload
<i>instruction</i> := daload
<i>instruction</i> := aaload
<i>instruction</i> := baload
<i>instruction</i> := caload
<i>instruction</i> := saload
<i>instruction</i> := istore <i>number</i>
<i>instruction</i> := lstore <i>number</i>
<i>instruction</i> := fstore <i>number</i>
<i>instruction</i> := dstore <i>number</i>
<i>instruction</i> := astore <i>number</i>
<i>instruction</i> := istore_0
<i>instruction</i> := istore_1
<i>instruction</i> := istore_2
<i>instruction</i> := istore_3
<i>instruction</i> := lstore_0
<i>instruction</i> := lstore_1
<i>instruction</i> := lstore_2
<i>instruction</i> := lstore_3
<i>instruction</i> := fstore_0
<i>instruction</i> := fstore_1
<i>instruction</i> := fstore_2
<i>instruction</i> := fstore_3
<i>instruction</i> := dstore_0
<i>instruction</i> := dstore_1
<i>instruction</i> := dstore_2
<i>instruction</i> := dstore_3
<i>instruction</i> := astore_0
<i>instruction</i> := astore_1
<i>instruction</i> := astore_2
<i>instruction</i> := astore_3
<i>instruction</i> := iastore
<i>instruction</i> := lastore
<i>instruction</i> := fastore
<i>instruction</i> := dastore
<i>instruction</i> := aastore
<i>instruction</i> := bastore
<i>instruction</i> := castore
<i>instruction</i> := sastore
<i>instruction</i> := pop
<i>instruction</i> := pop2
<i>instruction</i> := dup
<i>instruction</i> := dup_x1
<i>instruction</i> := dup_x2
<i>instruction</i> := dup2
<i>instruction</i> := dup2_x1
<i>instruction</i> := dup2_x2
<i>instruction</i> := swap
<i>instruction</i> := iadd
<i>instruction</i> := ladd
<i>instruction</i> := fadd
<i>instruction</i> := dadd
<i>instruction</i> := isub
<i>instruction</i> := lsub
<i>instruction</i> := fsub
<i>instruction</i> := dsub
<i>instruction</i> := imul
<i>instruction</i> := lmul
<i>instruction</i> := fmul
<i>instruction</i> := dmul
<i>instruction</i> := idiv
<i>instruction</i> := ldiv
<i>instruction</i> := fdiv
<i>instruction</i> := ddiv
<i>instruction</i> := irem
<i>instruction</i> := lrem
<i>instruction</i> := frem
<i>instruction</i> := drem
<i>instruction</i> := ineg
<i>instruction</i> := lneg
<i>instruction</i> := fneg
<i>instruction</i> := dneg
<i>instruction</i> := ishl
<i>instruction</i> := lshl
<i>instruction</i> := ishr
<i>instruction</i> := lshr
<i>instruction</i> := iushr
<i>instruction</i> := lushr
<i>instruction</i> := iand
<i>instruction</i> := land
<i>instruction</i> := ior
<i>instruction</i> := lor
<i>instruction</i> := ixor
<i>instruction</i> := lxor
<i>instruction</i> := iinc <i>number number</i>
<i>instruction</i> := i2l
<i>instruction</i> := i2f
<i>instruction</i> := i2d
<i>instruction</i> := l2i
<i>instruction</i> := l2f
<i>instruction</i> := l2d
<i>instruction</i> := f2i
<i>instruction</i> := f2l
<i>instruction</i> := f2d
<i>instruction</i> := d2i
<i>instruction</i> := d2l
<i>instruction</i> := d2f
<i>instruction</i> := i2b
<i>instruction</i> := i2c
<i>instruction</i> := i2s
<i>instruction</i> := lcmp
<i>instruction</i> := fcmpl
<i>instruction</i> := fcmpg
<i>instruction</i> := dcmpl
<i>instruction</i> := dcmpg
<i>instruction</i> := ifeq <i>label</i>
<i>instruction</i> := ifne <i>label</i>
<i>instruction</i> := iflt <i>label</i>
<i>instruction</i> := ifge <i>label</i>
<i>instruction</i> := ifgt <i>label</i>
<i>instruction</i> := ifle <i>label</i>
<i>instruction</i> := if_icmpeq <i>label</i>
<i>instruction</i> := if_icmpne <i>label</i>
<i>instruction</i> := if_icmplt <i>label</i>
<i>instruction</i> := if_icmpge <i>label</i>
<i>instruction</i> := if_icmpgt <i>label</i>
<i>instruction</i> := if_icmple <i>label</i>
<i>instruction</i> := if_acmpeq <i>label</i>
<i>instruction</i> := if_acmpne <i>label</i>
<i>instruction</i> := goto <i>label</i>
<i>instruction</i> := jsr <i>label</i>
<i>instruction</i> := ret <i>number</i>
<i>instruction</i> := tableswitch { <i>switchCase*</i> }
<i>instruction</i> := lookupswitch { <i>switchCase*</i> }
<i>instruction</i> := ireturn
<i>instruction</i> := lreturn
<i>instruction</i> := freturn
<i>instruction</i> := dreturn
<i>instruction</i> := areturn
<i>instruction</i> := return
<i>instruction</i> := getstatic <i>fieldReference</i>
<i>instruction</i> := putstatic <i>fieldReference</i>
<i>instruction</i> := getfield <i>fieldReference</i>
<i>instruction</i> := putfield <i>fieldReference</i>
<i>instruction</i> := invokevirtual <i>methodReference</i>
<i>instruction</i> := invokespecial <i>methodReference</i>
<i>instruction</i> := invokestatic <i>methodReference</i>
<i>instruction</i> := invokeinterface <i>methodReference</i>
<i>instruction</i> := invokedynamic </i>invokedynamicReference</i>
<i>instruction</i> := new <i>type</i>
<i>instruction</i> := newarray <i>type</i>
<i>instruction</i> := anewarray <i>type</i>
<i>instruction</i> := arraylength
<i>instruction</i> := athrow
<i>instruction</i> := checkcast <i>type</i>
<i>instruction</i> := instanceof <i>type</i>
<i>instruction</i> := monitorenter
<i>instruction</i> := monitorexit
<i>instruction</i> := multianewarray <i>type number</i>
<i>instruction</i> := ifnull <i>label</i>
<i>instruction</i> := ifnonnull <i>label</i>
<i>instruction</i> := goto_w <i>label</i>
<i>instruction</i> := jsr_w <i>label</i>

<i>switchCase</i> := case <i>number</i> : <i>label</i>
<i>switchCase</i> := default : <i>label</i>
</pre>

Note that the `wide` instruction is not present, this instruction is replaced by the pseudo-instructions:

<pre>
<i>instruction</i> := iload_w <i>number</i>
<i>instruction</i> := lload_w <i>number</i>
<i>instruction</i> := fload_w <i>number</i>
<i>instruction</i> := dload_w <i>number</i>
<i>instruction</i> := aload_w <i>number</i>
<i>instruction</i> := istore_w <i>number</i>
<i>instruction</i> := lstore_w <i>number</i>
<i>instruction</i> := fstore_w <i>number</i>
<i>instruction</i> := dstore_w <i>number</i>
<i>instruction</i> := astore_w <i>number</i>
<i>instruction</i> := iinc_w <i>number number</i>
<i>instruction</i> := ret_w <i>number</i>
</pre>

Furthermore, pseudo-instructions exist for labels, try-catch blocks, local variables, local variable types, and line numbers:

<pre>
<i>instruction</i> := <i>label</i> :
<i>instruction</i> := catch <i>type label label</i>
<i>instruction</i> := catch any <i>label label</i>
<i>instruction</i> := startlocalvar <i>number type word</i>
<i>instruction</i> := endlocalvar <i>number</i>
<i>instruction</i> := startlocalvartype <i>number string word</i>
<i>instruction</i> := endlocalvartype <i>number</i>
<i>instruction</i> := line <i>number</i>

<i>label</i> := <i>word</i>
</pre>

A `catch` pseudo-instruction specifies an exception handler at the location of the pseudo-instruction.
The catch type, start, end, and handler will be added to the exception table in the Code attribute.

`startlocalvar` and `startlocalvartype`, `endlocalvar` and `endlocalvartype`, specify the start or end of a local variable or local variable type, respectively.
These pseudo-instructions modify the LocalVariableTable or LocalVariableTypeTable attributes in the Code attribute.
The `number` defines the index of the local variable or local variable type.
A `startlocalvar` and `startlocalvartype` must always have an accompanying `endlocalvar` or `endlocalvartype`, placed after the `startlocalvar` or `startlocalvartype` in the instructions.

`line` specifies the line `number` at a position in the bytecode. The line `number` and bytecode offset will be stored in a LineNumberTable attribute.

### Annotations attributes
<pre>
<i>attribute</i> := RuntimeVisibleAnnotations { <i>annotation*</i> }
<i>attribute</i> := RuntimeInvisibleAnnotations { <i>annotation*</i> }
<i>attribute</i> := RuntimeVisibleParameterAnnotations { <i>parameterAnnotation*</i> }
<i>attribute</i> := RuntimeInvisibleParameterAnnotations { <i>parameterAnnotation*</i> }
<i>attribute</i> := RuntimeVisibleTypeAnnotations { <i>typeAnnotation*</i> }
<i>attribute</i> := RuntimeInvisibleTypeAnnotations { <i>typeAnnotation*</i> }
<i>attribute</i> := AnnotationDefault <i>elementValue</i>

<i>annotation</i> := <i>type</i> { <i>(word</i> = <i>elementValue)*</i> }
<i>parameterAnnotation</i> := { <i>annotation*</i> }
<i>typeAnnotation</i> := <i>annotation targetInfo</i> { <i>typePath*</i> }
</pre>

Examples:
```
RuntimeVisibleAnnotations {
    java.lang.Deprecated {
        since = "sinceVersion";
        forRemoval = true;
    }
}
RuntimeInvisibleAnnotations {
    java.lang.Deprecated {} // Empty values
}
RuntimeVisibleParameterAnnotations {
    {} // Empty annotations for parameter 0
    {
        java.lang.Deprecated {
            since = "sinceVersion";
            forRemoval = true;
        }
    }
}
RuntimeInvisibleParameterAnnotations {
    {
        java.lang.Deprecated {} // Empty values
    }
    {} // Empty annotations for parameter 1
    {} // Empty annotations for parameter 2
    {} // Empty annotations for parameter 3
}
RuntimeVisibleTypeAnnotations {
    java.lang.Deprecated {
        since = "sinceVersion";
        forRemoval = true;
    } local_variable {
        start0 end0 0;
        start10 end10 10;
    } {} // Empty type path
}
RuntimeVisibleTypeAnnotations {
    java.lang.Deprecated {} argument_generic_method_new newLabel 1 {
        array;
        type_argument 1;
    }
}
RuntimeInvisibleTypeAnnotations {
    java.lang.Deprecated {} field {} // Empty values, empty type path
}
AnnotationDefault {
    false; // Boolean element value
    true; // Boolean element value
    (byte) 1; // Byte element value
    '2'; // Char element value
    3.0D; // Double element value
    4F; // Float element value
    5; // Int element value
    6l; // Long element value
    (short) 7; // Short element value
    "string"; // String element value
    java.lang.Class; // Class element value
    Enum#Constant; // Enum constant element value
    @java.lang.Deprecated {} // Annotation element value
    {} // Array element value
} // Array element value
```

#### Element values
<pre>
<i>elementValue</i> := <i>booleanConstant</i> ;
<i>elementValue</i> := <i>byteConstant</i> ;
<i>elementValue</i> := <i>charConstant</i> ;
<i>elementValue</i> := <i>doubleConstant</i> ;
<i>elementValue</i> := <i>floatConstant</i> ;
<i>elementValue</i> := <i>intConstant</i> ;
<i>elementValue</i> := <i>longConstant</i> ;
<i>elementValue</i> := <i>shortConstant</i> ;
<i>elementValue</i> := <i>stringConstant</i> ;
<i>elementValue</i> := <i>classConstant</i> ;

<i>elementValue</i> := (Enum) type # word ;
<i>elementValue</i> := <i>type</i> # <i>word</i> ;
<i>elementValue</i> := (Annotation) <i>annotation</i>
<i>elementValue</i> := @ <i>annotation</i>
<i>elementValue</i> := (Array) { <i>elementValue*</i> }
<i>elementValue</i> := { <i>elementValue*</i> }
</pre>

Apart from the usual primitive constants, string constants, and class constants, element values can also denote enum constants (enum type + constant name), annotations and arrays.
Note that annotation element values and array element values do not end with a `;`, as they already (either implicitly or explicitly) end with a `}`.

#### Target infos
<pre>
<i>targetInfo</i> := parameter_generic_class <i>number</i>
<i>targetInfo</i> := parameter_generic_method <i>number</i>
<i>targetInfo</i> := extends <i>number</i>
<i>targetInfo</i> := bound_generic_class <i>number number</i>
<i>targetInfo</i> := bound_generic_method <i>number number</i>
<i>targetInfo</i> := field
<i>targetInfo</i> := return
<i>targetInfo</i> := receiver
<i>targetInfo</i> := parameter <i>number</i>
<i>targetInfo</i> := throws <i>number</i>
<i>targetInfo</i> := local_variable { <i>localVar*</i> }
<i>targetInfo</i> := resource_variable</i> { <i>localVar*</i> }
<i>targetInfo</i> := catch <i>number</i>
<i>targetInfo</i> := instance_of <i>label</i>
<i>targetInfo</i> := new <i>label</i>
<i>targetInfo</i> := method_reference_new <i>label</i>
<i>targetInfo</i> := method_reference <i>label</i>
<i>targetInfo</i> := cast <i>label number</i>
<i>targetInfo</i> := argument_generic_method_new <i>label number</i>
<i>targetInfo</i> := argument_generic_method <i>label number</i>
<i>targetInfo</i> := argument_generic_method_reference_new <i>label number</i>
<i>targetInfo</i> := argument_generic_method_reference <i>label number</i>

<i>localVar</i> := <i>label label number</i> ;
</pre>

In general, the arguments of the target infos roughly match the ones specified in the Class File Format specification.

#### Type path
<pre>
<i>typePath</i> := array <i>number?</i> ;
<i>typePath</i> := inner_type <i>number?</i> ;
<i>typePath</i> := wildcard <i>number?</i> ;
<i>typePath</i> := type_argument <i>number?</i> ;
</pre>

Although every type path has an optional `number` argument, this argument only has meaning in combination with `type_argument`.
In that case, the `number` denotes which type argument is annotated (see the Class File Format specification for more details).

### Module attribute
<pre>
<i>attribute</i> := Module <i>accessFlag* word word?</i> { <i>moduleDirective*</i> }

<i>moduleDirective</i> := requires <i>accessFlag* word word?</i> ;
<i>moduleDirective</i> := exports <i>accessFlag* type exportsTo?</i> ;
<i>moduleDirective</i> := opens <i>accessFlag* type opensTo?</i> ;
<i>moduleDirective</i> := uses <i>type</i> ;
<i>moduleDirective</i> := provides <i>type providesWith?</i> ;

<i>exportsTo</i> := to <i>(word</i> ,<i>)* word</i>
<i>opensTo</i> := to <i>(word</i> ,<i>)* word</i>
<i>providesWith</i> := with <i>(type</i> ,<i>)* type</i>
</pre>

The module attribute specifies the module access flags, the module name, and an optional module version.
As the module version must be a `word`, it can not start with a `number`. 

_exports_, _opens_, and _provides_ all have optional arguments specifying the directive.
These arguments use the same syntax as their Java counterparts.

Example:
```
Module open synthetic mandated ModuleName v1.0 {
    requires transitive   some.package.RequiredModule v1.0;
    requires static_phase some.package.OtherRequiredModule;
    requires synthetic    some.package.SyntheticRequiredModule alpha;
    requires mandated     some.package.MandatedRequiredModule beta;

    exports synthetic some.package.exportedpackage;
    exports mandated  some.package.mandated.exportedpackage to some.package.export.to.package, some.package.export.to.otherpackage, some.package.export.to.finalpackage;

    opens synthetic some.package.openedpackage;
    opens mandated  some.package.mandated.openedpackage to some.package.open.to.package, some.package.open.to.otherpackage, some.package.open.to.finalpackage;

    uses some.package.UsedClass;
    uses some.package.OtherUsedClass;
    uses some.package.MoreUsedClass;
    uses some.package.FinalUsedClass;

    provides some.package.ProvidedClass;
    provides some.package.OtherProvidedClass with some.package.OtherProvidedClassImpl, some.package.OtherProvidedClassImpl1;
    provides some.package.FinalProvidedClass;
}
```

### ModuleMainClass attribute
<pre>
<i>attribute</i> := ModuleMainClass <i>type</i> ;
</pre>

Example:
```
ModuleMainClass some.package.ModuleMainClass;
```

### ModulePackages attribute
<pre>
<i>attribute</i> := ModulePackages { <i>type*</i> }
</pre>

Example:
```
ModulePackages {
    some.package;
    some.other.package;
}
```
