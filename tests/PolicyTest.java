import app.ataraxia.social.Policy;
public class PolicyTest {
    static int count=0;
    static void check(boolean value,String message){count++;if(!value)throw new AssertionError(message);}
    public static void main(String[]args){
        check(Policy.internal("https://www.instagram.com/direct/inbox/"),"inbox allowed");
        check(Policy.internal("https://instagram.com:443/"),"canonical port allowed");
        for(String u:new String[]{"http://instagram.com/","https://instagram.com.evil.test/","https://instagram.com@evil.test/","https://evil.test@instagram.com/","https://www.instagram.com:444/","javascript:alert(1)","file:///etc/passwd","intent://instagram.com/","https://evilinstagram.com/","https://instagram.com\\@evil.test/"})check(!Policy.internal(u),"reject "+u);
        for(String p:new String[]{"/reel/123/","/reels/","/explore/","/tv/abc/","/%72eels/","//reels/","/shop/","/shopping/item","/live/","/live/broadcast/","/maya/live/","/maya/live"})check(Policy.blocked("https://www.instagram.com"+p),"block "+p);
        check(!Policy.blocked("https://www.instagram.com/reels_are_bad/"),"do not match unrelated profiles");
        check(!Policy.blocked("https://www.instagram.com/liver/"),"do not match a profile named liver");
        check(!Policy.blocked("https://www.instagram.com/shopfront/"),"do not match a profile named shopfront");
        check(!Policy.blocked("https://www.instagram.com/p/live/"),"a post is not the live route");
        check(!Policy.blocked("https://www.instagram.com/direct/inbox/"),"inbox is not a blocked route");
        check(Policy.exempt("https://www.instagram.com/direct/t/123/"),"messages exempt");
        check(Policy.exempt("https://www.instagram.com/accounts/login/"),"login exempt");
        check(!Policy.exempt("https://www.instagram.com/director/"),"similar profile is not exempt");
        check(Policy.adHost("ads.doubleclick.net"),"ad subdomain blocked");
        check(!Policy.adHost("notdoubleclick.net"),"boundary required");
        check(!Policy.adHost("scontent.cdninstagram.com"),"media remains available");
        check("maya".equals(Policy.profileName("https://www.instagram.com/maya/")),"profile name");
        check("ada.b".equals(Policy.profileName("https://www.instagram.com/Ada.b")),"profile case folds");
        check(Policy.profileName("https://www.instagram.com/reels/") == null,"reels tab is not a profile");
        check(Policy.profileName("https://www.instagram.com/p/abc/") == null,"a post is not a profile");
        check(Policy.profileName("https://www.instagram.com/explore/") == null,"explore is not a profile");
        check("maya".equals(Policy.profileName("https://www.instagram.com/maya/reels/")),"profile reels tab still names the person");
        check(Policy.profileName("https://www.instagram.com/maya/live/") == null,"a live path is not a filing page");
        System.out.println("PASS: "+count+" navigation/security policy assertions");
    }
}
