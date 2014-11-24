package environment;


import syntaxtree.ClassDeclaration;
import syntaxtree.ClassExtendsDeclaration;
import syntaxtree.Goal;
import syntaxtree.MainClass;
import visitor.GJVoidDepthFirst;

/**
 * Author: Mickey Sweatt
 */
public class EnvironmentBuilderVisitor extends GJVoidDepthFirst<Environment> {
    public void visit(Goal g, Environment env) {
        g.f0.accept(this, env);
        g.f1.accept(this, env);
        EnvironmentBuilderUtil.flattenSubtyping(env);
    }

    public void visit(ClassDeclaration d, Environment env) {
        String class_name;
        VaporClass declared_class;

        class_name = EnvironmentUtil.classname(d);
        declared_class = env.getClass(class_name);
        if (null == declared_class) {
            declared_class = new VaporClass(class_name);
            env.addClass(declared_class);
        }
        EnvironmentBuilderUtil.addInstanceVariablesToClass(d.f3.nodes, declared_class);
        EnvironmentBuilderUtil.addMethodsToClass(d.f4.nodes, declared_class);
    }

    public void visit(MainClass m, Environment env) {
        String class_name;
        VaporClass declared_class;

        class_name = EnvironmentUtil.classname(m);
        declared_class = new VaporClass(class_name);
        MethodType main = new MethodType("void", "main");
        declared_class.addMethod(main, "main");
        env.addClass(declared_class);
        env.setMainClass(declared_class);
    }

    public void visit(ClassExtendsDeclaration d, Environment env) {
        String class_name, super_name;
        VaporClass declared_class, super_class;

        class_name = EnvironmentUtil.classname(d);
        super_name = EnvironmentUtil.syntaxTreeTypeToString(d.f3);
        declared_class = env.getClass(class_name);
        if (null == declared_class) {
            declared_class = new VaporClass(class_name);
            env.addClass(declared_class);
        }

        super_class = env.getClass(super_name);
        if (null == super_class) {
            super_class = new VaporClass(super_name);
            env.addClass(super_class);
        }
        declared_class.addSuper(super_class);
        EnvironmentBuilderUtil.addInstanceVariablesToClass(d.f5.nodes, declared_class);
        EnvironmentBuilderUtil.addMethodsToClass(d.f6.nodes, declared_class);
    }
}
