package app.ataraxia.social;
/** Native time limits do not depend on webpage scripts or Instagram markup. */
public final class Budget {
    public static boolean limited(long dailyMs,long sessionMs,int dailyMinutes,int sessionMinutes,long cooldownUntil,long now) {
        return dailyMs>=dailyMinutes*60000L || sessionMs>=sessionMinutes*60000L
            || cooldownUntil>now;
    }
    /** Away from counted browsing this long and the next visit starts a new session. */
    public static final long SESSION_GAP_MS = 30 * 60000L;
    /**
     * A session with usage ends after a long pause. A running break decides on its own, and a clock
     * set backwards (now before lastActive) never ends a session early. lastActive 0 means the counter
     * predates this rule (alpha11 and older) and is treated as stale.
     */
    public static boolean sessionStale(long sessionMs,long lastActive,long cooldownUntil,long now) {
        return cooldownUntil==0 && sessionMs>0 && now>=lastActive && now-lastActive>=SESSION_GAP_MS;
    }
    private Budget(){}
}
