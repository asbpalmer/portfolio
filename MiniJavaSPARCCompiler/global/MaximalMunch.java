package global;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import assem.*;
import tree.*;

public class MaximalMunch {

    public HashMap<NameOfTemp, String> tempMap = new HashMap(); // temp map to use with formatting
    private String memStr, memComment;                          // where are we holding a mem?
    public ArrayList<NameOfTemp> currentTemps = new ArrayList();       // what are the current, relevant temps
    // NOTE: once we use a temp as a source, we can get rid of it to minimize the space we need to manage
    public ArrayList<String> allocatedRegs = new ArrayList();          // what registers are we using to hold relevant temps
    public int maxTemps = 0;                                           // what is the maximum number of temps we've had be relevant at once?

    public MaximalMunch(){
        String incoming, outgoing, locals, globals;
        for (int i = 0; i < 8; i++){
            incoming = String.format("%%i%d", i);
            outgoing = String.format("%%o%d", i);
            locals = String.format("%%l%d", i);
            globals = String.format("%%g%d", i);
            tempMap.put(new NameOfTemp(incoming), incoming);
            tempMap.put(new NameOfTemp(outgoing), outgoing);
            tempMap.put(new NameOfTemp(locals), locals);
            tempMap.put(new NameOfTemp(globals), globals);
        }
        tempMap.put(new NameOfTemp("%fp"), "%fp");
        tempMap.put(new NameOfTemp("%sp"), "%sp");
    }

    public List<String> formatStms(List<Stm> statements){
        return format(maximalMunch(statements));
    }

    public List<String> formatStms(Stm s){
        return format(maximalMunch(s));
    }

    public List<String> format(List<Instruction> instructions){
        ArrayList<String> lines = new ArrayList();
        for (Instruction i: instructions){
            lines.add(i.format(tempMap));
        }
        return lines;
    }

    public List<Instruction> maximalMunch(List<Stm> statements){
        ArrayList<Instruction> munched = new ArrayList();

        for (Stm s: statements){
            munched.addAll(maximalMunch(s));
        }
        return munched;
    }

    public List<Instruction> maximalMunch(Stm s){
        ArrayList<Instruction> munched = new ArrayList();
        munchStm(s, munched);

        return munched;
    }

    private int c = 0;
    private NameOfTemp generateTemp(){
        return generateTemp("t");
    }

    private NameOfTemp generateTemp(String tName){
        NameOfTemp t = new NameOfTemp(String.format("%s%03d", tName, ++c));
        putTemp(t);
        return t;
    }

    // where do we add our temp into the list/in memory?
    private void putTemp(NameOfTemp t){
        String loc = null;
        boolean set = false;

        // see if the local registers are allocated
        for (int l = 0; !set &&  l < 8; l++){
            loc = "%l" + l;
            // if the register isn't allocated
            if (!allocatedRegs.contains(loc)){
                tempMap.put(t, loc);
                allocatedRegs.add(loc);
                currentTemps.add(t);
                set = true;
            }
        }
        // if we couldn't find a place for it, try putting it in the outgoing registers
        for (int o = 0; !set && o < 6; o++){
            loc = "%o" + o;
            // if the register isn't allocated
            if (!allocatedRegs.contains(loc)){
                tempMap.put(t, loc);
                allocatedRegs.add(loc);
                currentTemps.add(t);
                set = true;
            }
        }
        // if we couldn't find a place for it then, try putting it in the global registers
        for (int g = 1; !set && g < 8; g++){
            loc = "%g" + g;
            // if the register isn't allocated
            if (!allocatedRegs.contains(loc)){
                tempMap.put(t, loc);
                allocatedRegs.add(loc);
                currentTemps.add(t);
                set = true;
            }
        }

        // If we still haven't found a place for it, well oopsie daisy
        if (!set) {
            // store it in the heap, just above the formals
            // requires more handling than I can put it in at the moment
            loc = String.format("%%fp - 4*(LOCLS+%d)", currentTemps.size() - 8);
            tempMap.put(t, loc);
            allocatedRegs.add(loc);
            currentTemps.add(t);
        }

        // if we need more temp space, make sure to note that
        if (currentTemps.size() > maxTemps ){
            maxTemps = currentTemps.size();
        }
    }

