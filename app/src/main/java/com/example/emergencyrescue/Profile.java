package com.example.emergencyrescue;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Objects;
import java.util.UUID;

public class Profile extends MainActivity implements
        View.OnClickListener {

    private DatabaseReference mDatabase;
    FirebaseUser user = mAuth.getCurrentUser();
    private EditText profileName;
    private EditText profileMobile;
    private EditText profileEmail;
    private EditText profileUserType;
    private EditText profileUserService;
    private EditText profileBloodGroup;
    private String profilePassword;

    // views for button
    private Button btnSelect;

    // Uri indicates, where the image will be picked from
    private Uri filePath;

    // request code
    private final int PICK_IMAGE_REQUEST = 22;

    // instance for firebase storage and StorageReference
    FirebaseStorage storage;
    StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createDynamicView(R.layout.activity_profile, R.id.nav_profile);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        profileName = findViewById(R.id.profileName);
        profileMobile = findViewById(R.id.profileMobile);
        profileEmail = findViewById(R.id.profileEmail);
        profileUserType = findViewById(R.id.profileUserType);
        profileUserService = findViewById(R.id.profileUserService);
        profileBloodGroup = findViewById(R.id.profileBloodGroup);

        Intent intent = getIntent();
        profilePassword = intent.getStringExtra("profilePassword");

        // initialise views
        btnSelect = findViewById(R.id.btnChoose);

        // get the Firebase storage reference
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // on pressing btnSelect SelectImage() is called
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                SelectImage();
            }
        });
    }

    private boolean validateForm() {
        boolean valid = true;

        String name = profileName.getText().toString();
        if (TextUtils.isEmpty(name)) {
            profileName.setError("Required.");
            valid = false;
        } else {
            profileName.setError(null);
        }

        String mobile = profileMobile.getText().toString();
        if (TextUtils.isEmpty(mobile)) {
            profileMobile.setError("Required.");
            valid = false;
        } else {
            profileMobile.setError(null);
        }

        String userType = profileUserType.getText().toString();
        if (TextUtils.isEmpty(userType)) {
            profileUserType.setError("Required.");
            valid = false;
        } else {
            profileUserType.setError(null);
        }

        String bloodGroup = profileBloodGroup.getText().toString();
        if (TextUtils.isEmpty(bloodGroup)) {
            profileBloodGroup.setError("Required.");
            valid = false;
        } else {
            profileBloodGroup.setError(null);
        }

        return valid;
    }

    private void updateProfile(String userId, String name, String mobile, String userType, String userService, String bloodGroup) {
        if (!validateForm()) {
            return;
        }
        mDatabase.child("Users").child(userId).child("name").setValue(name);
        mDatabase.child("Users").child(userId).child("mobile").setValue(mobile);
        mDatabase.child("Users").child(userId).child("userType").setValue(userType);
        mDatabase.child("Users").child(userId).child("bloodGroup").setValue(bloodGroup);
        if(filePath != null){
            uploadImage();
        }
        if(userType.equals("user")) {
            mDatabase.child("Users").child(userId).child("service").setValue("");
        }else{
            mDatabase.child("Users").child(userId).child("service").setValue(userService);
        }
        hideKeyboardFrom(Profile.this);
        View parentLayout = findViewById(R.id.profileRoot);
        Snackbar.make(parentLayout, "Profile Updated", Snackbar.LENGTH_LONG).show();
        /*final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
                overridePendingTransition(0, 0);
                startActivity(getIntent());
                overridePendingTransition(0, 0);
            }
        }, 3000);*/
    }

    private void reAuthenticateUser(String userId, final String email, final String password) {
        AuthCredential credential = EmailAuthProvider
                .getCredential(Objects.requireNonNull(user.getEmail()), password); // Current Login Credentials \\
        // Prompt the user to re-provide their sign-in credentials
        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            //----------------Code for Changing Email Address----------\\
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                user.updateEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        //if (task.isSuccessful()) {
                                            //Toast.makeText(Profile.this, "Email Success", Toast.LENGTH_SHORT).show();
                                        //}
                                    }
                                });
                            }
