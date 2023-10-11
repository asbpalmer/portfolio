package global;
import syntax.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.TreeSet;

public final class LookupChecker implements SyntaxTreeVisitor <Type>  {
    HashMap<String, HashMap> lookupTable; // stores all info
    private HashMap<String, HashMap<String, HashMap>> classScope; // stores current class info
    private HashMap<String, HashMap> methodScope; // stores current method info
    String currentClass, currentMethod;

    int errors = 0;
    boolean verbose;
    String fileName;
    PrintWriter debugOut;

    public int getErrors(){ return errors; }

    public LookupChecker (HashMap table, String fileNameIn)
    { lookupTable = table; fileName = fileNameIn; debugOut = null; verbose = false; }

    public LookupChecker (HashMap table, String fileNameIn, String verboseOut) throws FileNotFoundException
    { lookupTable = table; fileName = fileNameIn; debugOut=new PrintWriter(verboseOut); verbose = true;}

    public LookupChecker (HashMap table, String fileNameIn, PrintWriter verboseOut)
    { lookupTable = table; fileName = fileNameIn; debugOut = verboseOut; verbose = true;};

    private Type stringToType(String s){
        if (s != null) {
            switch (s) {
                case "int":
                    return Type.THE_INTEGER_TYPE;
                case "int[]":
                    return Type.THE_INT_ARRAY_TYPE;
                case "boolean":
                    return Type.THE_BOOLEAN_TYPE;
                case "void":
                    return Type.THE_VOID_TYPE;
                default:
                    return new IdentifierType(0, 0, s);
            }
        }
        return Type.THE_VOID_TYPE;
    }

    private boolean classExists(String className){
        return lookupTable.containsKey(className);
    }

    private HashMap<String, String> varInMethod(String varName){
        HashMap<String, HashMap> scope;
        HashMap<String, String> ret = null;
        if(classScope.containsKey("$fields")){
            scope = classScope.get("$fields");
            if (scope.containsKey(varName)){
                ret = scope.get(varName);
            }
        }
        // we use arguments and locals before we do fields
        // lookup creator guarantees that no arguments and locals share a name
        if(methodScope.containsKey(varName)){
            ret = methodScope.get(varName);
        }
        return ret;
    }

    // returns true if the types match, otherwise false
    private boolean typesMatch(Type type1, Type type2){
        // two identifier comparison is the only special case
        if (type1 instanceof IdentifierType && type2 instanceof IdentifierType){
            return ((IdentifierType) type1).nameOfType.equals(((IdentifierType) type2).nameOfType);
        }
        return type1.toString().equals(type2.toString());
    }

    private void sendDebugMessage(String message){
        if(verbose){
            debugOut.print(message);
        }
    }

    private void sendErrorMessage(String message){
        System.err.print(message);
        sendDebugMessage(message);
        errors++;
    }

    // Subcomponents of Program:  MainClass m; List<ClassDecl> cl;
    public Type visit (final Program n) {
        sendDebugMessage(String.format("filename=%s:000:000 -- Visiting Program...\n",
                fileName));

        if(n == null){
            sendErrorMessage(String.format("filename=%s:000:000 -- Null Program!", fileName));
        } else if (n.m == null){
            sendErrorMessage(String.format("filename=%s:000:000 -- Null Main Class!", fileName));
        } else{
            n.m.accept (this);

            for (ClassDecl c: n.cl) {
                c.accept(this);
            }
        }
        sendDebugMessage(String.format("filename=%s:000:000 -- Returning From Program...\n",
                fileName));
        return Type.THE_VOID_TYPE;
    }

