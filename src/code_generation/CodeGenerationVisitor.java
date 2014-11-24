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

        for (Node n : g.f1.nodes) {
            curr_code = n.accept(this, new EnvironmentTemporaryPair(env, curr_temp));
            curr_temp = curr_code.getNextAvailableTemporary();
        }
        System.out.println("");
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
        return new CodeTemporaryPair(code, curr_temp);
    }

    public CodeTemporaryPair visit(ClassDeclaration c, EnvironmentTemporaryPair envTemp) {
        // visit the local methods
        Environment env;
        CodeTemporaryPair curr_pair;
        Vector<Node> methodDeclarations;
        int curr_temp = envTemp.getNextAvailableTemporary();
        env = envTemp.getEnvironment();
        methodDeclarations = c.f4.nodes;

        curr_class = env.getClass(EnvironmentUtil.classname(c));
        Environment classEnv  = EnvironmentBuilderUtil.buildLocalEnvironment(c, env);

        EnvironmentTemporaryPair localEnvTempPair;

        for (Node n : methodDeclarations) {
            Environment methodEnv;
            MethodDeclaration method = (MethodDeclaration) n;
            MethodType method_type = curr_class.getMethod(EnvironmentUtil.identifierToString(method.f2));
            // get definition of method
            methodEnv = EnvironmentBuilderUtil.buildLocalEnvironment(method, method_type, classEnv);

            localEnvTempPair = new EnvironmentTemporaryPair(methodEnv, curr_temp);
            curr_pair = n.accept(this, localEnvTempPair);
            curr_temp = curr_pair.getNextAvailableTemporary();

            //CodeGenerationUtil.prettyPrintMethod(curr_pair.getCode());
            method_type.setDefinition(curr_pair.getCode());
        }
        return new CodeTemporaryPair("", curr_temp);
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
        functionHeader += ")";
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
        String retLoc = "t." + curr_temp++;
        //combine
        String code = String.format("%s\n", functionHeader) +
                      String.format("%s\n", method_body) +
                      String.format("%s = %s\n", retLoc, ret.getResultLocation()) +
                      String.format("ret %s\n", retLoc);
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
        CodeTemporaryPair lhs;

        // compile lhs
        lhs = m.f0.accept(this, new EnvironmentTemporaryPair(env, curr_temp));
        curr_temp = lhs.getNextAvailableTemporary();
        // get the reciever class
        String lhs_classname = CodeGenerationUtil.classType(m.f0, env);
        lhs_class = env.getClass(lhs_classname);

        // handle  different types of lhs
        if (m.f0.f0.choice instanceof AllocationExpression) {
            objLoc = lhs.getResultLocation();
            objCode = "";
        } else if (m.f0.f0.choice instanceof ThisExpression) {
            objLoc = env.getVariable("this").getLocation();
            objCode = String.format("%s = this\n", objLoc);
        } else if (m.f0.f0.choice instanceof Identifier) {
            Identifier id = (Identifier) m.f0.f0.choice;
            String varName = EnvironmentUtil.identifierToString(id);
            objLoc = env.getVariable(varName).getLocation();
            objCode = "";
        } else if (m.f0.f0.choice instanceof BracketExpression) {
            lhs_class = env.getClass(lhs_classname);
            objLoc = lhs.getResultLocation();
            objCode = lhs.getCode();
        } else {
            System.err.println("unrecognized lhs");
            return null;
        }
        // Get method offset
        String method_name = EnvironmentUtil.identifierToString(m.f2);
        int offset = lhs_class.getMethodOffset(method_name);

        // set up Load load call
        String callLocation = "t." + curr_temp++;
        String code = String.format("%s\n", lhs.getCode()) +
                      String.format("%s\n", objCode) +
                      String.format("%s = [%s]\n", callLocation, objLoc) +
                      String.format("%s = [%s + %d]\n", callLocation, callLocation, offset);
        // compile parameters
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

    public CodeTemporaryPair visit(ExpressionRest e, EnvironmentTemporaryPair envTemp) {
        return e.f1.accept(this, envTemp);
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

    public CodeTemporaryPair visit(TypeDeclaration d, EnvironmentTemporaryPair envTemp) {
        return d.f0.accept(this, envTemp);
    }

    public CodeTemporaryPair visit(NodeChoice n, EnvironmentTemporaryPair envTemp) {
        return n.choice.accept(this, envTemp);
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

        lhs = CodeGenerationUtil.handleLHSLocs(a.f0, envTemp);
        curr_temp = lhs.getNextAvailableTemporary();
        rhs = a.f2.accept(this, new EnvironmentTemporaryPair(env, curr_temp));
        curr_temp = rhs.getNextAvailableTemporary();

        String code = String.format("%s\n", rhs.getCode()) +
                      String.format("%s\n", lhs.getCode()) +
                      String.format("%s = %s\n", lhs.getResultLocation(), rhs.getResultLocation());

        return new CodeTemporaryPair(code, curr_temp);
    }

    public CodeTemporaryPair visit(IfStatement i, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();

        CodeTemporaryPair condition, thenBranch, elseBranch;
        String code, tag;
        tag = String.format("if%d", curr_temp++);

        condition = i.f2.accept(this, new EnvironmentTemporaryPair(env, curr_temp));
        thenBranch = i.f4.accept(this, new EnvironmentTemporaryPair(env, condition.getNextAvailableTemporary()));
        elseBranch = i.f6.accept(this, new EnvironmentTemporaryPair(env, thenBranch.getNextAvailableTemporary()));

        code = String.format("%s\n" +
                        "if0 %s goto :%s_else\n" +
                        "%s\n" +
                        "goto :%s_end\n" +
                        "%s_else:\n" +
                        "%s\n" +
                        "%s_end:\n",
                        condition.getCode(),
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
        Environment env = envTemp.getEnvironment();
        int curr_temp   = envTemp.getNextAvailableTemporary();
        String bodyTag     =  "Loop_body_" + curr_temp;
        String conditonTag =  "Loop_condition_" + curr_temp;
        curr_temp++;
        CodeTemporaryPair conditon = w.f2.accept(this, new EnvironmentTemporaryPair(env, curr_temp));
        CodeTemporaryPair body     = w.f4.accept(this, new EnvironmentTemporaryPair(env, conditon.getNextAvailableTemporary()));
        String code = String.format("goto :%s\n" +
                                    "%s:\n" +
                                    "%s\n" +
                                    "%s:\n" +
                                    "%s\n" +
                                    "if %s goto :%s\n", conditonTag,
                                                      bodyTag,
                                                      body.getCode(),
                                                      conditonTag,
                                                      conditon.getCode(),
                                                      conditon.getResultLocation(),
                                                      bodyTag);
        //System.out.println(code);
        return new CodeTemporaryPair(code, body.getNextAvailableTemporary());
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
        String location = "t." + curr_temp;
        CodeTemporaryPair left = e.f0.accept(this, new EnvironmentTemporaryPair(env, curr_temp + 1));
        CodeTemporaryPair right = e.f2.accept(this, new EnvironmentTemporaryPair(env, left.getNextAvailableTemporary()));

        String code = String.format("%s\n" +
                        "%s\n" +
                        "%s = MulS(%s %s)\n", left.getCode(),
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
        CodeTemporaryPair lhs = c.f0.accept(this, new EnvironmentTemporaryPair(env, curr_temp + 1));
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
        String result_location = "t." + curr_temp;
        CodeTemporaryPair left = e.f0.accept(this, new EnvironmentTemporaryPair(env, curr_temp + 1));
        CodeTemporaryPair right = e.f2.accept(this, new EnvironmentTemporaryPair(env, left.getNextAvailableTemporary()));

        String code = String.format("%s\n" +
                        "%s\n" +
                        "%s = Add(%s %s)\n", left.getCode(),
                right.getCode(),
                result_location,
                left.getResultLocation(),
                right.getResultLocation());
        return new CodeTemporaryPair(code, right.getNextAvailableTemporary() , result_location);
    }

    public CodeTemporaryPair visit(MinusExpression e, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();
        String result_location = "t." + curr_temp;
        CodeTemporaryPair left = e.f0.accept(this, new EnvironmentTemporaryPair(env, curr_temp + 1));
        CodeTemporaryPair right = e.f2.accept(this, new EnvironmentTemporaryPair(env, left.getNextAvailableTemporary()));

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
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();

        String location = "t." + curr_temp;
        CodeTemporaryPair intermediateValue = n.f1.accept(this, new EnvironmentTemporaryPair(env, curr_temp + 1));
        curr_temp = intermediateValue.getNextAvailableTemporary();
        String set_label = "set" + curr_temp++;
        String set_close = set_label + "_end";
        String code = String.format("%s\n", intermediateValue.getCode()) +
                      String.format("%s = Eq(0 %s)\n"   , location, intermediateValue.getResultLocation()) +
                      String.format("if %s goto :%s\n" , location, set_label) +
                      String.format("%s = 0\n"          , location) +
                      String.format("goto :%s\n"        , set_close) +
                      String.format("%s:\n"             , set_label) +
                      String.format("%s = 1\n"          , location) +
                      String.format("%s:\n"             , set_close);
        return new CodeTemporaryPair(code, curr_temp, location);
    }

    public CodeTemporaryPair visit(ThisExpression e, EnvironmentTemporaryPair envTemp) {
        Variable thisVar = envTemp.getEnvironment().getVariable("this");
        int curr_temp = envTemp.getNextAvailableTemporary();
        if (null ==  thisVar) {
            thisVar = new Variable("this", curr_class.getName(), "this", Variable.SCOPE.INSTANCE_VAR);
            envTemp.getEnvironment().addVarsInScope(thisVar);
            curr_temp++;
        }
        return new CodeTemporaryPair("", curr_temp, thisVar.getLocation());
    }

    public CodeTemporaryPair visit(TimesExpression e, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();
        String result_location = "t." + curr_temp;
        curr_temp++;
        CodeTemporaryPair left = e.f0.accept(this, new EnvironmentTemporaryPair(env, curr_temp + 1));
        CodeTemporaryPair right = e.f2.accept(this, new EnvironmentTemporaryPair(env, left.getNextAvailableTemporary()));

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
        CodeGenerationUtil.Array.produceArrayAllocateMethod();

        int curr_temp = envTemp.getNextAvailableTemporary();
        String resultLoc = "t." + curr_temp++;
        CodeTemporaryPair arraySize =  a.f3.accept(this, envTemp);
        String code = String.format("%s\n" +
                                    "%s =  call :class.ArrayAllocate(%s)\n",
                                        arraySize.getCode(),
                                        resultLoc,
                                        arraySize.getResultLocation());

        return new CodeTemporaryPair(code, curr_temp, resultLoc);
    }

    public CodeTemporaryPair visit(ArrayAssignmentStatement a, EnvironmentTemporaryPair envTemp) {

        Environment env = envTemp.getEnvironment();
        int         curr_temp   = envTemp.getNextAvailableTemporary();

        CodeTemporaryPair lhs   = a.f0.accept(this, new EnvironmentTemporaryPair(env, curr_temp + 1));
        CodeTemporaryPair index = a.f2.accept(this, new EnvironmentTemporaryPair(env, lhs.getNextAvailableTemporary()));
        CodeTemporaryPair rhs   = a.f5.accept(this, new EnvironmentTemporaryPair(env, index.getNextAvailableTemporary()));
        curr_temp = rhs.getNextAvailableTemporary();
        String i_loc     = "t." + curr_temp++;

        // Start of with evaluating all terms
        String code = String.format("%s\n", lhs.getCode()) +
                      String.format("%s\n", index.getCode()) +
                      String.format("%s\n", rhs.getCode());

        code += String.format("%s = call :class.ArrayAccess(%s %s)\n", i_loc,
                                                                       lhs.getResultLocation(),
                                                                       index.getResultLocation());
        // next do the thing!
        code += String.format("[%s] = %s\n"      , i_loc, rhs.getResultLocation());

        return new CodeTemporaryPair(code, curr_temp + 1);
    }

    public CodeTemporaryPair visit(ArrayLength a, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp = envTemp.getNextAvailableTemporary();

        String resultLoc = "t." + curr_temp++;
        CodeTemporaryPair lhs =  a.f0.accept(this, new EnvironmentTemporaryPair(env, curr_temp));
        curr_temp = lhs.getNextAvailableTemporary();
        String code = String.format("%s\n" +
                                    "%s = [%s]", lhs.getCode(),
                                                 resultLoc,
                                                 lhs.getResultLocation());
        return new CodeTemporaryPair(code, curr_temp, resultLoc);
    }

    public CodeTemporaryPair visit(ArrayLookup a, EnvironmentTemporaryPair envTemp) {
        Environment env  = envTemp.getEnvironment();
        int curr_temp    = envTemp.getNextAvailableTemporary();
        String resultLoc = "t." + curr_temp;


        CodeTemporaryPair lhs   = a.f0.accept(this, new EnvironmentTemporaryPair(env, curr_temp + 1));
        CodeTemporaryPair index = a.f2.accept(this, new EnvironmentTemporaryPair(env, lhs.getNextAvailableTemporary()));
        curr_temp = index.getNextAvailableTemporary();

        String lhs_loc   = lhs.getResultLocation();
        String index_loc = index.getResultLocation();

        String code;
        code =  String.format("%s\n", lhs.getCode()) +
                String.format("%s\n", index.getCode()) +
                String.format("%s = call :class.ArrayAccess(%s %s)\n", resultLoc, lhs_loc, index_loc) +
                String.format("%s = [%s]", resultLoc, resultLoc);
        return new CodeTemporaryPair(code, curr_temp, resultLoc);
    }

    // ENVIRONMENT
    public CodeTemporaryPair visit(Identifier id, EnvironmentTemporaryPair envTemp) {
        Environment env = envTemp.getEnvironment();
        int curr_temp   = envTemp.getNextAvailableTemporary();
        String location;

        Variable v = env.getVariable(EnvironmentUtil.identifierToString(id));
        if (null == v) {
            System.err.println("bad identifier passed");
            return null;
        }

        // FIXME...probably?
        String code;
        if (Variable.SCOPE.INSTANCE_VAR == v.getScope()) {
            location = "t." + curr_temp++;
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
        // boolean literal does not consume a temp
        curr_temp = envTemp.getNextAvailableTemporary();
        return new CodeTemporaryPair("", curr_temp, "1");
    }

    public CodeTemporaryPair visit(FalseLiteral b, EnvironmentTemporaryPair envTemp) {
        int curr_temp;
        // boolean literal does not consume a temp
        curr_temp = envTemp.getNextAvailableTemporary();
        return new CodeTemporaryPair("", curr_temp, "0");
    }

    public CodeTemporaryPair visit(IntegerLiteral i, EnvironmentTemporaryPair envTemp) {
        int curr_temp;
        // integer literal does not consume a temp
        curr_temp = envTemp.getNextAvailableTemporary();
        String intValue = EnvironmentUtil.nodeTokenToString(i.f0);
        return new CodeTemporaryPair("", curr_temp, intValue);
    }
}
