package global.IRTree;

import tree.*;
public class IRLoc extends LazyIRTree {
    Exp loc;
    public IRLoc(Exp location) { loc=location; }

    public Exp asExp(){
        return loc;
    }
    public Stm asStm(){
        throw new UnsupportedOperationException("Variable location cannot be used as statement.\n");
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("Variable location cannot be used as condition.\n");
    }

}