//                        //----------------------------------------------------------\\
//                        //----------------Code for Changing Password----------\\
//                        user.updatePassword(password).addOnCompleteListener(new OnCompleteListener<Void>() {
//                            @Override
//                            public void onComplete(@NonNull Task<Void> task) {
//                                if (task.isSuccessful()) {
//                                    Toast.makeText(Profile.this, "Password Success", Toast.LENGTH_SHORT).show();
//                                }
//                            }
//                        });
//                        //----------------------------------------------------------\\
                        } else {
                            hideKeyboardFrom(Profile.this);
                            Toast.makeText(Profile.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.profileBtn) {
            if(!TextUtils.isEmpty(profileEmail.getText().toString()) && !TextUtils.isEmpty(profilePassword)){
                reAuthenticateUser(user.getUid(), profileEmail.getText().toString(), profilePassword);
            }
            updateProfile(user.getUid(), profileName.getText().toString(), profileMobile.getText().toString(), profileUserType.getText().toString(), profileUserService.getText().toString(), profileBloodGroup.getText().toString());
        }
    }

    // Select Image method
    private void SelectImage()
    {
        // Defining Implicit Intent to mobile gallery
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(
                Intent.createChooser(
                        intent,
                        "Select Image from here..."),
                PICK_IMAGE_REQUEST);
    }

    // Override onActivityResult method
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data)
    {

        super.onActivityResult(requestCode,
                resultCode,
                data);

        // checking request code and result code
        // if request code is PICK_IMAGE_REQUEST and
        // resultCode is RESULT_OK
        // then set image in the image view
        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            // Get the Uri of data
            filePath = data.getData();
            /*try {

                // Setting image on image view using Bitmap
                Bitmap bitmap = MediaStore
                        .Images
                        .Media
                        .getBitmap(
                                getContentResolver(),
                                filePath);
                imageView.setImageBitmap(bitmap);
            }

            catch (IOException e) {
                // Log the exception
                e.printStackTrace();
            }*/
        }
    }

    // UploadImage method
    private void uploadImage()
    {
        if (filePath != null) {

            // Code for showing progressDialog while uploading
            final ProgressDialog progressDialog
                    = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            // Defining the child of storageReference
            final StorageReference ref
                    = storageReference
                    .child(
                            "images/"
                                    + UUID.randomUUID().toString());

            // adding listeners on upload
            // or failure of image
            ref.putFile(filePath)
                    .addOnSuccessListener(
                            new OnSuccessListener<UploadTask.TaskSnapshot>() {

                                @Override
                                public void onSuccess(
                                        UploadTask.TaskSnapshot taskSnapshot)
                                {
                                    ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
                                    {
                                        @Override
                                        public void onSuccess(Uri downloadUrl)
                                        {
                                            mDatabase.child("Users").child(user.getUid()).child("image").setValue(downloadUrl.toString());
                                        }
                                    });
                                    // Image uploaded successfully
                                    // Dismiss dialog
                                    progressDialog.dismiss();
                                    View parentLayout = findViewById(R.id.profileRoot);
                                    Snackbar.make(parentLayout, "Profile Updated", Snackbar.LENGTH_LONG).show();
                                }
                            })

                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e)
                        {

                            // Error, Image not uploaded
                            progressDialog.dismiss();
                            Toast
                                    .makeText(Profile.this,
                                            "Failed " + e.getMessage(),
                                            Toast.LENGTH_SHORT)
                                    .show();
                        }
                    })
                    .addOnProgressListener(
                            new OnProgressListener<UploadTask.TaskSnapshot>() {

                                // Progress Listener for loading
                                // percentage on the dialog box
                                @Override
                                public void onProgress(
                                        UploadTask.TaskSnapshot taskSnapshot)
                                {
                                    double progress
                                            = (100.0
                                            * taskSnapshot.getBytesTransferred()
                                            / taskSnapshot.getTotalByteCount());
                                    progressDialog.setMessage(
                                            "Uploaded "
                                                    + (int)progress + "%");
                                }
                            });
        }
    }
}
