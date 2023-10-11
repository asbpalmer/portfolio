package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRArrayAssign extends LazyIRTree {
    LazyIRTree arr, i, v;
    public IRArrayAssign(LazyIRTree loc, LazyIRTree ind, LazyIRTree val)
    { arr=loc; i=ind; v=val; }
    
    public Exp asExp(){
        throw new UnsupportedOperationException("Array assignment cannot be used as an expression.\n");
    }
    public Stm asStm(){
        return new MOVE(new MEM (new BINOP(
                BINOP.PLUS, arr.asExp(), new BINOP(BINOP.MUL, new BINOP(BINOP.PLUS, i.asExp(), new CONST(1)), new CONST(4))
            )), v.asExp());
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("Array assignment cannot be used as a condition.\n");
    }
}
