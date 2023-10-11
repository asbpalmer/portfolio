package global.IRTree;

import tree.*;

public class IRReturn extends LazyIRTree {
    LazyIRTree val;
    public IRReturn(LazyIRTree value){ val = value; }

    public Exp asExp(){
        throw new UnsupportedOperationException("Return statement cannot be used as an expression.\n");
    }
    public Stm asStm(){
        return new MOVE(new TEMP("%i0"), val.asExp());
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("Return cannot be used as condition.\n");
    }
}
