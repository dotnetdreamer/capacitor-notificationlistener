package ch.asinz.capacitornotificationlistener;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.util.Log;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@NativePlugin(
        permissions = {
                Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE
        }
)
public class NotificationListenerPlugin extends Plugin {
    private static final String TAG = NotificationListenerPlugin.class.getSimpleName();
    private static final String EVENT_NOTIFICATION_REMOVED = "notificationRemovedEvent";
    private static final String EVENT_NOTIFICATION_RECEIVED = "notificationReceivedEvent";

    private NotificationReceiver notificationreceiver;

    @PluginMethod()
    public void startListening(PluginCall call) {
        if (notificationreceiver != null) {
            call.success();
            Log.d(TAG, "NotificationReceiver already exists");
            return;
        }
        Log.d(TAG, "NotificationReceiver NotificationReceiver()");
        notificationreceiver = new NotificationReceiver();
        Log.d(TAG, "NotificationReceiver IntentFilter()");
        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationService.ACTION_RECEIVE);
        Log.d(TAG, "NotificationReceiver ACTION_REMOVE");
        filter.addAction(NotificationService.ACTION_REMOVE);
        Log.d(TAG, "NotificationReceiver registerReceiver()");
        getContext().registerReceiver(notificationreceiver, filter);
        Log.d(TAG, "NotificationReceiver Registered");
        call.success();
    }


    @PluginMethod
    public void hasPermission(PluginCall call) {
        ComponentName cn = new ComponentName(getContext(), NotificationService.class);
        String flat = Settings.Secure.getString(getContext().getContentResolver()
            , "enabled_notification_listeners");
        final boolean enabled = flat != null && flat.contains(cn.flattenToString());
        JSObject ret = new JSObject();
        ret.put("value",  enabled);
        call.resolve(ret);
    }

    @PluginMethod
    public void requestPermission(PluginCall call) {
        startActivityForResult(call, new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), 0);
        call.success();
    }

    @PluginMethod
    public void isListening(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("value",  NotificationService.isConnected);
        call.resolve(ret);
    }

    @PluginMethod()
    public void stopListening(PluginCall call) {
        NotificationService.blackListOfText = null;
        NotificationService.blackListOfPackages = null;

        if (notificationreceiver == null) {
            call.success();
            return;
        }
        getContext().unregisterReceiver(notificationreceiver);
        call.success();
    }


    @Override
    protected void handleOnStop() {
        super.handleOnStop();

        Log.d(TAG, "handleOnStop");
        if (notificationreceiver == null) {
            return;
        }

        NotificationService.blackListOfText = null;
        NotificationService.blackListOfPackages = null;
        getContext().unregisterReceiver(notificationreceiver);
    }

    @PluginMethod()
    public void setBlackList(PluginCall call) {
        JSArray bpArray = call.getArray("blackListOfPackages");
        JSArray bpTxtArray = call.getArray("blackListOfText");

        if(bpArray == null && bpTxtArray == null) {
            call.reject("You must set blackListOfPackages or blackListOfText");
            return;
        }

        try {
            if(bpArray != null) {
                List<String> bpList = bpArray.toList();
                NotificationService.blackListOfPackages = bpList;
            }

            if(bpTxtArray != null) {
                NotificationService.blackListOfText = bpTxtArray;
            }
            call.success();
        } catch (Exception e) {
            Log.e(TAG, "Error");
            call.reject(e.getLocalizedMessage());
        }
    }

    private class NotificationReceiver extends BroadcastReceiver {
        NotificationReceiver() {
            Log.d(TAG, "NotificationReceiver: NotificationReceiver constructor");

            NotificationService.blackListOfText = null;
            NotificationService.blackListOfPackages = null;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "NotificationReceiver: onReceive");

            JSObject jo = new JSObject();
            try {
                jo.put("key", intent.getStringExtra(NotificationService.ARG_KEY));
                jo.put("apptitle", intent.getStringExtra(NotificationService.ARG_APPTITLE));
                jo.put("text", intent.getStringExtra(NotificationService.ARG_TEXT));
                JSONArray ja = new JSONArray();
                for (String k : intent.getStringArrayExtra(NotificationService.ARG_TEXTLINES))
                    ja.put(k);
                jo.put("textlines", ja.toString());
                jo.put("title", intent.getStringExtra(NotificationService.ARG_TITLE));
                jo.put("time", intent.getLongExtra(NotificationService.ARG_TIME, System.currentTimeMillis()));
                jo.put("package", intent.getStringExtra(NotificationService.ARG_PACKAGE));
            } catch (Exception e) {
                Log.e(TAG, "JSObject Error");
                return;
            }
            Log.d(TAG, "NotificationReceiver: package:" + intent.getStringExtra(NotificationService.ARG_PACKAGE));

            switch (intent.getAction()){
                case NotificationService.ACTION_RECEIVE:
                    notifyListeners(EVENT_NOTIFICATION_RECEIVED, jo);
                    break;
                case NotificationService.ACTION_REMOVE :
                    notifyListeners(EVENT_NOTIFICATION_REMOVED, jo);
                    break;
            }
        }
    }
}
