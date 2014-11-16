package code_generation;


import environment.*;
import syntaxtree.*;
import visitor.GJDepthFirst;

import java.util.List;
import java.util.Vector;

/**
 * Author: Michael Sweatt
 */
public class CodeGenerationVisitor extends GJDepthFirst<CodeTemporaryPair, EnvironmentTemporaryPair> {
    VaporClass curr_class;
    MethodType curr_method;

    public CodeTemporaryPair visit(Goal g, EnvironmentTemporaryPair envTemp) {

        Environment env = envTemp.getEnvironment();
        int curr_temp;

        CodeTemporaryPair curr_code = g.f0.accept(this, envTemp);
        curr_temp = curr_code.getNextAvailableTemporary();
        CodeGenerationUtil.prettyPrintMethod(curr_code.getCode());
        for (Node n : g.f1.nodes) {
            n.accept(this, new EnvironmentTemporaryPair(env, curr_temp));
            curr_temp = curr_code.getNextAvailableTemporary();
        }
        return null;
    }

    // PRIMARY UNITS
    public CodeTemporaryPair visit(MainClass m, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();

        curr_class = env.getClass(EnvironmentUtil.classname(m));
        curr_method = curr_class.getMethod("main");
        Environment localEnv = EnvironmentBuilderUtil.buildLocalEnvironment(m, env);
        String code = "";
        CodeTemporaryPair curr_statement;
        for (Node n : m.f15.nodes) {
            curr_statement = n.accept(this, new EnvironmentTemporaryPair(localEnv, curr_temp));
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

    public CodeTemporaryPair visit(ClassDeclaration c, EnvironmentTemporaryPair envTemp) {
        // visit the local methods
        Environment env;
        int curr_temp;
        CodeTemporaryPair curr_pair;
        Vector<Node> methodDeclarations;

        curr_temp = envTemp.getNextAvailableTemporary();
        env = envTemp.getEnvironment();
        methodDeclarations = c.f4.nodes;

        curr_class = env.getClass(EnvironmentUtil.classname(c));
        Environment localEnv = EnvironmentBuilderUtil.buildLocalEnvironment(c, env);

        EnvironmentTemporaryPair localEnvTempPair = new EnvironmentTemporaryPair(localEnv, curr_temp);

        for (Node n : methodDeclarations) {
            MethodDeclaration method = (MethodDeclaration) n;
            MethodType method_type = curr_class.getMethod(EnvironmentUtil.identifierToString(method.f2));
            // get definition of method
            curr_pair = n.accept(this, localEnvTempPair);
            localEnvTempPair = new EnvironmentTemporaryPair(localEnv, 0);
            CodeGenerationUtil.prettyPrintMethod(curr_pair.getCode());
            method_type.setDefinition(curr_pair.getCode());
        }
        return null;
    }

    public CodeTemporaryPair visit(MethodDeclaration d, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();

        curr_method = curr_class.getMethod(EnvironmentUtil.methodname(d));
        CodeTemporaryPair curr_statement;
        String method_body = "";
        // get local variables
        Environment localEnv = EnvironmentBuilderUtil.buildLocalEnvironment(d, curr_method, env);

        // get function header
        String functionHeader = String.format("func %s (this ", curr_method.getLabel());
        List<Variable> param = curr_method.getParameters();
        if (param.size() > 0) {
            for (int i = 0; i < param.size() - 1; ++i) {
                functionHeader += param.get(i).getName() + " ";
            }
            // add last arg without SPACE
            functionHeader += param.get(param.size() - 1).getName();
        }
        functionHeader += "):";
        // visit body (line by line)

        for (Node n : d.f8.nodes) {
            curr_statement = n.accept(this, new EnvironmentTemporaryPair(localEnv, curr_temp));
            method_body += curr_statement.getCode();
            curr_temp = curr_statement.getNextAvailableTemporary();
        }

        // compile return expression
        CodeTemporaryPair ret = d.f10.accept(this, new EnvironmentTemporaryPair(localEnv, curr_temp));
        curr_temp = ret.getNextAvailableTemporary();
        method_body += ret.getCode();

        //combine
        String code = String.format("%s" + "\n" +
                "%s" + "\n" +
                "ret %s\n", functionHeader, method_body, ret.getResultLocation());
        //System.out.println("\n" + code);
        curr_method.setDefinition(code);
        return new CodeTemporaryPair(code, curr_temp);
    }

    public CodeTemporaryPair visit(MessageSend m, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();
        String resultLoc = "t." + curr_temp++;
        String objLoc;
        String objCode;

        VaporClass lhs_class;

        if (m.f0.f0.choice instanceof ThisExpression) {
            objLoc = "t." + curr_temp;
            curr_temp++;
            Variable thisVar = new Variable("this", objLoc);
            env.addVarsInScope(thisVar);
            objCode = String.format("%s = this\n", objLoc);
            lhs_class = curr_class;
        } else if (m.f0.f0.choice instanceof Identifier) {
            objCode = "";
            Identifier id = (Identifier) m.f0.f0.choice;
            String varName = EnvironmentUtil.identifierToString(id);
            objLoc = env.getVariable(varName).getLocation();
            String lhs_classname = CodeGenerationUtil.classType(m.f0, env);
            lhs_class = env.getClass(lhs_classname);
        } else {
            System.err.println("unrecognized lhs");
            return null;
        }
        // Get method offset
        String method_name = EnvironmentUtil.identifierToString(m.f2);
        int offset = lhs_class.getMethodOffset(method_name);

        String callLocation = "t." + curr_temp++;

        CodeTemporaryPair lhs = m.f0.accept(this, new EnvironmentTemporaryPair(env, curr_temp));
        curr_temp = lhs.getNextAvailableTemporary();

        // set up Load load call
        String code = String.format("%s\n" +
                "%s\n" +
                "%s = [%s + %d]\n", lhs.getCode(), objCode, callLocation, objLoc, offset);
        CodeTemporaryPair params = CodeGenerationUtil.evaluateParameters(m.f4, objLoc, new EnvironmentTemporaryPair(env, curr_temp));

        code += String.format("%s\n" +
                "%s =  call %s %s", params.getCode(), resultLoc, callLocation, params.getResultLocation());
        curr_temp = params.getNextAvailableTemporary();
        return new CodeTemporaryPair(code, curr_temp, resultLoc);
    }

    // PASS THROUGH
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

    public CodeTemporaryPair visit(Block b, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();

        String code = "";
        CodeTemporaryPair curr_pair;

        for (Node n : b.f1.nodes) {
            curr_pair = n.accept(this, new EnvironmentTemporaryPair(env, curr_temp));
            code += curr_pair.getCode();
            curr_temp = curr_pair.getNextAvailableTemporary();
        }
        return new CodeTemporaryPair(code, curr_temp);
    }

    // STATEMENTS
    public CodeTemporaryPair visit(AssignmentStatement a, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp;

        CodeTemporaryPair lhs;
        CodeTemporaryPair rhs;

        lhs = a.f0.accept(this, envTemp);
        curr_temp = lhs.getNextAvailableTemporary();
        rhs = a.f2.accept(this, new EnvironmentTemporaryPair(env, curr_temp));
        curr_temp = rhs.getNextAvailableTemporary();

        String code = String.format("%s\n" +
                "%s = %s\n", rhs.getCode(), lhs.getResultLocation(), rhs.getResultLocation());

        return new CodeTemporaryPair(code, curr_temp);
    }

    public CodeTemporaryPair visit(IfStatement i, EnvironmentTemporaryPair envTemp) {
        int curr_temp = envTemp.getNextAvailableTemporary();

        CodeTemporaryPair condition, thenBranch, elseBranch;
        String code, tag;
        tag = String.format("if%d", curr_temp);

        condition = i.f2.accept(this, envTemp);
        thenBranch = i.f4.accept(this, envTemp);
        elseBranch = i.f6.accept(this, envTemp);

        code = String.format("%s\n" +
                        "if0 %s goto :%s_else\n" +
                        "%s\n" +
                        "goto :%s_end\n" +
                        "%s_else:\n" +
                        "%s\n" +
                        "%s_end:\n", condition.getCode(),
                condition.getResultLocation(),
                tag,
                thenBranch.getCode(),
                tag,
                tag,
                elseBranch.getCode(),
                tag);
        return new CodeTemporaryPair(code, elseBranch.getNextAvailableTemporary());
    }

    public CodeTemporaryPair visit(PrintStatement p, EnvironmentTemporaryPair envTemp) {
        CodeTemporaryPair compiledArg = p.f2.accept(this, envTemp);
        String code = compiledArg.getCode();
        code = String.format("%s\n" +
                "PrintIntS(%s)\n", code, compiledArg.getResultLocation());
        return new CodeTemporaryPair(code, compiledArg.getNextAvailableTemporary());
    }


    public CodeTemporaryPair visit(WhileStatement w, EnvironmentTemporaryPair envTemp) {
        // TODO: IMPLEMENT ME
        w.f2.accept(this, envTemp);
        w.f4.accept(this, envTemp);
        return null;
    }

    // EXPRESSIONS
    public CodeTemporaryPair visit(AllocationExpression e, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();

        String class_name = EnvironmentUtil.identifierToString(e.f1);
        return env.getClass(class_name).getConstructor(curr_temp);
    }

    public CodeTemporaryPair visit(AndExpression e, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();

        CodeTemporaryPair left = e.f0.accept(this, new EnvironmentTemporaryPair(env, curr_temp + 1));
        CodeTemporaryPair right = e.f2.accept(this, new EnvironmentTemporaryPair(env, left.getNextAvailableTemporary()));
        String location = "t." + curr_temp;
        String code = String.format("%s\n" +
                        "%s\n" +
                        "%s = And(%s %s)\n", left.getCode(),
                right.getCode(),
                location,
                left.getResultLocation(),
                right.getResultLocation());
        return new CodeTemporaryPair(code, right.getNextAvailableTemporary(), location);
    }

    public CodeTemporaryPair visit(CompareExpression c, EnvironmentTemporaryPair envTemp) {
        int curr_temp = envTemp.getNextAvailableTemporary();
        Environment env = envTemp.getEnvironment();
        String location = "t." + curr_temp;
        curr_temp++;
        CodeTemporaryPair lhs = c.f0.accept(this, new EnvironmentTemporaryPair(env, curr_temp));
        curr_temp = lhs.getNextAvailableTemporary();
        CodeTemporaryPair rhs = c.f2.accept(this, new EnvironmentTemporaryPair(env, curr_temp));
        curr_temp = rhs.getNextAvailableTemporary();
        String code = String.format("%s\n" +
                        "%s\n" +
                        "%s = LtS(%s %s)", lhs.getCode(),
                rhs.getCode(),
                location,
                lhs.getResultLocation(),
                rhs.getResultLocation());
        return new CodeTemporaryPair(code, curr_temp, location);
    }

    public CodeTemporaryPair visit(PlusExpression e, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();

        CodeTemporaryPair left = e.f0.accept(this, new EnvironmentTemporaryPair(env, curr_temp + 1));
        CodeTemporaryPair right = e.f2.accept(this, new EnvironmentTemporaryPair(env, left.getNextAvailableTemporary()));
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

        CodeTemporaryPair left = e.f0.accept(this, new EnvironmentTemporaryPair(env, curr_temp + 1));
        CodeTemporaryPair right = e.f2.accept(this, new EnvironmentTemporaryPair(env, left.getNextAvailableTemporary()));
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

    public CodeTemporaryPair visit(NotExpression n, EnvironmentTemporaryPair envTemp) {
        CodeTemporaryPair intermediateValue = n.f1.accept(this, envTemp);
        String location = "t." + intermediateValue.getNextAvailableTemporary();
        String code = String.format("%s\n" +
                        "%s = Not(%s)\n", intermediateValue.getCode(),
                location,
                intermediateValue.getResultLocation());
        return new CodeTemporaryPair(code, intermediateValue.getNextAvailableTemporary() + 1, location);
    }

    public CodeTemporaryPair visit(ThisExpression e, EnvironmentTemporaryPair envTemp) {
        Variable thisVar = envTemp.getEnvironment().getVariable("this");
        return new CodeTemporaryPair("", envTemp.getNextAvailableTemporary(), thisVar.getLocation());
    }

    public CodeTemporaryPair visit(TimesExpression e, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();

        CodeTemporaryPair left = e.f0.accept(this, new EnvironmentTemporaryPair(env, curr_temp + 1));
        CodeTemporaryPair right = e.f2.accept(this, new EnvironmentTemporaryPair(env, left.getNextAvailableTemporary()));
        String result_location = "t." + curr_temp;
        String code = String.format("%s\n" +
                        "%s\n" +
                        "%s = MulS(%s %s)\n", left.getCode(),
                right.getCode(),
                result_location,
                left.getResultLocation(),
                right.getResultLocation());

        return new CodeTemporaryPair(code, right.getNextAvailableTemporary(), result_location);
    }

    // ARRAYS
    public CodeTemporaryPair visit(ArrayAllocationExpression a, EnvironmentTemporaryPair envTemp) {
        // TODO: IMPLEMENT ME
        a.f3.accept(this, envTemp);
        return null;
    }

    public CodeTemporaryPair visit(ArrayAssignmentStatement a, EnvironmentTemporaryPair envTemp) {
        // TODO: IMPLEMENTED ME
        a.f0.accept(this, envTemp);
        a.f2.accept(this, envTemp);
        a.f5.accept(this, envTemp);
        return null;
    }

    public CodeTemporaryPair visit(ArrayLength a, EnvironmentTemporaryPair envTemp) {
        // TODO: IMPLEMENT ME
        a.f0.accept(this, envTemp);
        return null;
    }

    public CodeTemporaryPair visit(ArrayLookup a, EnvironmentTemporaryPair envTemp) {
        // TODO: IMPLEMENT ME
        a.f0.accept(this, envTemp);
        a.f2.accept(this, envTemp);
        return null;
    }

    // ENVIRONMENT
    public CodeTemporaryPair visit(Identifier id, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();

        Variable v = env.getVariable(EnvironmentUtil.identifierToString(id));
        if (null == v) {
            System.err.println("bad identifier passed");
            return null;
        }
        String location;
        // FIXME...probably?
        String code;
        if (Variable.SCOPE.INSTANCE_VAR == v.getScope()) {
            location = String.format("t.%d", curr_temp);
            curr_temp++;
            code = String.format("%s = [this + %s]\n", location, v.getLocation());
        } else {
            code = "";
            location = v.getLocation();
        }
        return new CodeTemporaryPair(code, curr_temp, location);
    }


    // LITERALS
    public CodeTemporaryPair visit(TrueLiteral b, EnvironmentTemporaryPair envTemp) {
        int curr_temp;
        // integer literal does not consume a temp
        curr_temp = envTemp.getNextAvailableTemporary();

        return new CodeTemporaryPair("", curr_temp, EnvironmentUtil.nodeTokenToString(b.f0));
    }

    public CodeTemporaryPair visit(FalseLiteral b, EnvironmentTemporaryPair envTemp) {
        int curr_temp;
        // integer literal does not consume a temp
        curr_temp = envTemp.getNextAvailableTemporary();

        return new CodeTemporaryPair("", curr_temp, EnvironmentUtil.nodeTokenToString(b.f0));
    }

    public CodeTemporaryPair visit(IntegerLiteral i, EnvironmentTemporaryPair envTemp) {
        int curr_temp;

        // integer literal does not consume a temp
        curr_temp = envTemp.getNextAvailableTemporary();
        String intValue = EnvironmentUtil.nodeTokenToString(i.f0);
        return new CodeTemporaryPair("", curr_temp, intValue);
    }
}
