import app.ataraxia.social.Budget;
public class BudgetTest {
 static int count;
 static void expect(boolean expected,long daily,long session,int posts,long cooldown,long now){
   count++;if(Budget.limited(daily,session,posts,15,5,10,cooldown,now)!=expected)throw new AssertionError("budget case "+count);
 }
 public static void main(String[]args){
   expect(false,0,0,0,0,1000);
   expect(false,899999,299999,9,0,1000);
   expect(true,900000,0,0,0,1000);
   expect(true,0,300000,0,0,1000);
   expect(true,0,0,10,0,1000);
   expect(true,0,0,0,2000,1000);
   expect(false,0,0,0,2000,2000);
   // Expired cooldown does not itself wipe persisted time/post counts.
   expect(true,0,300000,10,2000,3000);
   // Starting another session cannot erase the day's accumulated time.
   expect(true,900001,0,0,0,3000);
   System.out.println("PASS: "+count+" native budget boundary assertions");
 }
}
