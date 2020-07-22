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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class Auth extends MainActivity implements
        View.OnClickListener {

    FirebaseUser user = mAuth.getCurrentUser();
    private EditText authPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createDynamicView(R.layout.activity_auth, R.id.nav_profile);
        authPassword = findViewById(R.id.authPassword);
    }

    private boolean validateForm() {
        boolean valid = true;

        String name = authPassword.getText().toString();
        if (TextUtils.isEmpty(name)) {
            authPassword.setError("Required.");
            valid = false;
        } else {
            authPassword.setError(null);
        }
        return valid;
    }

    private void reAuthenticateUser(final String password) {
        if (!validateForm()) {
            return;
        }

        AuthCredential credential = EmailAuthProvider
                .getCredential(Objects.requireNonNull(user.getEmail()), password); // Current Login Credentials \\
        // Prompt the user to re-provide their sign-in credentials
        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(Auth.this, Profile.class);
                            intent.putExtra("profilePassword", password);
                            startActivity(intent);
                        } else {
                            hideKeyboardFrom(Auth.this);
                            Toast.makeText(Auth.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.authBtn) {
            reAuthenticateUser(authPassword.getText().toString());
        }
    }
}
