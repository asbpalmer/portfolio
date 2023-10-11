package global.IRTree;

import tree.*;

public class IRStatementBlock extends LazyIRTree {
    LazyIRTree s1, s2;
    public IRStatementBlock(LazyIRTree statement1, LazyIRTree statement2){
        s1 = statement1; s2 = statement2;
    }

    public Exp asExp(){
        throw new UnsupportedOperationException("Return statement cannot be used as an expression.\n");
    }
    public Stm asStm(){
        return new SEQ(s1.asStm(), s2.asStm());
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("Return cannot be used as condition.\n");
    }
}