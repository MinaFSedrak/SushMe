package minasedrak.shushme;

import android.Manifest;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.List;

import minasedrak.shushme.provider.PlaceContract;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
         GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 111;
    private static final int PLACE_PICKER_REQUEST = 5;

    private PlaceAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private GoogleApiClient mGoogleApiClient;
    private Geofences mGeofences;
    private boolean mGeofenceIsEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        mRecyclerView = (RecyclerView) findViewById(R.id.places_RecyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PlaceAdapter(this, null);
        mRecyclerView.setAdapter(mAdapter);

        Switch geoOnOffSwitch = (Switch) findViewById(R.id.enable_switch);
        mGeofenceIsEnabled = getPreferences(MODE_PRIVATE).getBoolean(getString(R.string.setting_enabled), false);
        geoOnOffSwitch.setChecked(mGeofenceIsEnabled);
        geoOnOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                editor.putBoolean(getString(R.string.setting_enabled), isChecked);
                mGeofenceIsEnabled = isChecked;
                editor.commit();
                if(isChecked){
                    mGeofences.registerGeofences();}
                else {
                    mGeofences.unregisterGeofences();}
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, this)
                .build();

        mGeofences = new Geofences(this, mGoogleApiClient);
    }


    // Add New Location Button Click Listener
    public void onAddPlaceButtonClicked(View view) {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Need Location Permission", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Location Permission Granted", Toast.LENGTH_LONG).show();

        try {
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            Intent placePickerIntent = builder.build(this);
            startActivityForResult(placePickerIntent, PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            Log.e(TAG, String.format("GooglePlayServices Not Available %s", e.getMessage()));
        } catch (GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, String.format("GooglePlayServices Not Available %s", e.getMessage()));
        } catch (Exception e){
            Log.e(TAG, String.format("PlacePicker Exception: %s", e.getMessage()));
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Toast.makeText(this, "OnActivityResult requestCode: " + requestCode + " \n resultCode: " +
        //        "" + resultCode, Toast.LENGTH_SHORT).show();

        if(requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK){

            Place place = PlacePicker.getPlace(this, data);
            if (place == null){
                Log.i(TAG, "No place selected");
               // Toast.makeText(this, "No PLace Selected", Toast.LENGTH_SHORT).show();
                return;
            }

            // Extract Place informations from API
            String placeName = place.getName().toString();
            String placeAddress = place.getAddress().toString();
            String placeID = place.getId();
           // Toast.makeText(this, "Selected PlaceId = " + placeID, Toast.LENGTH_SHORT).show();

            // Insert PlaceID to Database
            ContentValues values = new ContentValues();
            values.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID, placeID);
            getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI, values);

            // Getting Live Places Data
            refreshPlacesData();

        }




    }


    private void refreshPlacesData() {
        //Toast.makeText(this, "Refresh has been called", Toast.LENGTH_SHORT).show();
        Uri uri = PlaceContract.PlaceEntry.CONTENT_URI;

        Cursor places_ids_cursor = getContentResolver().query(uri,
                null,
                null,
                null,
                null);

        if(places_ids_cursor == null || places_ids_cursor.getCount() == 0){
            //Toast.makeText(this, "Empty", Toast.LENGTH_SHORT).show();
            return;}

        List<String> places_ids = new ArrayList<>();

        while (places_ids_cursor.moveToNext()){
            places_ids.add(places_ids_cursor.getString(places_ids_cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID)));
            //Toast.makeText(this, places_ids_cursor.getString(places_ids_cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID)), Toast.LENGTH_SHORT).show();
        }

        places_ids_cursor.close();

        PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient,
                places_ids.toArray(new String[places_ids.size()]));

        placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {
                mAdapter.swapPlaces(places);
                mGeofences.createGeofencesList(places);
                if(mGeofenceIsEnabled){ mGeofences.registerGeofences();}
            }
        });


    }


    // Location Permission CheckBox Click Listener
    public void onLocationPermissionClicked(View view) {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_FINE_LOCATION);
    }


    // Ringer Permission CheckBox Click Listener
    public void onRingerPermissionsClicked(View view) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        startActivity(intent);
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Initialize location permissions checkbox
        CheckBox locationPermissions = (CheckBox) findViewById(R.id.location_permission_checkbox);
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissions.setChecked(false);
        } else {
            locationPermissions.setChecked(true);
            locationPermissions.setEnabled(false);
        }

        // Initialize ringer permissions checkbox
        CheckBox ringerPermissions = (CheckBox) findViewById(R.id.ringer_permissions_checkbox);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if( Build.VERSION.SDK_INT >= 24 && !notificationManager.isNotificationPolicyAccessGranted()){
            ringerPermissions.setChecked(false);
        }else {
            ringerPermissions.setChecked(true);
            ringerPermissions.setEnabled(false);
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        refreshPlacesData();
        Log.i(TAG, "API Client Connection Successful!");
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "API Client Connection Suspended!");
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "API Client Connection Failed!");
    }



}
