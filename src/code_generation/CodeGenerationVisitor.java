package code_generation;


import environment.*;
import syntaxtree.*;
import visitor.GJDepthFirst;

import java.util.List;
import java.util.Vector;

/**
 * Author: Michael Sweatt
 */
public class CodeGenerationVisitor extends GJDepthFirst<CodeTemporaryPair,EnvironmentTemporaryPair> {
    VaporClass curr_class;
    MethodType curr_method;

    public CodeTemporaryPair visit (Goal g, EnvironmentTemporaryPair envTemp) {

        Environment env       = envTemp.getEnvironment();
        int         curr_temp;

        CodeTemporaryPair curr_code = g.f0.accept(this, envTemp);
        curr_temp = curr_code.getNextAvailableTemporary();
        CodeGenerationUtil.prettyPrintMethod(curr_code.getCode());
        for (Node n : g.f1.nodes) {
            n.accept(this, new EnvironmentTemporaryPair(curr_temp, env));
            curr_temp = curr_code.getNextAvailableTemporary();
        }
        return null;
    }

    public CodeTemporaryPair visit(MainClass m, EnvironmentTemporaryPair envTemp)
    {
        Environment env       = envTemp.getEnvironment();
        int         curr_temp = envTemp.getNextAvailableTemporary();

        curr_class  = env.getClass(EnvironmentUtil.classname(m));
        curr_method = curr_class.getMethod("main");
        Environment localEnv = EnvironmentBuilderUtil.buildLocalEnvironment(m, env);
        String code = "";
        CodeTemporaryPair curr_statement;
        for (Node n: m.f15.nodes) {
            curr_statement = n.accept(this, new EnvironmentTemporaryPair(curr_temp, localEnv));
            curr_temp = curr_statement.getNextAvailableTemporary();
            code += curr_statement.getCode();
        }
        code = String.format("func Main()" + "\n" +
                             "%s\n" +
                             "ret\n", code);
        curr_method.setDefinition(code);
        //System.out.println(code);
        return new CodeTemporaryPair(code, curr_temp);
    }

    public CodeTemporaryPair visit(ClassDeclaration c, EnvironmentTemporaryPair envTemp)
    {
        // visit the local methods
        Environment       env;
        int               curr_temp;
        CodeTemporaryPair curr_pair;
        Vector<Node>      methodDeclarations;

        curr_temp          = envTemp.getNextAvailableTemporary();
        env                = envTemp.getEnvironment();
        methodDeclarations = c.f4.nodes;

        curr_class = env.getClass(EnvironmentUtil.classname(c));
        Environment localEnv = EnvironmentBuilderUtil.buildLocalEnvironment(c, env);
        EnvironmentTemporaryPair localEnvTempPair = new EnvironmentTemporaryPair(curr_temp, localEnv);

        for (Node n : methodDeclarations) {
            MethodDeclaration method = (MethodDeclaration)n;
            MethodType method_type = curr_class.getMethod(EnvironmentUtil.identifierToString(method.f2));
            // get definition of method
            curr_pair =  n.accept(this, localEnvTempPair);
            CodeGenerationUtil.prettyPrintMethod(curr_pair.getCode());
            method_type.setDefinition(curr_pair.getCode());
        }
        return null;
    }

    public CodeTemporaryPair visit(MethodDeclaration d, EnvironmentTemporaryPair envTemp)
    {
        Environment env         = envTemp.getEnvironment();
        int         curr_temp   = envTemp.getNextAvailableTemporary();

        curr_method = curr_class.getMethod(EnvironmentUtil.methodname(d));
        CodeTemporaryPair curr_statement;
        String method_body = "";
        // get local variables
        Environment localEnv = EnvironmentBuilderUtil.buildLocalEnvironment(d, curr_method, env);

        // get function header
        String functionHeader = String.format("func %s (", curr_method.getLabel());
        List<String> param = curr_method.getParameters();
        if (param.size() > 0) {
            for (int i = 0; i < param.size() - 1; ++i) {
                functionHeader += param.get(i) + ", ";
            }
            // add last arg without ,
            functionHeader += param.get(param.size() - 1);
        }
        functionHeader += "):";

        // visit body (line by line)
        for (Node n: d.f8.nodes) {
            curr_statement = n.accept(this, new EnvironmentTemporaryPair(curr_temp, localEnv));
            method_body += curr_statement.getCode();
            curr_temp = curr_statement.getNextAvailableTemporary();
        }

        // compile return expression
        CodeTemporaryPair ret = d.f10.accept(this, new EnvironmentTemporaryPair(curr_temp, localEnv));
        curr_temp = ret.getNextAvailableTemporary();
        method_body += ret.getCode();

        //combine
        String code = String.format("%s" + "\n" +
                                    "%s" +"\n" +
                                    "ret %s\n", functionHeader, method_body, ret.getResultLocation());
        //System.out.println("\n" + code);
        curr_method.setDefinition(code);
        return new CodeTemporaryPair(code, curr_temp);
    }

