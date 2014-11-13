package code_generation;

import com.sun.org.apache.bcel.internal.classfile.Code;
import environment.*;
import sun.org.mozilla.javascript.ast.VariableDeclaration;
import syntaxtree.*;
import visitor.GJDepthFirst;

import java.lang.reflect.Method;
import java.util.Vector;

/**
 * Created by michael on 11/10/14.
 */
public class CodeGenerationVisitor extends GJDepthFirst<CodeTemporaryPair,EnvironmentTemporaryPair> {
    VaporClass curr_class;
    MethodType curr_method;

    public CodeTemporaryPair visit(MainClass m, EnvironmentTemporaryPair envTemp)
    {
        Environment env       = envTemp.getEnvironment();
        int         curr_temp = envTemp.getNextAvailableTemporary();

        Environment localEnv = EnvironmentBuilderUtil.buildLocalEnvironment(m, env);

        return null;
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
        curr_pair          = null;
        methodDeclarations = c.f4.nodes;

        curr_class = env.getClass(EnvironmentUtil.classname(c));
        Environment localEnv = EnvironmentBuilderUtil.buildLocalEnvironment(c, env);
        EnvironmentTemporaryPair localEnvTempPair = new EnvironmentTemporaryPair(curr_temp, localEnv);
        for (Node n : methodDeclarations) {
            MethodDeclaration method = (MethodDeclaration)n;
            MethodType method_type = curr_class.getMethod(EnvironmentUtil.identifierToString(method.f2));
            // get definition of method
            curr_pair =  n.accept(this, envTemp);
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

        // visit body (line by line)
        for (Node n: d.f8.nodes) {
            curr_statement = n.accept(this, new EnvironmentTemporaryPair(curr_temp, localEnv));
//            method_body += curr_statement.getCode();
//            curr_temp = curr_statement.getNextAvailableTemporary();
        }





        return null;
    }

    public CodeTemporaryPair visit(BooleanType b, EnvironmentTemporaryPair envTemp)
    {
        Environment       env;
        int               curr_temp;

        // integer literal does not consume a temp
        env       = envTemp.getEnvironment();
        curr_temp = envTemp.getNextAvailableTemporary();

        return new CodeTemporaryPair(EnvironmentUtil.nodeTokenToString(b.f0), curr_temp);
    }

     public CodeTemporaryPair visit(IntegerLiteral i, EnvironmentTemporaryPair envTemp)
     {
         Environment       env;
         int               curr_temp;

         // integer literal does not consume a temp
         env       = envTemp.getEnvironment();
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

     public CodeTemporaryPair visit(AssignmentStatement a, EnvironmentTemporaryPair envTemp) {
         Environment       env;
         int               curr_temp;
         String            lhs_name;
         CodeTemporaryPair rhs;

         env       = envTemp.getEnvironment();
         curr_temp = envTemp.getNextAvailableTemporary();
         lhs_name = EnvironmentUtil.identifierToString(a.f0);

         Variable lhs_var = env.getVariable(lhs_name);
         if (null == lhs_var) {
             System.err.println("Invalid variable lookup");
         }

         rhs = a.f2.accept(this, envTemp);
         String code = String.format("%s = %s\n", lhs_var.getLocation(), rhs.getCode());

         return new CodeTemporaryPair(code, rhs.getNextAvailableTemporary());
     }

     public CodeTemporaryPair visit(PlusExpression e, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();

        CodeTemporaryPair left  = e.f0.accept(this, new EnvironmentTemporaryPair(curr_temp + 1, env));
        CodeTemporaryPair right = e.f2.accept(this, new EnvironmentTemporaryPair(left.getNextAvailableTemporary(), env));
        String result_location = "t." + curr_temp;
        String code = String.format("%s" +
                                    "%s" +
                                    "%s = Add(%s %s)", left.getCode(),
                                                       right.getCode(),
                                                       result_location,
                                                       left.getResultLocation(),
                                                       right.getResultLocation());

        return new CodeTemporaryPair(code, right.getNextAvailableTemporary(), result_location);
    }


}
