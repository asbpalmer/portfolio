package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRWhile extends LazyIRTree {
    LazyIRTree c, s;
    public LABEL whileB = LABEL.generateLABEL("whileStart");
    public LABEL whileI = LABEL.generateLABEL("whileIn");
    public LABEL whileE = LABEL.generateLABEL("whileEnd");
    public IRWhile(LazyIRTree cond, LazyIRTree stm) { c=cond; s=stm; }
    
    public Exp asExp(){
        throw new UnsupportedOperationException("While statement cannot be used as a condition.\n");
    }
    public Stm asStm(){
        return new SEQ( whileB,
                new SEQ ( c.asCond(whileI, whileE),
                 new SEQ (whileI,
                  new SEQ (s.asStm(),
                   new SEQ (new JUMP(whileB.label), whileE)))));
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("While statement cannot be used as a condition.\n");
    }
}
