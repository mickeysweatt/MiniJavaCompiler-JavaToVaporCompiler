package environment;

import syntaxtree.*;

import java.util.LinkedList;
import java.util.Vector;

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
            MethodType curr_method = new MethodType(getVariableList(methodNode.f4.node));
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

    public static Environment buildLocalEnvironment(ClassDeclaration d, Environment env) {
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
            localEnv.addVarsInScope(new Variable(var_name, var_name, Variable.SCOPE.LOCAL_VAR));
        }
        return localEnv;
    }
}


/*
    public static void flattenSubtyping(GlobalEnvironment env) {
        Map<String, ClassType> classes = env.getClasses();

        for (Map.Entry<String, ClassType> pair : classes.entrySet()) {
            ClassType cl = pair.getValue();
            getSuperClasses(cl, null);
        }
        evaluateOverridenMethods(env);
        flattenInstanceVars(env);
    }

    public static void flattenInstanceVars(GlobalEnvironment env) {
        Map<String, ClassType> classes = env.getClasses();
        // for each class
        for (Map.Entry<String, ClassType> pair : classes.entrySet()) {
            ClassType cl = pair.getValue();
            if (null == cl.getSuperClasses()) {
                continue;
            }
            // for each super class
            for (ClassType sup : cl.getSuperClasses()) {
                if (null == sup.getInstanceVariables()) {
                    continue;
                }
                // get each variable
                for (VarType v : sup.getInstanceVariables()) {
                    if (!cl.containsInstanceVar(v)) {
                        cl.addInstanceVar(v);
                    }
                    // to support shadowing this will use the most derived definition of any variable
                }
            }
        }
    }

    public static void evaluateOverridenMethods(GlobalEnvironment env)
    {
        Map<String, ClassType> classes = env.getClasses();
        for (Map.Entry<String, ClassType> pair : classes.entrySet()) {
            ClassType cl = pair.getValue();
            if (null ==  cl.getSuperClasses())
            {
                continue;
            }
            for (ClassType sup : cl.getSuperClasses()) {
                if (null ==  cl.getMethods())
                {
                    continue;
                }
                for (MethodType method : sup.getMethods()) {

                    String method_name = method.typeName();
                    if (cl.containsMethod(method_name))
                    {
                        // check for overloading
                        LinkedList<VarType> subclass_params   =  cl.getMethod(method_name).getParameterList();
                        LinkedList<VarType> superclass_params =  method.getParameterList();
                        Type subclass_retVal   =  cl.getMethod(method_name).getReturnType();
                        Type superclass_retVal =  method.getReturnType();
                        if (!subclass_retVal.equals(superclass_retVal)) {
                            TypeError.close("No overloading allowed");
                        }
                        if (null == superclass_params || null == subclass_params ||
                            (subclass_params.size() != superclass_params.size()))
                        {
                            if (subclass_params != superclass_params)
                            {
                                TypeError.close("No overloading allowed");
                            }
                        }

                        Iterator<VarType> superclass_paramIter = superclass_params.iterator();
                        Iterator<VarType> subclass_paramIter   = subclass_params.iterator();
                        while (subclass_paramIter.hasNext() && superclass_paramIter.hasNext())
                        {
                            VarType super_param = superclass_paramIter.next();
                            VarType sub_param   = subclass_paramIter.next();
                            if (!sub_param.typeName().equals(super_param.typeName()))
                            {
                                TypeError.close("No overloading allowed");
                            }
                        }
                    }
                    // if this is just inherited, put it into scope
                    else
                    {
                        cl.addMethod(method);
                    }
                }
            }
        }
    }


    private static void getSuperClasses(ClassType c, Set<ClassType> visited)
    {
        if (null == visited)
        {
            visited = new HashSet<ClassType>();
        }
        else if (visited.contains(c))
        {
            TypeError.close("Cycle detected in class hierarchy.");
        }
        visited.add(c);
        // get first value out of the super classes (should only be one if any at this point)
        Set<ClassType> supList = c.getSuperClasses();

        if (null != supList)
        {
            // get direct super
            ClassType sup = supList.iterator().next();
            // fill out super classes of direct super
            getSuperClasses(sup, visited);
            supList = sup.getSuperClasses();
            if (supList != null) {
                c.getSuperClasses().addAll(sup.getSuperClasses());
            }
        }
    }

    // expects the class type of the class where the method resides
    public static boolean addInstanceVariablesToClass(NodeListOptional varList, ClassType curr_class, GlobalEnvironment env)
    {
        for ( Enumeration<Node> e = varList.elements(); e.hasMoreElements(); ) {
            VarType instanceVar = EnvironmentUtil.vardecl((VarDeclaration) e.nextElement(), env);
            instanceVar.setScope(curr_class);
            if (curr_class.containsInstanceVar(instanceVar))
            {
                close("Redefining instance variable " + instanceVar.variableName() +
                        " in class " + curr_class.getClassName());
            }
            curr_class.addInstanceVar(instanceVar);
        }

        return true;
    }

    public static boolean addMethodToClass(MethodDeclaration m, ClassType curr_class, GlobalEnvironment env)
    {
        String method_name = EnvironmentUtil.methodname(m);
        Type returnType = EnvironmentUtil.SyntaxTreeTypeToEnvironmentType(m.f1.f0.choice, env);
        if (curr_class.containsMethod(method_name)) {
            close("Redefining method " + method_name + " in class " + curr_class.getClassName());
            return false;
        }
        else {
            MethodType method;
            method = new MethodType(method_name, returnType, curr_class, null);
            getVariableList(m.f4.node, method, env);
            HashSet<VarType> localVars = method.getLocalVars();
            if (null == localVars)
            {
                localVars = new HashSet<VarType>();
            }
            getVariableList(m.f7.nodes, localVars, env);
            curr_class.addMethod(method);
        }
        return true;
    }

    public static void getVariableList(Node parameter, MethodType m, GlobalEnvironment env)
    {
        if (null != parameter && parameter instanceof FormalParameterRest)
        {
            getVariableList(((FormalParameterRest) parameter).f1, m, env);
        }

        else if (null != parameter && parameter instanceof FormalParameter)
        {
            FormalParameter fp = (FormalParameter) parameter;
            environment.Type parameterType = EnvironmentUtil.SyntaxTreeTypeToEnvironmentType(fp.f0.f0.choice, env);
            String parameterName = fp.f1.f0.toString();
            VarType parameter_type = new VarType(parameterType, parameterName, m);
            if (!m.containsParameter(parameter_type)) {
                m.addParameter(parameter_type);
            } else {
                close("Redeclaring parameter " + parameterName + " in " + m.typeName());
            }
        }
        else if (null != parameter && parameter instanceof FormalParameterList)
        {
            FormalParameterList pl = (FormalParameterList) parameter;
            getVariableList(pl.f0, m, env);
            for (Node n : pl.f1.nodes)
            {
                getVariableList(n, m, env);
            }
        }
    }

    public static void getVariableList(Vector<Node> variableDecls, Collection<VarType> localVars, GlobalEnvironment env)
    {
        for (Node node : variableDecls)
        {
            VarDeclaration decl = (VarDeclaration) node;
            Type varType = EnvironmentUtil.SyntaxTreeTypeToEnvironmentType(decl.f0.f0.choice, env);
            String varName = EnvironmentUtil.identifierToString(decl.f1);
            VarType vt = new VarType(varType, varName, null);
            if (null == localVars || !localVars.contains(vt))
            {
                localVars.add(vt);
            }
            else
            {
                close("Redeclaring parameter");
            }
        }
    }

    public static void getVariableList(Vector<Node> variableDecls, Environment localVars, GlobalEnvironment env)
    {
        for (Node node : variableDecls)
        {
            VarDeclaration decl = (VarDeclaration) node;
            Type varType = EnvironmentUtil.SyntaxTreeTypeToEnvironmentType(decl.f0.f0.choice, env);
            String varName = EnvironmentUtil.identifierToString(decl.f1);
            VarType vt = new VarType(varType, varName, null);
            if (!localVars.containsEntry(vt.variableName()))
            {
                localVars.addEntry(vt);
            }
            else
            {
                close("Redeclaring parameter");
            }
        }
    }

    public static ScopedEnvironment buildLocalEnvironment(MainClass mainClassDeclaration, Environment env)
    {
        GlobalEnvironment g_env = (GlobalEnvironment) env;
        String class_name = EnvironmentUtil.identifierToString(mainClassDeclaration.f1);
        ClassType main_class = g_env.getClass(class_name);
        MethodType main_method = main_class.getMethod("main");
        ScopedEnvironment curr_env = new ScopedEnvironment(g_env, main_method);
        // add variables declared in main function
        getVariableList(mainClassDeclaration.f14.nodes, curr_env.getLocalVariables(), g_env);
        // add parameters to local variables
        LinkedList<VarType> parameterList = main_method.getParameterList();
        if (null != parameterList) {
            for( VarType v : parameterList)
            {
                curr_env.addLocalVariable(v);
            }
        }
        return curr_env;
    }


    public static ScopedEnvironment buildLocalEnvironment(ClassDeclaration classDeclaration, Environment env)
    {
        GlobalEnvironment g_env = (GlobalEnvironment) env;
        String class_name = EnvironmentUtil.identifierToString(classDeclaration.f1);
        ClassType curr_class = g_env.getClass(class_name);
        return new ScopedEnvironment(g_env, curr_class);
    }

    public static ScopedEnvironment buildLocalEnvironment(ClassExtendsDeclaration classDeclaration, Environment env)
    {
        GlobalEnvironment g_env = (GlobalEnvironment) env;
        String class_name = EnvironmentUtil.identifierToString(classDeclaration.f1);
        ClassType curr_class = g_env.getClass(class_name);
        return new ScopedEnvironment(g_env, curr_class);
    }

    public static ScopedEnvironment  buildLocalEnvironment(MethodDeclaration methodDeclaration, Environment env)
    {
        GlobalEnvironment g_env = env.getGlobalEnvironment();
        String method_name = EnvironmentUtil.identifierToString(methodDeclaration.f2);
        ClassType scoping_class = (ClassType)((ScopedEnvironment)env).getScope();
        MethodType curr_method = scoping_class.getMethod(method_name);
        ScopedEnvironment curr_env = new ScopedEnvironment(g_env, curr_method);
        getVariableList(methodDeclaration.f7.nodes, curr_env.getLocalVariables(), g_env);

        // add parameters to local variables
        LinkedList<VarType> parameterList = curr_method.getParameterList();
        if (null != parameterList) {
            for( VarType v : parameterList)
            {
                curr_env.addLocalVariable(v);
            }
        }

        return curr_env;
    }
}


*/