package com.example.emergencyrescue;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class History extends MainActivity implements
        View.OnClickListener  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createDynamicView(R.layout.activity_history, R.id.nav_history);
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("PickUpRequest");
        reference.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try{
                    String accidentLocation = Objects.requireNonNull(dataSnapshot.child("g").getValue()).toString();
                    final TextView accidentLocationView = findViewById(R.id.accidentLocation);
                    if(accidentLocationView != null) {
                        accidentLocationView.setText("Accident Location : "+accidentLocation);
                    }
                }catch (Exception e){

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Toast.makeText(MainActivity.this, databaseError.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
    }
}
