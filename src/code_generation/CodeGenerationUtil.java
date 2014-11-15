package code_generation;

import environment.Environment;
import environment.MethodType;
import environment.VaporClass;

import java.util.Map;

/**
 * Author: Michael Sweatt
 */
public class CodeGenerationUtil {
    public static String vtableLabel(VaporClass c) {
        return String.format("const %s.VTable", c.getName());
    }
    public static void outputVtables(Environment env) {
        // TODO add subtyping
        for (Map.Entry<String, VaporClass> class_entry : env.getClasses().entrySet()) {
            System.out.println(vtableLabel(class_entry.getValue()));
            for (Map.Entry<String, MethodType> method_entry: class_entry.getValue().getMethods()) {
                System.out.println(String.format("\t:%s", method_entry.getValue().getLabel()));
            }
            System.out.println("");
        }
    }
    public static void prettyPrintMethod(String code) {
        // remove blank lines
        code = code.replaceAll("(?m)^[ \t]*\r?\n", "");

        // seperate the lines
        String[] lines = code.split(System.getProperty("line.separator"));

        System.out.println("\n" + lines[0]);
        for (int i = 1; i < lines.length; ++i)
        {
            System.out.println("\t" + lines[i]);
        }
    }
}
