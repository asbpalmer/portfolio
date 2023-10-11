package global.IRTree;

import java.util.ArrayList;
import java.util.HashMap;
import tree.*;
import java.lang.UnsupportedOperationException;
import java.util.List;

public class IRCall extends LazyIRTree {
    LazyIRTree o; List<Exp> al; NAME i;

    // handling a call will require a bit of extra work
    public IRCall(LazyIRTree origin, NAME identifier, List<LazyIRTree> args){
        o = origin; i = identifier;
        al = new ArrayList();
        al.add(o.asExp());
        for (LazyIRTree arg: args){
            al.add(arg.asExp());
        }
    }
    
    public Exp asExp(){
        return new CALL(i, al);
    }
    public Stm asStm(){
        return new EVAL(this.asExp());
    }
    public Stm asCond(LABEL tt, LABEL ff){
        return new CJUMP(CJUMP.EQ, this.asExp(), CONST.TRUE, tt.label, ff.label);
    }
}