    private void removeTemp(NameOfTemp t){
        allocatedRegs.remove(tempMap.get(t));
        currentTemps.remove(t);
    }

    // statement subtypes
    // Only have to return stuff for RET expressions, which need to know where the last statement stored their output
    private NameOfTemp munchStm(Stm s, List<Instruction> addTo){
        if(s instanceof MOVE){
            return munchMove((MOVE)s, addTo);
        } else if (s instanceof EVAL) {
            return munchEval((EVAL)s, addTo);
        } else if (s instanceof JUMP) {
            return munchJump((JUMP)s, addTo);
        } else if (s instanceof CJUMP) {
            return munchCJump((CJUMP)s, addTo);
        } else if (s instanceof SEQ) {
            return munchSeq((SEQ)s, addTo);
        } else if (s instanceof LABEL) {
            return munchLabel((LABEL)s, addTo);
        } else {
            throw new UnsupportedOperationException(
                    String.format("Found unexpected Statement Type: %s", s.getClass()));
        }
    }

    // handles differently if it is mem
    // add logic later
    // only care about storing most recent temp if there is a temp involved
    private NameOfTemp munchMove(MOVE m, List<Instruction> addTo){
        NameOfTemp to = null, from;
        MoveInstruction mov;

        if (m.src instanceof MEM){
            if (m.dst instanceof MEM){
                from = generateTemp("asgn");

                munchMem((MEM)m.src, addTo);
                addTo.add( new OperationInstruction(
                        String.format("\tld\t%s, `d0", memStr),
                        String.format("%s := MEM%s", from.toString(), memComment),
                        from, null
                ));

                munchMem((MEM)m.dst, addTo);
                addTo.add( new OperationInstruction(
                        String.format("\tst\t`s0, %s", memStr),
                        String.format("MEM%s := %s", memComment, from.toString()),
                        null, from
                ));
                removeTemp(from);

            } else {
                to = munchExp(m.dst, addTo);
                munchMem((MEM)m.src, addTo);
                addTo.add( new OperationInstruction(
                        String.format("\tld\t%s, `d0", memStr),
                        String.format("%s := MEM%s", to.toString(), memComment),
                        to, null
                ));
            }
        } else {
            if (m.dst instanceof MEM){
                from = munchExp(m.src, addTo);

                munchMem((MEM)m.dst, addTo);
                addTo.add( new OperationInstruction(
                        String.format("\tst\t`s0, %s", memStr),
                        String.format("MEM%s := %s", memComment, from.toString()),
                        null, from
                ));
                removeTemp(from);

            } else {
                // get the destination our result ended up in
                from = munchExp(m.src, addTo);
                // get the destination we are moving our thing to
                to = munchExp(m.dst, addTo);

                mov = new MoveInstruction("\tmov\t`s0, `d0",
                        String.format("%s := %s", to.toString(), from.toString()), to, from);
                addTo.add(mov);
                removeTemp(from);
            }
        }

        return null;
    }

    // do something and throw away the result
    private NameOfTemp munchEval(EVAL e, List<Instruction> addTo){
        if (e.exp instanceof CONST){
            addTo.add(new OperationInstruction("\tnop", "delay"));
        } else {
            // have our call prepare itself
            munchExp(e.exp, addTo);
        }
        return null;
    }

    // unconditional jump, JUMP inheritly has the list of targets in j.targets
    private NameOfTemp munchJump(JUMP j, List<Instruction> addTo){
        addTo.add(new OperationInstruction(String.format("\tba\t`j%d", 0), "unconditional GOTO",
                    null, null, j.targets));
        addTo.add(new OperationInstruction("\tnop", "delay for jump"));

        return null;
    }

    private String cJumpToASM(int c){
        switch (c){
            case CJUMP.EQ:
                return "be";
            case CJUMP.NE:
                return "bne";
            case CJUMP.LT:
                return "bl";
            case CJUMP.GT:
                return "bg";
            case CJUMP.LE:
                return "ble";
            case CJUMP.GE:
                return "bge";
            case CJUMP.ULT:
                return "blu";
            case CJUMP.ULE:
                return "bleu";
            case CJUMP.UGT:
                return "bgu";
            case CJUMP.UGE:
                return "bgeu";
            default:
                throw new UnsupportedOperationException(String.format("Could not find CJUMP #%d", c));
        }
    }

