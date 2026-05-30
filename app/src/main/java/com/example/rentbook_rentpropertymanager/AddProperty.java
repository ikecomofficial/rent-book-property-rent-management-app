package com.example.rentbook_rentpropertymanager;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class AddProperty extends AppCompatActivity {

    private EditText etPropertyName, etPropertyAddress, etDefaultRentAmount, etUnitRate;
    private TextView textTotalRooms, textTotalShops;
    private int currTotalRooms = 0, currTotalShops = 0;
    private String user_id, pid;
    private long currTimestamp;
    private String propertyName, propertyAddress;
    private DatabaseReference propertyReference;
    private DatabaseReference activityLogReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_property);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.text_toolbar_add_new_prop);
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        assert user != null;
        user_id = user.getUid();

        etPropertyName = findViewById(R.id.editTextPropertyName);
        etPropertyAddress = findViewById(R.id.editTextPropertyAddress);
        etDefaultRentAmount = findViewById(R.id.editTextDefaultRent);
        etUnitRate = findViewById(R.id.editTextUnitRate);
        MaterialCardView mcvBtnRoomsMinus = findViewById(R.id.mcvBtnRoomsMinus);
        textTotalRooms = findViewById(R.id.textTotalRooms);
        MaterialCardView mcvBtnRoomsPlus = findViewById(R.id.mcvBtnRoomsPlus);
        MaterialCardView mcvBtnShopsMinus = findViewById(R.id.mcvBtnShopsMinus);
        textTotalShops = findViewById(R.id.textTotalShops);
        MaterialCardView mcvBtnShopsPlus = findViewById(R.id.mcvBtnShopsPlus);
        MaterialCardView btnCreateProperty = findViewById(R.id.btnCreateProperty);

        textTotalRooms.setText(String.valueOf(currTotalRooms));
        textTotalShops.setText(String.valueOf(currTotalShops));

        btnCreateProperty.setOnClickListener(view -> {
            if (savePropertyToFirebase()){
                createRoomsShopsInFirebase();
                addPropertyActivityLog();
            }
        });

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        propertyReference = databaseReference.child("properties").child(user_id);
        activityLogReference = databaseReference.child("activity_log").child(user_id);

        //Minus Buttons Action On click
        mcvBtnRoomsMinus.setOnClickListener(view -> {
            if (currTotalRooms > 0){
                currTotalRooms--;
                textTotalRooms.setText(String.valueOf(currTotalRooms));
            }
        });
        mcvBtnShopsMinus.setOnClickListener(view -> {
            if (currTotalShops > 0){
                currTotalShops--;
                textTotalShops.setText(String.valueOf(currTotalShops));
            }
        });

        // Plus Buttons Action On click
        mcvBtnRoomsPlus.setOnClickListener(view -> {
            currTotalRooms++;
            textTotalRooms.setText(String.valueOf(currTotalRooms));
        });
        mcvBtnShopsPlus.setOnClickListener(view -> {
            currTotalShops++;
            textTotalShops.setText(String.valueOf(currTotalShops));
        });
    }

    private boolean savePropertyToFirebase(){

        propertyName = Objects.requireNonNull(etPropertyName.getText()).toString().trim();
        propertyAddress = Objects.requireNonNull(etPropertyAddress.getText()).toString().trim();
        String propertyDefaultRent = Objects.requireNonNull(etDefaultRentAmount.getText()).toString().trim();
        String propertyDefaultUnitRate = Objects.requireNonNull(etUnitRate.getText()).toString().trim();

        currTimestamp = System.currentTimeMillis();

        if (propertyName.isEmpty()) {
            etPropertyName.setError("Enter property name");
            return false;
        }
        if (propertyAddress.isEmpty()) {
            etPropertyAddress.setError("Enter city/address");
            return false;
        }
        if (currTotalRooms == 0 && currTotalShops == 0){
            Toast.makeText(AddProperty.this, "Please Add Rooms or Shops", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (propertyDefaultRent.isEmpty()) {
            etDefaultRentAmount.setError("Enter Rent");
            return false;
        }
        if (propertyDefaultUnitRate.isEmpty()){
            etUnitRate.setError("Enter Electricity Unit Rate");
            return false;
        }

        // Create unique property ID
        pid = propertyReference.push().getKey();
        HashMap<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("property_name", propertyName);
        propertyMap.put("property_address", propertyAddress);
        propertyMap.put("prop_room_rent", Integer.parseInt(propertyDefaultRent));
        propertyMap.put("prop_unit_rate", Double.parseDouble(propertyDefaultUnitRate));
        propertyMap.put("total_rooms", Integer.parseInt(String.valueOf(currTotalRooms)));
        propertyMap.put("total_shops", Integer.parseInt(String.valueOf(currTotalShops)));
        propertyMap.put("rooms_occupied", 0);
        propertyMap.put("shops_occupied", 0);
        propertyMap.put("property_created_on", currTimestamp);

        if (pid != null){
            propertyReference.child(pid).setValue(propertyMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Property Added Successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
        return true;
    }

    private void createRoomsShopsInFirebase() {
        DatabaseReference roomsReference = FirebaseDatabase.getInstance().getReference().child("rooms").child(pid);

        for (int i = 1; i <= currTotalRooms; i++) {
            String room_id = roomsReference.push().getKey();
            if (room_id != null) {
                HashMap<String, Object> roomsMap = new HashMap<>();
                roomsMap.put("room_no", i);
                roomsMap.put("room_name", String.format(Locale.US, "Room %02d", i));
                roomsMap.put("room_rent", Integer.parseInt(etDefaultRentAmount.getText().toString().trim()));
                roomsMap.put("elc_unit_rate", Double.parseDouble(etUnitRate.getText().toString().trim()));
                roomsMap.put("user_id", user_id);
                roomsMap.put("property_id", pid);
                roomsMap.put("is_room", true);
                roomsMap.put("is_occupied", false);
                roomsMap.put("created_on", currTimestamp);
                roomsMap.put("is_rent_custom", false);
                roomsMap.put("is_unit_custom", false);

                // Last month paid monthKey.
                roomsMap.put("tenant_id", "null");
                roomsMap.put("last_unit_paid", 0);
                roomsMap.put("last_rent_month", "2025-07");

                roomsReference.child(room_id).setValue(roomsMap)
                        .addOnSuccessListener(aVoid -> {
                            finish();
                        });

            }
        }
        for (int i = 1; i <= currTotalShops; i++) {
            String room_id = roomsReference.push().getKey();
            if (room_id != null) {
                HashMap<String, Object> roomsMap = new HashMap<>();
                roomsMap.put("room_no", currTotalRooms + i);
                roomsMap.put("room_name", String.format(Locale.US, "Shop %02d", i));
                roomsMap.put("room_rent", Integer.parseInt(etDefaultRentAmount.getText().toString().trim()));
                roomsMap.put("elc_unit_rate", Double.parseDouble(etUnitRate.getText().toString().trim()));
                roomsMap.put("user_id", user_id);
                roomsMap.put("property_id", pid);
                roomsMap.put("is_room", false);
                roomsMap.put("is_occupied", false);
                roomsMap.put("created_on", currTimestamp);
                roomsMap.put("is_rent_custom", false);
                roomsMap.put("is_unit_custom", false);

                // Last month paid monthKey.
                roomsMap.put("tenant_id", "null");
                roomsMap.put("last_unit_paid", 0);
                roomsMap.put("last_rent_month", "2025-07");

                roomsReference.child(room_id).setValue(roomsMap)
                        .addOnSuccessListener(aVoid -> {
                            finish();
                        });
            }
        }
    }

    public void addPropertyActivityLog(){

        String finalLogTitle = "Property Added";

        String finalLogDesc = propertyAddress + " • Rooms: " + currTotalRooms + " • Shops: " + currTotalShops;

        // Create unique Activity Log ID
        String log_id = activityLogReference.push().getKey();
        HashMap<String, Object> logMap = new HashMap<>();
        logMap.put("log_title", finalLogTitle);
        logMap.put("log_desc", finalLogDesc);
        logMap.put("log_primary_value", propertyName);
        logMap.put("log_entity", "PROPERTY");
        logMap.put("log_type", "PROP_ADDED");
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