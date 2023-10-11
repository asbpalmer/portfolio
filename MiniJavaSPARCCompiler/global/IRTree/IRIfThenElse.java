package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRIfThenElse extends LazyIRTree {

    LazyIRTree c, eThen, eElse;
    public LABEL thenL= LABEL.generateLABEL("if", "then");
    public LABEL elseL= LABEL.generateLABEL("if", "else");
    public LABEL ifEnd= LABEL.generateLABEL("if", "end");
    public IRIfThenElse(LazyIRTree cond, LazyIRTree thenStm, LazyIRTree elseStm) {
        c = cond; eThen = thenStm; eElse = elseStm;
    }
    
    public Exp asExp(){
        throw new UnsupportedOperationException("If statement cannot be used as an expression.\n");
    }
    public Stm asStm(){
        return new SEQ( c.asCond(thenL, elseL),
                new SEQ( thenL,
                 new SEQ( eThen.asStm(),
                  new SEQ( new JUMP(ifEnd.label),
                   new SEQ( elseL,
                    new SEQ( eElse.asStm(),
                     new SEQ( new JUMP(ifEnd.label) , ifEnd)))))));
    }
    public Stm asCond(LABEL tt, LABEL ff){
        throw new UnsupportedOperationException("If statement cannot be used as a condition.\n");
    }
}
