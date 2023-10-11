package global;
import syntax.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.ListIterator;

public class LookupCreator implements SyntaxTreeVisitor <HashMap>  {
    HashMap<String, HashMap> staticTable = null;
    int errors = 0;
    boolean verbose;
    String fileName, expectedMainClassName;
    PrintWriter debugOut;

    public LookupCreator (String fileName)
        { debugOut = null; verbose = false; init(fileName); }
    public LookupCreator (String fileName, String verboseOut) throws FileNotFoundException
        { debugOut=new PrintWriter(verboseOut); verbose = true; init(fileName); }
    public LookupCreator (String fileName, PrintWriter verboseOut)
        { debugOut= verboseOut; verbose = true; init(fileName); }

    private void init(String fileName){
        String[] fileParts;
        this.fileName = fileName;

        fileParts = fileName.split("\\\\");
        fileParts = fileParts[fileParts.length -1].split("\\.");
        expectedMainClassName = fileParts[0];
    }

    public int getErrors(){ return errors; }
    public HashMap getTable(){ return staticTable; }
    public HashMap formTable(final Program n) { return visit(n); }

    // calculate the class closure of our map and make sure it isn't recursive
    // (if it is, we don't calculate inheritance for that class and we throw an error)
    private void classClosure(HashMap<String, HashMap> root){
        HashMap<String, TreeSet<String>> inheritances = inheritanceClosure(root);
        int numMethods = 0;
        HashMap<String, HashMap> method;

        // now we can see if anything is recursive
        if (inheritances != null) {
            for (String key : inheritances.keySet()) {
                if (inheritances.get(key).contains(key)) {
                    System.err.printf("filename=%s:000:000 -- Class %s inherits itself recursively!\n", fileName, key);
                    errors++;
                } else {
                    doInheritances(inheritances, root, key);
                }
            }
        } else {
            for (String c: root.keySet()){
                for (String m: ((HashMap<String, HashMap>) root.get(c)).keySet()){
                    method = (HashMap<String, HashMap>)root.get(c).get(m);
                    numMethods = countMethods(method);
                    method.get("$info").put("$numdecls", numMethods);
                }
            }
        }
    }

    // calculate the inheritance closure of our class
    private HashMap<String, TreeSet<String>> inheritanceClosure(HashMap<String, HashMap> root){
        boolean changed = false;
        HashMap<String, TreeSet<String>> inheritances = new HashMap();
        HashMap<String, String> classInfo;
        TreeSet<String> close = null;
        String inhClass = null;

        // get all of the inheriting elements in inheritances
        for (String key : root.keySet()) {
            // if there is inheritance for the key
            classInfo = ((HashMap<String,HashMap>) root.get(key)).get("$info");
            if (classInfo != null && classInfo.containsKey("$inherit")){
                // create the list to close
                close = new TreeSet();
                // add what we are inheriting from to our closures
                inhClass = classInfo.get("$inherit");
                if (root.containsKey(inhClass)) {
                    close.add(inhClass);
                    inheritances.put(key, close);
                    changed = true;
                } else {
                    // print out that the inherited class could not be found and throw an error
                    System.err.printf("filename=%s:000:000 -- Class %s (inherited by class %s) does not exist!\n",
                            fileName, inhClass, key);
                    errors++;
                }
            }
        }
        // since we rooted out those that don't inherit, we can now calculate the recursive inheritance easily
        while(changed) {
            changed = false;
            for (String key : inheritances.keySet()) {
                for (String inherits : inheritances.keySet()) {
                    if (!key.equals(inherits) && inheritances.get(key).contains(inherits)) {
                        // if there are any changes during the iteration, we need to keep that info
                        changed = changed || inheritances.get(key).addAll(inheritances.get(inherits));
                    }
                }
            }
        }
        return inheritances;
    }

