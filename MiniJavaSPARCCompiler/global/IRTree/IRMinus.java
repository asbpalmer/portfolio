package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRMinus extends LazyIRTree {

    LazyIRTree el, er;
    public IRMinus(LazyIRTree exp1, LazyIRTree exp2) { el=exp1;er=exp2; }

    public Exp asExp(){
        return new BINOP(BINOP.MINUS, el.asExp(), er.asExp());
    }
    public Stm asStm(){
        throw new UnsupportedOperationException("Minus expression cannot be used as statement.\n");
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("Minus expression cannot be used as conditional.\n");
    }
}
