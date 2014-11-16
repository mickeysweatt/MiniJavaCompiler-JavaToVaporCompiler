package environment; /**
 * Author: Mickey Sweatt
 */

import syntaxtree.*;

public class EnvironmentUtil {
    public static String classname(Node n) {
        if (n instanceof MainClass) {
            return identifierToString(((MainClass) n).f1);
        } else if (n instanceof ClassDeclaration) {
            return identifierToString(((ClassDeclaration) n).f1);
        } else if (n instanceof ClassExtendsDeclaration) {
            return identifierToString((((ClassExtendsDeclaration) n)).f1);
        } else {
            System.out.println("classname method called with incorrect parameter!");
            System.exit(-1);
            return null;
        }
    }

    public static String methodname(MethodDeclaration m) {
        return identifierToString(m.f2);
    }

    public static String identifierToString(Identifier id) {
        return id.f0.toString();
    }

    public static String syntaxTreeTypeToString(Node n) {
        if (n instanceof Type) {
            Type t = (Type) n;
            return syntaxTreeTypeToString(t.f0.choice);
        }
        if (n instanceof IntegerType) {
            return "int";
        } else if (n instanceof BooleanType) {
            return "boolean";
        } else if (n instanceof Identifier) {
            Identifier id = (Identifier) n;
            return identifierToString(id);
        } else {
            System.err.println("syntaxTreeTypeToString called with unrecongized type");
            return null;
        }
    }

    public static String nodeTokenToString(NodeToken t) {
        return t.tokenImage;
    }

}
