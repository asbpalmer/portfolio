package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRTrue extends LazyIRTree {
    public IRTrue() { }
    
    public Exp asExp(){
        return CONST.TRUE;
    }

    public Stm asStm(){
        throw new UnsupportedOperationException("Constant true cannot be used as a statement.\n");
    }

    public Stm asCond(LABEL t, LABEL f){
        return new JUMP(t.label);
    }
}
