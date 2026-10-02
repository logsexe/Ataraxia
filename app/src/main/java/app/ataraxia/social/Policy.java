package app.ataraxia.social;
import java.net.URI;
import java.util.Locale;

/** Pure policy: no Android dependencies, exercised by the local JVM tests. */
public final class Policy {
    public static boolean internal(String raw) {
        try {
            URI u = new URI(raw);
            String host = u.getHost();
            return "https".equalsIgnoreCase(u.getScheme()) && u.getRawUserInfo() == null
                && (u.getPort() == -1 || u.getPort() == 443)
                && ("instagram.com".equalsIgnoreCase(host) || "www.instagram.com".equalsIgnoreCase(host));
        } catch (Exception e) { return false; }
    }
    public static String path(String raw) {
        try { return new URI(raw).getPath().toLowerCase(Locale.ROOT).replaceAll("/+", "/"); }
        catch (Exception e) { return "/"; }
    }
    public static boolean blocked(String raw) {
        String p = path(raw);
        if (p.matches("^/(reel|reels|explore|tv|shop|shopping|live)(/.*)?$")) return true;
        // A live broadcast on a profile. Post, inbox, and login paths are not live.
        return p.matches("^/(?!(p|reel|reels|explore|tv|direct|accounts|stories)/)[^/]+/live(/.*)?$");
    }
    public static boolean exempt(String raw) {
        String p = path(raw);
        return p.matches("^/(direct|accounts|challenge|checkpoint|two_factor)(/.*)?$");
    }
    public static boolean feed(String raw) { return path(raw).equals("/"); }
    /** A profile path, or null. Reserved sections such as /reel/ and /explore/ are not profiles. */
    public static String profileName(String raw) {
        String p = path(raw);
        if (p == null || !p.matches("^/[a-z0-9._]{1,30}(/(reels|tagged|saved)/?)?/?$")) return null;
        String name = p.substring(1);
        int slash = name.indexOf('/');
        if (slash >= 0) name = name.substring(0, slash);
        if (name.matches("^(p|reel|reels|explore|tv|shop|shopping|live|direct|accounts|stories|about|legal|privacy)$"))
            return null;
        return name;
    }
    public static boolean adHost(String host) {
        if (host == null) return false;
        host = host.toLowerCase(Locale.ROOT);
        for (String domain : new String[]{"doubleclick.net", "googlesyndication.com", "googleadservices.com"})
            if (host.equals(domain) || host.endsWith("." + domain)) return true;
        return false;
    }
    private Policy() { }
}