    private String cJumpToOper(int c){
        switch (c){
            case CJUMP.EQ:
                return "==";
            case CJUMP.NE:
                return "!=";
            case CJUMP.LT:
                return "<";
            case CJUMP.GT:
                return ">";
            case CJUMP.LE:
                return "<=";
            case CJUMP.GE:
                return ">=";
            case CJUMP.ULT:
                return "u<";
            case CJUMP.ULE:
                return "u<=";
            case CJUMP.UGT:
                return "u>";
            case CJUMP.UGE:
                return "u>=";
            default:
                throw new UnsupportedOperationException(String.format("Could not find CJUMP #%d", c));
        }
    }

    // we need to handle mems, consts, and temps
    private NameOfTemp munchCJump(CJUMP c, List<Instruction> addTo){
        Exp c1 = c.left, c2 = c.right;
        NameOfTemp tl, tr;
        int r = c.relop, right;
        // where we jump to depends on the condition
        String asm = cJumpToASM(r), op = cJumpToOper(r);

        if (c.left instanceof MEM){
            tl = generateTemp("cmp");
            munchMem((MEM)c.left, addTo);
            addTo.add(new OperationInstruction(
                    String.format("\tld\t%s, `d0", memStr),
                    String.format("%s := MEM%s", tl.toString(), memComment),
                    tl, null
            ));
        } else {
            tl = munchExp(c.left, addTo);
        }

        if (c.right instanceof CONST){
            addTo.add( new OperationInstruction(
                    String.format("\tcmp\t`s0, %d", ((CONST)c.right).value),
                    String.format("Test if %s %s %d", tl.toString(), op, ((CONST)c.right).value),
                    null, tl
            ));
            removeTemp(tl);
        } else if (c.right instanceof MEM){
            tr = generateTemp("asgn");
            munchMem((MEM)c.right, addTo);
            addTo.add(new OperationInstruction(
                    String.format("\tld\t%s, `d0", memStr),
                    String.format("%s := MEM%s", tr.toString(), memComment),
                    tr, null
            ));

            addTo.add( new OperationInstruction(
                    String.format("\tcmp\t`s0, `s1"),
                    String.format("Test if %s %s %s", tl.toString(), op, tr.toString()),
                    null, tl, tr
            ));
            removeTemp(tl);
            removeTemp(tr);
        } else {
            tr = munchExp(c.right, addTo);
            addTo.add( new OperationInstruction(
                    String.format("\tcmp\t`s0, `s1"),
                    String.format("Test if %s %s %s", tl.toString(), op, tr.toString()),
                    null, tl, tr
            ));
            removeTemp(tl);
            removeTemp(tr);
        }

        addTo.add(new OperationInstruction(
                String.format("\t%s\t%s", asm, c.iftrue)));
        addTo.add(new OperationInstruction("\tnop", "delay slot"));
        addTo.add(new OperationInstruction(String.format("\tba\t%s", c.iffalse)));
        addTo.add(new OperationInstruction("\tnop", "delay slot"));

        return null;
    }

    // handle the left before the right
    private NameOfTemp munchSeq(SEQ s, List<Instruction> addTo){
        munchStm(s.left, addTo);
        munchStm(s.right, addTo);
        return null;
    }

    private NameOfTemp munchLabel(LABEL l, List<Instruction> addTo){
        addTo.add(new LabelInstruction(l.label));
        return null;
    }


    // expression subtypes
    // need to return the name of the temps they are storing themselves in
    // most of these will also have to handle mems, temps and constants as their own things
    private NameOfTemp munchExp(Exp e, List<Instruction> addTo){
        if(e instanceof BINOP){
            return munchBinOp((BINOP)e, addTo);
        } else if (e instanceof CALL) {
            return munchCall((CALL)e, addTo);
        } else if (e instanceof CONST) {
            return munchConst((CONST)e, addTo);
        } else if (e instanceof RET) {
            return munchRet((RET)e, addTo);
        } else if (e instanceof MEM) {
            return munchMem((MEM)e, addTo);
        } else if (e instanceof NAME) {
            return munchName((NAME)e, addTo);
        } else if (e instanceof TEMP) {
            return munchTemp((TEMP) e, addTo);
        } else {
            throw new UnsupportedOperationException(
                    String.format("Found unexpected Expression Type: %s", e.getClass()));
        }
    }

