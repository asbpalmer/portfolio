package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRNewArray extends LazyIRTree {
    LazyIRTree s;
    public IRNewArray(LazyIRTree size) { s=size; }

    // include an index at the beginning to hold the array's size
    public Exp asExp(){
        TEMP store = new TEMP("t");
        TEMP size = new TEMP("s");
        return
            new RET(
                new SEQ(
                    new MOVE(size, s.asExp()),
                    new SEQ(
                        new MOVE(
                                // move the allocated object into our store
                                store,
                                new CALL(new NameOfLabel("alloc_obj"), new BINOP(BINOP.PLUS, size, new CONST(1)))
                        ), new MOVE( new MEM(store), new MEM(size)))),
            store );
    }

    public Stm asStm(){
        throw new UnsupportedOperationException("Array allocation cannot be used as an expression.\n");
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("Array allocation cannot be used as a conditional.\n");
    }
}
