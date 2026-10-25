package tools;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Command-line utility for generating BCrypt password hashes with cost factor 10.
 * <p>
 * Usage:
 * java -cp "build/web/WEB-INF/classes;lib/jbcrypt-0.4.jar" tools.PasswordHashTool "Password@123"
 */
public class PasswordHashTool {

    public static void main(String[] args) {
        if (args.length < 1 || args[0] == null || args[0].trim().isEmpty()) {
            System.err.println("Usage: java tools.PasswordHashTool <password>");
            System.exit(1);
        }
        String password = args[0];
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(10));
        System.out.println(hash);
    }
}
