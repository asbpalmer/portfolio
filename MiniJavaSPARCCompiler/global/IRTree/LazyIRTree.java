package global.IRTree;
import tree.*;

public abstract class LazyIRTree {
    public abstract Exp asExp();
    public abstract Stm asStm();
    public abstract Stm asCond(LABEL t, LABEL f);
}
