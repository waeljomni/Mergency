package com.example.emergencyrescue;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LocationUpdate extends Service {
    private FusedLocationProviderClient mLocationProviderClient;
    private LocationCallback locationUpdatesCallback;
    private LocationRequest locationRequest;
    public LocationUpdate() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        setUpLocationRequest();
    }


    private void setUpLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String keyValue = intent.getStringExtra("key");
        if(keyValue!=null && keyValue.equals("stop")){
            stopSelf();
        }else {
            setUpLocationUpdatesCallback();
            mLocationProviderClient.requestLocationUpdates(locationRequest, locationUpdatesCallback, null);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocationProviderClient.removeLocationUpdates(locationUpdatesCallback);
    }

    private void setUpLocationUpdatesCallback() {
        locationUpdatesCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if(locationResult!=null){
                    Location lastLocation = locationResult.getLastLocation();
                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                    FirebaseUser user = mAuth.getCurrentUser();
                    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
                    if (user != null) {
                        mDatabase.child("Users").child(user.getUid()).child("lat").setValue(lastLocation.getLatitude());
                        mDatabase.child("Users").child(user.getUid()).child("long").setValue(lastLocation.getLongitude());
                    }
                }
            }
        };
    }
}