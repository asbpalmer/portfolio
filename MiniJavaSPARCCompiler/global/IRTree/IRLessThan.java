package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRLessThan extends LazyIRTree {
    LazyIRTree el, er;
    public IRLessThan(LazyIRTree exp1, LazyIRTree exp2) { el=exp1;er=exp2; }

    public Exp asExp(){
        NameOfLabel l1 = NameOfLabel.generateLabel("LT","True"),
                    l2 = NameOfLabel.generateLabel("LT","False"),
                    l3 = NameOfLabel.generateLabel("LT","End");
        return new RET(
                   new SEQ(
                       new CJUMP(CJUMP.LT, el.asExp(), er.asExp(), l1, l2),
                       new SEQ(
                           new LABEL(l1),
                           new SEQ(
                               new MOVE(new TEMP("t"), CONST.TRUE),
                               new SEQ(
                                   new JUMP(l3),
                                   new SEQ(
                                       new LABEL(l2),
                                       new SEQ(
                                           new MOVE(new TEMP("t"), CONST.FALSE),
                                           new LABEL(l3))
                                   ))))), new TEMP("t"));
    }
    public Stm asStm(){
        throw new UnsupportedOperationException("Less than expression cannot be used as statement.\n");
    }
    public Stm asCond(LABEL t, LABEL f){
        return new CJUMP(CJUMP.LT, el.asExp(), er.asExp(), t.label, f.label);
    }
}
