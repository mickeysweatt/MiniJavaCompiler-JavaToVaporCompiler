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
    public static class  Array {
        public static void produceArrayAllocateMethod()
        {
            String bodyLabel = "class.ARRAY_ALLOC_BODY";
            String lenLoc    = "t.0";
            String arrayLoc  = "t.1";
            String checkingCode = String.format("if LtS(0 n) goto : %s\n" +
                                                "Error(\"Arrays sizes must be larger than 0\")" , bodyLabel);
            String sizeComputationCode = String.format("%s = Add(n 1)\n" +
                                                       "%s = MulS(t.0 4)\n", lenLoc, lenLoc);

            String allocationCode = String.format("%s   = HeapAllocZ(%s)\n" +
                                                  "[%s]  = size\n", arrayLoc, lenLoc, arrayLoc);
            String code = String.format("func class.ArrayAllocate(n)\n" +
                                        "%s\n" +
                                        ":%s\n" +
                                        "%s\n" +
                                        "%s\n" +
                                        "ret %s\n", checkingCode, bodyLabel, sizeComputationCode, allocationCode, arrayLoc);
            prettyPrintMethod(code);
        }
    }

    public static String vtableLabel(VaporClass c) {
        return String.format("%s.VTable", c.getName());
    }
    public static void outputVtables(Environment env) {
        // TODO add subtyping
        for (Map.Entry<String, VaporClass> class_entry : env.getClasses().entrySet()) {
            if (env.getMainClass() == class_entry.getValue()) {
                continue;
            }
            System.out.println(" const " + vtableLabel(class_entry.getValue()));
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
