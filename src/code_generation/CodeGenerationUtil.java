package code_generation;

import environment.Environment;
import environment.EnvironmentUtil;
import environment.MethodType;
import environment.VaporClass;
import syntaxtree.*;

import java.util.Map;

/**
 * Author: Michael Sweatt
 */
public class CodeGenerationUtil {
    public static void produceCompilerReservedMethods(Environment env)
    {
        VaporClass compilerReservedMethods = new VaporClass("class");
        MethodType m = new MethodType("ArrayAllocate");
        m.setDefinition(Array.produceArrayAllocateMethod());
        compilerReservedMethods.addMethod(m, "ArrayAllocate");

        m = new MethodType("produceArrayAccess");
        m.setDefinition(Array.produceArrayAccess());
        compilerReservedMethods.addMethod(m, "ArrayAccess");

        env.addClass(compilerReservedMethods);
    }
    public static class  Array {

        static String produceArrayAllocateMethod()
        {
            String bodyLabel = "class.ARRAY_ALLOC_BODY";
            String lenLoc    = "len";
            String tempLoc   = "t.0";
            String arrayLoc  = "arr";
            String checkingCode =   String.format("%s = LtS(0 n)\n", tempLoc) +
                    String.format("if %s goto :%s\n", tempLoc, bodyLabel) +
                    "Error(\"Arrays sizes must be larger than 0\")";
            String sizeComputationCode = String.format("%s = Add(n 1)\n" +
                    "%s = MulS(%s 4)\n", lenLoc, lenLoc, lenLoc);

            String allocationCode = String.format("%s   = HeapAllocZ(%s)\n" +
                    "[%s]  = n\n", arrayLoc, lenLoc, arrayLoc);
            return  String.format("func class.ArrayAllocate(n)\n" +
                                  "%s\n" +
                                  "%s:\n" +
                                  "%s\n" +
                                  "%s\n" +
                                  "ret %s\n", checkingCode, bodyLabel, sizeComputationCode, allocationCode, arrayLoc);
        }

        static String produceArrayAccess()
        {
            int         curr_temp   = 1;
            //String      resultLoc  = "t." + curr_temp;
            String      lenLabel    = "LENGTH_" + curr_temp;
            String      assignLabel = "ASSIGN_" + curr_temp;


            String i_loc     = "t." + curr_temp;
            String len_loc   = "len";
            String code = "";

            // start with code to check for a negative index
            code += String.format("%s = LtS(-1 idx)\n" +
                    "if %s goto :%s\n" +
                    "Error(\"Negative index\")\n", i_loc, i_loc, lenLabel);

            // add code to get length
            code += String.format("%s:\n" +
                    "%s = [a]\n", lenLabel, len_loc);

            // next bounds check
            code += String.format("%s = LtS(idx %s)\n",i_loc, len_loc)  +
                    String.format("if %s goto :%s\n", i_loc, assignLabel) +
                    "Error(\"array index out of bounds\")\n";

            // next compute the offset
            code += String.format("%s:\n",             assignLabel)           +
                    String.format("pos = Add(4 a)\n" )                        +
                    String.format("%s = MulS(idx 4)\n", i_loc)                +
                    String.format("pos = Add(%s pos)\n", i_loc)               +
                    String.format("ret pos");
            code = "func class.ArrayAccess(a idx)\n" + code;

            return code;
        }
    }

    public static void printMethodDefinitions(Environment env) {
        for (Map.Entry<String, VaporClass> c : env.getClasses()) {
            for (Map.Entry<String, MethodType> m : c.getValue().getMethods()) {
                prettyPrintMethod(m.getValue().getDefinition());
            }
        }
    }
    public static String vtableLabel(VaporClass c) {
        return String.format("%s.VTable", c.getName());
    }
    public static void outputVtables(Environment env) {
        // TODO add subtyping
        for (Map.Entry<String, VaporClass> class_entry : env.getClasses()) {
            if (env.getMainClass() == class_entry.getValue()) {
                continue;
            }
            System.out.println("const " + vtableLabel(class_entry.getValue()));
            for (Map.Entry<String, MethodType> method_entry: class_entry.getValue().getMethods()) {
                System.out.println(String.format("\t:%s", method_entry.getValue().getLabel()));
            }
            System.out.println("");
        }
    }

    public static String classType(Node n, Environment env) {
        if (n instanceof PrimaryExpression) {
            PrimaryExpression p = (PrimaryExpression) n;
            return classType(p.f0.choice, env);
        } else if (n instanceof AllocationExpression) {
            AllocationExpression a = (AllocationExpression) n;
            return EnvironmentUtil.identifierToString(a.f1);
        } else if (n instanceof Identifier) {
            Identifier varID = (Identifier) n;
            return env.getVariable(EnvironmentUtil.identifierToString(varID)).getType();
        }
        System.err.println("Unknown type passed to classType");
        return null;
    }

    public static CodeTemporaryPair evaluateParameters(NodeOptional n, String objLoc, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();
        String code = "";
        String parameters = "(" + objLoc;

        if (n.present()) {
            CodeGenerationVisitor v = new CodeGenerationVisitor();
            ExpressionList e = (ExpressionList) n.node;
            CodeTemporaryPair param = e.f0.accept(v, new EnvironmentTemporaryPair(env, curr_temp));
            code += param.getCode();
            curr_temp = param.getNextAvailableTemporary();
            parameters += " " + param.getResultLocation();
            for (Node node : e.f1.nodes) {
                param = node.accept(v, new EnvironmentTemporaryPair(env, curr_temp));
                code += param.getCode();
                curr_temp = param.getNextAvailableTemporary();
                parameters += " " + param.getResultLocation();
            }
        }
        parameters += ")";
        return new CodeTemporaryPair(code, curr_temp, parameters);
    }

    public static void prettyPrintMethod(String code) {
        // remove blank lines
        code = code.replaceAll("(?m)^[ \t]*\r?\n", "");
        int lvl = 1;
        // seperate the lines
        String[] lines = code.split(System.getProperty("line.separator"));

        System.out.println("\n" + lines[0]);
        for (int i = 1; i < lines.length; ++i)
        {
            String line = lines[i];
            // handle labels
            if (line.endsWith(":") || line.startsWith("if")) {
                System.out.println(line);
                // if end label, return code lvl 1
                if (line.contains("end")) {
                    lvl = 1;
                }
                // otherwise indent
                else {
                    lvl = 2;
                }
            } else {
                for (int j = 0; j < lvl; ++j) {
                    System.out.print("    ");
                }
                System.out.println(line);
            }
        }
    }
}
