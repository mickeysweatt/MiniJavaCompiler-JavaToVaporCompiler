package environment;

import syntaxtree.*;

import java.util.*;

/**
 * Author: Michael Sweatt
 */

public class EnvironmentBuilderUtil {
    public static void addInstanceVariablesToClass(Vector<Node> varList, VaporClass c) {
        for (Node n : varList) {
            VarDeclaration var = (VarDeclaration) n;
            String var_name = EnvironmentUtil.identifierToString(var.f1);
            String var_type = EnvironmentUtil.syntaxTreeTypeToString(var.f0);
            c.addInstanceVariable(var_name, var_type);
        }
    }

    public static void addMethodsToClass(Vector<Node> methodList, VaporClass c) {
        for (Node n : methodList) {
            MethodDeclaration methodNode = (MethodDeclaration) n;
            String method_name = EnvironmentUtil.identifierToString(methodNode.f2);
            String method_type = EnvironmentUtil.syntaxTreeTypeToString(methodNode.f1);
            MethodType curr_method = new MethodType(method_type, getVariableList(methodNode.f4.node));
            c.addMethod(curr_method, method_name);
        }
    }

    public static LinkedList<Variable> getVariableList(Node parameter) {
        LinkedList<Variable> vars = new LinkedList<Variable>();
        if (null != parameter && parameter instanceof FormalParameterRest) {
            vars = getVariableList(((FormalParameterRest) parameter).f1);
        } else if (null != parameter && parameter instanceof FormalParameter) {
            FormalParameter fp = (FormalParameter) parameter;
            String parameterName = fp.f1.f0.toString();
            String parameterType = EnvironmentUtil.syntaxTreeTypeToString(fp.f0);
            vars = new LinkedList<Variable>();
            vars.add(new Variable(parameterName, parameterType));
        } else if (null != parameter && parameter instanceof FormalParameterList) {
            FormalParameterList pl = (FormalParameterList) parameter;
            vars = new LinkedList<Variable>();
            vars.addAll(getVariableList(pl.f0));
            for (Node n : pl.f1.nodes) {
                vars.addAll(getVariableList(n));
            }
        }
        return vars;
    }

    public static Environment buildLocalEnvironment(MainClass m, Environment env) {
        Environment localEnv = new Environment(env);
        String class_name = EnvironmentUtil.classname(m);
        VaporClass curr_class = localEnv.getClass(class_name);

        for (Variable v : curr_class.getVariables()) {
            localEnv.addVarsInScope(v);
        }

        for (Node n : m.f14.nodes) {
            VarDeclaration var = (VarDeclaration) n;
            String var_name = EnvironmentUtil.identifierToString(var.f1);
            String var_type = EnvironmentUtil.syntaxTreeTypeToString(var.f0);
            localEnv.addVarsInScope(new Variable(var_name, var_type));
        }

        return localEnv;
    }

    public static Environment buildLocalEnvironment(Node c, Environment env) {
        if (c instanceof ClassExtendsDeclaration) {
            return buildLocalEnvironment((ClassExtendsDeclaration)c, env);
        }
        else if (c instanceof  ClassDeclaration) {
            return buildLocalEnvironment((ClassDeclaration)c, env);
        }
        else {
            System.err.println("Unrecongnized type");
            return null;
        }
    }

    public static Environment buildLocalEnvironment(ClassDeclaration d, Environment env) {
        Environment localEnv = new Environment(env);
        String class_name = EnvironmentUtil.classname(d);
        VaporClass curr_class = localEnv.getClass(class_name);

        for (Variable v : curr_class.getVariables()) {
            localEnv.addVarsInScope(v);
        }
        return localEnv;
    }

    public static Environment buildLocalEnvironment(ClassExtendsDeclaration d, Environment env) {
        Environment localEnv = new Environment(env);
        String class_name = EnvironmentUtil.classname(d);
        VaporClass curr_class = localEnv.getClass(class_name);

        for (Variable v : curr_class.getVariables()) {
            localEnv.addVarsInScope(v);
        }
        return localEnv;
    }

    public static Environment buildLocalEnvironment(MethodDeclaration d, MethodType m, Environment env) {
        Environment localEnv = new Environment(env);
        for (Variable parameter : m.getParameters()) {
            // for a parameter the location is the name
            localEnv.addVarsInScope(parameter);
        }

        for (Node n : d.f7.nodes) {
            VarDeclaration v = (VarDeclaration) n;
            String var_name = EnvironmentUtil.identifierToString(v.f1);
            String var_type = EnvironmentUtil.syntaxTreeTypeToString(v.f0);
            localEnv.addVarsInScope(new Variable(var_name, var_type, Variable.SCOPE.LOCAL_VAR));
        }
        return localEnv;
    }


    public static void flattenSubtyping(Environment env) {
        Set<Map.Entry<String, VaporClass>> classes = env.getClasses();

        for (Map.Entry<String, VaporClass> pair : classes) {
            VaporClass cl = pair.getValue();
            getSuperClasses(cl);
            flattenInstanceVars(cl);
            evaluateOverridenMethods(cl);
        }
    }

    protected static void getSuperClasses(VaporClass cl) {
        VaporClass super_class = cl.getSuper();
        // non-leaf class
        if (null != super_class) {
            getSuperClasses(super_class);
            LinkedHashSet<VaporClass> hierarchy = new LinkedHashSet<VaporClass>();
            // add super classes hierarchy, then itself
            hierarchy.addAll(super_class.getHierarchy());
            hierarchy.add(super_class);
            cl.setHierarchy(hierarchy);
        }
    }

    public static void flattenInstanceVars(VaporClass cl) {

        VaporClass super_class = cl.getSuper();
        if (null != super_class) {
            LinkedHashSet<Variable> vars = new LinkedHashSet<Variable>();
            // recursively evaluate super classes instance vars
            flattenInstanceVars(super_class);
            // first layout all the super_classes instance vars
            vars.addAll(super_class.getVariables());
            // then local
            vars.addAll(cl.getVariables());
            cl.setInstanceVars(vars);
        }
    }

    public static void evaluateOverridenMethods(VaporClass cl) {
        VaporClass super_class = cl.getSuper();
        if (null != super_class) {
            LinkedHashMap<String, MethodType> methods = new LinkedHashMap<String, MethodType>();
            evaluateOverridenMethods(super_class);
            for (Map.Entry<String, MethodType> entry : super_class.getMethods()) {
                String method_name = entry.getKey();
                MethodType method = entry.getValue();
                if (null != cl.getMethod(method_name)) {
                    method = cl.getMethod(method_name);
                }
                methods.put(method_name, method);
            }

            for (Map.Entry<String, MethodType>  entry : cl.getMethods()) {
                String     method_name = entry.getKey();
                MethodType method      = entry.getValue();
                methods.put(method_name, method);
            }
            cl.setMethods(methods);
        }
    }
}