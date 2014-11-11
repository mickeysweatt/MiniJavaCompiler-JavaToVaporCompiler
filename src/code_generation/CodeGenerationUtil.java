package code_generation;

import environment.Environment;
import environment.MethodType;
import environment.VaporClass;

import java.util.Map;

/**
 * Created by admin on 11/10/14.
 */
public class CodeGenerationUtil {
    public static void outputVtables(Environment env) {
        // TODO add subtyping
        for (Map.Entry<String, VaporClass> class_entry : env.getClasses().entrySet()) {
            System.out.println(String.format("const %s.VTable", class_entry.getKey()));
            for (Map.Entry<String, MethodType> method_entry: class_entry.getValue().getMethods()) {
                System.out.println(String.format(":%s", method_entry.getValue().getLabel()));
            }
        }
    }
}
