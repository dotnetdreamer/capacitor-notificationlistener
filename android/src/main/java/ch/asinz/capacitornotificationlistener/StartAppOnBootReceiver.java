package ch.asinz.capacitornotificationlistener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

public class StartAppOnBootReceiver extends BroadcastReceiver {
   private static final String TAG = StartAppOnBootReceiver.class.getSimpleName();

   @Override
   public void onReceive(Context context, Intent intent) {
      Toast.makeText(context, "BroadcastReceiver received: " + intent.getAction(), Toast.LENGTH_LONG).show();
      if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
              || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())) {
         Log.d(TAG, "Application restarting");

         PackageManager packageManager = context.getPackageManager();
         String packageName = context.getPackageName();
         Log.d(TAG, "Launching Application: " + packageName);

         try {
            //if there is lock screen, the app won't start when ACTION_LOCKED_BOOT_COMPLETED is received...
            try {
               Intent i = context.getPackageManager().getLaunchIntentForPackage(packageName);
               if(i == null) {
                  Toast.makeText(context, "Couldn't launch app. Intent is null", Toast.LENGTH_LONG).show();
                  return;
               }

               i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               context.startActivity(i);

               Toast.makeText(context, packageName + " started", Toast.LENGTH_LONG).show();
            } catch (Exception ex) {
               //can fail e.g if device has a lock screen. So ignore as it will be tried again by ACTION_BOOT_COMPLETED
               Log.e(TAG, ex.getLocalizedMessage(), ex);
            }

            //move app to background
            /*
               //Activity activity = (Activity) context;
               Intent bi = new Intent(Intent.ACTION_MAIN);
               bi.addCategory(Intent.CATEGORY_HOME);
               context.startActivity(bi);

               //activity.moveTaskToBack(true);
             */
         } catch (Exception ex) {
            Toast.makeText(context, ex.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, ex.getLocalizedMessage(), ex);
         }
      }
   }
}