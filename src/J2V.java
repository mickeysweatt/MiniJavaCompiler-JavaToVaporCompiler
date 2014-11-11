import code_generation.CodeGenerationUtil;
import code_generation.CodeGenerationVisitor;
import environment.Environment;
import environment.EnvironmentBuilderVisitor;
import environment.VaporClass;
import syntaxtree.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

public class J2V {
    public static void main(String [] args) {
            File testDir = new File("tests");
            MiniJavaParser parse = null;
            for (final File fileEntry : testDir.listFiles()) {
                if (fileEntry.isFile()) {
                    FileInputStream in = null;
                    try {
                        in = new FileInputStream(fileEntry.getAbsoluteFile());
                    }
                    catch (FileNotFoundException err) {
                        err.printStackTrace();
                    }

                    try {
                        System.out.println("Processing: " + fileEntry.getName());
                        if (null == parse) {
                            parse = new MiniJavaParser(in);
                        }
                        else {
                            parse.ReInit(in);
                        }
                        Goal g = MiniJavaParser.Goal();
                        // First build up the environment (All class, with vtable, and instance vars)
                        EnvironmentBuilderVisitor e = new EnvironmentBuilderVisitor();
                        Environment env = new Environment();

                        g.accept(e, env);
                        // Output static data
                        CodeGenerationUtil.outputVtables(env);
                        CodeGenerationVisitor cgen = new CodeGenerationVisitor();
                        g.accept(cgen, env);
                    }
                    catch (ParseException e) {
                        System.out.println(e.toString());
                    }
                }
            }
        }
}

