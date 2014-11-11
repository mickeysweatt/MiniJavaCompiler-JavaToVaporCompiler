package environment;


import visitor.GJDepthFirst;
import visitor.GJVoidDepthFirst;
import syntaxtree.*;

/**
 * Created by michael on 11/9/14.
 */
public class EnvironmentBuilderVisitor extends GJVoidDepthFirst<Environment>{

    public void visit(ClassDeclaration d, Environment env) {
        String class_name;
        VaporClass declared_class;

        class_name = EnvironmentUtil.classname(d);
        declared_class = new VaporClass(class_name);

        EnvironmentBuilderUtil.addInstanceVariablesToClass(d.f3.nodes, declared_class);
        EnvironmentBuilderUtil.addMethodsToClass(d.f4.nodes, declared_class);

        env.addClass(declared_class);

    }

}