    // add all our inherited stuff to the classes which inherit them
    private void doInheritances(HashMap<String, TreeSet<String>> inheritances,
                                HashMap<String, HashMap> root, String key){
        HashMap<String, HashMap> absent = null; // only used to check if putIfAbsent put something or not
        HashMap<String, HashMap> base = null, inhBase = null;
        HashMap<String, HashMap> bMethod = null, inhMethod = null;

        int numDecls = 0, newNum = 0;

        // for each class it is inheriting from
        for (String inherited : inheritances.get(key) ){
            // define our current base and inherited base scopes
            base = ((HashMap<String, HashMap>)root.get(key));
            inhBase = ((HashMap<String, HashMap>)root.get(inherited));

            // loop over the methods we might be inheriting
            for (String method : inhBase.keySet()){
                // what method in particular are we looking at for both the base and inherited
                bMethod = (HashMap<String, HashMap>)base.get(method);
                inhMethod = (HashMap<String, HashMap>)inhBase.get(method);

                // if we are merging fields
                if (method.equals("$fields")){
                    // iterate over fields and add them
                    for (String inhField : inhMethod.keySet()){
                        // but only add fields that are not already there
                        absent = base.putIfAbsent(inhField, bMethod);
                        if (absent == null) {
                            base.get("$info").put("$numfields", ((int) base.get("$info").get("$numfields"))+1);
                        }
                    }
                // we just skip info since we don't want to inherit it
                } else if (!method.equals("$info")) {
                    // add all the method declarations
                    if (bMethod != null) {
                        // merge the methods and see how many we ended up with
                        mergeMethods(bMethod, inhMethod);
                        newNum = countMethods(bMethod);
                        bMethod.get("$info").put("$numdecls", newNum);
                    } else {
                        // if we don't find a declared method, we can just add it simply like this
                        base.put(method, inhMethod);
                    }
                }
            }
        }
    }

    // takes in a base class' declaration of a method and a class' method to inherit from
    // as well as the number of currently declared methods of the same name
    // recursively adds all the new definitions from inherit into our base
    private void mergeMethods(HashMap<String, HashMap> base, HashMap<String, HashMap> inherit){
        int newNum = 0;
        if (inherit != null) {
            for (String type : inherit.keySet()) {
                if(type.equals("$end")){
                    if(!base.containsKey("$end")){
                        // if there is a new ending, add the new info, otherwise ignore
                        base.put("$end", inherit.get("$end"));
                    }
                // ignore info since we don't want to overwrite that
                } else if (!type.equals("$info")) {
                    // if the base already contains the key, we need to go deeper
                    if (base.containsKey(type)){
                        mergeMethods(base.get(type), inherit.get(type));
                    } else {
                        // otherwise, we can simply add
                        base.put(type, inherit.get(type));
                    }
                }
            }
        }
    }

    private int countMethods(HashMap<String, HashMap> methodBase) {
        return countMethods(methodBase, 0);
    }

    private int countMethods(HashMap<String, HashMap> methodBase, int current_count){
        int count = current_count;
        for (String key: methodBase.keySet()){
            // grab all the methods and relabel them
            if(key.equals("$end")){
                count = count + 1;
                ((HashMap)methodBase.get(key).get("$info")).put("$declnum", String.format("%03d", count));
            } else if (!key.equals("$info")) {
                // again, we ignore info
                // if we aren't at an end, we move to the next arg and go again
                count = count + countMethods(methodBase.get(key), count);
            }
        }
        return count;
    }

    private void tabOver(int amount, PrintWriter to){
        if (amount > 0) {
            for (int i = 0; i < amount; i++) {
                to.print("|   ");
            }
            to.print("+-> ");
        }
    }

