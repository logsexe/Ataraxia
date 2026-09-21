package app.ataraxia.social;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Portable settings only. No cookies, credentials, counters or cooldown fields. */
public final class SettingsBackup {
    public static final int MAX_BYTES=16384;
    public final boolean focused;
    public final int posts,sessionMinutes,dailyMinutes;
    public final SortedSet<String> profiles;
    public SettingsBackup(boolean focused,int posts,int sessionMinutes,int dailyMinutes,Collection<String> profiles) {
        this.focused=focused;
        this.posts=allowed(posts,5,10,15,20);
        this.sessionMinutes=allowed(sessionMinutes,2,5,10);
        this.dailyMinutes=allowed(dailyMinutes,5,15,30,60);
        if(profiles==null || profiles.size()>20) throw new IllegalArgumentException("Invalid profile count");
        TreeSet<String> clean=new TreeSet<>();
        for(String raw:profiles) {
            String name=Policy.profileName(raw);
            if(name==null || !clean.add(name)) throw new IllegalArgumentException("Invalid or duplicate profile");
        }
        this.profiles=Collections.unmodifiableSortedSet(clean);
    }
    private static int allowed(int value,int... options) {
        for(int option:options) if(value==option)return value;
        throw new IllegalArgumentException("Unsupported boundary value");
    }
    public byte[] encode() {
        StringBuilder text=new StringBuilder("# Ataraxia settings only; saved usernames are not encrypted.\nformat=ataraxia-settings-v1\n");
        text.append("focused=").append(focused).append("\nposts=").append(posts)
            .append("\nsessionMinutes=").append(sessionMinutes).append("\ndailyMinutes=").append(dailyMinutes)
            .append("\nprofiles=").append(String.join(",",profiles)).append('\n');
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }
    public static SettingsBackup decode(InputStream input) throws IOException {
        if(input==null) throw new IOException("No input");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        byte[] buffer=new byte[1024];int count;
        while((count=input.read(buffer))!=-1) {
            if(bytes.size()+count>MAX_BYTES) throw new IOException("Backup too large");
            bytes.write(buffer,0,count);
        }
        Properties values=new Properties() {
            @Override public synchronized Object put(Object key,Object value) {
                if(containsKey(key)) throw new IllegalArgumentException("Duplicate setting");
                return super.put(key,value);
            }
        };
        values.load(new StringReader(bytes.toString(StandardCharsets.UTF_8.name())));
        Set<String> keys=new HashSet<>(Arrays.asList("format","focused","posts","sessionMinutes","dailyMinutes","profiles"));
        if(!values.stringPropertyNames().equals(keys) || !"ataraxia-settings-v1".equals(values.getProperty("format")))
            throw new IllegalArgumentException("Unsupported settings file");
        String focused=values.getProperty("focused");
        if(!"true".equals(focused) && !"false".equals(focused)) throw new IllegalArgumentException("Invalid mode");
        String names=values.getProperty("profiles");
        return new SettingsBackup(Boolean.parseBoolean(focused),Integer.parseInt(values.getProperty("posts")),
            Integer.parseInt(values.getProperty("sessionMinutes")),Integer.parseInt(values.getProperty("dailyMinutes")),
            names.isEmpty()?Collections.emptyList():Arrays.asList(names.split(",",-1)));
    }
}
