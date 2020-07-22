package com.example.emergencyrescue;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class SignIn extends CommonActivity implements
        View.OnClickListener  {
    private EditText signInEmail;
    private EditText signInPassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        hideProgressDialog();
        signInEmail = findViewById(R.id.signInEmail);
        signInPassword = findViewById(R.id.signInPassword);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Intent intent = new Intent(SignIn.this, Home.class);
            startActivity(intent);
        }
    }

    private void checkAccount(String email, String password) {
        if (!validateForm()) {
            return;
        }
        showProgressDialog("Signing In");
        // sign in
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(SignIn.this, Home.class);
                            startActivity(intent);
                        } else {
                            hideKeyboardFrom(SignIn.this);
                            Toast.makeText(SignIn.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        hideProgressDialog();
                    }
                });
        /////////
    }

    private boolean validateForm() {
        boolean valid = true;

        String email = signInEmail.getText().toString();
        if (TextUtils.isEmpty(email)) {
            signInEmail.setError("Required.");
            valid = false;
        } else {
            signInEmail.setError(null);
        }

        String password = signInPassword.getText().toString();
        if (TextUtils.isEmpty(password)) {
            signInPassword.setError("Required.");
            valid = false;
        } else {
            signInPassword.setError(null);
        }

        return valid;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.signInBtn) {
            checkAccount(signInEmail.getText().toString(), signInPassword.getText().toString());
        } else if (i == R.id.linkSignUp) {
            Intent intent = new Intent(this, Signup.class);
            startActivity(intent);
        }
    }
}
