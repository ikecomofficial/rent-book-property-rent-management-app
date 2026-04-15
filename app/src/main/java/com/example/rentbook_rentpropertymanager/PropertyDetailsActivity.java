package com.example.rentbook_rentpropertymanager;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rentbook_rentpropertymanager.adapter.RoomCardAdapter;
import com.example.rentbook_rentpropertymanager.model.Rooms;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PropertyDetailsActivity extends AppCompatActivity {

    // 📊 Property Info
    private String property_name, property_address, property_id;
    private int property_default_rent;
    private double property_unit_rate;

    // 📈 Occupancy Stats
    private long rooms_occupied, shops_occupied, total_rooms, total_shops;
    private TextView tvProgressLabel;
    private CircularProgressIndicator occupancyProgressBar;

    // 💰 Monthly Summary (Rent & Electricity)
    private TextView tvMonthTotalRent, tvMonthTotalElcBill, tvMonthTotalUnitsUsed;

    // 🏠 Room Data & Adapter
    private RoomCardAdapter roomCardAdapter;
    private List<Rooms> roomsList;

    // 🔗 Firebase References
    private DatabaseReference roomsReference;
    private DatabaseReference propertiesReference, databaseReference, collectionsReference, activityLogReference;
    private DatabaseReference tenantReference;
    private DatabaseReference rentsReference;
    private DatabaseReference e_billReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_property_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        assert user != null;
        String user_id = user.getUid();

        databaseReference = FirebaseDatabase.getInstance().getReference();
        property_id = getIntent().getStringExtra("property_id");
        property_name = getIntent().getStringExtra("property_name");
        property_address = getIntent().getStringExtra("property_address");


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(property_name);
            getSupportActionBar().setSubtitle(property_address);
        }

        propertiesReference = databaseReference.child("properties").child(property_id);
        roomsReference = databaseReference.child("rooms");
        tenantReference = databaseReference.child("tenants");
        rentsReference = databaseReference.child("rents");
        e_billReference = databaseReference.child("e-bills");
        collectionsReference = databaseReference.child("collections").child(property_id);
        activityLogReference = databaseReference.child("activity_log").child(user_id);

        // 📈 Occupancy Stats
        tvProgressLabel = findViewById(R.id.tvProgressLabel);
        occupancyProgressBar = findViewById(R.id.occupancyProgressBar);

        // 💰 Monthly Summary (Rent & Electricity)
        tvMonthTotalRent = findViewById(R.id.tvMonthTotalRent);
        tvMonthTotalElcBill = findViewById(R.id.tvMonthTotalElcBill);
        tvMonthTotalUnitsUsed = findViewById(R.id.tvMonthTotalUnitsUsed);

        // 📅 Current Month Info
        TextView currMonthYear = findViewById(R.id.currMonthYear);
        currMonthYear.setText(getCurrentMonthYear());

        // 🏠 Rooms List
        RecyclerView roomsRecyclerView = findViewById(R.id.roomsListRecyclerView);

        roomsRecyclerView.setHasFixedSize(true);
        roomsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        roomsList = new ArrayList<>();
        roomCardAdapter = new RoomCardAdapter(this, roomsList);
        roomCardAdapter.setOnItemClickListener(room -> {
            Intent roomDetailsIntent = new Intent(PropertyDetailsActivity.this, RoomDetailsActivity.class);
            roomDetailsIntent.putExtra("property_id", property_id);
            roomDetailsIntent.putExtra("room_id", room.getRoom_id());
            roomDetailsIntent.putExtra("room_name", room.getRoom_name());
            roomDetailsIntent.putExtra("property_name", property_name);
            roomDetailsIntent.putExtra("is_room", room.isIs_room());

            startActivity(roomDetailsIntent);

        });

        roomsRecyclerView.setAdapter(roomCardAdapter);
        propertiesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    Integer rent = snapshot.child("prop_room_rent").getValue(Integer.class);
                    property_default_rent = rent != null ? rent : 0;

                    Double unitRate = snapshot.child("prop_unit_rate").getValue(Double.class);
                    property_unit_rate = unitRate != null ? unitRate : 0.0;

                    Long rOcc = snapshot.child("rooms_occupied").getValue(Long.class);
                    rooms_occupied = rOcc != null ? rOcc : 0;

                    Long sOcc = snapshot.child("shops_occupied").getValue(Long.class);
                    shops_occupied = sOcc != null ? sOcc : 0;

                    Long tRooms = snapshot.child("total_rooms").getValue(Long.class);
                    total_rooms = tRooms != null ? tRooms : 0;

                    Long tShops = snapshot.child("total_shops").getValue(Long.class);
                    total_shops = tShops != null ? tShops : 0;

                    long totalRoomShopOcc = rooms_occupied + shops_occupied;
                    long totalRoomShop = total_rooms + total_shops;

                    setProgressBarData((int) totalRoomShopOcc, (int) totalRoomShop);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        loadCurrentMonthCollectionFromFirebase();
        loadRooms();

    }

    private String getCurrentMonthYear() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.ENGLISH);
        return sdf.format(new Date());
    }

    public void setProgressBarData(int occupied, int total) {
        if (total > 0) {
            int bar_percentage = (int) ((occupied * 100) / total);

            occupancyProgressBar.setMax(100); // make sure max is 100
            occupancyProgressBar.setProgress(bar_percentage);

            // Update label
            String progressLabelPercent = occupied + "/" + total;
            tvProgressLabel.setText(progressLabelPercent);
        } else {
            occupancyProgressBar.setProgress(0);
            tvProgressLabel.setText("N/A");
        }
    }

    public void loadCurrentMonthCollectionFromFirebase() {
        Date date = new Date();

        String monthYearLabel =
                new SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(date);

        String monthYearKey =
                new SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(date);

        DatabaseReference collectionsReference = databaseReference.child("collections").child(property_id).child(monthYearKey);

        collectionsReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer rentVal = snapshot.child("total_rent").getValue(Integer.class);
                    int currentMonthRent = rentVal != null ? rentVal : 0;

                    Integer elcBillVal = snapshot.child("total_elc_bill").getValue(Integer.class);
                    int currentMonthElcBill = elcBillVal != null ? elcBillVal : 0;

                    Integer unitsUsedVal = snapshot.child("total_units_used").getValue(Integer.class);
                    int currentMonthUnitsUsed = unitsUsedVal != null ? unitsUsedVal : 0;

                    tvMonthTotalRent.setText(formatAmount(currentMonthRent));
                    tvMonthTotalElcBill.setText(formatAmount(currentMonthElcBill));
                    tvMonthTotalUnitsUsed.setText(String.valueOf(currentMonthUnitsUsed));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // handle error
            }
        });
    }

    public static String formatAmount(int amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        format.setMaximumFractionDigits(0);
        format.setMinimumFractionDigits(0);
        return format.format(amount);
    }


    private void loadRooms() {
        roomsReference.orderByChild("property_id").equalTo(property_id)
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot roomsSnapshot) {
                        roomsList.clear();

                        for (DataSnapshot roomSnap : roomsSnapshot.getChildren()) {
                            String roomId = roomSnap.getKey();
                            String roomName = roomSnap.child("room_name").getValue(String.class);
                            Integer rentAmount = roomSnap.child("room_rent").getValue(Integer.class);
                            Integer lastUnitPaid = roomSnap.child("last_unit_paid").getValue(Integer.class);
                            Boolean isOccupied = roomSnap.child("is_occupied").getValue(Boolean.class);
                            String tenantId = roomSnap.child("tenant_id").getValue(String.class);
                            Integer roomNo = roomSnap.child("room_no").getValue(Integer.class);
                            Boolean isRoom = roomSnap.child("is_room").getValue(Boolean.class);

                            Rooms model = new Rooms();
                            model.setRoom_id(roomId);
                            model.setRoom_name(roomName);
                            model.setRoom_rent(rentAmount != null ? rentAmount : 0);
                            model.setLast_unit_paid(lastUnitPaid != null ? lastUnitPaid : 0);

                            model.setIs_occupied(isOccupied != null && isOccupied);
                            model.setRoom_no(roomNo != null ? roomNo : 0);
                            model.setIs_room(isRoom != null && isRoom);

                            if (tenantId != null && !tenantId.equals("null") && !tenantId.isEmpty()) {
                                assert roomId != null;
                                tenantReference.child(roomId).child(tenantId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @SuppressLint("NotifyDataSetChanged")
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot tenantSnap) {
                                        if (tenantSnap.exists()) {
                                            model.setTenant_id(tenantId);
                                            model.setTenant_name(tenantSnap.child("tenant_name").getValue(String.class));
                                            model.setTenant_phone(tenantSnap.child("tenant_phone").getValue(String.class));
                                            model.setThumb_tenant_url(tenantSnap.child("thumb_tenant_url").getValue(String.class));
                                        } else {
                                            model.setTenant_name("No Tenant");
                                            model.setTenant_phone("");
                                            model.setThumb_tenant_url(null);
                                        }
                                        roomsList.add(model);
                                        // 🔥 sort by room_name (numeric if possible)
                                        roomsList.sort(Comparator.comparingInt(Rooms::getRoom_no));
                                        roomCardAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                    }
                                });

                            } else {
                                model.setTenant_name("No Tenant");
                                model.setTenant_phone("");
                                model.setThumb_tenant_url(null);
                                roomsList.add(model);
                                // 🔥 sort by room_name (numeric if possible)
                                roomsList.sort(Comparator.comparingInt(Rooms::getRoom_no));
                                roomCardAdapter.notifyDataSetChanged();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void deletePropertyFromFirebase() {

        // Query all rooms
        roomsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot roomDeleteSnap : snapshot.getChildren()) {
                    String roomPropertyId = roomDeleteSnap.child("property_id").getValue(String.class);

                    if (roomPropertyId != null && roomPropertyId.equals(property_id)) {
                        String del_room_id = roomDeleteSnap.getKey();

                        // Delete Tenant, Rent and Elc Bills for all rooms of the opened property.
                        assert del_room_id != null;
                        rentsReference.child(del_room_id).removeValue();
                        e_billReference.child(del_room_id).removeValue();
                        tenantReference.child(del_room_id).removeValue();
                        // Delete this room
                        roomsReference.child(del_room_id).removeValue();
                    }
                }
                collectionsReference.removeValue().addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(PropertyDetailsActivity.this, "Failed to delete collections", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    propertiesReference.removeValue().addOnCompleteListener(task2 -> {
                        if (task2.isSuccessful()) {
                            Toast.makeText(PropertyDetailsActivity.this, "Property Deleted", Toast.LENGTH_SHORT).show();
                            deletePropertyActivityLog();
                            finish();
                        } else {
                            Toast.makeText(PropertyDetailsActivity.this, "Failed to delete property", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), "Failed to delete rooms: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void propertyDeleteConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String final_title = "Delete Property: " + property_name + "?";
        builder.setTitle(final_title);
        builder.setMessage("This will permanently delete all data for this property."); //To confirm type DELETE below.

        // Positive button -> Yes
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Perform deletion here
                deletePropertyFromFirebase();
            }
        });

        // Negative button -> No
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing, just dismiss
                Toast.makeText(PropertyDetailsActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
            }
        });

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void deletePropertyActivityLog(){

        String finalLogTitle = "Property Deleted";

        String finalLogDesc = "Property: " + property_name + ", " + property_address + " deleted.";

        long currTimestamp = System.currentTimeMillis();

        // Create unique Activity Log ID
        String log_id = activityLogReference.push().getKey();
        HashMap<String, Object> logMap = new HashMap<>();
        logMap.put("log_title", finalLogTitle);
        logMap.put("log_desc", finalLogDesc);
        logMap.put("log_entity", "PROPERTY");
        logMap.put("log_type", "PROP_DELETED");
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
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.property_details_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_edit_property) {

            Intent editPropertyIntent = new Intent(PropertyDetailsActivity.this, EditProperty.class);
            editPropertyIntent.putExtra("property_id", property_id);
            editPropertyIntent.putExtra("property_name", property_name);
            editPropertyIntent.putExtra("property_address", property_address);
            editPropertyIntent.putExtra("prop_room_rent", property_default_rent);
            editPropertyIntent.putExtra("prop_unit_rate", property_unit_rate);
            startActivity(editPropertyIntent);

            return true;
        } else if (id == R.id.action_delete_property) {
            // Confirm and delete property
            propertyDeleteConfirmation();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onItemClick(Rooms room) {

    }
}