package global;
import syntax.*;

import global.IRTree.*;
import tree.*;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public final class IRTranslator implements SyntaxTreeVisitor <LazyIRTree>  {
    private String filename;
    private PrintWriter pw;
    private boolean verbose;

    // holds our lookup data
    private HashMap<String, HashMap> lookupTable;
    private String currentClass, currentMethod;
    private HashMap<String, HashMap> classScope, methodScope;

    // holds all the fragments to return later
    private ArrayList<Stm> fragments = new ArrayList();

    // we shouldn't find errors at this point, but just to be sure
    private int errors = 0;

    public IRTranslator (String fileName, PrintWriter pw, HashMap<String, HashMap> lookup)
        { filename = fileName; this.pw = pw; verbose = true; lookupTable = lookup; }

    public IRTranslator (String fileName, HashMap<String, HashMap> lookup)
        { filename = fileName; lookupTable = lookup; pw = null; verbose=false; }

    public int getErrors(){ return errors; }

    // answers where we can find our variable in memory
    private LazyIRTree getVarLoc(String varName){
        LazyIRTree ret = null;
        HashMap<String, HashMap> scope = null;
        HashMap<String, Object> methodFind = null;
        int loc = 0;

        // if there are fields, search there
        if (classScope.containsKey("$fields")){
            scope = classScope.get("$fields");
            if (scope.containsKey(varName)){
                // if its a field, we go back to where our object is
                loc = (int)scope.get(varName).get("$fieldloc");
                ret = new IRVariable(new BINOP(BINOP.PLUS, new TEMP("%i0"),
                        new CONST(loc * 4)));
            }
        }

        // prioritize using locals and args before fields
        methodFind = findVarInMethod(varName);
        if (methodFind != null){
            if (methodFind.containsKey("$argloc")){
                // if it is an incoming argument
                loc = (int)methodFind.get("$argloc");
                ret = new IRVariable(new TEMP(String.format("%%i%d", loc)));
            } else if (methodFind.containsKey("$locloc")) {
                // if it is a local declaration, we move onto the heap
                loc = (int)methodFind.get("$locloc");
                ret = new IRVariable(new BINOP(BINOP.MINUS, new TEMP("%fp"), new CONST(4 * loc)));
            }
        }

        if (ret == null){
            System.err.printf("Could not find declaration of %s in class %s, method %s\n", varName, currentClass, currentMethod);
            errors++;
        }

        return ret;
    }

    private HashMap<String, Object> getVarInMethod(String varName){
        HashMap<String, Object> ret = null, rTemp;
        HashMap<String, HashMap> scope = null;
        int loc = 0;

        // check our fields first
        if (classScope.containsKey("$fields")){
            scope = classScope.get("$fields");
            if (scope.containsKey(varName)){
                ret = scope.get(varName);
            }
        }

        // we prioritize using args and locals over fields, and args and locals will never have the same name
        rTemp = findVarInMethod(varName);
        if (rTemp != null){
            ret = rTemp;
        }

        return ret;
    }

    private HashMap<String, Object> findVarInMethod(String varName){
        return findVarInMethod(varName, methodScope);
    }

    private HashMap<String, Object> findVarInMethod(String varName, HashMap<String, HashMap> current){
        return current.get(varName);
    }


    private void SendDebugMessage(String message){
        if (verbose){
            pw.print(message);
        }
    }

    private Type stringToType(String s){ return stringToType(s, 0, 0); }

    private Type stringToType(String s, int l, int c){
        if(s.equals("int")){
            return Type.THE_INTEGER_TYPE;
        } else if (s.equals("int[]")){
            return Type.THE_INT_ARRAY_TYPE;
        } else if (s.equals("boolean")){
            return Type.THE_BOOLEAN_TYPE;
        } else if (s.equals("void")){
            return Type.THE_VOID_TYPE;
        } else {
            return new IdentifierType(l, c, s);
        }
    }

    private Type getTypeOfExpression(Expression e){
        String idType;
        Call call;
        Type originT;
        HashMap<String, HashMap> scope;
        HashMap<String, Object> var;
        Set<String> expectedArgs;

        // handle the integer types
        if (e instanceof IntegerLiteral || e instanceof ArrayLength || e instanceof ArrayLookup ||
            e instanceof Plus || e instanceof Minus || e instanceof Times ){
            return Type.THE_INTEGER_TYPE;
        // and the booleans
        } else if (e instanceof And || e instanceof LessThan || e instanceof True || e instanceof False ||
                   e instanceof False || e instanceof Not){
            return Type.THE_BOOLEAN_TYPE;
        // and the int array
        } else if (e instanceof NewArray) {
            return Type.THE_INT_ARRAY_TYPE;
        // and the new object
        } else if (e instanceof NewObject){
            return new IdentifierType(e.lineNumber, e.columnNumber, ((NewObject) e).i.toString());
        // what is the type of the variable we are calling?
        } else if (e instanceof IdentifierExp){
            idType = (String) getVarInMethod(((IdentifierExp) e).s).get("$type");
            return stringToType(idType, e.lineNumber, e.columnNumber);
        // we're keeping track of what class we're in, so this is pretty easy
        } else if (e instanceof This){
            return new IdentifierType(e.lineNumber, e.columnNumber, currentClass);
        // we may have multiple return types for the same name of method
        // so we have to look for the return type, but first we have to find what type our origin is
        } else if (e instanceof Call){
            call = (Call)e;

            originT = getTypeOfExpression(call.e);
            idType = originT.toString();
            scope = lookupTable.get(idType);

            if (!(originT instanceof IdentifierType)){
                System.err.printf("filename=%s:%03d:%03d -- Call must have Object type, found %s\n",
                        filename, e.lineNumber, e.columnNumber, idType);
                errors++;
            } else {
                scope = scope.get(call.i.s);
                for (Expression exp: call.el){
                    idType = getTypeOfExpression(exp).toString();
                    scope = scope.get(idType);
                }
                scope = scope.get("$end");
                return stringToType((String) scope.get("$info").get("$type"));
            }

            return Type.THE_VOID_TYPE;
        } else{
            return Type.THE_VOID_TYPE;
        }
    }

    public ArrayList<Stm> getFragments(final Program program){
        if (fragments.size() == 0){
            visit(program);
        }
        return fragments;
    }

    // Subcomponents of Program:  MainClass m; List<ClassDecl> cl;
    public LazyIRTree visit (final Program n) {
        SendDebugMessage("Entering program...\n");
        if (n!=null && n.m!=null) {
            fragments.add(n.m.accept (this).asStm());
            for (ClassDecl c: n.cl) c.accept (this).asStm();
        }
        SendDebugMessage("Exiting program...\n");
        if (verbose){
            pw.flush();
        }
        return new IRNull();
    }

    // Subcomponents of MainClass:  Identifier i1, i2; Statement s;
    public LazyIRTree visit (final MainClass n) {
        LazyIRTree method;
        SendDebugMessage("Entering Main Class...\n");
        n.nameOfMainClass.accept(this);
        n.nameOfCommandLineArgs.accept(this);

        currentClass = n.nameOfMainClass.s;
        classScope = lookupTable.get(currentClass);
        currentMethod = "main";
        methodScope = ((HashMap<String, HashMap>) classScope.get(currentMethod).get("String")).get("$end");

        method = new IRMethod(currentClass, currentMethod,
                (String) methodScope.get("$info").get("$declnum"), n.body.accept(this));
        // statement:  body of main


        SendDebugMessage("Exiting Main Class...\n");
        return method;
    }

    // Subcomponents of SimpleClassDecl: Identifier i; List<FieldDecl> vl; List<MethodDecl> ml;
    public LazyIRTree visit (final SimpleClassDecl n) {
        SendDebugMessage(String.format("Entering SimpleClassDecl for %s...\n", n.i.s));
        n.i.accept (this);

        currentClass = n.i.s;
        classScope = lookupTable.get(currentClass);

        for (FieldDecl v: n.fields){
            v.accept (this).asExp();
        }
        for (MethodDecl m: n.methods) {
            fragments.add(m.accept(this).asStm());
        }

        currentClass = null;
        classScope = null;

        // Does end with a newline
        SendDebugMessage(String.format("Exiting SimpleClassDecl for %s...\n", n.i.s));
        return new IRNull();
    }

    // Subcomponents of ExtendingClassDecl: Identifier i, j; List<FieldDecl> vl; List<MethodDecl> ml;
    public LazyIRTree visit (final ExtendingClassDecl n) {
        SendDebugMessage(String.format("Entering ExtendingClassDecl for %s @ %03d:%03d...\n",
                n.i.s, n.i.lineNumber, n.i.columnNumber));
        n.i.accept (this);
        n.j.accept (this);

        currentClass = n.i.s;
        classScope = lookupTable.get(currentClass);

        for (final FieldDecl v: n.fields) {
            v.accept(this);
        }
        for (final MethodDecl m: n.methods) fragments.add(m.accept (this).asStm());

        currentClass = null;
        classScope = null;

        SendDebugMessage(String.format("Exiting ExtendingClassDecl for %s @ %03d:%03d...\n",
                n.i.s, n.i.lineNumber, n.i.columnNumber));
        return new IRNull();
    }

    // Subcomponents of MethodDecl:
    // Type t; Identifier i; List<FormalDecl> fl; List<LocalDecl> locals; List<Statement>t sl; Expression e;
    public LazyIRTree visit (final MethodDecl n) {
        SendDebugMessage(String.format("Entering MethodDecl for %s @ %03d:%03d...\n",
                n.i.s, n.i.lineNumber, n.i.columnNumber));

        int n_formal = 0;
        int loc_local= 0;
        LazyIRTree statements = null;
        Statement s = null;
        ListIterator<Statement> slIterator = n.sl.listIterator(n.sl.size());
        String methodNum;
        HashMap<String, HashMap> methodInfo;

        n.i.accept(this);

        currentMethod = n.i.s;
        methodScope = classScope.get(currentMethod);

        // visit our formals and determine what method declaration in particular we are looking at
        for (FormalDecl f: n.formals){
            f.accept(this);
            methodScope = methodScope.get(f.t.toString());
        }
        methodScope = methodScope.get("$end");
        methodNum = (String) methodScope.get("$info").get("$declnum");

        // we visit our locals
        for (final LocalDecl v: n.locals) {
            v.accept(this);
        }

        // load in our return statement first
        statements = new IRReturn(n.e.accept(this));
        // iterate backwards over the statements list
        while (slIterator.hasPrevious()) {
            s = slIterator.previous();
            statements = new IRStatementBlock(s.accept(this), statements);
        }

        statements = new IRMethod(currentClass, currentMethod, methodNum, statements);

        currentMethod = null;
        methodScope = null;

        SendDebugMessage(String.format("Exiting MethodDecl for %s @ %03d:%03d...\n",
                n.i.s, n.i.lineNumber, n.i.columnNumber));
        return statements;
    }

    public LazyIRTree visit (FieldDecl n) {
        SendDebugMessage(String.format("Entering MethodDecl for %s @ %03d:%03d...\n",
                n.i.s, n.i.lineNumber, n.i.columnNumber));

        n.i.accept(this);
        n.t.accept(this);

        SendDebugMessage(String.format("Exiting FieldDecl for %s @ %03d:%03d...\n",
                n.i.s, n.i.lineNumber, n.i.columnNumber));
        return new IRNull();
    }

    public LazyIRTree visit (LocalDecl n) {
        SendDebugMessage(String.format("Entering LocalDecl for %s @ %03d:%03d...\n",
                n.i.s, n.i.lineNumber, n.i.columnNumber));

        n.i.accept(this);
        n.t.accept(this);

        SendDebugMessage(String.format("Exiting LocalDecl for %s @ %03d:%03d...\n",
                n.i.s, n.i.lineNumber, n.i.columnNumber));

        return new IRNull();
    }

    // Subcomponents of FormalDecl:  Type t; Identifier i;
    public LazyIRTree visit (FormalDecl n) {
        SendDebugMessage(String.format("Entering FormalDecl for %s @ %03d:%03d...\n",
                n.i.s, n.i.lineNumber, n.i.columnNumber));

        n.i.accept(this);
        n.t.accept(this);

        SendDebugMessage(String.format("Exiting FormalDecl for %s @ %03d:%03d...\n",
                n.i.s, n.i.lineNumber, n.i.columnNumber));

        return new IRNull();
    }

    public LazyIRTree visit (IntArrayType n) {
        SendDebugMessage(String.format("Entering int[] @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        SendDebugMessage(String.format("Exiting int[] @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));
        return new IRNull();
    }

    public LazyIRTree visit (BooleanType n) {
        SendDebugMessage(String.format("Entering bool @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        SendDebugMessage(String.format("Exiting bool @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));
        return new IRNull();
    }

    public LazyIRTree visit (IntegerType n) {
        SendDebugMessage(String.format("Entering int @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        SendDebugMessage(String.format("Exiting int @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));
        return new IRNull();
    }

    public LazyIRTree visit (VoidType n) {
        SendDebugMessage(String.format("Entering void @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        SendDebugMessage(String.format("Exiting void @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));
        return new IRNull();
    }

    // String nameOfType;
    public LazyIRTree visit (IdentifierType n) {
        SendDebugMessage(String.format("Entering type %s @ %03d:%03d...\n",
                n.nameOfType, n.lineNumber, n.columnNumber));

        SendDebugMessage(String.format("Exiting type %s @ %03d:%03d...\n",
                n.nameOfType, n.lineNumber, n.columnNumber));
        return new IRNull();
    }

    // Subcomponents of Block statement:  StatementList sl;
    public LazyIRTree visit (final Block n) {
        SendDebugMessage(String.format("Entering Block @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ListIterator<Statement> sIterator = n.sl.listIterator(n.sl.size());
        int numStms;
        LazyIRTree lastSeq = null;
        LazyIRTree retSeq = null;

        if(sIterator.hasPrevious()){
            retSeq = sIterator.previous().accept(this);
            while(sIterator.hasPrevious()){
                retSeq = new IRStatementBlock(sIterator.previous().accept(this), retSeq);
            }
        } else {
            retSeq = new IRNull();
        }

        SendDebugMessage(String.format("Exiting Block @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return retSeq;
    }

    // Subcomponents of If statement: Expression e; Statement s1,s2;
    public LazyIRTree visit (final If n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering If statement @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRIfThenElse(n.e.accept(this), n.s1.accept(this), n.s2.accept(this));

        SendDebugMessage(String.format("Exiting If statement @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));
        return ret;
    }

    // Subcomponents of While statement: Expression e, Statement s
    public LazyIRTree visit (final While n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering While statement @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRWhile(n.e.accept(this), n.s.accept(this));

        SendDebugMessage(String.format("Exiting While statement @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));
        return ret;
    }

    // Subcomponents of Print statement:  Expression e;
    public LazyIRTree visit (final Print n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering Print statement @ %03d:%03d...\n",
            n.lineNumber, n.columnNumber));

        ret = new IRPrint(n.e.accept(this));

        SendDebugMessage(String.format("Exiting Print statement @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));
        return ret;
    }

    // subcomponents of Assignment statement:  Identifier i; Expression e;
    public LazyIRTree visit (final Assign n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering Assign statement @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        n.i.accept(this);
        ret = new IRAssign(getVarLoc(n.i.s), n.e.accept(this));

        SendDebugMessage(String.format("Exiting Assign statement @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }

    // Subcomponents of ArrayAssign:  Identifier nameOfArray; Expression indexInArray, Expression e;
    public LazyIRTree visit (final ArrayAssign n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering Array Assign statement @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRArrayAssign(n.nameOfArray.accept(this), n.indexInArray.accept(this), n.e.accept(this));

        SendDebugMessage(String.format("Exiting Array Assign statement @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }

    // Expression e1,e2;
    public LazyIRTree visit (final And n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering And expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRAnd(n.e1.accept(this), n.e2.accept(this));

        SendDebugMessage(String.format("Exiting And expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }

    // Expression e1,e2;
    public LazyIRTree visit (final LessThan n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering Less Than expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRLessThan(n.e1.accept(this), n.e2.accept(this));

        SendDebugMessage(String.format("Exiting Less Than expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }

    // Expression e1,e2;
    public LazyIRTree visit (final Plus n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering Plus expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRPlus(n.e1.accept(this), n.e2.accept(this));

        SendDebugMessage(String.format("Exiting Plus expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }

    // Expression e1,e2;
    public LazyIRTree visit (final Minus n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering Minus expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRMinus(n.e1.accept(this), n.e2.accept(this));

        SendDebugMessage(String.format("Exiting Minus expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }

    // Expression e1,e2;
    public LazyIRTree visit (final Times n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering Times expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRTimes(n.e1.accept(this), n.e2.accept(this));

        SendDebugMessage(String.format("Exiting Times expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }

    // Expression expressionForArray, indexInArray;
    public LazyIRTree visit (final ArrayLookup n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering Array Lookup expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRArrayLookup(n.expressionForArray.accept(this), n.indexInArray.accept(this));

        SendDebugMessage(String.format("Exiting Array Lookup expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }

    // Expression expressionForArray;
    public LazyIRTree visit (final ArrayLength n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering Array Length expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRArrayLength(n.expressionForArray.accept(this));

        SendDebugMessage(String.format("Exiting Array Length expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }


    // Subcomponents of Call:  Expression e; Identifier i; ExpressionList el;
    public LazyIRTree visit (Call n) {
        Type typeT = getTypeOfExpression(n.e);
        List<LazyIRTree> expList = new ArrayList();
        LazyIRTree origin = n.e.accept(this), ret = new IRNull();
        String cName = typeT.toString(), mName = n.i.s, number;
        HashMap<String, HashMap> scope = lookupTable;

        SendDebugMessage(String.format("Entering Call to %s @ %03d:%03d...\n",
                mName, n.lineNumber, n.columnNumber));

        n.i.accept(this);

        if (!(typeT instanceof IdentifierType)){
            System.err.printf("filename=%s:%03d:%03d -- Cannot handle call %s from primitive type: %s.\n",
                    filename, n.lineNumber, n.columnNumber, mName, cName);
            errors++;
        } else if (!lookupTable.containsKey(cName)){
            System.err.printf("filename=%s:%03d:%03d -- Could not find type of caller: %s.\n",
                    filename, n.lineNumber, n.columnNumber, cName);
            errors++;
        } else if (!lookupTable.get(cName).containsKey(mName)){
            System.err.printf("filename=%s:%03d:%03d -- Could not find method %s in class %s.\n",
                    filename, n.lineNumber, n.columnNumber, mName, cName);
            errors++;
        } else {
            scope = (HashMap<String, HashMap>) lookupTable.get(cName).get(mName);
            for (Expression e: n.el){
                typeT = getTypeOfExpression(e);
                if(scope != null && scope.containsKey(typeT.toString())){
                    scope = scope.get(typeT.toString());
                    expList.add(e.accept(this));
                } else {
                    scope = null;
                }
            }
            if (scope != null && scope.containsKey("$end")){
                scope = scope.get("$end");
                number = (String) scope.get("$info").get("$declnum");
                ret = new IRCall(origin, new NAME(new NameOfLabel(cName, mName, number)), expList);
            } else {
                System.err.printf("filename=%s:%03d:%03d -- No matching method declaration for %s in class %s found.\n",
                        filename, n.lineNumber, n.columnNumber, mName, cName);
                errors++;
                ret = new IRNull();
            }
        }

        SendDebugMessage(String.format("Exiting Call to %s @ %03d:%03d...\n",
                mName, n.lineNumber, n.columnNumber));

        return ret;
    }

    public LazyIRTree visit (True n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering True expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRTrue();

        SendDebugMessage(String.format("Exiting True expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }

    public LazyIRTree visit (False n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering False expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRFalse();

        SendDebugMessage(String.format("Exiting False expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }

    public LazyIRTree visit (IntegerLiteral n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering IntegerLiteral expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRIntegerLiteral(n.i);

        SendDebugMessage(String.format("Exiting IntegerLiteral expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }

    // Subcompoents of identifier statement: String:s
    public LazyIRTree visit (IdentifierExp n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering Identifier expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = getVarLoc(n.s);

        SendDebugMessage(String.format("Exiting Identifier expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }

    public LazyIRTree visit (This n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering This expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRThis();

        SendDebugMessage(String.format("Exiting This expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }

    // Expression e;
    public LazyIRTree visit (NewArray n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering New Array expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRNewArray(n.e.accept(this));

        SendDebugMessage(String.format("Exiting New Array expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }

    // Identifier i;
    public LazyIRTree visit (NewObject n) {
        String cName = n.i.s;
        LazyIRTree ret;

        n.i.accept(this);

        SendDebugMessage(String.format("Entering New %s expression @ %03d:%03d...\n",
                cName, n.lineNumber, n.columnNumber));

        ret = new IRNewObject((int)classScope.get("$info").get("$numfields"));

        SendDebugMessage(String.format("Exiting New %s expression @ %03d:%03d...\n",
                cName, n.lineNumber, n.columnNumber));

        return ret;
    }

    // Expression e;
    public LazyIRTree visit (Not n) {
        LazyIRTree ret;

        SendDebugMessage(String.format("Entering Not expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        ret = new IRNot(n.e.accept(this));

        SendDebugMessage(String.format("Exiting Not expression @ %03d:%03d...\n",
                n.lineNumber, n.columnNumber));

        return ret;
    }

    // String s;
    public LazyIRTree visit (Identifier n) {

        SendDebugMessage(String.format("Entering Identifier %s @ %03d:%03d...\n",
                n.s, n.lineNumber, n.columnNumber));

        SendDebugMessage(String.format("Exiting Identifier %s @ %03d:%03d...\n",
                n.s, n.lineNumber, n.columnNumber));

        return new IRNull();
    }
}
