package com.example.rentbook_rentpropertymanager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class EditRoom extends AppCompatActivity {

    private String room_id, newRoomRent, newUnitRate;
    private int rent_amount = 0;
    private double unit_rate = 0;
    private TextInputEditText etUpdateRoomRent, etUpdateUnitRate;
    private DatabaseReference roomReference, activityLogReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_room);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        String user_id = user.getUid();

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.text_toolbar_edit_room_details);
        }

        room_id = getIntent().getStringExtra("room_id");
        boolean is_room = getIntent().getBooleanExtra("is_room", false);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        roomReference = databaseReference.child("rooms");
        activityLogReference = databaseReference.child("activity_log").child(user_id);

        etUpdateRoomRent = findViewById(R.id.etUpdateRoomRent);
        etUpdateUnitRate = findViewById(R.id.etUpdateUnitRate);
        MaterialCardView btnUpdateRoom = findViewById(R.id.btnUpdateRoom);

        loadRoomDataFromFirebase();

        btnUpdateRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateRoomToFirebase();
            }
        });

    }

    private void updateRoomToFirebase(){
        newRoomRent = etUpdateRoomRent.getText().toString().trim();
        newUnitRate = etUpdateUnitRate.getText().toString().trim();

        if (newRoomRent.isEmpty()) {
            etUpdateRoomRent.setError("Enter Rent Amount");
            return;
        } if (newUnitRate.isEmpty()){
            etUpdateUnitRate.setError("Enter Electricity Rate");
            return;
        }

        HashMap<String, Object> roomUpdateMap = new HashMap<>();
        if (!newRoomRent.equals(String.valueOf(rent_amount))){
            roomUpdateMap.put("room_rent", Integer.parseInt(newRoomRent));
            roomUpdateMap.put("is_rent_custom", true);
        }
        if (!newUnitRate.equals(String.valueOf(unit_rate))){
            roomUpdateMap.put("elc_unit_rate", Double.parseDouble(newUnitRate));
            roomUpdateMap.put("is_unit_custom", true);
        }
        if (roomUpdateMap.isEmpty()){
            Toast.makeText(this, "No Changes Made", Toast.LENGTH_SHORT).show();
            return;
        }

        roomReference.child(room_id).updateChildren(roomUpdateMap).addOnSuccessListener(aVoid -> {
            editRoomActivityLog();
            Toast.makeText(this, "Room Updated", Toast.LENGTH_SHORT).show();
            finish();
        });

    }

    private void loadRoomDataFromFirebase(){
        roomReference.child(room_id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long rent = snapshot.child("room_rent").getValue(Long.class);
                rent_amount = rent != null ? rent.intValue() : 0;
                Double elcRate = snapshot.child("elc_unit_rate").getValue(Double.class);
                unit_rate = elcRate != null ? elcRate : 0;
                etUpdateRoomRent.setText(String.valueOf(rent_amount));
                etUpdateUnitRate.setText(String.valueOf(unit_rate));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void editRoomActivityLog(){

        String finalLogTitle = "Room Updated";

        String finalLogDesc = "Updated Details - rent: " + newRoomRent + " & elc unit rate ₹" + newUnitRate + "/unit";

        long currTimestamp = System.currentTimeMillis();

        // Create unique Activity Log ID
        String log_id = activityLogReference.push().getKey();
        HashMap<String, Object> logMap = new HashMap<>();
        logMap.put("log_title", finalLogTitle);
        logMap.put("log_desc", finalLogDesc);
        logMap.put("log_entity", "PROPERTY");
        logMap.put("log_type", "ROOM_EDITED");
        logMap.put("log_ts", currTimestamp);

        if (log_id != null){
            activityLogReference.child(log_id).setValue(logMap)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("ActivityLog", "Log added successfully");
                    })
                    .addOnFailureListener(e ->
                            Log.e("ActivityLog",
                                    "Failed to add log: " + e.getMessage()));
        }

    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        finish();
    }

}