    private String binOpToASM(int i){
        switch(i) {
            case BINOP.PLUS:
                return "add";
            case BINOP.MINUS:
                return "sub";
            case BINOP.MUL:
                return "smul";
            case BINOP.DIV:
                return "sdiv";
            case BINOP.AND:
                return "and";
            case BINOP.OR:
                return "or";
            case BINOP.LSHIFT:
                return "sll";
            case BINOP.RSHIFT:
                return "srl";
            case BINOP.ARSHIFT:
                return "sra";
            case BINOP.XOR:
                return "xor";
            default:
                throw new UnsupportedOperationException(String.format("Could not find BINOP #%d", i));
        }
    }

    private String binOpToSymbol(int i){
        switch(i) {
            case BINOP.PLUS:
                return "+";
            case BINOP.MINUS:
                return "-";
            case BINOP.MUL:
                return "*";
            case BINOP.DIV:
                return "/";
            case BINOP.AND:
                return "&&";
            case BINOP.OR:
                return "||";
            case BINOP.LSHIFT:
                return "<<";
            case BINOP.RSHIFT:
                return ">>";
            case BINOP.ARSHIFT:
                return "a>";
            case BINOP.XOR:
                return "XOR";
            default:
                throw new UnsupportedOperationException(String.format("Could not find BINOP #%d", i));
        }
    }

    // since binop resolves depth first, left to right
    // we can evaluate the left side using two temps, and create another temp to evaluate the right side
    // for example, x + y * z will be represented as +x*yz
    // tLeft will start out storing x, and tRight will start out holding *yz, which needs to be broken down
    // but we can't without overriding data, so we make a new temp to hold z and make tRight hold y
    // tLeft will hold our results, so we return that
    // but we also have to handle constants and mems as a special case

