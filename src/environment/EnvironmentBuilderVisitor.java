package environment;


import syntaxtree.ClassDeclaration;
import syntaxtree.MainClass;
import visitor.GJVoidDepthFirst;

/**
 * Author: Mickey Sweatt
 */
public class EnvironmentBuilderVisitor extends GJVoidDepthFirst<Environment> {

    public void visit(ClassDeclaration d, Environment env) {
        String class_name;
        VaporClass declared_class;

        class_name = EnvironmentUtil.classname(d);
        declared_class = new VaporClass(class_name);

        EnvironmentBuilderUtil.addInstanceVariablesToClass(d.f3.nodes, declared_class);
        EnvironmentBuilderUtil.addMethodsToClass(d.f4.nodes, declared_class);

        env.addClass(declared_class);

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

}
