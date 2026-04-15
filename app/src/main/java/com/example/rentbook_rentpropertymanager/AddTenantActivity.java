package com.example.rentbook_rentpropertymanager;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AddTenantActivity extends AppCompatActivity {

    private TextInputEditText etTenantName, etTenantPhone, etTenantAddress;
    private static final int PICK_CONTACT = 1001;
    private String user_id, room_id, tenant_id, property_id;
    private String tenantName, tenantPhone, tenantAddress;
    private boolean is_room;
    private DatabaseReference tenantReference;
    private DatabaseReference roomReference;
    private DatabaseReference propertyReference, activityLogReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_tenant);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add New Tenant");
        }

        room_id = getIntent().getStringExtra("room_id");
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        assert user != null;
        user_id = user.getUid();

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        roomReference = databaseReference.child("rooms").child(room_id);
        tenantReference = databaseReference.child("tenants");
        propertyReference = databaseReference.child("properties");
        activityLogReference = databaseReference.child("activity_log").child(user_id);

        etTenantName = findViewById(R.id.editTextTenantName);
        etTenantPhone = findViewById(R.id.editTextTenantPhone);
        etTenantAddress = findViewById(R.id.editTextTenantAddress);
        MaterialCardView btnAddTenant = findViewById(R.id.btnAddTenant);
        ImageView btnPickContact = findViewById(R.id.btnPickContact);

        btnAddTenant.setOnClickListener(view -> {
            if (saveTenantToFirebase()){
                updateRoomDatabase();
                updatePropertyOccupancyDataInFirebase();
            }
        });
        btnPickContact.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(intent, PICK_CONTACT);
        });

    }

    private boolean saveTenantToFirebase(){
        tenantName = etTenantName.getText().toString().trim();
        tenantPhone = etTenantPhone.getText().toString().trim();
        tenantAddress = etTenantAddress.getText().toString().trim();
        String todayDate = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
                .format(new java.util.Date());

        if (tenantName.isEmpty()) {
            etTenantName.setError("Enter Tenant Name");
            return false;
        }
        if (tenantPhone.startsWith("+91")){
            Toast.makeText(this, "Remove Country Code", Toast.LENGTH_SHORT).show();
            return false;
        } else if (tenantPhone.length() < 10 && !tenantPhone.isEmpty()) {
            Toast.makeText(this, "Enter Valid Mobile Number", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (tenantAddress.isEmpty()) {
            etTenantAddress.setError("Enter City/Address");
            return false;
        }

        String tenantNumber;
        if (tenantPhone.isEmpty()){
            tenantNumber = "N/A";
        }else {
            tenantNumber = "+91 " + tenantPhone;
        }

        // Create unique tenant ID
        tenant_id = tenantReference.push().getKey();
        HashMap<String, Object> tenantMap = new HashMap<>();
        tenantMap.put("tenant_name", tenantName);
        tenantMap.put("tenant_phone", tenantNumber);
        tenantMap.put("tenant_address", tenantAddress);
        tenantMap.put("tenant_profile_url", "default");
        tenantMap.put("thumb_tenant_url", "default");
        tenantMap.put("tenant_start_date", todayDate);
        tenantMap.put("tenant_end_date", "null");
        tenantMap.put("billing_start_day", 1);
        tenantMap.put("user_id", user_id);

        if (tenant_id != null) {
            tenantReference.child(room_id).child(tenant_id).setValue(tenantMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Tenant Added Successfully", Toast.LENGTH_SHORT).show();
                        tenantAddedActivityLog();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
        return true;
    }

    private void updateRoomDatabase(){
        Map<String, Object> roomUpdates = new HashMap<>();
        roomUpdates.put("is_occupied", true);
        roomUpdates.put("tenant_id", tenant_id);
        roomReference.updateChildren(roomUpdates);
    }

    private void updatePropertyOccupancyDataInFirebase(){

        // Get pid from rooms -> rid data
        roomReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                property_id = snapshot.child("property_id").getValue(String.class);
                is_room = Boolean.TRUE.equals(snapshot.child("is_room").getValue(Boolean.class));

                propertyReference.child(property_id)
                        .runTransaction(new Transaction.Handler() {

                            @NonNull
                            @Override
                            public Transaction.Result doTransaction(@NonNull MutableData data) {

                                if (is_room) {
                                    Integer currentOccRooms =
                                            data.child("rooms_occupied").getValue(Integer.class);

                                    if (currentOccRooms == null) currentOccRooms = 0;

                                    data.child("rooms_occupied").setValue(currentOccRooms + 1);

                                } else {
                                    Integer currentOccShops =
                                            data.child("shops_occupied").getValue(Integer.class);

                                    if (currentOccShops == null) currentOccShops = 0;

                                    data.child("shops_occupied").setValue(currentOccShops + 1);
                                }

                                return Transaction.success(data);
                            }

                            @Override
                            public void onComplete(
                                    DatabaseError error,
                                    boolean committed,
                                    DataSnapshot snapshot
                            ) {
                                if (error != null) {
                                    Log.e("Firebase", "Occupancy update failed", error.toException());
                                } else if (committed) {
                                    Log.d("Firebase", "Occupancy updated successfully");
                                }
                            }
                        });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CONTACT && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};

            assert contactUri != null;
            try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    String number = cursor.getString(numberIndex);
                    etTenantPhone.setText(cleanPhoneNumberPick(number));
                }
            }
        }
    }

    // Format Picked contact to proper 10 digit number
    private String cleanPhoneNumberPick(String cleanNumber){

        if (cleanNumber == null) return "";

        // 1️⃣ Remove all spaces
        String cleaned = cleanNumber.replaceAll("\\s+", "");

        // 2️⃣ Remove any non-digit characters (like +, -, etc.)
        cleaned = cleaned.replaceAll("\\D", "");

        // 3️⃣ If there are more than 10 digits, take the last 10
        if (cleaned.length() > 10) {
            cleaned = cleaned.substring(cleaned.length() - 10);
        }

        return cleaned;

    }

    public void tenantAddedActivityLog(){

        String finalLogTitle = "Tenant Added";

        String finalLogDesc = "Tenant: " + tenantName + " (" + tenantPhone + ") " +
                " added at " + tenantAddress;

        long currTimestamp = System.currentTimeMillis();

        // Create unique Activity Log ID
        String log_id = activityLogReference.push().getKey();
        HashMap<String, Object> logMap = new HashMap<>();
        logMap.put("log_title", finalLogTitle);
        logMap.put("log_desc", finalLogDesc);
        logMap.put("log_entity", "TENANT");
        logMap.put("log_type", "TENANT_ADDED");
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