package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRNot extends LazyIRTree {
    LazyIRTree e;
    public IRNot(LazyIRTree exp) { e=exp; }
    
    public Exp asExp(){
        return new BINOP(BINOP.XOR, e.asExp(), new CONST(1));
    }
    public Stm asStm(){
        throw new UnsupportedOperationException("Not expression cannot be used as statement.\n");
    }
    public Stm asCond(LABEL t, LABEL f){
        return new CJUMP(CJUMP.EQ, e.asExp(), CONST.TRUE, f.label, t.label);
    }
}