    public CodeTemporaryPair visit(BooleanType b, EnvironmentTemporaryPair envTemp)
    {
        int               curr_temp;
        // integer literal does not consume a temp
        curr_temp = envTemp.getNextAvailableTemporary();

        return new CodeTemporaryPair(EnvironmentUtil.nodeTokenToString(b.f0), curr_temp);
    }

     public CodeTemporaryPair visit(IntegerLiteral i, EnvironmentTemporaryPair envTemp)
     {
         int               curr_temp;

         // integer literal does not consume a temp
         curr_temp = envTemp.getNextAvailableTemporary();
         String intValue = EnvironmentUtil.nodeTokenToString(i.f0);
         return new CodeTemporaryPair("", curr_temp, intValue);
     }

     public CodeTemporaryPair visit(Expression e, EnvironmentTemporaryPair envTemp) {
         return e.f0.accept(this, envTemp);
     }

    public CodeTemporaryPair visit(PrimaryExpression e, EnvironmentTemporaryPair envTemp) {
        return e.f0.accept(this, envTemp);
    }

     public CodeTemporaryPair visit(Statement s, EnvironmentTemporaryPair e) {
        return s.f0.accept(this, e);
    }

     public CodeTemporaryPair visit(BracketExpression b, EnvironmentTemporaryPair envTemp) {
         return b.f1.accept(this, envTemp);
     }

     public CodeTemporaryPair visit(AssignmentStatement a, EnvironmentTemporaryPair envTemp) {
         CodeTemporaryPair lhs;
         CodeTemporaryPair rhs;

         lhs = a.f0.accept(this, envTemp);
         rhs = a.f2.accept(this, envTemp);

         String code = String.format("%s\n" +
                                     "%s = %s\n", rhs.getCode(), lhs.getResultLocation(), rhs.getResultLocation());

         return new CodeTemporaryPair(code, rhs.getNextAvailableTemporary());
     }

     public CodeTemporaryPair visit(PlusExpression e, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();

        CodeTemporaryPair left  = e.f0.accept(this, new EnvironmentTemporaryPair(curr_temp + 1, env));
        CodeTemporaryPair right = e.f2.accept(this, new EnvironmentTemporaryPair(left.getNextAvailableTemporary(), env));
        String result_location = "t." + curr_temp;
        String code = String.format("%s\n" +
                                    "%s\n" +
                                    "%s = Add(%s %s)\n", left.getCode(),
                                                       right.getCode(),
                                                       result_location,
                                                       left.getResultLocation(),
                                                       right.getResultLocation());

        return new CodeTemporaryPair(code, right.getNextAvailableTemporary(), result_location);
    }

    public CodeTemporaryPair visit(MinusExpression e, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();

        CodeTemporaryPair left  = e.f0.accept(this, new EnvironmentTemporaryPair(curr_temp + 1, env));
        CodeTemporaryPair right = e.f2.accept(this, new EnvironmentTemporaryPair(left.getNextAvailableTemporary(), env));
        String result_location = "t." + curr_temp;
        String code = String.format("%s\n" +
                                    "%s\n" +
                                    "%s = Sub(%s %s)\n", left.getCode(),
                                                          right.getCode(),
                                                          result_location,
                                                          left.getResultLocation(),
                                                          right.getResultLocation());

        return new CodeTemporaryPair(code, right.getNextAvailableTemporary(), result_location);
    }

    public CodeTemporaryPair visit(TimesExpression e, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();

        CodeTemporaryPair left  = e.f0.accept(this, new EnvironmentTemporaryPair(curr_temp + 1, env));
        CodeTemporaryPair right = e.f2.accept(this, new EnvironmentTemporaryPair(left.getNextAvailableTemporary(), env));
        String result_location = "t." + curr_temp;
        String code = String.format("%s\n" +
                        "%s\n" +
                        "%s = Mul(%s %s)\n", left.getCode(),
                                             right.getCode(),
                                             result_location,
                                             left.getResultLocation(),
                                             right.getResultLocation());

        return new CodeTemporaryPair(code, right.getNextAvailableTemporary(), result_location);
    }

    public CodeTemporaryPair visit(PrintStatement p, EnvironmentTemporaryPair envTemp) {
        CodeTemporaryPair compiledArg = p.f2.accept(this, envTemp);
        String code = compiledArg.getCode();
        code = String.format("%s\n" +
                             "PrintIntS(%s)\n",code, compiledArg.getResultLocation());
        return new CodeTemporaryPair(code, compiledArg.getNextAvailableTemporary());
    }

    public CodeTemporaryPair visit(Identifier id, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp   = envTemp.getNextAvailableTemporary();

        Variable v  = env.getVariable(EnvironmentUtil.identifierToString(id));
        if (null == v) {
            System.err.println("bad identifier passed");
            return null;
        }
        return new CodeTemporaryPair("", curr_temp, v.getLocation());
    }


}
