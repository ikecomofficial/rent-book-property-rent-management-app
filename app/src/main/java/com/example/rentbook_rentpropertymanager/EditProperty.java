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
import java.util.Map;

public class EditProperty extends AppCompatActivity {

    private TextInputEditText etPropertyName, etPropertyAddress, etDefaultRentAmount, etDefaultUnitRate;
    private String user_id, property_id, property_name, property_address;
    private String newPropertyName, newPropertyAddress, newpPropertyDefaultRent, newPropertyUnitRate;
    private int default_rent, newRent;
    private double default_unit_rate, newUnitRate;
    private DatabaseReference propertyReference, roomReference, activityLogReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_property);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.text_toolbar_edit_prop_details);
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        assert user != null;
        user_id = user.getUid();

        property_id = getIntent().getStringExtra("property_id");
        property_name = getIntent().getStringExtra("property_name");
        property_address = getIntent().getStringExtra("property_address");
        default_rent = getIntent().getIntExtra("prop_room_rent",0);
        default_unit_rate = getIntent().getDoubleExtra("prop_unit_rate", 0);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        propertyReference = databaseReference.child("properties").child(property_id);
        roomReference = databaseReference.child("rooms");
        activityLogReference = databaseReference.child("activity_log").child(user_id);

        etPropertyName = findViewById(R.id.etUpdatePropertyName);
        etPropertyAddress = findViewById(R.id.etUpdatePropertyAddress);
        etDefaultRentAmount = findViewById(R.id.etUpdateDefaultRent);
        etDefaultUnitRate = findViewById(R.id.etUpdateUnitRate);
        MaterialCardView updateProperty = findViewById(R.id.btnUpdateProperty);

        etPropertyName.setText(property_name);
        etPropertyAddress.setText(property_address);
        etDefaultRentAmount.setText(String.valueOf(default_rent));
        etDefaultUnitRate.setText(String.valueOf(default_unit_rate));

        updateProperty.setOnClickListener(view -> {
            updatePropertyToFirebase();
            updateRoomRentToFirebase();
        });

    }

    private void updatePropertyToFirebase(){
        newPropertyName = etPropertyName.getText().toString().trim();
        newPropertyAddress = etPropertyAddress.getText().toString().trim();
        newpPropertyDefaultRent = etDefaultRentAmount.getText().toString().trim();
        newPropertyUnitRate = etDefaultUnitRate.getText().toString().trim();

        if (newPropertyName.isEmpty()) {
            etPropertyName.setError("Enter property name");
            return;
        }
        if (newPropertyAddress.isEmpty()) {
            etPropertyAddress.setError("Enter city/address");
            return;
        }
        if (newpPropertyDefaultRent.isEmpty()) {
            etDefaultRentAmount.setError("Enter Rent Amount");
            return;
        }
        if (newPropertyUnitRate.isEmpty()){
            etDefaultUnitRate.setError("Enter Electricity Unit Rate");
            return;
        }

        HashMap<String, Object> propertyUpdateMap = new HashMap<>();
        propertyUpdateMap.put("prop_room_rent", Integer.parseInt(newpPropertyDefaultRent));

        if(!newPropertyName.equals(property_name)){
            propertyUpdateMap.put("property_name", newPropertyName);
        }
        if (!newPropertyAddress.equals(property_address)){
            propertyUpdateMap.put("property_address", newPropertyAddress);
        }
        newRent = Integer.parseInt(newpPropertyDefaultRent);
        if (newRent != default_rent){
            propertyUpdateMap.put("property_default_rent", Integer.parseInt(newpPropertyDefaultRent));
        }
        newUnitRate = Double.parseDouble(newPropertyUnitRate);
        if (newUnitRate != default_unit_rate){
            propertyUpdateMap.put("prop_unit_rate", Double.parseDouble(newPropertyUnitRate));
        }

        propertyReference.updateChildren(propertyUpdateMap).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Property Updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());

    }

    private void updateRoomRentToFirebase(){
        roomReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                for (DataSnapshot roomSnapshot : snapshot.getChildren()){
                    String room_id = roomSnapshot.getKey();
                    String pid = roomSnapshot.child("property_id").getValue(String.class);
                    Boolean isRentCustom = roomSnapshot.child("is_rent_custom").getValue(Boolean.class);
                    Boolean isUnitCustom = roomSnapshot.child("is_unit_custom").getValue(Boolean.class);
                    if (pid != null && pid.equals(property_id)) {
                        if (room_id != null) {
                            if (Boolean.FALSE.equals(isRentCustom)){
                                updates.put(room_id + "/room_rent", newRent);
                            }
                            if (Boolean.FALSE.equals(isUnitCustom)){
                                updates.put(room_id + "/elc_unit_rate", newUnitRate);
                            }
                        }
                    }
                }
                if (!updates.isEmpty()) {
                    roomReference.updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(EditProperty.this, "Rooms Updated", Toast.LENGTH_SHORT).show();
                                editPropertyActivityLog();
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(EditProperty.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void editPropertyActivityLog(){

        String finalLogTitle = "Property Updated";

        String finalLogDesc = "Property Updated - " + newPropertyName + ", " + newPropertyAddress + " with rent: ₹"
                + newpPropertyDefaultRent + " & elc rate ₹" + newPropertyUnitRate + "/unit";

        long currTimestamp = System.currentTimeMillis();

        // Create unique Activity Log ID
        String log_id = activityLogReference.push().getKey();
        HashMap<String, Object> logMap = new HashMap<>();
        logMap.put("log_title", finalLogTitle);
        logMap.put("log_desc", finalLogDesc);
        logMap.put("log_entity", "PROPERTY");
        logMap.put("log_type", "PROP_EDITED");
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