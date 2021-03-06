package ch.asinz.capacitornotificationlistener;

import android.app.Notification;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.getcapacitor.JSArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class NotificationService extends NotificationListenerService {
    public static final String ACTION_RECEIVE      = "ch.asinz.P8WatchApp.NOTIFICATION_RECEIVE_EVENT";
    public static final String ACTION_REMOVE      = "ch.asinz.P8WatchApp.NOTIFICATION_REMOVE_EVENT";

    public static final String ARG_KEY= "notification_event_key";
    public static final String ARG_PACKAGE = "notification_event_package";
    public static final String ARG_TITLE = "notification_event_title";
    public static final String ARG_APPTITLE = "notification_event_apptitle";
    public static final String ARG_TEXT = "notification_event_text";
    public static final String ARG_TEXTLINES = "notification_event_textlines";
    public static final String ARG_TIME = " notification_event_time";

    public static boolean isConnected = false;
    public static List<String> blackListOfPackages;
    public static JSArray blackListOfText;
    private static final String TAG = NotificationService.class.getSimpleName();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "onNotificationPosted");

        Intent i = notificationToIntent(sbn, ACTION_RECEIVE);
        sendBroadcast(i);
        Log.d(TAG, "onNotificationPosted: sendBroadcast");
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "onNotificationRemoved");

        Intent i = notificationToIntent(sbn, ACTION_REMOVE);
        sendBroadcast(i);
        Log.d(TAG, "onNotificationRemoved: sendBroadcast");
    }

    @Override
    public void onListenerConnected() {
        isConnected = true;
    }

    @Override
    public void onListenerDisconnected() {
        isConnected = false;
    }

    private Intent notificationToIntent(StatusBarNotification sbn, String action) {
        Intent i = new Intent(action);
        Notification n = sbn.getNotification();

        String k = sbn.getKey();
        i.putExtra(ARG_KEY, k);

        CharSequence pkg =  sbn.getPackageName();
        i.putExtra(ARG_PACKAGE, charSequenceToString(pkg));

        CharSequence title =  n.tickerText;
        i.putExtra(ARG_TITLE, charSequenceToString(title));

        CharSequence text =  n.extras.getCharSequence("android.text");
        String txtStr = charSequenceToString(text);
        i.putExtra(ARG_TEXT, txtStr);

        CharSequence[] textlines =  n.extras.getCharSequenceArray("android.textLines");
        String[] textlinesStr = charSequenceArrayToStringArray(textlines);
        i.putExtra(ARG_TEXTLINES, textlinesStr);

        CharSequence apptitle  = n.extras.getCharSequence("android.title");
        String titleStr = charSequenceToString(apptitle);
        i.putExtra(ARG_APPTITLE, titleStr);

        i.putExtra(ARG_TIME, n.when);

        //Don't bubble up...
        //blackListOfPackages = Arrays.asList(new String[] { "com.microsoft.office.outlook" });
        if(NotificationService.blackListOfPackages != null
                && NotificationService.blackListOfPackages.contains(pkg)) {
            Log.d(TAG, "Cancelling for package:" + pkg);
            this.cancelNotification(k);
        }

        if(NotificationService.blackListOfText != null) {
            boolean found = false;
            for (int j = 0; j < NotificationService.blackListOfText.length(); j++) {
                try {
                    JSONObject item = NotificationService.blackListOfText.getJSONObject(j);
                    String rule = item.getString("rule");
                    String value = item.getString("value");
                    String appPackage = item.getString("package");
                    if(rule.equals("contains") && txtStr.contains(value) && appPackage.equals(pkg)) {
                        found = true;
                        break;
                    } else if(rule.equals("startsWith") && txtStr.contains(value) && appPackage.equals(pkg)) {
                        found = true;
                        break;
                    } else if(rule.equals("exact") && txtStr.contains(value) && appPackage.equals(pkg)) {
                        found = true;
                        break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if(found) {
                Log.d(TAG, "Cancelling for package:" + pkg);
                this.cancelNotification(k);
            }
        }
        return i;
    }

    private String charSequenceToString(CharSequence c) {
        return  (c == null ) ? "" : String.valueOf(c);
    }

    private String[] charSequenceArrayToStringArray(CharSequence[] c) {
        if (c == null) return new String[0];
        String[] out = new String[c.length];
        for (int i = 0; i < c.length; i++) {
            Log.d(TAG, String.valueOf(c[i]));
            out[i] = charSequenceToString(c[i]);
        }
        return out;
    }

}
