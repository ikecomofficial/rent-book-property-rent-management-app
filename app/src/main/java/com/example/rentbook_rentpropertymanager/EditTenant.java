package com.example.rentbook_rentpropertymanager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

public class EditTenant extends AppCompatActivity {

    private TextInputEditText etTenantName, etTenantPhone, etTenantAddress;
    private String tenant_name, tenant_phone, tenant_address;
    private String newTenantName, newTenantPhone, newTenantAddress;
    private DatabaseReference tenantReference, activityLogReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_tenant);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        assert user != null;
        String user_id = user.getUid();

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.text_toolbar_edit_tenant_details);
        }
        String room_id = getIntent().getStringExtra("room_id");
        String tenant_id = getIntent().getStringExtra("tenant_id");
        tenant_name = getIntent().getStringExtra("tenant_name");
        tenant_phone = getIntent().getStringExtra("tenant_phone");
        tenant_address = getIntent().getStringExtra("tenant_address");

        assert room_id != null;
        assert tenant_id != null;
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        tenantReference = databaseReference.child("tenants")
                .child(room_id).child(tenant_id);
        activityLogReference = databaseReference.child("activity_log").child(user_id);

        etTenantName = findViewById(R.id.etEditTenantName);
        etTenantPhone = findViewById(R.id.etEditTenantPhone);
        etTenantAddress = findViewById(R.id.etEditTenantAddress);
        MaterialCardView updateTenant = (MaterialCardView) findViewById(R.id.btnUpdateTenant);

        etTenantName.setText(tenant_name);
        if (tenant_phone.length() > 10){
            tenant_phone = tenant_phone.substring(tenant_phone.length() - 10);
        }
        etTenantPhone.setText(tenant_phone);
        etTenantAddress.setText(String.valueOf(tenant_address));

        updateTenant.setOnClickListener(view -> updateTenantToFirebase());
    }

    private void updateTenantToFirebase(){
        newTenantName = etTenantName.getText().toString().trim();
        newTenantPhone = etTenantPhone.getText().toString().trim();
        newTenantAddress = etTenantAddress.getText().toString().trim();

        if (newTenantName.isEmpty()) {
            etTenantName.setError("Enter Tenant Name");
            return;
        }if (newTenantPhone.isEmpty()) {
            etTenantPhone.setError("Enter Enter Tenant Phone Number");
            return;
        }
        if (newTenantPhone.startsWith("+91")){
            Toast.makeText(this, "Remove Country Code", Toast.LENGTH_SHORT).show();
            return;
        } else if (newTenantPhone.length() < 10) {
            Toast.makeText(this, "Enter Valid Mobile Number", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newTenantAddress.isEmpty()) {
            etTenantAddress.setError("Enter Tenant Address");
            return;
        }
        HashMap<String, Object> tenantUpdateMap = new HashMap<>();

        if(!newTenantName.equals(tenant_name)){
            tenantUpdateMap.put("tenant_name", newTenantName);
        }
        if(!newTenantPhone.equals(tenant_phone)){
            String tenantMobile = "+91 " + newTenantPhone;
            tenantUpdateMap.put("tenant_phone", tenantMobile);
        }
        if(!newTenantAddress.equals(tenant_address)){
            tenantUpdateMap.put("tenant_address", newTenantAddress);
        }
        if (tenantUpdateMap.isEmpty()){
            Toast.makeText(this, "No Changes Made", Toast.LENGTH_SHORT).show();
            return;
        }
        tenantReference.updateChildren(tenantUpdateMap).addOnSuccessListener(aVoid -> {
            editTenantActivityLog();
                    Toast.makeText(this, "Tenant Updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());


    }

    public void editTenantActivityLog(){

        String finalLogTitle = "Tenant Updated";

        //

        String finalLogDesc = "Updated Tenant - " + newTenantName + " from " + newTenantAddress + " with contact number: "
                + newTenantPhone;

        long currTimestamp = System.currentTimeMillis();

        // Create unique Activity Log ID
        String log_id = activityLogReference.push().getKey();
        HashMap<String, Object> logMap = new HashMap<>();
        logMap.put("log_title", finalLogTitle);
        logMap.put("log_desc", finalLogDesc);
        logMap.put("log_entity", "TENANT");
        logMap.put("log_type", "TENANT_EDITED");
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