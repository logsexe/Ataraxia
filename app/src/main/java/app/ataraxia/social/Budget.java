package app.ataraxia.social;
/** Native time limits do not depend on webpage scripts or Instagram markup. */
public final class Budget {
    public static boolean limited(long dailyMs,long sessionMs,int dailyMinutes,int sessionMinutes,long cooldownUntil,long now) {
        return dailyMs>=dailyMinutes*60000L || sessionMs>=sessionMinutes*60000L
            || cooldownUntil>now;
    }
    private Budget(){}
}
