package global.IRTree;

import tree.*;
import java.lang.UnsupportedOperationException;

public class IRNull extends LazyIRTree {
    public IRNull() { }

    public Exp asExp(){
        return new CONST(0);
    }

    public Stm asStm(){
        return Stm.NOOP;
    }

    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("NOOP cannot be used as a conditional.\n");
    }
}
