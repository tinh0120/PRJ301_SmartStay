package tools;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks that every function in web/WEB-INF/tld/smartstay.tld maps to a public static method of
 * util.ElFunctions with the same name, parameter types and return type.
 * Run from the SmartStay folder: java tools.TldConsistencyTest [path/to/smartstay.tld]
 */
public class TldConsistencyTest {

    public static void main(String[] args) throws Exception {
        String tldPath = args.length > 0 ? args[0] : "web/WEB-INF/tld/smartstay.tld";
        String tld = new String(Files.readAllBytes(Paths.get(tldPath)), StandardCharsets.UTF_8);
        Class<?> fn = Class.forName("util.ElFunctions");

        Pattern p = Pattern.compile("<function>.*?<name>\\s*(\\w+)\\s*</name>.*?<function-class>\\s*([\\w.]+)\\s*</function-class>"
                + ".*?<function-signature>\\s*([\\w.$\\[\\]]+)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*</function-signature>.*?</function>",
                Pattern.DOTALL);
        Matcher m = p.matcher(tld);
        int total = 0;
        int failed = 0;
        while (m.find()) {
            total++;
            String name = m.group(1);
            String cls = m.group(2);
            String ret = m.group(3);
            String method = m.group(4);
            String params = m.group(5).trim();
            String problem = null;
            if (!"util.ElFunctions".equals(cls)) {
                problem = "function-class is " + cls;
            } else {
                String[] types = params.isEmpty() ? new String[0] : params.split("\\s*,\\s*");
                Class<?>[] pt = new Class<?>[types.length];
                for (int i = 0; i < types.length; i++) {
                    pt[i] = load(types[i]);
                }
                try {
                    Method mt = fn.getMethod(method, pt);
                    if (!Modifier.isStatic(mt.getModifiers())) {
                        problem = "method is not static";
                    } else if (!load(ret).isAssignableFrom(mt.getReturnType())) {
                        problem = "return type " + mt.getReturnType().getName() + " does not match " + ret;
                    }
                } catch (NoSuchMethodException e) {
                    problem = "no public method " + method + "(" + params + ")";
                }
            }
            if (problem == null) {
                System.out.println("  [PASS] e:" + name);
            } else {
                System.err.println("  [FAIL] e:" + name + " - " + problem);
                failed++;
            }
        }
        System.out.println("RESULT: " + total + " TLD functions, " + (total - failed) + " matched, " + failed + " failed");
        if (failed > 0 || total == 0) {
            System.exit(1);
        }
    }

    private static Class<?> load(String type) throws ClassNotFoundException {
        switch (type) {
            case "int": return int.class;
            case "long": return long.class;
            case "boolean": return boolean.class;
            case "double": return double.class;
            default:
                try {
                    return Class.forName(type);
                } catch (ClassNotFoundException e) {
                    // Nested class written with a dot (util.Outer.Inner) -> util.Outer$Inner
                    int dot = type.lastIndexOf('.');
                    if (dot > 0) {
                        return Class.forName(type.substring(0, dot) + "$" + type.substring(dot + 1));
                    }
                    throw e;
                }
        }
    }
}