    // going to have to rework to manage Stan's temps
    private NameOfTemp munchBinOp(BINOP b, List<Instruction> addTo){

        boolean lConst = b.left instanceof CONST, rConst = b.right instanceof CONST;
        CONST l, r;
        NameOfTemp tRight, tLeft, tRet;
        int i = b.binop;
        String oper = binOpToASM(i), symbol = binOpToSymbol(i);

        tRet = generateTemp("binop");
        // determine if left and/or right expressions are constants
        if (lConst && rConst){
            l = (CONST) b.left;
            r = (CONST) b.right;

            // store the left constant into a temp
            tLeft = munchConst(l, addTo);
            // add the result from the operation back into the same place
            addTo.add(new OperationInstruction(
                        String.format("\t%s\t`s%d, %d, `d%d", oper, 0, r.value, 0),
                        String.format("%s := %s %s %d", tRet, tLeft, symbol, r.value),
                        tRet, tLeft)
            );
            removeTemp(tLeft);

        } else if (lConst){
            l = (CONST) b.left;
            // just use the left side so we can return easily
            if (b.right instanceof MEM){
                munchMem((MEM)b.right, addTo);
                addTo.add(new OperationInstruction(
                        String.format("\tld\t%s, `d0", memStr),
                        String.format("%s := MEM%s", tRet.toString(), memComment),
                        tRet, null
                ));

                addTo.add(new OperationInstruction(
                        String.format("\t%s\t`s0, %d, `d0", oper, l.value),
                        String.format("%s := %s %s %d", tRet.toString(), tRet.toString(), symbol, l.value),
                        tRet, tRet
                ));
                // do not remove since we are storing in place

            } else {
                tLeft = munchExp(b.right, addTo);
                addTo.add(new OperationInstruction(
                        String.format("\t%s\t`s0, %d, `d0", oper, l.value),
                        String.format("%s := %s %s %d", tRet.toString(), tLeft.toString(), symbol, l.value),
                        tRet, tLeft)
                );
                removeTemp(tLeft);
            }
        } else if (rConst){
            r = (CONST) b.right;

            if (b.left instanceof MEM){
                munchMem((MEM)b.left, addTo);
                addTo.add(new OperationInstruction(
                        String.format("\tld\t%s, `d0", memStr),
                        String.format("%s := MEM%s", tRet.toString(), memComment),
                        tRet, null
                ));

                addTo.add(new OperationInstruction(
                        String.format("\t%s\t`s0, %d, `d0", oper, r.value),
                        String.format("%s := %s %s %d", tRet.toString(), tRet.toString(), symbol, r.value),
                        tRet, tRet
                ));
                // do not remove since we are storing in place

            } else {
                tLeft = munchExp(b.left, addTo);
                // add the result from the operation back into the same place
                addTo.add(new OperationInstruction(
                        String.format("\t%s\t`s0, %d, `d0", oper, r.value),
                        String.format("%s := %s %s %d", tRet.toString(), tLeft.toString(), symbol, r.value),
                        tRet, tLeft
                ));
                removeTemp(tLeft);
            }
        // all of the constant cases have been handled
        } else {
            if (b.left instanceof MEM){
                munchMem((MEM)b.left, addTo);
                addTo.add(new OperationInstruction(
                        String.format("\tld\t%s, `d0", memStr),
                        String.format("%s := MEM%s", tRet.toString(), memComment),
                        tRet, null
                ));

                if (b.right instanceof MEM){
                    tRight = generateTemp("mem");
                    munchMem((MEM)b.right, addTo);

                    addTo.add(new OperationInstruction(
                            String.format("\tld\t%s, `d0", memStr),
                            String.format("%s := MEM%s", tRight.toString(), memComment),
                            tRight, null
                    ));

                    addTo.add(new OperationInstruction(
                            String.format("\t%s\t`s0, `s1, `d0", oper),
                            String.format("%s := %s %s %s", tRet, tRet, symbol, tRight),
                            tRet, tRet, tRight
                    ));
                    // only remove tRight since we are storing tRet in place
                    removeTemp(tRight);

                } else {
                    tRight = munchExp(b.right, addTo);
                    addTo.add(new OperationInstruction(
                            String.format("\t%s\t`s0, `s1, `d0", oper),
                            String.format("%s := %s %s %s", tRet, tRet, symbol, tRight),
                            tRet, tRet, tRight
                    ));
                    // only remove tRight since we are storing tRet in place
                    removeTemp(tRight);
                }
            // left expression is not memory
            } else {
                tLeft = munchExp(b.left, addTo);
                if (b.right instanceof MEM){
                    tRight = generateTemp("mem");
                    munchMem((MEM)b.right, addTo);

                    addTo.add(new OperationInstruction(
                            String.format("\tld\t%s, `d0", memStr),
                            String.format("%s := MEM%s", tRight.toString(), memComment),
                            tRight, null
                    ));

                    addTo.add(new OperationInstruction(
                            String.format("\t%s\t`s0, `s1, `d0", oper),
                            String.format("%s := %s %s %s", tRet, tLeft, symbol, tRight),
                            tRet, tLeft, tRight
                    ));
                    removeTemp(tLeft);
                    removeTemp(tRight);
                // right expression is not memory
                } else {
                    tRight = munchExp(b.right, addTo);
                    addTo.add(new OperationInstruction(
                            String.format("\t%s\t`s0, `s1, `d0", oper),
                            String.format("%s := %s %s %s", tRet, tLeft, symbol, tRight),
                            tRet, tLeft, tRight
                    ));
                    removeTemp(tLeft);
                    removeTemp(tRight);
                }
            }
        }

        return tRet;
    }

