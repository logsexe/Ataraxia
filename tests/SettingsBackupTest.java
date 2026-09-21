import app.ataraxia.social.SettingsBackup;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public class SettingsBackupTest {
    static int checks;
    static void check(boolean ok){checks++;if(!ok)throw new AssertionError("backup case "+checks);}
    static SettingsBackup read(String s) throws Exception{return SettingsBackup.decode(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));}
    static void reject(String s) throws Exception{
        boolean failed=false;try{read(s);}catch(IllegalArgumentException|IOException e){failed=true;}check(failed);
    }
    public static void main(String[] args)throws Exception {
        SettingsBackup source=new SettingsBackup(false,15,10,30,Arrays.asList("@MovingLogs","fuelledbylogs"));
        String text=new String(source.encode(),StandardCharsets.UTF_8);
        SettingsBackup restored=read(text);
        check(!restored.focused && restored.posts==15 && restored.sessionMinutes==10 && restored.dailyMinutes==30);
        check(restored.profiles.equals(new TreeSet<>(Arrays.asList("movinglogs","fuelledbylogs"))));
        check(Arrays.equals(source.encode(),restored.encode()));
        check(!text.contains("cookie") && !text.contains("dailyMs") && !text.contains("cooldown") && !text.contains("seen="));
        reject(text+"cooldown=0\n");reject(text+"focused=true\n");reject(text.replace("focused=false","focused=maybe"));
        reject(text.replace("posts=15","posts=999"));reject(text.replace("sessionMinutes=10","sessionMinutes=-1"));
        reject(text.replace("dailyMinutes=30","dailyMinutes=999999999999999999"));
        reject(text.replace("v1","v2"));reject(text.replace("movinglogs","../reels"));
        reject(text.replace("fuelledbylogs,movinglogs","movinglogs,MovingLogs"));
        reject(text.replace("fuelledbylogs,movinglogs","a,"));
        reject(text.replace("fuelledbylogs,movinglogs",String.join(",",Collections.nCopies(21,"a"))));
        reject("#"+"a".repeat(SettingsBackup.MAX_BYTES));reject("");
        SettingsBackup empty=read(new String(new SettingsBackup(true,5,2,5,Collections.emptyList()).encode(),StandardCharsets.UTF_8));
        check(empty.focused && empty.profiles.isEmpty());
        try{restored.profiles.add("test");throw new AssertionError("mutable profiles");}catch(UnsupportedOperationException expected){checks++;}
        System.out.println("PASS: "+checks+" backup round-trip, bounded input and hostile input assertions");
    }
}
