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
        return p.matches("^/(reel|reels|explore|tv)(/.*)?$");
    }
    public static boolean blocked(String raw, boolean focused) {
        return blocked(raw) || (focused && feed(raw));
    }
    public static boolean exempt(String raw) {
        String p = path(raw);
        return p.matches("^/(direct|accounts|challenge|checkpoint|two_factor)(/.*)?$");
    }
    public static boolean feed(String raw) { return path(raw).equals("/"); }
    public static boolean adHost(String host) {
        if (host == null) return false;
        host = host.toLowerCase(Locale.ROOT);
        for (String domain : new String[]{"doubleclick.net", "googlesyndication.com", "googleadservices.com"})
            if (host.equals(domain) || host.endsWith("." + domain)) return true;
        return false;
    }
    private Policy() { }
}