    // Subcomponents of MainClass:  Identifier i1, i2; Statement s;
    public Type visit (final MainClass n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting main class %s...\n",
                fileName, n.nameOfMainClass.lineNumber, n.nameOfMainClass.columnNumber, n.nameOfMainClass.s));

        classScope = lookupTable.get(n.nameOfMainClass.s);
        currentClass = n.nameOfMainClass.s;

        methodScope = ((HashMap<String, HashMap>)classScope.get("main").get("String")).get("$end");

        n.nameOfMainClass.accept(this);
        n.nameOfCommandLineArgs.accept(this);

        if (n.body != null) {
            n.body.accept(this);   // statement:  body of main
        }

        classScope = null;
        currentClass = null;

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from main class %s...\n",
                fileName, n.nameOfMainClass.lineNumber, n.nameOfMainClass.columnNumber, n.nameOfMainClass.s));
        return Type.THE_VOID_TYPE;
    }

    // Subcomponents of SimpleClassDecl: Identifier i; List<FieldDecl> vl; List<MethodDecl> ml;
    public Type visit (final SimpleClassDecl n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting class %s...\n",
                fileName, n.i.lineNumber, n.i.columnNumber, n.i.s));

        classScope = lookupTable.get(n.i.s);
        currentClass = n.i.s;

        n.i.accept (this);

        for (FieldDecl v: n.fields) {
            v.accept(this);
        }
        for (MethodDecl m: n.methods) {
            m.accept(this);
        }
        classScope = null;
        currentClass = null;

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from class %s.\n",
                fileName, n.i.lineNumber, n.i.columnNumber, n.i.s));
        return Type.THE_VOID_TYPE;
    }

    // Subcomponents of ExtendingClassDecl: Identifier i, j; List<FieldDecl> vl; List<MethodDecl> ml;
    public Type visit (final ExtendingClassDecl n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting class %s...\n",
                fileName, n.i.lineNumber, n.i.columnNumber, n.i.s));

        classScope = lookupTable.get(n.i.s);
        currentClass = n.i.s;

        n.i.accept (this);
        n.j.accept (this);

        for (FieldDecl v: n.fields) {
            v.accept(this);
        }
        for (MethodDecl m: n.methods) {
            m.accept(this);
        }

        classScope = null;
        currentClass = null;

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from class %s.\n",
                fileName, n.i.lineNumber, n.i.columnNumber, n.i.s));
        return Type.THE_VOID_TYPE;
    }

    // Subcomponents of MethodDecl:
    // Type t; Identifier i; List<FormalDecl> fl; List<LocalDecl> locals; List<Statement>t sl; Expression e;
    public Type visit (final MethodDecl n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting method declaration for %s...\n",
                fileName, n.i.lineNumber, n.i.columnNumber, n.i.s));

        methodScope = classScope.get(n.i.s);
        currentMethod = n.i.s;
        n.i.accept(this);

        Type returnType = n.t.accept(this);
        // not one of the primitive types, and class type never declared
        if (!(typesMatch(returnType, Type.THE_INTEGER_TYPE) || typesMatch(returnType, Type.THE_BOOLEAN_TYPE) ||
                typesMatch(returnType, Type.THE_INT_ARRAY_TYPE) || typesMatch(returnType, Type.THE_VOID_TYPE))){
            if (!classExists(n.t.toString())) {
                sendErrorMessage(String.format("filename=%s:%03d:%03d -- Class %s (return type of method %s in class %s)" +
                                " is never declared.\n",
                        fileName, n.i.lineNumber, n.i.columnNumber, n.t.toString(), n.i.s, currentClass));
            }
        }

        // find out what specific method declaration we are using
        for (FormalDecl f: n.formals){
            f.accept(this);
            methodScope = methodScope.get(f.t.toString());
        }
        methodScope = methodScope.get("$end");

        // check all the locals
        for (final LocalDecl v: n.locals) {
            v.accept(this);
        }
        for (final Statement s: n.sl) s.accept(this);

        // Return statement
        Type foundType = n.e.accept(this);
        if (!typesMatch(foundType, returnType)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Method %s was expecting to return type %s, found %s",
                    fileName, n.i.lineNumber, n.i.columnNumber, n.i.s, returnType.toString(), foundType.toString()));
        }

        methodScope = null;

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from method declaration for %s.\n",
                fileName, n.i.lineNumber, n.i.columnNumber, n.i.s));
        return Type.THE_VOID_TYPE;
    }

    public Type visit (FieldDecl n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting field declaration for %s...\n",
                fileName, n.i.lineNumber, n.i.columnNumber, n.i.s));
        n.i.accept(this);
        n.t.accept(this);

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from field declaration for %s.\n",
                fileName, n.i.lineNumber, n.i.columnNumber, n.i.s));
        return Type.THE_VOID_TYPE;
    }

    public Type visit (LocalDecl n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting local declaration for %s...\n",
                fileName, n.i.lineNumber, n.i.columnNumber, n.i.s));
        n.t.accept(this);
        n.i.accept(this);

        // not one of the primitive types, and class type never declared
        if (!(typesMatch(n.t, Type.THE_INTEGER_TYPE) || typesMatch(n.t, Type.THE_BOOLEAN_TYPE) ||
                typesMatch(n.t, Type.THE_INT_ARRAY_TYPE) || typesMatch(n.t, Type.THE_VOID_TYPE))){
            if (!classExists(n.t.toString())) {
                sendErrorMessage(String.format("filename=%s:%03d:%03d -- Class %s (type of %s) is never declared.\n",
                        fileName, n.i.lineNumber, n.i.columnNumber, n.t.toString(), n.i.s));
            }
        }

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from local declaration for %s.\n",
                fileName, n.i.lineNumber, n.i.columnNumber, n.i.s));
        return Type.THE_VOID_TYPE;
    }

    public Type visit (FormalDecl n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting formal declaration for %s...\n",
                fileName, n.i.lineNumber, n.i.columnNumber, n.i.s));
        n.t.accept(this);
        n.i.accept(this);

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from formal declaration %s.\n",
                fileName, n.i.lineNumber, n.i.columnNumber, n.i.s));
        return Type.THE_VOID_TYPE;
    }

    public Type visit (IntArrayType n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting int[] type...\n",
                fileName, n.lineNumber, n.columnNumber));
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from int[] type.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_INT_ARRAY_TYPE;
    }

    public Type visit (BooleanType n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting boolean type...\n",
                fileName, n.lineNumber, n.columnNumber));
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from boolean type.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_BOOLEAN_TYPE;
    }

    public Type visit (IntegerType n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting int type...\n",
                fileName, n.lineNumber, n.columnNumber));
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from int type.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_INTEGER_TYPE;
    }

    public Type visit (VoidType n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting void type...\n",
                fileName, n.lineNumber, n.columnNumber));
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from void type.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_VOID_TYPE;
    }

    // String nameOfType;
    public Type visit (IdentifierType n) {

        if (n.nameOfType == null){
            sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting identifier type...\n",
                    fileName, n.lineNumber, n.columnNumber));
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Null Identifier type\n",
                    fileName, n.lineNumber, n.columnNumber));
        } else if (!classExists(n.nameOfType)){
            sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting identifier type %s...\n",
                    fileName, n.lineNumber, n.columnNumber, n.nameOfType));
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Class %s does not exist!\n",
                    fileName, n.lineNumber, n.columnNumber, n.nameOfType));
        } else {
            sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting identifier type %s...\n",
                    fileName, n.lineNumber, n.columnNumber, n.nameOfType));
        }

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning identifier type %s.\n",
                fileName, n.lineNumber, n.columnNumber, n.nameOfType));
        return n;
    }

    public Type visit (final Block n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting block of statements...\n",
                fileName, n.lineNumber, n.columnNumber));

        for (Statement s: n.sl) s.accept (this);

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from block of statements.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_VOID_TYPE;
    }

    public Type visit (final If n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting If statement...\n",
                fileName, n.lineNumber, n.columnNumber));
        Type returnType = n.e.accept(this);
        if(!typesMatch(returnType, Type.THE_BOOLEAN_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- If condition was expecting boolean, found %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }
        n.s1.accept(this);
        n.s2.accept(this);
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from If statement.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_VOID_TYPE;
    }

    public Type visit (final While n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting While statement...\n",
                fileName, n.lineNumber, n.columnNumber));
        Type returnType = n.e.accept(this);
        if(!typesMatch(returnType, Type.THE_BOOLEAN_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- While condition was expecting boolean, found %s\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }
        n.s.accept(this);
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from While statement.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_VOID_TYPE;
    }

    public Type visit (final Print n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting Print statement...\n",
                fileName, n.lineNumber, n.columnNumber));
        Type returnType = n.e.accept(this);
        if (!typesMatch(returnType, Type.THE_INTEGER_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Print expression was expecting int, found %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from Print statement.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_VOID_TYPE;
    }

    public Type visit (final Assign n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting Assign statement...\n",
                fileName, n.lineNumber, n.columnNumber));
        Type returnType, expectedType;
        HashMap<String, String> varInfo;

        n.i.accept (this);

        varInfo = varInMethod(n.i.s);
        if (varInfo == null) {
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Could not find var %s in scope of class %s method %s.\n",
                    fileName, n.lineNumber, n.columnNumber, n.i.s, currentClass, currentMethod));
        } else {
            expectedType = stringToType(varInfo.get("$type"));
            returnType = n.e.accept(this);
            if (!typesMatch(expectedType, returnType)) {
                sendErrorMessage(String.format("filename=%s:%03d:%03d -- Assign statement was expecting %s, got %s.\n",
                        fileName, n.lineNumber, n.columnNumber, expectedType.toString(), returnType.toString()));
            }
        }

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from Assign statement.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_VOID_TYPE;
    }

    // Subcomponents of ArrayAssign:  Identifier nameOfArray; Expression indexInArray, Expression e;
    public Type visit (final ArrayAssign n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting ArrayAssign statement...\n",
                fileName, n.lineNumber, n.columnNumber));
        Type returnType;

        n.nameOfArray.accept(this);
        returnType = stringToType(varInMethod(n.nameOfArray.s).get("$type"));
        // make sure that the identifier is of array type
        if(!typesMatch(returnType, Type.THE_INT_ARRAY_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Array identifier for assignment was" +
                            " expecting int[], got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        returnType = n.indexInArray.accept(this);
        if(!typesMatch(returnType, Type.THE_INTEGER_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Array index expression for assigment was" +
                            " expecting int, got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        returnType = n.e.accept(this);
        if(!typesMatch(returnType, Type.THE_INTEGER_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Array assign expression was expecting int, got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from ArrayAssign statement.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_VOID_TYPE;
    }

    // Expression e1,e2;
    public Type visit (final And n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting And statement...\n",
                fileName, n.lineNumber, n.columnNumber));

        Type returnType;
        // make sure both evaluate to boolean
        returnType = n.e1.accept(this);
        if(!typesMatch(returnType, Type.THE_BOOLEAN_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- And Expression 1 was expecting boolean, got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        returnType = n.e2.accept(this);
        if(!typesMatch(returnType, Type.THE_BOOLEAN_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- And Expression 2 was expecting boolean, got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from And statement.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_BOOLEAN_TYPE;
    }

    // Expression e1,e2;
    public Type visit (final LessThan n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting Less Than statement...\n",
                fileName, n.lineNumber, n.columnNumber));
        Type returnType;
        // make sure both are integers
        returnType = n.e1.accept(this);
        if(!typesMatch(returnType, Type.THE_INTEGER_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Less Than Expression 1 was expecting int, got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        returnType = n.e2.accept(this);
        if(!typesMatch(returnType, Type.THE_INTEGER_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Less Than Expression 2 was expecting int, got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from Less Than statement.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_BOOLEAN_TYPE;
    }

    // Expression e1,e2;
    public Type visit (final Plus n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting Plus statement...\n",
                fileName, n.lineNumber, n.columnNumber));
        Type returnType;
        // make sure both are integers!
        returnType = n.e1.accept(this);
        if(!typesMatch(returnType, Type.THE_INTEGER_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Plus Expression 1 was expecting int, got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        returnType = n.e2.accept(this);
        if(!typesMatch(returnType, Type.THE_INTEGER_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Plus Expression 2 was expecting int, got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from Plus statement.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_INTEGER_TYPE;
    }

    // Expression e1,e2;
    public Type visit (final Minus n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting Minus statement...\n",
                fileName, n.lineNumber, n.columnNumber));
        Type returnType;
        // make sure that both are integers
        returnType = n.e1.accept(this);
        if(!typesMatch(returnType, Type.THE_INTEGER_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Minus Expression 1 was expecting int, got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        returnType = n.e2.accept(this);
        if(!typesMatch(returnType, Type.THE_INTEGER_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Minus Expression 2 was expecting int, got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from Minus statement.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_INTEGER_TYPE;
    }

    // Expression e1,e2;
    public Type visit (final Times n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting Times statement...\n",
                fileName, n.lineNumber, n.columnNumber));
        // make sure both are ints
        Type returnType;
        returnType = n.e1.accept(this);
        if(!typesMatch(returnType, Type.THE_INTEGER_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Times Expression 1 was expecting int, got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        returnType = n.e2.accept(this);
        if(!typesMatch(returnType, Type.THE_INTEGER_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Times Expression 2 was expecting int, got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from Times statement.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_INTEGER_TYPE;
    }

    // Expression expressionForArray, indexInArray;
    public Type visit (final ArrayLookup n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting Array Lookup statement...\n",
                fileName, n.lineNumber, n.columnNumber));
        Type returnType;

        returnType = n.expressionForArray.accept(this);
        // make sure that type of array is actually int[]
        if(!typesMatch(returnType, Type.THE_INT_ARRAY_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Array identifier for lookup was" +
                            " expecting int[], got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        returnType = n.indexInArray.accept(this);
        if(!typesMatch(returnType, Type.THE_INTEGER_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Array index expression for lookup was" +
                            " expecting int, got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from Array Lookup statement.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_INTEGER_TYPE;
    }

    // Expression expressionForArray;
    public Type visit (final ArrayLength n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting Array Length statement...\n",
                fileName, n.lineNumber, n.columnNumber));
        Type returnType;

        returnType = n.expressionForArray.accept(this);
        if(!typesMatch(returnType, Type.THE_INT_ARRAY_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Array identifier for length was " +
                            "expecting int[], got %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from Array Length statement.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_INTEGER_TYPE;
    }


    // Subcomponents of Call:  Expression e; Identifier i; ExpressionList el;
    public Type visit (Call n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting call for %s...\n",
                fileName, n.lineNumber, n.columnNumber, n.i.s));
        Type returnType = Type.THE_VOID_TYPE;
        String argType, argTStr = "(";
        String methodName = n.i.s, callClassName;
        IdentifierType callReceiver;
        HashMap<String, HashMap> callClassInfo;
        HashMap<String, HashMap> methodInfo;
        boolean complete = true;
        int i = 0;

        n.i.accept (this);

        // make sure that e is of a type that has our call
        returnType = n.e.accept (this);
        if(typesMatch(returnType, Type.THE_INT_ARRAY_TYPE) || typesMatch(returnType, Type.THE_INTEGER_TYPE) ||
                typesMatch(returnType, Type.THE_BOOLEAN_TYPE) || typesMatch(returnType, Type.THE_VOID_TYPE)) {
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Call was expecting " +
                            "Object type, found %s. Skipping to end of call.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        } else {
            callReceiver = (IdentifierType) returnType;
            callClassName = callReceiver.nameOfType;
            callClassInfo = lookupTable.get(callClassName);

            if (callClassInfo == null) {
                sendErrorMessage(String.format("filename=%s:%03d:%03d -- Class %s does not exist.\n",
                        fileName, n.lineNumber, n.columnNumber, callClassName));
                returnType = Type.THE_VOID_TYPE;
            } else {
                methodInfo = callClassInfo.get(methodName);
                if (methodInfo == null) {
                    if (methodName.equals("_init")){
                        sendErrorMessage(String.format("filename=%s:%03d:%03d -- Class %s is not a record.\n",
                                fileName, n.lineNumber, n.columnNumber, callClassName, methodName));
                    } else {
                        sendErrorMessage(String.format("filename=%s:%03d:%03d -- " +
                                        "Class %s has no declarations for method %s.\n",
                                fileName, n.lineNumber, n.columnNumber, callClassName, methodName));
                    }
                    returnType = Type.THE_VOID_TYPE;
                } else {
                    // Iterate over the types of the args to find what method we are calling
                    for (Expression e : n.el) {
                        argType = e.accept(this).toString();
                        argTStr = argTStr.concat(argType.concat(", "));
                        if (methodInfo != null && methodInfo.containsKey(argType)) {
                            methodInfo = methodInfo.get(argType);
                        } else {
                            complete = false;
                            methodInfo = null;
                        }
                    }
                    argTStr = argTStr.substring(0, Math.max(1, argTStr.length() - 2)).concat(")");
                    // did we find a valid function declaration?
                    if (complete && methodInfo.containsKey("$end")) {
                        returnType = stringToType((String)((HashMap<String, Object>)methodInfo.get("$end").get("$info")).get("$type"));
                    } else {
                        // if not, our error message changes if it is a call to record initialization
                        if (methodName.equals("_init")){
                            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Record %s%s is not declared.\n",
                                    fileName, n.lineNumber, n.columnNumber, callClassName, argTStr));
                        } else {
                            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Class %s has no declarations for %s%s.\n",
                                    fileName, n.lineNumber, n.columnNumber, callClassName, methodName, argTStr));
                        }

                        returnType = Type.THE_VOID_TYPE;
                    }
                }
            }
        }

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from call for %s.\n",
                fileName, n.i.lineNumber, n.i.columnNumber, n.i.s));

        return returnType;
    }

    public Type visit (True n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting True...\n",
                fileName, n.lineNumber, n.columnNumber));
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from True.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_BOOLEAN_TYPE;
    }

    public Type visit (False n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting False...\n",
                fileName, n.lineNumber, n.columnNumber));
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from call for True.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_BOOLEAN_TYPE;
    }

    public Type visit (IntegerLiteral n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting IntegerLiteral...\n",
                fileName, n.lineNumber, n.columnNumber));
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from IntegerLiteral.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_INTEGER_TYPE;
    }

    // Subcompoents of identifier statement: String:s
    public Type visit (IdentifierExp n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting IdentifierExp...\n",
                fileName, n.lineNumber, n.columnNumber));

        String type = null;
        HashMap<String, String> var = varInMethod(n.s);
        if (var == null){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Could not find variable %s within scope of " +
                    "class %s, method %s\n", fileName, n.lineNumber, n.columnNumber, n.s, currentClass, currentMethod));
        } else {
            type = var.get("$type");
        }

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from IdentifierExp.\n",
                fileName, n.lineNumber, n.columnNumber));
        return stringToType(type);
    }

    public Type visit (This n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting This...\n",
                fileName, n.lineNumber, n.columnNumber));
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from This.\n",
                fileName, n.lineNumber, n.columnNumber));
        // returns the class that this is in
        return stringToType(currentClass);
    }

    // Expression e;
    public Type visit (NewArray n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting New Array...\n",
                fileName, n.lineNumber, n.columnNumber));
        Type returnType;
        returnType = n.e.accept (this);
        if(!typesMatch(returnType, Type.THE_INTEGER_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Array size for allocation was expecting type int," +
                            " found %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from New Array.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_INT_ARRAY_TYPE;
    }

    // Identifier i;
    public Type visit (NewObject n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting New Object...\n",
                fileName, n.lineNumber, n.columnNumber));
        Type returnType;
        n.i.accept(this);
        returnType = new IdentifierType(n.lineNumber, n.columnNumber, n.i.s);

        if (!classExists(n.i.s)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Class %s does not exist.\n",
                    fileName, n.lineNumber, n.columnNumber, n.i.s));
        }

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from Object.\n",
                fileName, n.lineNumber, n.columnNumber));
        return returnType;
    }

    // Expression e;
    public Type visit (Not n) {
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting Not...\n",
                fileName, n.lineNumber, n.columnNumber));
        Type returnType;
        returnType = n.e.accept (this);

        if(!typesMatch(returnType, Type.THE_BOOLEAN_TYPE)){
            sendErrorMessage(String.format("filename=%s:%03d:%03d -- Not operator was expecting expression of" +
                            " type bool, found %s.\n",
                    fileName, n.lineNumber, n.columnNumber, returnType.toString()));
        }

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning from Not.\n",
                fileName, n.lineNumber, n.columnNumber));
        return Type.THE_BOOLEAN_TYPE;
    }

    // String s;
    public Type visit (Identifier n) {
        Type varT;
        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Visiting Identifier...\n",
                fileName, n.lineNumber, n.columnNumber));

        sendDebugMessage(String.format("filename=%s:%03d:%03d -- Returning Identifier.\n",
                fileName, n.lineNumber, n.columnNumber));

        return Type.THE_VOID_TYPE;
    }
}
