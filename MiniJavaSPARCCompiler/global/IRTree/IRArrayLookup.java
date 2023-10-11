package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRArrayLookup extends LazyIRTree {
    LazyIRTree arr, i;
    public IRArrayLookup(LazyIRTree array, LazyIRTree ind) { arr=array; i=ind; }

    // arrays have their length at the zeroth index, so we need to move one int further to get the first element
    public Exp asExp(){
        return new MEM(new BINOP(BINOP.PLUS, new MEM(arr.asExp()),
                new BINOP( BINOP.PLUS, new BINOP(BINOP.MUL, i.asExp(), new CONST(4)), new CONST(4))));
    }
    public Stm asStm(){
        throw new UnsupportedOperationException("Array Lookup cannot be used as a statement");
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("Array Lookup cannot be used as a conditional");
    }
}
