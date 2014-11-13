package code_generation;

import environment.Environment;
import environment.MethodType;
import environment.VaporClass;

import java.util.Map;

/**
 * Created by admin on 11/10/14.
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
        }
    }
}
