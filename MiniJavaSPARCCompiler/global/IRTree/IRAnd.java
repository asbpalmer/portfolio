package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRAnd extends LazyIRTree {
    LazyIRTree el, er;
    public IRAnd(LazyIRTree exp1, LazyIRTree exp2) { el = exp1; er = exp2; }

    public Exp asExp(){
        return new BINOP(BINOP.AND, el.asExp(), er.asExp());
    }
    public Stm asStm(){
        throw new UnsupportedOperationException("And expression cannot be used as statement");
    }
    public Stm asCond(LABEL t, LABEL f){
        NameOfLabel nextLabel = NameOfLabel.generateLabel("andPart");
        return new SEQ( el.asCond(new LABEL(nextLabel), f),
                new SEQ( new LABEL(nextLabel),
                 new SEQ( er.asCond(t, f), t)));
    }
}
