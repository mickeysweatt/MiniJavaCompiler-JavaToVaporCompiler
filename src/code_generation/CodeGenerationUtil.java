package code_generation;

import environment.*;
import syntaxtree.*;

import java.util.Map;
import java.util.Vector;

/**
 * Author: Michael Sweatt
 */
public class CodeGenerationUtil {
    public static void produceCompilerReservedMethods(Environment env)
    {
        VaporClass compilerReservedMethods = new VaporClass("class");
        MethodType m = new MethodType("int[]", "ArrayAllocate");
        m.setDefinition(Array.produceArrayAllocateMethod());
        compilerReservedMethods.addMethod(m, "ArrayAllocate");

        m = new MethodType("int[]", "produceArrayAccess");
        m.setDefinition(Array.produceArrayAccess());
        compilerReservedMethods.addMethod(m, "ArrayAccess");

        env.addClass(compilerReservedMethods);
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
        } else if (n instanceof BracketExpression) {
            return classType(((BracketExpression) n).f1.f0.choice, env);
        } else if (n instanceof MessageSend) {
            // look up lhs class
            MessageSend m = (MessageSend)n;
            String lhs_class = classType(m.f0 , env);
            String methodName =  EnvironmentUtil.identifierToString(m.f2);
            return env.getClass(lhs_class).getMethod(methodName).getReturnType();
        } else if ( n instanceof ThisExpression) {
            return env.getVariable("this").getType();
        }
        System.err.println("Unknown type passed to classType");
        return null;
    }

    public static CodeTemporaryPair evaluateParameters(CodeGenerationVisitor v, NodeOptional n, String objLoc, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();
        String code = "";
        String parameters = "(" + objLoc;

        if (n.present()) {
            ExpressionList e = (ExpressionList) n.node;

            // handle the first parameter
            CodeTemporaryPair param = e.f0.accept(v, new EnvironmentTemporaryPair(env, curr_temp));

            curr_temp = param.getNextAvailableTemporary();
            String paramLoc  = param.getResultLocation();
            String paramCode = param.getCode();
            code += paramCode;
            parameters += " " + paramLoc;
            for (Node node : e.f1.nodes) {
                param = node.accept(v, new EnvironmentTemporaryPair(env, curr_temp));
                paramLoc  = param.getResultLocation();
                paramCode = param.getCode();
                code += paramCode;
                curr_temp = param.getNextAvailableTemporary();
                parameters += " " + paramLoc;
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

    public static CodeTemporaryPair compileClass(CodeGenerationVisitor    v,
                                                 Node                     c,
                                                 Vector<Node>             methodDeclarations,
                                                 EnvironmentTemporaryPair envTemp)
    {
        // visit the local methods
        Environment env;
        CodeTemporaryPair curr_pair;
        int curr_temp = envTemp.getNextAvailableTemporary();
        env = envTemp.getEnvironment();

        Environment classEnv  = EnvironmentBuilderUtil.buildLocalEnvironment(c, env);
        EnvironmentTemporaryPair localEnvTempPair;
        for (Node n : methodDeclarations) {
            Environment methodEnv;
            MethodDeclaration method = (MethodDeclaration) n;
            MethodType method_type = v.curr_class.getMethod(EnvironmentUtil.identifierToString(method.f2));
            // get definition of method
            methodEnv = EnvironmentBuilderUtil.buildLocalEnvironment(method, method_type, classEnv);

            localEnvTempPair = new EnvironmentTemporaryPair(methodEnv, curr_temp);
            curr_pair = n.accept(v, localEnvTempPair);
            curr_temp = curr_pair.getNextAvailableTemporary();

            //CodeGenerationUtil.prettyPrintMethod(curr_pair.getCode());
            method_type.setDefinition(curr_pair.getCode());
        }
        return new CodeTemporaryPair("", curr_temp);
    }

    public static CodeTemporaryPair handleLHSLocs(Node n, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp   = envTemp.getNextAvailableTemporary();
        String location;

        Variable v = env.getVariable(EnvironmentUtil.identifierToString((Identifier)n));
        if (null == v) {
            System.err.println("bad identifier passed");
            return null;
        }

        String code;
        if (Variable.SCOPE.INSTANCE_VAR == v.getScope()) {
            curr_temp++;
            location = String.format("[this + %s]", v.getLocation());
            code = "";
        } else {
            code = "";
            location = v.getLocation();
        }
        return new CodeTemporaryPair(code, curr_temp, location);
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
            String      nullLabel   = "NULL_"   + curr_temp;
            String      lenLabel    = "LENGTH_" + curr_temp;
            String      assignLabel = "ASSIGN_" + curr_temp;


            String i_loc     = "t." + curr_temp;
            String len_loc   = "len";
            String code = "";

            // start with code to check for a negative index
            code += String.format("%s = LtS(-1 idx)\n" +
                    "if %s goto :%s\n" +
                    "Error(\"Negative index\")\n", i_loc, i_loc, nullLabel);
            // add code to see if the array pointer is null
            code += String.format("%s:\n", nullLabel) +
                    String.format("if a goto :%s\n", lenLabel) +
                                  "Error(\"null pointer\")\n";
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
}
