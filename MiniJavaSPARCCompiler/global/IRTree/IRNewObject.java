package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRNewObject extends LazyIRTree {
    LazyIRTree s;
    public IRNewObject(LazyIRTree size) { s=size; }
    public IRNewObject(int size) { s = new IRIntegerLiteral(size);}
    
    public Exp asExp(){
        return new CALL(new NameOfLabel("alloc_obj"), s.asExp());
    }
    public Stm asStm(){
        throw new UnsupportedOperationException("Object instantiation cannot be used as a statement.\n");
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("Object instantiation cannot be used as a condition.\n");
    }
}
