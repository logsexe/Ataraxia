import app.ataraxia.social.Budget;
public class BudgetTest {
 static int count;
 static void expect(boolean expected,long daily,long session,long cooldown,long now){
   count++;if(Budget.limited(daily,session,15,5,cooldown,now)!=expected)throw new AssertionError("budget case "+count);
 }
 static void stale(boolean expected,long session,long last,long cooldown,long now,String why){
   count++;if(Budget.sessionStale(session,last,cooldown,now)!=expected)throw new AssertionError(why);
 }
 public static void main(String[]args){
   expect(false,0,0,0,1000);
   expect(false,899999,299999,0,1000);
   expect(true,900000,0,0,1000);
   expect(true,0,300000,0,1000);
   expect(true,0,0,2000,1000);
   expect(false,0,0,2000,2000);
   // Expired cooldown does not itself wipe persisted time usage.
   expect(true,0,300000,2000,3000);
   // Starting another session cannot erase the day's accumulated time.
   expect(true,900001,0,0,3000);
   long gap=Budget.SESSION_GAP_MS;
   // Short check-ins across the day must not add up into one session.
   stale(false,120000,1000,0,1000+gap-1,"short pause keeps the session");
   stale(true,120000,1000,0,1000+gap,"long pause ends the session");
   stale(false,0,1000,0,1000+gap*4,"nothing to reset");
   stale(false,300000,1000,5000,1000+gap,"a running break decides, not the idle rule");
   stale(false,120000,5000000,0,1000,"clock set backwards never ends a session early");
   stale(true,120000,0,0,System.currentTimeMillis(),"counters from older builds are stale");
   System.out.println("PASS: "+count+" native budget boundary assertions");
 }
}
