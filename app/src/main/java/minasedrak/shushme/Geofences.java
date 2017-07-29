package minasedrak.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MinaSedrak on 7/28/2017.
 */

public class Geofences implements ResultCallback {

    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private PendingIntent mGeofencesPendingIntent;
    private List<Geofence> mGeofencesList;

    private static final long GEOFENCE_TIMEOUT = 24 * 60 * 60 * 1000;
    private static final float GEOFENCE_RADIUS = 50;

    public Geofences (Context mContext, GoogleApiClient mGoogleApiClient){
        this.mContext = mContext;
        this.mGoogleApiClient = mGoogleApiClient;
        mGeofencesPendingIntent = null;
        mGeofencesList = new ArrayList<>();
    }


    public void registerGeofences(){

        if(mGoogleApiClient == null || !mGoogleApiClient.isConnected() ||
                mGeofencesList == null || mGeofencesList.size() == 0){
            return;}

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    getGeofencesRequest(),
                    getGeofencesPendingIntent()
            ).setResultCallback(this);
        }catch (SecurityException securityException){
            Log.e("Geofences", securityException.getMessage());
        }

    }


    public void unregisterGeofences(){

        if(mGoogleApiClient == null || !mGoogleApiClient.isConnected()){return;}

        try {
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    getGeofencesPendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException securityException){
            Log.e("Geofences", securityException.getMessage());
        }
    }


    private GeofencingRequest getGeofencesRequest(){
        GeofencingRequest.Builder request = new GeofencingRequest.Builder();
        request.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        request.addGeofences(mGeofencesList);

        return request.build();
    }


    private PendingIntent getGeofencesPendingIntent(){

        if(mGeofencesPendingIntent != null){return mGeofencesPendingIntent;}

        Intent geofenceBroadcastReceiverIntent = new Intent(mContext, GeofencesBroadcastReceiver.class);
        mGeofencesPendingIntent = PendingIntent.getBroadcast(mContext, 0, geofenceBroadcastReceiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return mGeofencesPendingIntent;
    }


    public void createGeofencesList(PlaceBuffer places){
        mGeofencesList = new ArrayList<>();

        if(places == null || places.getCount() == 0){return;}

        for(Place place : places){

            String placeID = place.getId();
            double placeLat = place.getLatLng().latitude;
            double placeLng = place.getLatLng().longitude;

            Geofence newGeofence = new Geofence.Builder()
                    .setRequestId(placeID)
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(placeLat, placeLng, GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

            mGeofencesList.add(newGeofence);
        }
    }


    @Override
    public void onResult(@NonNull Result result) {
        Log.e("Geofences", String.format("Error add or remove geofences %s", result.getStatus().toString() ));
    }

}
