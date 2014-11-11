package code_generation;

import environment.Environment;
import syntaxtree.ClassDeclaration;
import visitor.GJDepthFirst;

/**
 * Created by michael on 11/10/14.
 */
public class CodeGenerationVisitor extends GJDepthFirst<CodeTemporaryPair,Environment> {
    public CodeTemporaryPair visit(ClassDeclaration c, Environment env)
    {
        // visit the local methods
        return null;
    }
}
