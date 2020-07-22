package com.example.emergencyrescue;
import android.content.Intent;
import android.os.Bundle;
import com.google.firebase.auth.FirebaseAuth;

public class SignOut extends CommonActivity  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();
        Intent intent = new Intent(this, SignIn.class);
        startActivity(intent);
        hideProgressDialog();
    }
}
