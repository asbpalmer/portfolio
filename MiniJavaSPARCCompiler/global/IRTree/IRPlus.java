package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRPlus extends LazyIRTree {
    LazyIRTree el, er;
    public IRPlus(LazyIRTree exp1, LazyIRTree exp2) { el=exp1;er=exp2; }

    public Exp asExp(){
        return new BINOP(BINOP.PLUS, el.asExp(), er.asExp());
    }
    public Stm asStm(){
        throw new UnsupportedOperationException("Plus expression cannot be used as statement.\n");
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("Plus expression cannot be used as conditional.\n");
    }
}