    private NameOfTemp munchCall(CALL c, List<Instruction> addTo){
        NameOfTemp t;
        int argNum = 0;
        String s = ((NAME)c.func).label.toString();
        // in general, move the expressions into their places, call, add a nop, then move o1 into a temp
        // handle special cases of print and alloc
        addTo.add(new Comment(String.format("preparation to call %s", s)));
        if (s.equals("print_int") || s.equals("alloc_object")){
            if (c.args.head instanceof CONST){
                addTo.add(new OperationInstruction(
                        String.format("\tset\t%d, %%o0", ((CONST)c.args.head).value),
                        String.format("%%o0 := %d", ((CONST) c.args.head).value)
                ));
            } else if (c.args.head instanceof MEM){
                munchMem((MEM)c.args.head, addTo);

                addTo.add(new OperationInstruction(
                        String.format("\tld\t%s, %%o0", memStr),
                        String.format("%%o0 := MEM%s", memComment)
                ));
            // not mem, not const
            } else {
                t = munchExp(c.args.head, addTo);

                addTo.add(new MoveInstruction(
                        "\tmov\t`s0, %o0", String.format("%%o0 := %s", t.toString()), null, t
                ));
                removeTemp(t);
            }
        } else {
            // handles all user specified calls with java format
            for (Exp arg: c.args.toList()){
                if (arg instanceof CONST){
                    addTo.add(new OperationInstruction(
                            String.format("\tset\t%d, %%o%d", ((CONST)arg).value, argNum),
                            String.format("%%o%d := %d", argNum, ((CONST) arg).value)
                    ));
                } else if (arg instanceof MEM){
                    munchMem((MEM)arg, addTo);

                    addTo.add(new OperationInstruction(
                            String.format("\tld\t%s, %%o%d", memStr, argNum),
                            String.format("%%o%d := MEM%s", argNum, memComment)
                    ));
                // not mem, not const
                } else {
                    t = munchExp(arg, addTo);

                    addTo.add(new MoveInstruction(
                            String.format("\tmov\t`s0, %%o%d", argNum),
                            String.format("%%o%d := %s", argNum, t.toString()), null, t
                    ));
                    removeTemp(t);
                }
                argNum++;
            }
        }

        // call
        addTo.add(new OperationInstruction(String.format("\tcall\t%s", s)));
        // delay slot
        addTo.add(new OperationInstruction("\tnop", "delay after returning from call"));

        return new NameOfTemp("%o0");
    }

    // we can handle consts in our binop, call, etc.
    // but for some things left operands must be registers, so we add a move instruction into store
    private NameOfTemp munchConst(CONST c, List<Instruction> addTo){
        NameOfTemp store = generateTemp("const");
        addTo.add(new MoveInstruction(
                String.format("\tset\t%d, `d0", c.value),
                String.format("%s := %d", store.toString(), c.value),
                store, null));

        return store;
    }

    // Flattening will get rid of our rets, this should never run
    // Just in case it does, process each part
    private NameOfTemp munchRet(RET e, List<Instruction> addTo){
        NameOfTemp t = null;

        munchStm(e.stm, addTo);
        t = munchExp(e.exp, addTo);

        return t;
    }

