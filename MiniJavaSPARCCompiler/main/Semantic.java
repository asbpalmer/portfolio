package main;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.IOException;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import java.lang.reflect.Field;

import parser.source.TokenGetter;
import parser.source.ParseException;
import syntax.*;
import tree.*;
import canon.*;
import assem.*;

import global.*;
class Semantic {
	private static HashMap<String, HashMap> getMethodInfo(HashMap<String, HashMap> tree, String methodName){
		HashMap<String, HashMap> scope;
		String[] parts = methodName.split("\\$"); // split the method name into parts

		scope = tree.get(parts[0]);
		scope = scope.get(parts[1]);

		return findMethodDecl(scope, parts[parts.length - 1]);
	}

	private static HashMap<String, HashMap> findMethodDecl(HashMap<String, HashMap> methodDecls, String number){
		HashMap<String, HashMap> found = null;

		for (String t: methodDecls.keySet()){
			if (t.equals("$end")){
				found = methodDecls.get(t);
				if (((String)found.get("$info").get("$declnum")).equals(number)){
					break;
				}
			} else if (!t.startsWith("$")) {
				found = findMethodDecl(methodDecls.get(t), number);
			}
		}

		return found;
	}

	public static void main(String[] args)
			throws ClassNotFoundException, IOException, IllegalAccessException, ParseException {
		// are we debugging and what options do we have enabled?
		boolean opts = true;
		boolean verboseOpt = false;

		// find the file we are scanning and its location
		final String argIn = args[args.length - 1];
		final String[] path = argIn.split("[/\\\\]");
		final String fileName = path[path.length - 1];
		final String debugLoc = fileName.split("\\.")[0];
		final String entryPoint = debugLoc + "$main$001";

		// where are we putting our final .s file?
		// right next to the original file
		String assembleLoc;
		if (path.length > 1) {
			assembleLoc = path[0];
			for (String part : Arrays.copyOfRange(path, 1, path.length - 1)) {
				assembleLoc = assembleLoc + "/" + part;
			}
		} else {
			assembleLoc = ".";
		}
		assembleLoc = assembleLoc + "/" + debugLoc + ".s";

		// set up our input file and our parser
		final FileInputStream inFile = new FileInputStream(argIn);
		final TokenGetter checker = new TokenGetter(inFile);

		// variables for tree printing
		PrintWriter treePrintLoc = null;
		syntax.PrettyPrint printer = null;
		// table creation
		PrintWriter tablePrintLoc = null;
		LookupCreator creator = null;
		// table checking
		PrintWriter checkPrintLoc = null;
		LookupChecker tableChecker = null;
		// IR Translation
		PrintWriter IRPrintLoc = null;
		IRTranslator translate = null;
		ArrayList<Stm> fragments;
		// IR Tree printing
		PrintWriter IRTreePrint = null;
		// Output from canonicalization
		ArrayList<Stm> statements = new ArrayList();
		// Fragmentation output
		List<Stm> linear;
		PrintWriter LinWriter = null;
		// Applies maximal munch
		MaximalMunch muncher;
		// Current method info
		String methodDecl = null, curMethod = null;
		HashMap<String, Object> methodInfo = null;
		int nArgs = 0, nLocs = 0, nTemps = 0;
		// Output from Maximal Munch
		ArrayList<String> lines = new ArrayList();
		ArrayList<String> munched = null;
		// Final .s output
		PrintWriter FinalOut = null;

		// if we are debugging, we can process additional instructions
		for(int i = 0; i < args.length-1; i++) {
			if (args[i].equals("-d")) {
				opts = true;
			} else if (opts && args[i].equalsIgnoreCase("verbose")) {
				verboseOpt = true;
			}
		}

		// store our standard output stream
		final PrintStream stdout = new PrintStream(System.out);

		// if we are doing verbose debugging, then create a new printstream to our debug file
		PrintStream verbose = null;
		if (verboseOpt){
			verbose = new PrintStream("./debug/verbose/" + debugLoc + ".debug");
		}

		// if we are verbose debugging, enable tracing and send the output to the verbose debug file
		System.setOut(verbose);
		if(verboseOpt){checker.enable_tracing();} else checker.disable_tracing();

		// parse the program, count the errors
		int[] errorPoint = new int[1];
		int errors;
		final syntax.Program tree = checker.ProgramEntry(fileName, errorPoint);
		errors = errorPoint[0];

		if (errors == 0) {
			// if we are debugging, print the tree
			if (verboseOpt) {
				treePrintLoc = new PrintWriter("./debug/verbose/" + debugLoc + ".treeprint.debug");
				printer = new PrettyPrint(treePrintLoc);
				printer.visit(tree);
				treePrintLoc.close();
			}

			// first dispatch to create the table
			if (verboseOpt) {
				tablePrintLoc = new PrintWriter("./debug/verbose/" + debugLoc + ".tablemake.debug");
				creator = new LookupCreator(fileName, tablePrintLoc);
				errors = errors + creator.getErrors();
			} else {
				creator = new LookupCreator(fileName);
			}
			creator.formTable(tree);
			errors = errors + creator.getErrors();
			if (verboseOpt){
				// if we printed, close our file
				tablePrintLoc.close();
			}

			if(errors == 0) {
				// second dispatch to check
				if (verboseOpt) {
					checkPrintLoc = new PrintWriter("./debug/verbose/" + debugLoc + ".tablecheck.debug");
					tableChecker = new LookupChecker(creator.getTable(), fileName, checkPrintLoc);
				} else {
					tableChecker = new LookupChecker(creator.getTable(), fileName);
				}
				tableChecker.visit(tree);
				errors = errors + tableChecker.getErrors();
				if (verboseOpt){
					// if we printed, close our file
					checkPrintLoc.close();
				}

				if (errors == 0) {
					// Translate the program to IR
					if (errors == 0){
						if (verboseOpt){
							IRPrintLoc = new PrintWriter("./debug/verbose/" + debugLoc + ".ircreate.debug");
							translate = new IRTranslator(fileName, IRPrintLoc, creator.getTable());
						} else {
							translate = new IRTranslator(fileName, creator.getTable());
						}
						// get the fragments from the tree
						fragments = translate.getFragments(tree);
						if(verboseOpt){
							IRPrintLoc.close();
						}

						// by this point, we should not have any errors, but just in case
						errors = errors + translate.getErrors();
						if (errors == 0){

							// print out the debugging from our IR Tree
							if(verboseOpt){
								IRTreePrint = new PrintWriter("./debug/verbose/" + debugLoc + ".irtree.debug");
								TreePrint.print(IRTreePrint, fragments);
								IRTreePrint.close();

								LinWriter = new PrintWriter("./debug/verbose/" + debugLoc + ".linear.debug");
							}

							// at first, add global info, header info
							lines.add( new OperationInstruction("\t.global start").format());
							lines.add( new LabelInstruction( new NameOfLabel("start")).format());
							lines.add( new LabelInstruction( new NameOfLabel(entryPoint)).format());
							lines.add( new Comment("Main doesn't allocate stack space").format());
							lines.add( new OperationInstruction("\tba\t"+entryPoint+"$prologueEnd").format());
							lines.add( new OperationInstruction("\tnop").format());

							// add all our code
							for(Stm f: fragments){
								muncher = new MaximalMunch();
								// what method?
								methodDecl = ((LABEL)((SEQ)f).left).label.toString();
								curMethod = methodDecl.substring(0, methodDecl.lastIndexOf("$"));

								// applies linearization to the fragment
								munched = new ArrayList();
								linear = Canon.linearize(f);
								if (verboseOpt){
									LinWriter.println("Method: " + curMethod);
									TreePrint.print(LinWriter, linear);
									LinWriter.println();
								}
								munched.addAll(muncher.formatStms(linear));

								// if we are on the main function, handle it differently than all other methods
								if (curMethod.equals(entryPoint)){
									// header info already added
									// add the method code
									lines.addAll(munched);
									lines.add(new Comment("Finish program").format());
									// on main, just clear o0 and exit
									lines.add(
										new LabelInstruction(new NameOfLabel(entryPoint + "$epilogueBegin")).format());
									lines.add( new OperationInstruction("\tclr\t%o0").format());
									lines.add( new OperationInstruction("\tcall\texit").format());
									lines.add( new OperationInstruction("\tnop").format());
								} else {
									methodInfo = (HashMap<String, Object>)getMethodInfo(creator.getTable(), curMethod).get("$info");
									nArgs = (int)methodInfo.get("$numargs");
									nLocs = (int)methodInfo.get("$numlocs");
									nTemps = muncher.maxTemps;

									// add header info
									lines.add( new Comment().format() );
									lines.add( new Comment(
										String.format(
											"Method Declaration of %s -- #locs = %d, #args (including this)"+
											" = %d, #temps (max) = %d", curMethod, nLocs, nArgs + 1, nTemps
									)).format() );
									lines.add(new Comment( "\tRegister save area = 16 words, return area = 1 word" ).format());
									lines.add(new LabelInstruction(new NameOfLabel(curMethod)).format());
									lines.add(new OperationInstruction(
											String.format("\t.set\tLOCS, %d", nLocs)).format());
									lines.add(new OperationInstruction(
											String.format("\t.set\tARGS, %d", nArgs)).format());
									lines.add(new OperationInstruction(
											String.format("\t.set\tTMPS, %d", nTemps)).format());
									lines.add(new OperationInstruction(
											"\tsave\t%sp, -4*(LOCS+TMPS+ARGS+1+16)&-8, %sp"
									).format());

									// add the method code
									lines.addAll(munched);
									// add aftermath stuff
									lines.add(new Comment(
										String.format("\tPrepare to return from %s", curMethod)).format());
									lines.add(new LabelInstruction(
											new NameOfLabel(curMethod + "$epilogueBegin")).format());
									lines.add(new OperationInstruction(
											"\tret", "Return from " + curMethod).format());
									lines.add(new OperationInstruction(
											"\trestore", "In the delay slot").format());
									lines.add(new Comment().format());
								}
							}

							if(verboseOpt){
								LinWriter.close();
							}

							FinalOut = new PrintWriter(assembleLoc);
							for (String s: lines){
								FinalOut.println(s);
							}

							FinalOut.close();
						}
					}
				}
			}
		}
		// print the output to stdout
		stdout.println(String.format("filename=%s, errors=%d", argIn, errors));
	}
}

