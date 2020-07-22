package com.example.emergencyrescue;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.telephony.gsm.SmsManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class CommonActivity extends AppCompatActivity
        implements SensorEventListener {

    @VisibleForTesting
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    FusedLocationProviderClient mFusedLocationClient;
    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    public ProgressDialog mProgressDialog;
    AlertDialog dialog;
    public double currentLatitude;
    public double currentLongitude;
    Geocoder geocoder;
    List<Address> addresses;
    Vibrator vibrator;
    MediaPlayer mp;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseUser user = mAuth.getCurrentUser();
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    String victimName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (sensorManager != null) {
            sensorManager.registerListener(this,sensorManager.getDefaultSensor
                    (Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
    
    public void showProgressDialog(String message) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            if(message == null || message.equals("")){
                mProgressDialog.setMessage(getString(R.string.Loading));
            }else{
                mProgressDialog.setMessage(message);
            }
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    public static void hideKeyboardFrom(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(Objects.requireNonNull(activity.getCurrentFocus()).getWindowToken(), 0);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        hideProgressDialog();
    }

    public void clearForm(ViewGroup group) {
        for (int i = 0, count = group.getChildCount(); i < count; ++i) {
            View view = group.getChildAt(i);
            if (view instanceof EditText) {
                ((EditText)view).setText("");
            }

            if(view instanceof ViewGroup && (((ViewGroup)view).getChildCount() > 0))
                clearForm((ViewGroup)view);
        }
    }

    public boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netinfo = cm != null ? cm.getActiveNetworkInfo() : null;

        if (netinfo != null && netinfo.isConnectedOrConnecting()) {
            android.net.NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            android.net.NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            return (mobile != null && mobile.isConnectedOrConnecting()) || (wifi != null && wifi.isConnectedOrConnecting());
        } else
            return false;
    }

    public AlertDialog.Builder buildDialog(Context c, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //finish();
            }
        });
        return builder;
    }

    public void locationCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (manager != null && !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This app requires access to the location.")
                .setCancelable(false)
                .setPositiveButton("Turn on", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @IgnoreExtraProperties
    public class Emergency_Contacts {

        public String contactName;
        public String contactNumber;

        public Emergency_Contacts() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        public Emergency_Contacts(String contactName, String contactNumber) {

            this.contactName = contactName;
            this.contactNumber = contactNumber;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // onAccuracyChanged
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){

            float xVal = event.values[0];
            double xValSquare = Math.pow(xVal, 2);
            float yVal = event.values[1];
            double yValSquare = Math.pow(yVal, 2);
            float zVal = event.values[2];
            double zValSquare = Math.pow(zVal, 2);

            double a = Math.sqrt(xValSquare + yValSquare + zValSquare);
            double gForceValue = (a / 9.81);

            if(gForceValue > 4) {
                String userId = user.getUid();
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                reference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String isAutoMonitoring = Objects.requireNonNull(dataSnapshot.child("autoMonitoring").getValue()).toString();
                        if(isAutoMonitoring.equals("1")){
                            acccidentDetect();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        //Toast.makeText(MainActivity.this, databaseError.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    public void acccidentDetect(){
        if(dialog != null && dialog.isShowing()) {
            return;
        }
        dialog = new AlertDialog.Builder(this)
                .setTitle("WARNING (Accident Detected)!")
                .setMessage("Please 'Cancel' to abort sending emergency notification.")
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendEmergencyNotification();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopWarning();
                    }
                })
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            private static final int AUTO_DISMISS_MILLIS = 15000;
            @Override
            public void onShow(final DialogInterface dialog) {
                final Button defaultButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                final CharSequence negativeButtonText = defaultButton.getText();
                new CountDownTimer(AUTO_DISMISS_MILLIS, 100) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        defaultButton.setText(String.format(
                                Locale.getDefault(), "%s (%d)",
                                negativeButtonText,
                                TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1 //add one so it never displays zero
                        ));
                    }
                    @Override
                    public void onFinish() {
                        if (((AlertDialog) dialog).isShowing()) {
                            dialog.dismiss();
                            sendEmergencyNotification();
                        }
                    }
                }.start();
            }
        });
        dialog.show();
        startWarning();
    }

    public void startWarning(){
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(15000);
        mp = MediaPlayer.create(this, R.raw.warning);
        mp.start();
        mp.setLooping(true);
    }

    public void stopWarning(){
        if(vibrator != null){
            vibrator.cancel();
        }

        if(mp != null){
            mp.stop();
            mp.reset();
            mp.setLooping(false);
        }
    }

    public void sendEmergencyNotification(){
        if (user != null) {
            try {
                locationCheck();
                geocoder = new Geocoder(this, Locale.getDefault());
                addresses = geocoder.getFromLocation(currentLatitude, currentLongitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5

                String currentAddress = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                /*String city = addresses.get(0).getLocality();
                String state = addresses.get(0).getAdminArea();
                String country = addresses.get(0).getCountryName();
                String postalCode = addresses.get(0).getPostalCode();
                String knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL*/

                mDatabase.child("PickUpRequest").child(user.getUid()).child("g").setValue(currentAddress);
                mDatabase.child("PickUpRequest").child(user.getUid()).child("l").child("0").setValue(currentLatitude);
                mDatabase.child("PickUpRequest").child(user.getUid()).child("l").child("1").setValue(currentLongitude);
                View parentLayout = findViewById(R.id.content_frame);
                sendFCM();
                sendSMS(currentLatitude, currentLongitude);
                Snackbar.make(parentLayout, "Emergency notification/SMS has been sent to nearby responders/emergency contacts!", Snackbar.LENGTH_LONG).show();
                stopWarning();
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void sendSMS(final double currentLatitude, final double currentLongitude){
        final SmsManager smsManager = SmsManager.getDefault();
        FirebaseDatabase.getInstance().getReference().child("Users").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        victimName = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                        final String smsAlertMessage = "Emergency Alert: "+victimName+" added you as emergency contact, help them by responding at "+currentLatitude+", "+currentLongitude;
                        FirebaseDatabase.getInstance().getReference().child("EmergencyContacts").child(user.getUid())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                            String contactNumber = Objects.requireNonNull(snapshot.child("contactNumber").getValue()).toString();
                                            smsManager.sendTextMessage(contactNumber, null, smsAlertMessage, null, null);
                                        }
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                    }
                                });
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    public void sendFCM(){
        FirebaseDatabase.getInstance().getReference().child("Users").orderByChild("userType").equalTo("Responder")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String token = Objects.requireNonNull(snapshot.child("token").getValue()).toString();

                            RequestQueue MyRequestQueue = Volley.newRequestQueue(CommonActivity.this);
                            JSONObject json = new JSONObject();
                            try {
                                JSONObject userData=new JSONObject();
                                userData.put("pickUpRequestUserId", user.getUid());
                                json.put("data", userData);
                                json.put("to", token);
                            }
                            catch (JSONException e) {
                                e.printStackTrace();
                            }

                            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", json, new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    //Log.i("onResponse", "" + response.toString());
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {

                                }
                            }) {
                                @Override
                                public Map<String, String> getHeaders() throws AuthFailureError {
                                    Map<String, String> params = new HashMap<String, String>();
                                    params.put("Authorization", "key=AAAAnLYPSh4:APA91bHQewdT4UEQa7KrKvLKZtZMI6K5k0g3tp4bpAVoWqorSBH1Y-57UjMNxo2jWsy3HHlFmTlk1C6pKWygXBSR4ZfDiSkYPWSZsrAScNBU6ooTN_gGKIQy4Bmhyneo3Kt1uLd9AcZb");
                                    params.put("Content-Type", "application/json");
                                    return params;
                                }
                            };
                            MyRequestQueue.add(jsonObjectRequest);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }
}