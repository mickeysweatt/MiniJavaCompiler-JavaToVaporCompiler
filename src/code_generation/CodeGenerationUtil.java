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
        return String.format("%s.VTable", c.getName());
    }
    public static void outputVtables(Environment env) {
        // TODO add subtyping
        for (Map.Entry<String, VaporClass> class_entry : env.getClasses().entrySet()) {
            System.out.println(" const " + vtableLabel(class_entry.getValue()));
            for (Map.Entry<String, MethodType> method_entry: class_entry.getValue().getMethods()) {
                System.out.println(String.format("\t:%s", method_entry.getValue().getLabel()));
            }
            System.out.println("");
        }
    }
    public static void prettyPrintMethod(String code) {
        // remove blank lines
        code = code.replaceAll("(?m)^[ \t]*\r?\n", "");
        int lvl = 1;
        // seperate the lines
        String[] lines = code.split(System.getProperty("line.separator"));

        System.out.println("\n" + lines[0]);
        for (int i = 1; i < lines.length; ++i)
        {
            String line = lines[i];
            // handle labels
            if (line.endsWith(":") || line.startsWith("if")) {
                System.out.println(line);
                // if end label, return code lvl 1
                if (line.contains("end")) {
                    lvl = 1;
                }
                // otherwise indent
                else {
                    lvl = 2;
                }
            } else {
                for (int j = 0; j < lvl; ++j) {
                    System.out.print("    ");
                }
                System.out.println(line);
            }
        }
    }
}
