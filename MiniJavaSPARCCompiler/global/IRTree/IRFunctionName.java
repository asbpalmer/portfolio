package global.IRTree;

import tree.*;

public class IRFunctionName extends LazyIRTree {
    String mName, cName;
    public IRFunctionName(String methodName, String className) { mName = methodName; cName = className; }

    public Exp asExp(){
        return new NAME(new NameOfLabel(cName, mName));
    }
    public Stm asStm(){
        throw new UnsupportedOperationException("Function name be used as a statement");
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("Function name be used as a condition");
    }
}
