package global.IRTree;
import tree.*;
import java.lang.UnsupportedOperationException;

public class IRMethod extends LazyIRTree {
    NameOfLabel prologueEnd;
    NameOfLabel epilogueBegin;
    LazyIRTree b;
    public IRMethod(String className, String methodName, String methodNum, LazyIRTree statements) {
        prologueEnd = new NameOfLabel(className, methodName, methodNum, "prologueEnd");
        epilogueBegin = new NameOfLabel(className, methodName, methodNum, "epilogueBegin");
        b = statements;
    }

    public IRMethod(String className, String methodName, String methodNum, LazyIRTree statements, LazyIRTree ret) {
        prologueEnd = new NameOfLabel(className, methodName.concat(methodNum), "prologueEnd");
        epilogueBegin = new NameOfLabel(className, methodName.concat(methodNum), "epilogueBegin");
        b = new IRStatementBlock(statements, ret);
    }

    public Exp asExp(){
        throw new UnsupportedOperationException("Method Declaration cannot be used as an expression.\n");
    }
    public Stm asStm(){
        return new SEQ(new LABEL(prologueEnd), new SEQ(b.asStm(), new JUMP(epilogueBegin)));
    }
    public Stm asCond(LABEL t, LABEL f){
        throw new UnsupportedOperationException("Method Declaration be used as a condition.\n");
    }
}
