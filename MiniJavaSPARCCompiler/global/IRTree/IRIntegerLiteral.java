package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRIntegerLiteral extends LazyIRTree {
    int i;
    public IRIntegerLiteral(int val) { i = val; }
    public IRIntegerLiteral(String n) { i = Integer.parseInt(n); }
    
    public Exp asExp(){
        return new CONST(i);
    }
    public Stm asStm(){
        throw new UnsupportedOperationException("Integer literal cannot be used as statement.\n");
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("Integer literal cannot be used as condition.\n");
    }
}
