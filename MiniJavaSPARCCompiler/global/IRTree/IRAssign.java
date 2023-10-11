package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRAssign extends LazyIRTree {
    LazyIRTree var, v;
    public IRAssign(LazyIRTree variable, LazyIRTree value) { var=variable; v=value; }

    public Exp asExp(){
        throw new UnsupportedOperationException("Variable Assignment cannot be used as an expression.\n");
    }
    public Stm asStm(){
        return new MOVE(var.asExp(), v.asExp());
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("Variable Assignment cannot be used as a conditional.\n");
    }
}
