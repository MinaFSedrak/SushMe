package minasedrak.shushme;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/**
 * Created by MinaSedrak on 7/28/2017.
 */

public class GeofencesBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = GeofencesBroadcastReceiver.class.getSimpleName();


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "OnReceive has been called");

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if(geofencingEvent.hasError()){
            Log.e(TAG, String.format("Error %d", geofencingEvent.getErrorCode()));
            return;}

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER){
            setRingerMode(context, AudioManager.RINGER_MODE_SILENT);
        } else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
            setRingerMode(context, AudioManager.RINGER_MODE_NORMAL);
        } else {
            Log.e(TAG, String.format("Unknown Transition %d", geofenceTransition));
            return;
        }

        sendNotification(context, geofenceTransition);
    }


    private void sendNotification(Context context, int geofenceTransitionType) {

        Intent notificationIntent = new Intent(context, MainActivity.class);

        // Creates a fake Stack to let user go back for the MainActivity when he press backButton
        // instead of any other stack already exist in the background
         TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(notificationIntent);

        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);



        // Build Notification
        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(context);

        if(geofenceTransitionType == Geofence.GEOFENCE_TRANSITION_ENTER){
            notifyBuilder.setSmallIcon(R.drawable.ic_volume_off_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.ic_volume_off_white_24dp))
                    .setContentTitle(context.getString(R.string.silent_mode_activated));
        } else if(geofenceTransitionType == Geofence.GEOFENCE_TRANSITION_EXIT){
            notifyBuilder.setSmallIcon(R.drawable.ic_volume_up_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.ic_volume_up_white_24dp))
                    .setContentTitle(context.getString(R.string.back_to_normal));
        }


        notifyBuilder.setContentText(context.getString(R.string.touch_to_relaunch));
        notifyBuilder.setContentIntent(notificationPendingIntent);

        //notifyBuilder.build().flags |= Notification.FLAG_AUTO_CANCEL;

        // Dismiss notification once the user touch it
        notifyBuilder.setAutoCancel(true);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(0, notifyBuilder.build());


    }


    private void setRingerMode(Context mContext, int mode){

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT < 24 ||
                (Build.VERSION.SDK_INT >= 24 && notificationManager.isNotificationPolicyAccessGranted())){

            AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setRingerMode(mode);

        }
    }
}
