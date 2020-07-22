package com.example.emergencyrescue;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AddContact extends MainActivity implements
        View.OnClickListener  {

    Button addContactBtn;
    ListView showContactList;
    ArrayList<String> listShowContacts = new ArrayList<>();
    ArrayAdapter<String> adapter = null;

    DatabaseReference dreference;
    private FirebaseAuth mAddAuthentication;
    String currentUserId;

    private ValueEventListener mValueListner;
    private ChildEventListener mChildListner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createDynamicView(R.layout.activity_add_contact, R.id.nav_addContact);

        showContactList = findViewById(R.id.showContactList);
        addContactBtn = (Button) findViewById(R.id.addContactBtn);

        mAddAuthentication = FirebaseAuth.getInstance();
        FirebaseUser user = mAddAuthentication.getCurrentUser();
        currentUserId = user.getUid();

        dreference = FirebaseDatabase.getInstance().getReference("EmergencyContacts");
        dreference = dreference.child(currentUserId);



        /* CODE FOR FETCHING DATA AGAINST USER WHEN USER COME TO ADD CONTACT */

        listShowContacts.removeAll(listShowContacts);
        adapter = new ArrayAdapter<String>(AddContact.this, android.R.layout.simple_dropdown_item_1line, listShowContacts);
        showContactList.setAdapter(adapter);

        showContactList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView <?> parent, View view, int position, long id) {

                final String item = (String) parent.getItemAtPosition(position);
//                    Toast.makeText(AddContact.this,item,Toast.LENGTH_LONG).show();

                AlertDialog diaBox = AskOption(item,position);
                diaBox.show();
            }

            private AlertDialog AskOption(final String item, final int position) {

                AlertDialog myQuittingDialogBox = new AlertDialog.Builder(AddContact.this)
                        // set message, title, and icon
                        .setTitle("Delete")
                        .setMessage("Do you want to Delete")

                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {

                                //your deleting code

                                dialog.dismiss();

                                /* DELETE LIST POSITIION CHILD */

                                String[] itemSeparated = item.split("\n");

                                String contactId   = ((itemSeparated[0].split(":"))[1]).trim();

                                listShowContacts.remove(position);
                                adapter.notifyDataSetChanged();

                                dreference.child(contactId).removeValue();

                                /* DELETE LIST POSITIION CHILD */

                            }

                        })
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();

                            }
                        })
                        .create();

                return myQuittingDialogBox;
            }
        });


        mValueListner = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {

                    String Contactname = childSnapshot.child("contactName").getValue(String.class);
                    String ContactNumber = childSnapshot.child("contactNumber").getValue(String.class);
                    String ContactKey = childSnapshot.getKey();

                    listShowContacts.add("ID: " + ContactKey + "\n" + "Name: " + Contactname + "\n" + "Phone No: " + ContactNumber);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        dreference.addValueEventListener(mValueListner);

        /* CODE FOR FETCHING DATA AGAINST USER WHEN USER COME TO ADD CONTACT */


        /* CODE FOR START ACTIVITY LOAD_CONTACT */

        addContactBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                dreference.removeEventListener(mValueListner);
                Intent intent = new Intent(AddContact.this, LoadContact.class);
                startActivityForResult(intent, 1);

            }
        });

        /* CODE FOR START ACTIVITY LOAD_CONTACT */
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        dreference.removeEventListener(mValueListner);
        dreference.removeEventListener(mChildListner);
    }


    /* CODE TO DESTROY VALUE EVENT LISTENER */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dreference.removeEventListener(mValueListner);
        dreference.removeEventListener(mChildListner);
    }

    /* CODE TO DESTROY VALUE EVENT LISTENER */


    /* THIS CODE RUN WHEN SELECT CONTACT ACTIVITY ENDS */


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1){
            if(resultCode == RESULT_OK){

                listShowContacts.removeAll(listShowContacts);
                showContactList.setAdapter(adapter);

                mChildListner = new ChildEventListener(){
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                        String Contactname = dataSnapshot.child("contactName").getValue(String.class);
                        String ContactNumber = dataSnapshot.child("contactNumber").getValue(String.class);
                        String ContactKey = dataSnapshot.getKey();

                        listShowContacts.add("ID: " + ContactKey + "\n" + "Name: " + Contactname + "\n" + "Phone No: " + ContactNumber);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

//                        listShowContacts.remove(dataSnapshot.getValue(String.class));
//                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                        Toast.makeText(AddContact.this, "Child Moved", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                };

                dreference.addChildEventListener(mChildListner);
            }
        }
    }


    /* THIS CODE RUN WHEN SELECT CONTACT ACTIVITY ENDS */

    @Override
    public void onClick(View v) {
        int i = v.getId();
    }
}
