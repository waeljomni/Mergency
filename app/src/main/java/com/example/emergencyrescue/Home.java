package com.example.emergencyrescue;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.List;
import java.util.Objects;

public class Home extends MainActivity
        implements OnMapReadyCallback, View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabase.child("Users").child(user.getUid()).child("token").setValue(FirebaseInstanceId.getInstance().getToken());
        String userId = user.getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String userType = Objects.requireNonNull(dataSnapshot.child("userType").getValue()).toString();
                Switch s;
                if(userType.equals("Responder")){
                    createDynamicView(R.layout.activity_home_responder, R.id.nav_home);
                    s = findViewById(R.id.switchResponder);
                }else{
                    createDynamicView(R.layout.activity_home, R.id.nav_home);
                    s = findViewById(R.id.switch1);
                }

                if(s != null) {
                    if (autoMonitoring.equals("1") && userTypeG.equals("User")) {
                        s.setChecked(true);
                    }else if (isOnline.equals("1") && userTypeG.equals("Responder")) {
                        s.setChecked(true);
                    } else {
                        s.setChecked(false);
                    }
                }

                locationCheck();
                updateCurrentLocation();
                if (s != null) {
                    s.setOnCheckedChangeListener(Home.this);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Toast.makeText(MainActivity.this, databaseError.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0;
        String permission = Manifest.permission.SEND_SMS;
        if (ContextCompat.checkSelfPermission(Home.this, permission) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(Home.this, permission)) {
                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(Home.this, new String[]{permission}, MY_PERMISSIONS_REQUEST_SEND_SMS);
            } else {
                ActivityCompat.requestPermissions(Home.this, new String[]{permission}, MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000); // two minute interval
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mGoogleMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        }
        else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mGoogleMap.setMyLocationEnabled(true);
        }
    }

    public void updateCurrentLocation() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFrag != null) {
            mapFrag.getMapAsync(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCurrentLocation();
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                currentLatitude =  location.getLatitude();
                currentLongitude =  location.getLongitude();
                mLastLocation = location;
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Current Position");
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);
                //move map camera
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16.0f));
            }
        }
    };

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app requires access to the location.")
                        .setPositiveButton("Turn on", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(Home.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    mGoogleMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(this, "Permission denied...", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.requestEmergencyBtn) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Warning");
            builder.setMessage("This will send emergency notification. Are you sure?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sendEmergencyNotification();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //finish();
                }
            });
            builder.show();
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        View parentLayout = findViewById(R.id.content_frame);
        if(isChecked) {
            if(userTypeG.equals("User")){
                mDatabase.child("Users").child(user.getUid()).child("autoMonitoring").setValue("1");
                Snackbar.make(parentLayout, "Auto monitoring activated!", Snackbar.LENGTH_LONG).show();
            }else{
                mDatabase.child("Users").child(user.getUid()).child("isOnline").setValue("1");
                Snackbar.make(parentLayout, "Online status activated!", Snackbar.LENGTH_LONG).show();
            }
        } else {
            if(userTypeG.equals("User")){
                mDatabase.child("Users").child(user.getUid()).child("autoMonitoring").setValue("0");
                Snackbar.make(parentLayout, "Auto monitoring deactivated!", Snackbar.LENGTH_LONG).show();
            }else{
                mDatabase.child("Users").child(user.getUid()).child("isOnline").setValue("0");
                Snackbar.make(parentLayout, "Online status deactivated!", Snackbar.LENGTH_LONG).show();
            }
        }
    }
}
