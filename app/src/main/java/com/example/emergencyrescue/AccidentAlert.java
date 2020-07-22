package com.example.emergencyrescue;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class AccidentAlert extends MainActivity  {
    public String pickUpAddress;
    public double pickUpLat;
    public double pickUpLong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createDynamicView(R.layout.activity_accident_alert, R.id.nav_home);
        View parentLayout = findViewById(R.id.content_frame);
        parentLayout.setBackgroundColor(getResources().getColor(android.R.color.background_dark));

        final String pickUpRequestUserId = getIntent().getStringExtra("pickUpRequestUserId");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("PickUpRequest");
        reference.child(pickUpRequestUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                pickUpAddress = Objects.requireNonNull(dataSnapshot.child("g").getValue()).toString();
                pickUpLat = (Double)  dataSnapshot.child("l").child("0").getValue();
                pickUpLong = (Double)  dataSnapshot.child("l").child("1").getValue();
                TextView victimCurrentAddress = findViewById(R.id.victimCurrentAddress);
                if(victimCurrentAddress != null) {
                    victimCurrentAddress.setText(pickUpAddress);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Toast.makeText(MainActivity.this, databaseError.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        Button navigateToAccidentBtn = findViewById(R.id.navigateToAccidentBtn);
        navigateToAccidentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AccidentAlert.this, NavigateToAccident.class);
                intent.putExtra("pickUpRequestUserId", pickUpRequestUserId);
                intent.putExtra("pickUpAddress", pickUpAddress);
                intent.putExtra("pickUpLat", pickUpLat);
                intent.putExtra("pickUpLong", pickUpLong);
                startActivity(intent);
            }
        });
    }
}
