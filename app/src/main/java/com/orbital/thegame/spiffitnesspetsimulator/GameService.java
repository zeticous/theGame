package com.orbital.thegame.spiffitnesspetsimulator;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class GameService extends Service {
    private final static String LOG = "GameService";
    public static Spirits UserSpirit;
    int stepCount;
    public final static int FACTOR = 2;
    public static final String MY_PREFS_NAME = "MyPrefsFile";

    public final static String TAG = "GoogleFitService";
    public final static String ALARM_RECEIVE = "com.orbital.thegame.spiffitnesspetsimulator.gameservice.alarmreceiver";
    private IntentFilter intentFilter = new IntentFilter(ALARM_RECEIVE);

    AlarmReceiver alarmManager;

    public final static String PTAG = "SharedPref";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG, "STARTED: onStartCommand");
        alarmManager = new AlarmReceiver();
        registerReceiver(alarmManager, intentFilter);
        alarmManager.setAlarm(this);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class AlarmReceiver extends BroadcastReceiver {
        private final String TAG = "AlarmReceiver";

        @Override
        public void onReceive (Context context, Intent intent){
            Log.d(TAG, "STARTED: onReceive");

            updateAffinityPoint();
            requestGoogleFitSync();

            int affinityLevel = UserSpirit.getAffinityLevel();
            if (UserSpirit.evolveCheck(affinityLevel)!=null){
                UserSpirit = UserSpirit.evolveCheck(affinityLevel);
            }
            saveSpirits();

        }

        public void setAlarm(Context context){
            Log.d(TAG, "STARTED: setAlarm");

            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent (ALARM_RECEIVE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0,intent,0);
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000*60, pendingIntent);
        }
    }

    public static void updateAffinityPoint(){
        int affinityLevel = UserSpirit.getAffinityLevel();
        int stepCount = UserSpirit.getStepCount();
        int affinityPoint = (stepCount - FACTOR*affinityLevel)/FACTOR;

        Log.d("GAMESERVICE", "AffinityLevel: " + affinityLevel);
        Log.d("GAMESERVICE", "StepCount: " + stepCount);
        Log.d("GAMESERVICE", "AffinityPoint: " + affinityPoint);
        UserSpirit.setAffinityPoint(affinityPoint);
    }

    public void requestGoogleFitSync() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter((GoogleFitSync.FIT_NOTIFY_INTENT)));
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(GoogleFitSync.STEP_COUNT));

        Log.e(TAG, "Executing requestFitConnection...");
        Intent service = new Intent(this, GoogleFitSync.class);
        service.putExtra(GoogleFitSync.TYPE_REQUEST_CONNECTION, GoogleFitSync.TYPE_TRUE);
        service.putExtra(GoogleFitSync.TYPE_GET_STEP_DATA, GoogleFitSync.TYPE_TRUE);
        service.putExtra("spiritStartTime", UserSpirit.getStartTime());
        service.putExtra("spiritEndTime", UserSpirit.getEndTime());
        startService(service);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Executing Connection onReceive");
            if (intent.hasExtra(GoogleFitSync.FIT_EXTRA_NOTIFY_FAILED_STATUS_CODE) &&
                    intent.hasExtra(GoogleFitSync.FIT_EXTRA_NOTIFY_FAILED_STATUS_CODE)) {
                int errorCode = intent.getIntExtra(GoogleFitSync.FIT_EXTRA_NOTIFY_FAILED_STATUS_CODE, 0);
                PendingIntent pendingIntent = intent.getParcelableExtra(GoogleFitSync.FIT_EXTRA_NOTIFY_FAILED_INTENT);

                Intent resolution = new Intent();
                intent.putExtra("PENDING_INTENT", pendingIntent);
                intent.putExtra("ERROR_CODE", errorCode);
                intent.putExtra("REQUEST_RESOLUTION", true);
                Log.d(TAG, "Fit connection failed - opening connect screen.");

                sendBroadcast(resolution);
            }
            if (intent.hasExtra(GoogleFitSync.FIT_EXTRA_CONNECTION_MESSAGE)) {
                Log.d(TAG, "Fit connection successful - closing connect screen if it's open.");
                fitHandleConnection();
            }
            if (intent.hasExtra(GoogleFitSync.STEP_COUNT)) {
                stepCount = intent.getIntExtra(GoogleFitSync.STEP_COUNT, 0);
                Log.e(TAG, "Broadcast Value: " + stepCount);
                UserSpirit.setStepCount(stepCount);
                saveSpirits();
            }
        }
    };

    private void fitHandleConnection() {
        Log.e(TAG, "Fit connected");
    }

    public void saveSpirits(){
        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt("stepCount", UserSpirit.getStepCount());
        editor.putInt("register", UserSpirit.getRegister());
        editor.putInt("affinityLevel", UserSpirit.getAffinityLevel());

        editor.putLong("startTime", UserSpirit.getStartTime());
        editor.putLong("endTime", UserSpirit.getEndTime());

        editor.commit();
        Log.d(PTAG, "Shared Preference saved");
    }
}
