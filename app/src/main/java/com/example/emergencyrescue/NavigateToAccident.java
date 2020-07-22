package com.example.emergencyrescue;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.emergencyrescue.directionhelpers.FetchURL;
import com.example.emergencyrescue.directionhelpers.TaskLoadedCallback;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Objects;

public class NavigateToAccident extends MainActivity implements OnMapReadyCallback, TaskLoadedCallback {

    private GoogleMap mGoogleMap;
    private MarkerOptions place1, place2;
    private Polyline currentPolyline;
    public String pickUpRequestUserId;
    public double pickUpLat;
    public double pickUpLong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createDynamicView(R.layout.activity_nav_to_accident, R.id.nav_home);
        pickUpRequestUserId = getIntent().getStringExtra("pickUpRequestUserId");
        pickUpLat = getIntent().getDoubleExtra("pickUpLat", 0);
        pickUpLong = getIntent().getDoubleExtra("pickUpLong", 0);
        locationCheck();
        updateCurrentLocation();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(pickUpRequestUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String userName = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                String userMobile = Objects.requireNonNull(dataSnapshot.child("mobile").getValue()).toString();
                String userBloodGroup = Objects.requireNonNull(dataSnapshot.child("bloodGroup").getValue()).toString();
                String userImage = Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString();

                TextView victimInformationBox = findViewById(R.id.victimInformationBox);
                ImageView victimImage = findViewById(R.id.victimImage);

                Glide.with(NavigateToAccident.this)
                        .load(userImage)
                        .into(victimImage);

                String information = "<b>Name</b>: "+userName;
                information += "<br><b>Phone:</b> "+userMobile;
                information += "<br><b>Blood Group:</b> "+userBloodGroup;
                information += "<br><b>Accident Address:</b> "+getIntent().getStringExtra("pickUpAddress");
                victimInformationBox.setText(Html.fromHtml(information));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Toast.makeText(MainActivity.this, databaseError.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
        return url;
    }

    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = mGoogleMap.addPolyline((PolylineOptions) values[0]);
    }

    public void updateCurrentLocation() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.accidentMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.accidentMap);
        if (mapFrag != null) {
            mapFrag.getMapAsync(this);
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
        mLocationRequest.setInterval(10000); // two minute interval
        mLocationRequest.setFastestInterval(10000);
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
                mGoogleMap.clear();

                place1 = new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).title("Your Location");
                place2 = new MarkerOptions().position(new LatLng(pickUpLat, pickUpLong)).title("Victim Location");

                mGoogleMap.addMarker(place2);

                new FetchURL(NavigateToAccident.this).execute(getUrl(place1.getPosition(), place2.getPosition(), "driving"), "driving");

                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Your Location");
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);
                //move map camera
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 14.0f));
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
                                ActivityCompat.requestPermissions(NavigateToAccident.this,
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
}
