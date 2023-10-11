package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRFalse extends LazyIRTree {
    
    public IRFalse() { }
    
    public Exp asExp(){
        return CONST.FALSE;
    }
    public Stm asStm(){
        throw new UnsupportedOperationException("Constant false cannot be used as a statement.\n");
    }
    public Stm asCond(LABEL t, LABEL f){
        return new JUMP(f.label);
    }
}
