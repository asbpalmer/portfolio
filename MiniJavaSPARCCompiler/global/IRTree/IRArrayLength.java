package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRArrayLength extends LazyIRTree {
    LazyIRTree arr;
    public IRArrayLength(LazyIRTree array) { arr=array; }

    public Exp asExp(){
        return new MEM(arr.asExp());
    }
    public Stm asStm(){
        throw new UnsupportedOperationException("Array length cannot be used as a statement.\n");
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("Array length cannot be used as an condition.\n");
    }
}
