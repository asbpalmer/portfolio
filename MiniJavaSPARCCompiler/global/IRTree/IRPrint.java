package global.IRTree;

import tree.*;

public class IRPrint extends LazyIRTree {
    LazyIRTree v;
    public IRPrint(LazyIRTree val) { v=val; }

    public Exp asExp(){
        throw new UnsupportedOperationException("Print statement cannot be used as an expression.\n");
    }
    public Stm asStm(){
        return new EVAL(new CALL(new NameOfLabel("print_int"), v.asExp()));
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("Print assignment cannot be used as an conditional.\n");
    }
}
