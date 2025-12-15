package ba.woodcraft.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    public static String hashLozinke(String lozinka) {
        return BCrypt.hashpw(lozinka, BCrypt.gensalt());
    }

    public static boolean provjeriLozinku(String lozinka, String hash) {
        if (hash == null) return false;
        return BCrypt.checkpw(lozinka, hash);
    }
}
