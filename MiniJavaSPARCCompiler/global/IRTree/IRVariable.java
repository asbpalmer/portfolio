package global.IRTree;

import tree.*;

public class IRVariable extends LazyIRTree {
    Exp loc;
    public IRVariable(Exp location) { loc = location; }

    public Exp asExp(){
        if (loc instanceof TEMP || loc instanceof MEM){
            return loc;
        }
        return new MEM(loc);
    }

    public Stm asStm(){
        throw new UnsupportedOperationException("Variable cannot be used as a statement.\n");
    }

    public Stm asCond(LABEL t, LABEL f){
        return new CJUMP(CJUMP.EQ, this.asExp(), CONST.TRUE, t.label, f.label);
    }
}