    // prints out our table to debugOut
    private void printTable(){
        HashMap<String, HashMap> classItem;
        HashMap<String, HashMap> methodItem;
        HashMap<String, Object> varsItem;
        int tab;
        for (String className : staticTable.keySet()){
            tab = 0;
            tabOver(tab, debugOut);
            debugOut.printf("Class %s\n", className);

            classItem = staticTable.get(className);
            for (String method : classItem.keySet()){

                if (method.equals("$info")){
                    varsItem = classItem.get(method);
                    if (varsItem != null) {
                        tab = 1;
                        tabOver(tab, debugOut);
                        debugOut.println("CLASS INFO:");
                        for (String infoKey : varsItem.keySet()) {
                            tab = 2;
                            tabOver(tab, debugOut);
                            debugOut.printf("%s: %s\n", infoKey, varsItem.get(infoKey));
                        }
                    }
                } else if (method.equals("$fields")){
                    methodItem = classItem.get(method);
                    if (methodItem != null){
                        tab = 1;
                        tabOver(tab, debugOut);
                        debugOut.println("FIELDS:");
                        for (String varsKey : methodItem.keySet()) {
                            varsItem = methodItem.get(varsKey);
                            if (varsItem != null) {
                                tab = 2;
                                tabOver(tab, debugOut);
                                debugOut.printf("%s\n", varsKey);
                                for (String infoKey : varsItem.keySet()) {
                                    tab = 3;
                                    tabOver(tab, debugOut);
                                    debugOut.printf("%s: %s\n", infoKey, varsItem.get(infoKey));
                                }
                            }
                        }
                    }
                } else {
                    methodItem = getMethods(classItem.get(method));
                    if(methodItem != null) {
                        tab = 1;
                        tabOver(tab, debugOut);
                        debugOut.println("METHOD: " + method);
                        for (String varsKey : methodItem.keySet()) {
                            tab = 2;
                            tabOver(tab, debugOut);
                            debugOut.printf("%s%s\n", method, varsKey);
                            varsItem = methodItem.get(varsKey);

                            if (varsItem != null) {
                                if (varsKey.equals("$info")){
                                    for (String infoKey : varsItem.keySet()) {
                                        tab = 3;
                                        tabOver(tab, debugOut);
                                        debugOut.printf("%s: %s\n", infoKey, varsItem.get(infoKey));
                                    }
                                } else {
                                    for (String infoKey : varsItem.keySet()) {
                                        tab = 3;
                                        tabOver(tab, debugOut);
                                        debugOut.printf("%s:\n", infoKey);
                                        for (String i : ((HashMap<String, HashMap>) varsItem.get(infoKey)).keySet()) {
                                            tab = 4;
                                            tabOver(tab, debugOut);
                                            debugOut.printf("%s: %s\n", i,
                                                    ((HashMap<String, HashMap>) varsItem.get(infoKey)).get(i));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private HashMap<String, HashMap> getMethods(HashMap<String, HashMap> base){
        return getMethods(base, new HashMap<String, HashMap>(), "(");
    }

    private HashMap<String, HashMap> getMethods(HashMap<String, HashMap> base,
                                                HashMap<String, HashMap> data, String prefix){
        if (base != null) {
            for (String key : base.keySet()) {
                if (key.equals("$info")) {
                    data.put("$info", base.get("$info"));
                } else if (key.equals("$end")) {
                    data.put(String.format("%s)", prefix.substring(0, Math.max(1, prefix.length()-2))), base.get("$end"));
                } else {
                    getMethods(base.get(key), data, String.format("%s%s, ", prefix, key));
                }
            }
            return data;
        } else {
            return null;
        }
    }

    // Subcomponents of Program: MainClass m; List<ClassDecl> cl;
    public HashMap visit (final Program n) {
        HashMap<String, HashMap> progTable = new HashMap(), method = null;
        int numMethods = 0;

        if (n==null) {
            System.err.printf("filename=%s:000:000 -- Null Program!\n", fileName);
            errors++;
            progTable = null;
        } else if (n.m==null) {
            System.err.printf("filename=%s:000:000 -- Null Main Program!\n", fileName);
            errors++;
            progTable = null;
        } else if (!n.m.nameOfMainClass.s.equals(expectedMainClassName)){
            System.err.printf("filename=%s:%03d:%03d -- Main Class name (%s) does not match filename!\n",
                    fileName, n.m.nameOfMainClass.lineNumber, n.m.nameOfMainClass.columnNumber, n.m.nameOfMainClass.s);
            errors++;
            progTable = null;
        } else {
            progTable.put(n.m.nameOfMainClass.s, n.m.accept(this));
            for (ClassDecl c : n.cl) {
                if (progTable.containsKey(c.i.s)){
                    System.err.printf("filename=%s:%03d:%03d -- Class (%s) is already declared!\n",
                            fileName, c.i.lineNumber, c.i.columnNumber, c.i.s);
                    errors++;
                } else {
                    progTable.put(c.i.s, c.accept(this));
                }
            }
        }

        // then calculate class closure, looking for cyclic inheritance
        // if we have discovered no errors so far, we can do inheritance
        if (errors == 0) {
            staticTable = progTable;
            classClosure(progTable);

            //and a count of all the method declarations
            for (String c : progTable.keySet()) {
                for (String m : ((HashMap<String, HashMap>) progTable.get(c)).keySet()) {
                    if (!m.startsWith("$")) {
                        method = (HashMap) progTable.get(c).get(m);
                        numMethods = countMethods(method);
                        method.get("$info").put("$numdecls", numMethods);
                    }
                }
            }
            // and print out our table
            if (verbose) {
                printTable();
                debugOut.flush();
            }
        }

        return progTable;
    }

    // Subcomponents of MainClass:  Identifier i1, i2; Statement s;
    public HashMap visit (final MainClass n) {
        HashMap<String, HashMap> mainTable = new HashMap(),
                                 mainTemp = new HashMap(),
                                 mainEnd  = new HashMap();
        HashMap<String, Object>  methodInfo = new HashMap(),
                                 mainInfo = new HashMap(),
                                 mainClassInfo = new HashMap();


        if (n.body != null) {
            n.body.accept(this);
            methodInfo.put("$declnum", "001");
            methodInfo.put("$type", "void");
            methodInfo.put("$numargs", 1);
            methodInfo.put("$numlocs", 0);

            mainEnd.put("$info", methodInfo);
            mainTemp.put("String", new HashMap<String, HashMap>());
            mainTemp.get("String").put("$end", mainEnd);

            mainInfo.put("$numdecls", 1);
            mainTemp.put("$info", mainInfo);

            mainTable.put("main", mainTemp);

            mainClassInfo.put("$numfields", 0);
            mainTable.put("$info", mainClassInfo);
            mainTable.put("$fields", new HashMap());

        } else {
            System.err.printf("filename=%s:000:000 -- Null Main class body.\n", fileName);
            errors++;
        }
        return mainTable;
    }

    // Subcomponents of SimpleClassDecl: Identifier i; List<FieldDecl> vl; List<MethodDecl> ml;
    public HashMap visit (final SimpleClassDecl n) {
        HashMap<String, HashMap> classTable = new HashMap();
        HashMap<String, HashMap> fieldsTable = new HashMap();
        HashMap<String, Object> infoTable = new HashMap();
        HashMap<String, HashMap> methodInfo = null, compare = null, iter = null;
        ArrayList<String> keys = null;
        String key = null;
        int numFields = 0;

        n.i.accept (this);

        for (FieldDecl v: n.fields) {
            if (!fieldsTable.containsKey(v.i.s)){
                fieldsTable.put(v.i.s, v.accept(this));
                fieldsTable.get(v.i.s).put("$loc", ++numFields);
            } else {
                System.err.printf("filename=%s:%03d:%03d -- Multiple definitions for field %s in class %s exist.\n",
                        fileName, v.i.lineNumber, v.i.columnNumber, v.i.s, n.i.s);
                errors++;
            }
        }
        for (MethodDecl m: n.methods) {
            methodInfo = m.accept(this);

            // if we already have a declaration of the method, we need to handle that
            if(classTable.containsKey(m.i.s)){
                compare = classTable.get(m.i.s);
                iter = methodInfo;

                // while we still have variable types to compare
                while (iter != null && compare != null){

                    // get the first (and only) key of our method declaration
                    keys = new ArrayList();
                    keys.addAll(iter.keySet());
                    key = keys.get(0);

                    if (compare.containsKey(key)){
                        // are there conflicting declarations?
                        if (key.equals("$end")){
                            System.err.printf("filename=%s:%03d:%03d -- Conflicting declarations for method %s" +
                                    " in class %s.\n", fileName, m.i.lineNumber, m.i.columnNumber, m.i.s, n.i.s);
                            errors++;
                            break;
                        // if not, we can move on
                        } else {
                            iter = iter.get(key);
                            compare = compare.get(key);
                        }
                        // if compare does not contain key, then we can just add it
                    } else {
                        compare.put(key, iter.get(key));
                        iter = null;
                    }
                }

            // if we don't already have a declaration, then adding the function is trivial
            } else {
                classTable.put(m.i.s, methodInfo);
            }
        }

        infoTable.put("$numfields", numFields);
        classTable.put("$info", infoTable);
        classTable.put("$fields", fieldsTable);
        return classTable;
    }

    // Subcomponents of ExtendingClassDecl: Identifier i, j; List<FieldDecl> vl; List<MethodDecl> ml;
    public HashMap visit (final ExtendingClassDecl n) {
        HashMap<String, HashMap> classTable = new HashMap();
        HashMap<String, HashMap> fieldsTable = new HashMap();
        HashMap<String, Object> infoTable = new HashMap();
        HashMap<String, HashMap> methodInfo = null, compare = null, iter = null;
        ArrayList<String> keys = null;
        String key = null;
        int numFields = 0;

        n.i.accept (this);
        n.j.accept (this);

        infoTable.put("$inherit", n.j.s);

        for (FieldDecl v: n.fields) {
            if (!fieldsTable.containsKey(v.i.s)){
                fieldsTable.put(v.i.s, v.accept(this));
                fieldsTable.get(v.i.s).put("$loc", ++numFields);
            } else {
                System.err.printf("filename=%s:%03d:%03d -- Multiple definitions for field %s in class %s exist.\n",
                        fileName, v.i.lineNumber, v.i.columnNumber, v.i.s, n.i.s);
                errors++;
            }
        }
        for (MethodDecl m: n.methods) {
            methodInfo = m.accept(this);
            // if we already have a declaration of the method, we need to handle that
            if(classTable.containsKey(m.i.s)){
                compare = classTable.get(m.i.s);
                iter = methodInfo;

                // while we still have variable types to compare
                while (iter != null && compare != null){

                    // get the first (and only) key of our method declaration
                    keys = new ArrayList();
                    keys.addAll(iter.keySet());
                    key = keys.get(0);

                    if (compare.containsKey(key)){
                        // are there conflicting declarations?
                        if (key.equals("$end")){
                            System.err.printf("filename=%s:%03d:%03d -- Conflicting declarations for method %s" +
                                    " in class %s.\n", fileName, m.i.lineNumber, m.i.columnNumber, m.i.s, n.i.s);
                            errors++;
                            // if not, we can move on
                        } else {
                            iter = iter.get(key);
                            compare = compare.get(key);
                        }
                        // if compare does not contain key, then we can just add it
                    } else {
                        compare.put(key, iter.get(key));
                        iter = null;
                    }
                }
            // if we don't already have a declaration, then adding the function is trivial
            } else {
                classTable.put(m.i.s, methodInfo);
            }
        }

        infoTable.put("$numfields", numFields);
        classTable.put("$info", infoTable);
        classTable.put("$fields", fieldsTable);

        return classTable;
    }

    // Subcomponents of MethodDecl:
    // Type t; Identifier i; List<FormalDecl> fl; List<LocalDecl> locals; List<Statement>t sl; Expression e;
    public HashMap visit (final MethodDecl n) {
        HashMap<String, HashMap> argsList = new HashMap(); // hashmap which hold our args in a tree
        HashMap<String, HashMap> varsTable = new HashMap(); // holds our variables
        HashMap<String, Object> infoTable = new HashMap(); // holds the info for the method
        HashMap<String, Object> varInfo = null; // holds the info for each variable
        HashMap<String, HashMap> nextFormal = null; // temp value to be able to move our args list
        FormalDecl firstArg = null;
        String varType = null;
        ListIterator<FormalDecl> fList = null;
        FormalDecl formal = null;
        HashMap<String, HashMap> absent = null;
        int args = 0, locals = 0;

        argsList.put("$end", varsTable);
        // work our way backwards to assemble the args list
        if (n.formals != null){
            // start at the end of the list
            fList = n.formals.listIterator(n.formals.size());
            while (fList.hasPrevious()) {
                // scan in our formal
                formal = fList.previous();
                // accept the declaration and get the info
                varInfo = formal.accept(this);
                // overwrite the info
                varInfo.replace("$argloc", ++args);
                // check for multiple definitions, then add if clear
                if (!varsTable.containsKey(formal.i.s)) {
                    varsTable.put(formal.i.s, varInfo);
                    // add the type to our args list
                    nextFormal = new HashMap();
                    nextFormal.put(formal.t.toString(), argsList);
                    argsList = nextFormal;
                } else {
                    System.err.printf("filename=%s:%03d:%03d -- Multiple definitions for var %s in method %s exist.\n",
                            fileName, formal.i.lineNumber, formal.i.columnNumber, formal.i.s, n.i.s);
                    errors++;
                }
            }
        }

        for (final LocalDecl v: n.locals) {
            if (v.i == null){
                System.err.printf("filename=%s:%03d:%03d -- Null Local declaration identifier.\n",
                        fileName, v.i.lineNumber, v.i.columnNumber, v.i.s);
                errors++;
            } else {
                absent = varsTable.putIfAbsent(v.i.s, v.accept(this));
                if (absent == null) {
                    varsTable.get(v.i.s).put("$locloc", ++locals);
                } else {
                    System.err.printf("filename=%s:%03d:%03d -- Multiple definitions for var %s in %s exist.\n",
                            fileName, v.i.lineNumber, v.i.columnNumber, v.i.s, n.i.s);
                    errors++;
                }
            }
        }
        for (final Statement s: n.sl) {
            s.accept(this);
        }
        infoTable.put("$numargs", args);
        infoTable.put("$numlocs", locals);
        infoTable.put("$type", n.t.toString());
        varsTable.put("$info", infoTable);

        argsList.put("$info", new HashMap());

        // Return statement
        n.e.accept (this);
        return argsList;
    }

    public HashMap visit (FieldDecl n) {
        HashMap<String, Object> infoHash = new HashMap();

        infoHash.put("$type", n.t.toString());
        infoHash.put("$fieldloc", -1);
        return infoHash;
    }
    
    public HashMap visit (LocalDecl n) {
        HashMap<String, Object> infoHash = new HashMap();
        infoHash.put("$type", n.t.toString());
        infoHash.put("$locloc", -1);
        return infoHash;
    }
    
    // Subcomponents of FormalDecl:  Type t; Identifier i;
    public HashMap visit (FormalDecl n) {
        HashMap<String, Object> infoHash = new HashMap();
        infoHash.put("$type", n.t.toString());
        infoHash.put("$argloc", -1);
        return infoHash;
    }

    /*
        Since only declarations can give us new information
        And declarations can only happen in certain spots we've already been to
        We can stop here and just return null on the rest
     */
    public HashMap visit (IntArrayType n) { return null; }

    public HashMap visit (BooleanType n) { return null; }

    public HashMap visit (IntegerType n) { return null; }

    public HashMap visit (VoidType n) { return null; }

    public HashMap visit (IdentifierType n) { return null; }

    // Subcomponents of Block statement:  StatementList sl;
    public HashMap visit (final Block n) {
        for (Statement s: n.sl){
            s.accept(this);
        }
        return null;
    }

    // Subcomponents of If statement: Expression e; Statement s1,s2;
    public HashMap visit (final If n) {
        if (n.e == null){
            System.err.printf("filename=%s:%03d:%03d -- Null if condition\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e.accept(this);
        }

        if (n.s1 == null){
            System.err.printf("filename=%s:%03d:%03d -- Null if-then statement\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.s1.accept(this);
        }

        // we may or may not have an else statement
        if (n.s2 == null){
            System.err.printf("filename=%s:%03d:%03d -- Null if-else statement\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.s2.accept(this);
        }

        return null;
    }

    // Subcomponents of While statement: Expression e, Statement s
    public HashMap visit (final While n) {
        if (n.e == null){
            System.err.printf("filename=%s:%03d:%03d -- Null While condition\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e.accept(this);
        }

        if (n.s == null){
            System.err.printf("filename=%s:%03d:%03d -- Null While statement\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.s.accept(this);
        }

        return null;
    }

    // Subcomponents of Print statement:  Expression e;
    public HashMap visit (final Print n) {
        if (n.e == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Print expression\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e.accept(this);
        }
        return null;
    }

    // subcomponents of Assignment statement:  Identifier i; Expression e;
    public HashMap visit (final Assign n) {
        if (n.i == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Assign identifier\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.i.accept(this);
        }

        if (n.e == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Assign expression\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e.accept(this);
        }

        return null;
    }

    // Subcomponents of ArrayAssign:  Identifier nameOfArray; Expression indexInArray, Expression e;
    public HashMap visit (final ArrayAssign n) {
        if (n.nameOfArray == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Array Assign identifier\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.nameOfArray.accept(this);
        }

        if (n.indexInArray == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Array Assign index\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.indexInArray.accept(this);
        }

        if (n.e == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Array Assign expression\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e.accept(this);
        }

        return null;
    }

    // Expression e1,e2;
    public HashMap visit (final And n) {
        if (n.e1 == null){
            System.err.printf("filename=%s:%03d:%03d -- Null And condition 1 expression\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e1.accept(this);
        }
        if (n.e2 == null){
            System.err.printf("filename=%s:%03d:%03d -- Null And condition 2 expression\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e2.accept(this);
        }
        return null;
    }

    // Expression e1,e2;
    public HashMap visit (final LessThan n) {
        if (n.e1 == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Less than expression 1\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e1.accept(this);
        }
        if (n.e2 == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Less than expression 2\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e2.accept(this);
        }
        return null;
    }

    // Expression e1,e2;
    public HashMap visit (final Plus n) {
        if (n.e1 == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Plus expression 1\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e1.accept(this);
        }
        if (n.e2 == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Plus expression 2\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e2.accept(this);
        }
        return null;
    }

    // Expression e1,e2;
    public HashMap visit (final Minus n) {
        if (n.e1 == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Minus expression 1\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e1.accept(this);
        }
        if (n.e2 == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Minus expression 2\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e2.accept(this);
        }
        return null;
    }

    // Expression e1,e2;
    public HashMap visit (final Times n) {
        if (n.e1 == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Times expression 1\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e1.accept(this);
        }
        if (n.e2 == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Times expression 2\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e2.accept(this);
        }
        return null;
    }

    // Expression expressionForArray, indexInArray;
    public HashMap visit (final ArrayLookup n) {
        if (n.expressionForArray == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Array Lookup expression 1\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.expressionForArray.accept(this);
        }
        if (n.indexInArray == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Array Lookup index expression\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.indexInArray.accept(this);
        }
        return null;
    }

    // Expression expressionForArray;
    public HashMap visit (final ArrayLength n) {
        if (n.expressionForArray == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Array Length expression\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.expressionForArray.accept(this);
        }
        return null;
    }

    public HashMap visit (Call n) {
        if (n.e == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Call expression origin\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e.accept(this);
        }

        if (n.i == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Call identifier\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.i.accept(this);
        }
        if (n.el != null){
            for (Expression arg: n.el){
                arg.accept(this);
            }
        }

        return null;
    }

    public HashMap visit (True n) { return null; }

    public HashMap visit (False n) { return null; }

    public HashMap visit (IntegerLiteral n) { return null; }

    // Subcompoents of identifier statement: String:s
    public HashMap visit (IdentifierExp n) {
        if (n.s == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Identifier string\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        }
        return null;
    }

    public HashMap visit (This n) { return null; }

    // Expression e;
    public HashMap visit (NewArray n) {
        if (n.e == null){
            System.err.printf("filename=%s:%03d:%03d -- Null New array size expression\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e.accept(this);
        }
        return null;
    }

    // Identifier i;
    public HashMap visit (NewObject n) {
        if (n.i == null){
            System.err.printf("filename=%s:%03d:%03d -- Null New object identifier\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.i.accept(this);
        }
        return null;
    }

    // Expression e;
    public HashMap visit (Not n) {
        if (n.e == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Not expression\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        } else {
            n.e.accept(this);
        }
        return null;
    }

    // String s;
    public HashMap visit (Identifier n) {
        if (n.s == null){
            System.err.printf("filename=%s:%03d:%03d -- Null Identifier string\n",
                    fileName, n.lineNumber, n.columnNumber);
            errors++;
        }
        return null;
    }
}