    // mems result in [address]
    // this puts the string representing the mem into memStr
    // loading values from memory into registers and storing values from registers into memory should be
    //          handled by the operation that wants to do it
    private NameOfTemp munchMem(MEM m, List<Instruction> addTo){
        BINOP b; Exp el, er;
        int oper; String opRep;
        NameOfTemp tl, tr;
        if (m.exp instanceof BINOP){
            b = (BINOP)m.exp;
            oper = b.binop;
            opRep = binOpToSymbol(oper);
            el = b.left;
            er = b.right;

            // mem can only work with these two
            if (oper == BINOP.PLUS || oper == BINOP.MINUS) {
                if (el instanceof CONST) {
                    if (er instanceof CONST) {
                        memStr = String.format("[%d %s %d]", ((CONST) el).value, opRep, ((CONST) er).value);
                        memComment = memStr;
                    } else if (er instanceof MEM) {
                        tr = munchExp(er, addTo);
                        addTo.add(new MoveInstruction(
                                String.format("\tld\t%s, `d0"),
                                String.format("%s := MEM%s", tr.toString(), memComment),
                                tr, null));

                        memStr = String.format("[%d %s %s]", ((CONST) el).value, opRep, tempMap.get(tr));
                        memComment = String.format("[%d %s %s]", ((CONST) el).value, opRep, tr.toString());
                    } else {
                        tr = munchExp(er, addTo);
                        memStr = String.format("[%d %s %s]", ((CONST) el).value, opRep, tempMap.get(tr));
                        memComment = String.format("[%d %s %s]", ((CONST) el).value, opRep, tr.toString());
                    }
                } else if (el instanceof MEM){
                    // first thing we have to do is load our mem on the left into a register/temp
                    munchMem((MEM)el, addTo);
                    tl = generateTemp();
                    addTo.add(new MoveInstruction(
                            String.format("\tld\t%s, `d0", memStr),
                            String.format("%s := MEM%s", tl.toString(), memComment),
                            tl, null
                    ));

                    if (er instanceof CONST) {
                        memStr = String.format("[%s %s %d]", tempMap.get(tl), opRep, ((CONST) er).value);
                        memComment = String.format("[%s %s %d]", tl.toString(), opRep, ((CONST) er).value);

                    } else if (er instanceof MEM) {
                        tr = munchMem((MEM)er, addTo);
                        addTo.add(new MoveInstruction(
                                String.format("\tld\t%s, `d0", memStr),
                                String.format("%s = MEM%s", tr.toString(), memComment),
                                tr, null));

                        memStr = String.format("[%s %s %s]", tempMap.get(tl), opRep, tempMap.get(tr));
                        memComment = String.format("[%s %s %s]", tl.toString(), opRep, tr.toString());
                    } else {
                        tr = munchExp(er, addTo);
                        memStr = String.format("[%s %s %s]", tempMap.get(tl), opRep, tempMap.get(tr));
                        memComment = String.format("[%s %s %s]", tl.toString(), opRep, tr.toString());
                    }
                // el is not const, temp, or mem
                } else {
                    tl = munchExp(b.left, addTo);
                    if (er instanceof CONST) {
                        memStr = String.format("[%s %s %d]", tempMap.get(tl), opRep, ((CONST) er).value);
                        memComment = String.format("[%s %s %d]", tl.toString(), opRep, ((CONST)er).value);

                    } else if (er instanceof MEM) {
                        tr = munchMem((MEM)er, addTo);
                        addTo.add(new MoveInstruction(
                                String.format("\tld\t%s, `d0", memStr),
                                String.format("%s = MEM%s", tr.toString(), memComment),
                                tr, null));

                        memStr = String.format("[%s %s %s]", tempMap.get(tl), opRep, tempMap.get(tr));
                        memComment = String.format("[%s %s %s]", tl.toString(), opRep, tr.toString());
                    } else {
                        tr = munchExp(er, addTo);
                        memStr = String.format("[%s %s %s]", tempMap.get(tl), opRep, tempMap.get(tr));
                        memComment = String.format("[%s %s %s]", tl.toString(), opRep, tr.toString());
                    }
                }
            // not + or - operator, so just disregard that it's a BINOP basically
            } else {
                tl = munchExp(m.exp, addTo);
                memStr = String.format("[%s]", tempMap.get(tl));
                memComment = String.format("[%s]", tl.toString());
            }
        } else if (m.exp instanceof MEM) {
            tl = generateTemp("mem");
            munchMem((MEM)m.exp, addTo);
            addTo.add(new MoveInstruction(
                    String.format("\tld\t%s, `d0", memStr),
                    String.format("%s := MEM%s", tl.toString(), memComment),
                    tl, null));
            memStr = String.format("[%s]", tempMap.get(tl));
            memComment = String.format("[%s]", tl.toString());
        } else if (m.exp instanceof CONST){
            memStr = String.format("[%d]", ((CONST)m.exp).value);
            memComment = memStr;
        } else {
            // expression  is not BINOP, TEMP, CONST, or MEM
            tl = munchExp(m.exp, addTo);
            memStr = String.format("[%s]", tempMap.get(tl));
            memComment = String.format("[%s]", tl.toString());
        }
        return null;
    }

    // call and jump already should handle these, and they aren't tied to a temp
    private NameOfTemp munchName(NAME n, List<Instruction> addTo){
        return null;
    }

    // on temp, return where it is
    private NameOfTemp munchTemp(TEMP t, List<Instruction> addTo) {
        if (!tempMap.containsKey(t.temp)){
            putTemp(t.temp);
        }

        return t.temp;
    }

}
