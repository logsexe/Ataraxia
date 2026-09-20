import app.ataraxia.social.Policy;
public class PolicyTest {
    static int count=0;
    static void check(boolean value,String message){count++;if(!value)throw new AssertionError(message);}
    public static void main(String[]args){
        check(Policy.internal("https://www.instagram.com/direct/inbox/"),"inbox allowed");
        check(Policy.internal("https://instagram.com:443/"),"canonical port allowed");
        for(String u:new String[]{"http://instagram.com/","https://instagram.com.evil.test/","https://instagram.com@evil.test/","https://evil.test@instagram.com/","https://www.instagram.com:444/","javascript:alert(1)","file:///etc/passwd","intent://instagram.com/","https://evilinstagram.com/","https://instagram.com\\@evil.test/"})check(!Policy.internal(u),"reject "+u);
        for(String p:new String[]{"/reel/123/","/reels/","/explore/","/tv/abc/","/%72eels/","//reels/"})check(Policy.blocked("https://www.instagram.com"+p),"block "+p);
        check(!Policy.blocked("https://www.instagram.com/reels_are_bad/"),"do not match unrelated profiles");
        check(Policy.exempt("https://www.instagram.com/direct/t/123/"),"messages exempt");
        check(Policy.exempt("https://www.instagram.com/accounts/login/"),"login exempt");
        check(!Policy.exempt("https://www.instagram.com/director/"),"similar profile is not exempt");
        check(Policy.adHost("ads.doubleclick.net"),"ad subdomain blocked");
        check(!Policy.adHost("notdoubleclick.net"),"boundary required");
        check(!Policy.adHost("scontent.cdninstagram.com"),"media remains available");
        check(Policy.blocked("https://www.instagram.com/?test=1",true),"Focused blocks home feed with query");
        check(!Policy.blocked("https://www.instagram.com/",false),"Balanced allows home feed");
        check(!Policy.blocked("https://www.instagram.com/direct/inbox/",true),"Focused allows inbox");
        check(!Policy.blocked("https://www.instagram.com/movinglogs/",true),"Focused allows profile");
        check(Policy.blocked("https://www.instagram.com/reels/",true),"Focused blocks reels");
        check(Policy.blocked("https://www.instagram.com/reels/",false),"Balanced blocks reels");
        System.out.println("PASS: "+count+" navigation/security policy assertions");
    }
}
