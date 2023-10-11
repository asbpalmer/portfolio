package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRThis extends LazyIRTree {
    
    public IRThis() { }
    
    public Exp asExp(){
        return new TEMP("%i0");
    }
    public Stm asStm(){
        throw new UnsupportedOperationException("This cannot be used as a statement.\n");
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("This cannot be used as a condition.\n");
    }
}
