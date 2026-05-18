package com.example.rentbook_rentpropertymanager;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.rentbook_rentpropertymanager.adapter.RentBillPagerAdapter;
import com.example.rentbook_rentpropertymanager.model.Tenants;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;


import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class RoomDetailsActivity extends AppCompatActivity {

    // 🏠 Room & Property Info
    private String room_id, property_id, room_name, property_name;
    private boolean is_occupied, is_room;

    // 👤 Tenant Info
    private String tenant_id, tenant_name, tenant_address, tenant_phone, thumb_tenant_url;
    private String tenant_start_date = "N/A";

    // 📄 Tenant UI Views
    private TextView tvRoomStatus, tvTenantName, tvTenantPhone, tvTenantStartDate;
    private EditText etReading;
    private BottomSheetDialog bottomSheetDialog;
    private CircleImageView cimgTenantProfilePic;
    private LinearLayout layoutViewTenantProfile, layoutViewPastTenant;

    // 🎯 Actions
    private MaterialCardView btnContact;
    private SpeedDialView addRecordSpeedDial;
    private ExtendedFloatingActionButton fabAddTenant;

    // 📊 Room Occupancy UI
    private MaterialCardView cardRoomOccupancy, dotRoomOccupancy;

    // 📑 Tabs & Navigation
    private TabLayout tabLayout;
    private ViewPager2 viewPager2;

    // 🔗 Firebase References
    private DatabaseReference roomReference, activityLogReference, propertyReference, tenantReference, allTenantReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_room_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        room_id = getIntent().getStringExtra("room_id");
        is_occupied = getIntent().getBooleanExtra("is_occupied", false);
        room_name = getIntent().getStringExtra("room_name");
        property_name = getIntent().getStringExtra("property_name");
        is_room = getIntent().getBooleanExtra("is_room", false);
        property_id = getIntent().getStringExtra("property_id");

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        assert user != null;
        String user_id = user.getUid();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(room_name);
            getSupportActionBar().setSubtitle(property_name);
        }

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        roomReference = databaseReference.child("rooms").child(room_id);
        tenantReference = databaseReference.child("tenants");
        propertyReference = databaseReference.child("properties");
        activityLogReference = databaseReference.child("activity_log").child(user_id);
        allTenantReference = tenantReference.child(room_id);


        // 📄 Tenant UI Views
        tvRoomStatus = findViewById(R.id.tvRoomStatus);
        tvTenantName = findViewById(R.id.tvTenantName);
        tvTenantPhone = findViewById(R.id.tvTenantPhone);
        tvTenantStartDate = findViewById(R.id.tvStartDate);
        cimgTenantProfilePic = findViewById(R.id.imgProfile);
        layoutViewTenantProfile = findViewById(R.id.layoutViewTenantProfile);
        layoutViewPastTenant = findViewById(R.id.layoutViewPastTenant);

        // 🎯 Actions
        btnContact = findViewById(R.id.btnContact);
        addRecordSpeedDial = findViewById(R.id.fabAddRecord);
        fabAddTenant = findViewById(R.id.fabAddTenant);

        // 📊 Room Occupancy UI
        cardRoomOccupancy = findViewById(R.id.cardRoomOccupancy);
        dotRoomOccupancy = findViewById(R.id.dotRoomOccupancy);

        // Fragment References
        tabLayout = findViewById(R.id.rentTabLayout);
        viewPager2 = findViewById(R.id.rentViewPager);
        RentBillPagerAdapter rentBillPagerAdapter = new RentBillPagerAdapter(this, room_id, property_id);
        viewPager2.setAdapter(rentBillPagerAdapter);


        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Objects.requireNonNull(tabLayout.getTabAt(position)).select();
            }
        });

        // Keep it expanded initially
        fabAddTenant.extend();

        // Schedule collapse after 10 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (fabAddTenant.isExtended()) {
                fabAddTenant.shrink();
            }
        }, 10_000); // 10 seconds = 10,000 ms

        fabAddTenant.setOnClickListener(view -> {

            Intent addTenantIntent = new Intent(RoomDetailsActivity.this, AddTenantActivity.class);
            addTenantIntent.putExtra("room_id", room_id);
            addTenantIntent.putExtra("room_name", room_name);
            addTenantIntent.putExtra("property_name", property_name);
            startActivity(addTenantIntent);

        });

        btnContact.setOnClickListener(view -> showContactBottomSheet());


        // Add menu items
        addRecordSpeedDial.addActionItem(
                new SpeedDialActionItem.Builder(R.id.fab_add_rent, R.drawable.ic_rupee_symbol)
                        .setFabBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.teal_700, null))
                        .setFabImageTintColor(ContextCompat.getColor(this, R.color.white))
                        .setLabel("Add Rent")
                        .setLabelColor(ContextCompat.getColor(this, R.color.white))
                        .setLabelBackgroundColor(ContextCompat.getColor(this, R.color.primary_main))
                        .create()
        );

        addRecordSpeedDial.addActionItem(
                new SpeedDialActionItem.Builder(R.id.fab_add_elc_bill, R.drawable.ic_meter_reading)
                        .setFabBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.teal_700, null))
                        .setFabImageTintColor(ContextCompat.getColor(this, R.color.white))
                        .setLabel("Add Electricity Bill")
                        .setLabelColor(ContextCompat.getColor(this, R.color.white))
                        .setLabelBackgroundColor(ContextCompat.getColor(this, R.color.primary_main))
                        .create()
        );

        // Handle click actions
        addRecordSpeedDial.getMainFab()
                .setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
        addRecordSpeedDial.setOnActionSelectedListener(actionItem -> {
            if (actionItem.getId() == R.id.fab_add_rent) {

                Intent addRentIntent = new Intent(RoomDetailsActivity.this, AddRentActivity.class);
                addRentIntent.putExtra("room_id", room_id);
                addRentIntent.putExtra("tenant_id", tenant_id);
                addRentIntent.putExtra("property_id", property_id);
                addRentIntent.putExtra("room_name", room_name);
                addRentIntent.putExtra("property_name", property_name);
                startActivity(addRentIntent);

                return false; // closes the speed dial
            } else if (actionItem.getId() == R.id.fab_add_elc_bill) {

                handleAddElectricityBill();
                return false;
            }
            return false;
        });
    }

    private void handleAddElectricityBill() {

        roomReference.child("meter_start_reading")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            // ❌ Not initialized
                            showMeterStartBottomSheet();
                        } else {
                            // ✅ Already initialized
                            openAddElectricityBillScreen();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void showMeterStartBottomSheet() {

        // Create BottomSheetDialog
        bottomSheetDialog = new BottomSheetDialog(this);

        // Inflate layout for bottom sheet
        @SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_meter_start_reading, null, false);
        bottomSheetDialog.setContentView(view);

        // Make sure we modify the bottom-sheet container after it is shown
        bottomSheetDialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // clear default background so your drawable shows through
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
                bottomSheet.setClipToPadding(false);
            }
        });

        etReading = view.findViewById(R.id.etMeterStartReading);
        MaterialCardView btnSave = view.findViewById(R.id.btnSave);
        MaterialCardView btnSaveAndAddBill = view.findViewById(R.id.btnSaveAndAddBill);
        TextView btnCancel = view.findViewById(R.id.btnCancel);

        bottomSheetDialog.show();

        btnCancel.setOnClickListener(v -> {

            bottomSheetDialog.dismiss();

        });

        btnSave.setOnClickListener(v -> validateMeterReading(false));

        btnSaveAndAddBill.setOnClickListener(v -> validateMeterReading(true));

    }

    private void validateMeterReading(boolean goToBills) {

        String input = etReading.getText().toString().trim();

        if (input.isEmpty()) {
            etReading.setError("Enter reading");
            return;
        }

        int reading = Integer.parseInt(input);

        if (reading < 0) {
            etReading.setError("Enter valid reading");
            return;
        }

        // ✅ Only called if valid
        saveMeterReading(reading, bottomSheetDialog, goToBills);
    }

    private void saveMeterReading(int reading, BottomSheetDialog dialog, boolean goToBills) {

        Map<String, Object> map = new HashMap<>();
        map.put("meter_start_reading", reading);
        map.put("last_unit_paid", reading);

        roomReference.updateChildren(map).addOnSuccessListener(unused -> {

            dialog.dismiss();

            if (goToBills) {
                openAddElectricityBillScreen();
            }
        });
    }

    private void openAddElectricityBillScreen() {

        Intent addBillIntent = new Intent(RoomDetailsActivity.this, AddEbillActivity.class);
        addBillIntent.putExtra("room_id", room_id);
        addBillIntent.putExtra("tenant_id", tenant_id);
        addBillIntent.putExtra("property_id", property_id);
        startActivity(addBillIntent);

    }

    private void loadTenantData() {
        tenantReference.child(room_id).child(tenant_id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tenant_name = snapshot.child("tenant_name").getValue(String.class);
                tenant_phone = snapshot.child("tenant_phone").getValue(String.class);
                thumb_tenant_url = snapshot.child("thumb_tenant_url").getValue(String.class);
                tenant_start_date = snapshot.child("tenant_start_date").getValue(String.class);
                tenant_address = snapshot.child("tenant_address").getValue(String.class);

                // ✅ Null-safe UI
                tvTenantName.setText(
                        (tenant_name == null || tenant_name.isEmpty()) ? "N/A" : tenant_name
                );

                tvTenantPhone.setText(
                        (tenant_phone == null || tenant_phone.isEmpty()) ? "N/A" : tenant_phone
                );

                tvTenantStartDate.setText(
                        (tenant_start_date == null || tenant_start_date.isEmpty()) ? "N/A" : tenant_start_date
                );


                if (thumb_tenant_url == null || thumb_tenant_url.trim().isEmpty() || thumb_tenant_url.equals("default")) {
                    // Show only placeholder

                    // ❗ IMPORTANT: Remove tint (because of view recycling)
                    cimgTenantProfilePic.clearColorFilter();

                    if (!isFinishing() && !isDestroyed()) {
                        Glide.with(RoomDetailsActivity.this)
                                .load(R.drawable.ic_tenant_profile_default)
                                .into(cimgTenantProfilePic);
                    }
                } else {
                    if (!isFinishing() && !isDestroyed()) {
                        Glide.with(RoomDetailsActivity.this)
                                .load(thumb_tenant_url)
                                .placeholder(R.drawable.ic_tenant_profile_default)
                                .into(cimgTenantProfilePic);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void showPlaceholderUI() {
        tvTenantName.setText(R.string.text_no_tenant_added);
        tvTenantPhone.setText(R.string.text_clk_add_tenant);
        tvTenantStartDate.setText(R.string.text_na);

        if (!isFinishing() && !isDestroyed()) {
            Glide.with(this)
                    .load(R.drawable.ic_no_tenant_profile_default)
                    .into(cimgTenantProfilePic);

            cimgTenantProfilePic.setImageResource(R.drawable.ic_no_tenant_profile_default);
            // Apply tint
            cimgTenantProfilePic.setColorFilter(
                    ContextCompat.getColor(this, R.color.text_heading),
                    PorterDuff.Mode.SRC_IN
            );

        }
    }

    @Override
    public void onStart() {
        super.onStart();

        roomReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    is_occupied = Boolean.TRUE.equals(snapshot.child("is_occupied").getValue(Boolean.class));
                    tenant_id = snapshot.child("tenant_id").getValue(String.class);

                    //GradientDrawable gradientDrawable = (GradientDrawable) tvRoomStatus.getBackground();

                    if (is_occupied) {
                        addRecordSpeedDial.setVisibility(View.VISIBLE);
                        fabAddTenant.setVisibility(View.GONE);
                        btnContact.setVisibility(View.VISIBLE);

                        tvRoomStatus.setText(R.string.text_occupied);   // "Occupied"
                        tvRoomStatus.setTextColor(
                                ContextCompat.getColor(RoomDetailsActivity.this, R.color.occ_status_text)); // green text

                        cardRoomOccupancy.setCardBackgroundColor(
                                ContextCompat.getColor(RoomDetailsActivity.this, R.color.occ_status_bg));

                        dotRoomOccupancy.setCardBackgroundColor(
                                ContextCompat.getColor(RoomDetailsActivity.this, R.color.occ_status_dot));

                        // Only load tenant data if tenantId exists
                        if (tenant_id != null && !tenant_id.isEmpty() && !"null".equals(tenant_id)) {
                            loadTenantData();
                        } else {
                            showPlaceholderUI();
                        }
                        enableTenantClicks();

                    } else {
                        // ✅ Vacant Room UI
                        fabAddTenant.setVisibility(View.VISIBLE);
                        btnContact.setVisibility(View.GONE);
                        addRecordSpeedDial.setVisibility(View.GONE);
                        tvRoomStatus.setText(R.string.text_vacant);   // "Vacant"
                        tvRoomStatus.setTextColor(
                                ContextCompat.getColor(RoomDetailsActivity.this, R.color.vac_status_text)); // red text

                        cardRoomOccupancy.setCardBackgroundColor(
                                ContextCompat.getColor(RoomDetailsActivity.this, R.color.vac_status_bg));

                        dotRoomOccupancy.setCardBackgroundColor(
                                ContextCompat.getColor(RoomDetailsActivity.this, R.color.vac_status_dot));

                        showPlaceholderUI();
                        disableTenantClicks();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void enableTenantClicks() {
        // Tenant Name
        tvTenantName.setOnClickListener(v -> showTenantProfileActivity());

        // Tenant Phone
        tvTenantPhone.setOnClickListener(v -> showTenantProfileActivity());

        // Tenant Profile Picture
        cimgTenantProfilePic.setOnClickListener(v -> showTenantProfileActivity());

        // Optional: Add visual feedback
        tvTenantName.setAlpha(1f);
        tvTenantPhone.setAlpha(1f);
        cimgTenantProfilePic.setAlpha(1f);

        layoutViewTenantProfile.setVisibility(View.VISIBLE);
        layoutViewPastTenant.setVisibility(View.GONE);

        layoutViewTenantProfile.setOnClickListener(v -> showTenantProfileActivity());
    }

    private void disableTenantClicks() {
        // Remove any previous click listeners
        tvTenantName.setOnClickListener(null);
        tvTenantPhone.setOnClickListener(null);
        cimgTenantProfilePic.setOnClickListener(null);

        // Optional: Make them look inactive
        tvTenantName.setAlpha(0.8f);
        tvTenantPhone.setAlpha(0.8f);
        cimgTenantProfilePic.setAlpha(0.8f);

        // Set view Past Tenants
        layoutViewTenantProfile.setVisibility(View.GONE);
        layoutViewPastTenant.setVisibility(View.VISIBLE);

        layoutViewPastTenant.setOnClickListener(v -> showBottomSheetTenants());
    }

    private void showTenantProfileActivity() {
        Intent tenantProfileIntent = new Intent(RoomDetailsActivity.this, TenantDetailsActivity.class);
        tenantProfileIntent.putExtra("tenant_id", tenant_id);
        tenantProfileIntent.putExtra("room_id", room_id);
        startActivity(tenantProfileIntent);


    }

    private void tenantRemoveFromRoomConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String title = "Remove Tenant: " + tenant_name + "?";
        builder.setTitle(title);
        builder.setMessage("Do you want to remove this tenant from this room.");

        // Positive button -> Yes
        builder.setPositiveButton("Remove", (dialog, which) -> removeTenantFromRoom());

        // Negative button -> No
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Do nothing, just dismiss
            Toast.makeText(RoomDetailsActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
        });

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void removeTenantFromRoom() {

        // Add End date to previous tenant id
        String tenantEndDate = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
                .format(new java.util.Date());

        tenantReference.child(room_id).child(tenant_id).child("tenant_end_date").setValue(tenantEndDate);

        HashMap<String, Object> tenantRemoveMap = new HashMap<>();
        tenantRemoveMap.put("is_occupied", false);
        tenantRemoveMap.put("tenant_id", "null");

        roomReference.updateChildren(tenantRemoveMap);

        updatePropertyDatabase();
        deleteTenantActivityLog();

    }

    private void updatePropertyDatabase() {
        // Now Update the occupied Rooms/Shops in PID
        propertyReference.child(property_id)
                .runTransaction(new Transaction.Handler() {

                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData data) {

                        if (is_room) {
                            Integer currentOccRooms =
                                    data.child("rooms_occupied").getValue(Integer.class);

                            if (currentOccRooms == null) currentOccRooms = 0;

                            data.child("rooms_occupied").setValue(currentOccRooms - 1);

                        } else {
                            Integer currentOccShops =
                                    data.child("shops_occupied").getValue(Integer.class);

                            if (currentOccShops == null) currentOccShops = 0;

                            data.child("shops_occupied").setValue(currentOccShops - 1);
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
                            finish();
                        }
                    }
                });
        Toast.makeText(RoomDetailsActivity.this, "Tenant Removed", Toast.LENGTH_SHORT).show();
    }

    public void deleteTenantActivityLog(){

        String finalLogTitle = "Tenant Deleted";

        String finalLogDesc = "+91 " + tenant_phone + " • " + tenant_address + " • "
                + room_name + " (" + property_name + ")";

        long currTimestamp = System.currentTimeMillis();

        // Create unique Activity Log ID
        String log_id = activityLogReference.push().getKey();
        HashMap<String, Object> logMap = new HashMap<>();
        logMap.put("log_title", finalLogTitle);
        logMap.put("log_desc", finalLogDesc);
        logMap.put("log_entity", "TENANT");
        logMap.put("log_type", "TENANT_DELETED");
        logMap.put("log_ts", currTimestamp);
        logMap.put("log_primary_value", tenant_name);

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

    private void showContactBottomSheet() {

        // Create BottomSheetDialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        // Inflate layout for bottom sheet
        @SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_contact_tenant, null, false);
        bottomSheetDialog.setContentView(view);

        // Make sure we modify the bottom-sheet container after it is shown
        bottomSheetDialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // clear default background so your drawable shows through
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
                bottomSheet.setClipToPadding(false);
            }
        });

        // Find the option buttons inside the sheet
        LinearLayout btnCall = view.findViewById(R.id.btnCall);
        LinearLayout btnWhatsapp = view.findViewById(R.id.btnWhatsapp);
        LinearLayout btnCopy = view.findViewById(R.id.btnCopy);

        // Set click listeners
        btnCall.setOnClickListener(v -> {
            // handle call action
            String cleanedPhone = cleanTenantPhone(tenant_phone);  // Clean the phone number with 10-Digit Number only.
            if (cleanedPhone != null) {
                bottomSheetDialog.dismiss();
                // Create an Intent to open the dialer
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + cleanedPhone));

                // Start the dialer
                startActivity(callIntent);
            }
        });

        btnWhatsapp.setOnClickListener(v -> {

            String cleanedPhone = cleanTenantPhone(tenant_phone);

            // Add country code (India example)
            String phoneWithCountry = "+91" + cleanedPhone;

            // Create WhatsApp chat link
            String url = "https://wa.me/" + phoneWithCountry;

            if (cleanedPhone != null) {
                bottomSheetDialog.dismiss();
                Intent intentWhatsApp = new Intent(Intent.ACTION_VIEW);
                intentWhatsApp.setData(Uri.parse(url));
                intentWhatsApp.setPackage("com.whatsapp");
                startActivity(intentWhatsApp);
            }

        });

        btnCopy.setOnClickListener(v -> {
            // handle copy action
            bottomSheetDialog.dismiss();

            String cleanedPhone = cleanTenantPhone(tenant_phone);
            // Get clipboard manager
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            // Create a ClipData object with the text
            ClipData clip = ClipData.newPlainText("Phone Number", cleanedPhone);

            // Set the clip to clipboard
            clipboard.setPrimaryClip(clip);
        });

        // Set the content and show
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();

    }

    private String cleanTenantPhone(String phone) {
        String cleaned;
        if (phone.length() < 10) {
            Toast.makeText(RoomDetailsActivity.this, "Invalid Number", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (phone.length() > 10) {
            cleaned = phone.substring(phone.length() - 10);
            return cleaned;
        }
        return phone;
    }


    private void showBottomSheetTenants() {
        // Create BottomSheetDialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        // Inflate layout for bottom sheet
        @SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_tenant_list, null, false);
        bottomSheetDialog.setContentView(view);

        // Make sure we modify the bottom-sheet container after it is shown
        bottomSheetDialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // clear default background so your drawable shows through
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
                bottomSheet.setClipToPadding(false);
            }
        });

        RecyclerView rvTenants = view.findViewById(R.id.rvAllPastTenants);
        TextView tvBSRoomName = view.findViewById(R.id.tvBSRoomName);
        TextView tvBSPropertyName = view.findViewById(R.id.tvBSPropertyName);

        LinearLayout layoutNoTenantHistory = view.findViewById(R.id.layoutNoTenantHistory);
        MaterialCardView btnBSAddTenant = view.findViewById(R.id.btnBSAddTenant);

        btnBSAddTenant.setOnClickListener(view1 -> {

            Intent addTenantIntent = new Intent(RoomDetailsActivity.this, AddTenantActivity.class);
            addTenantIntent.putExtra("room_id", room_id);
            addTenantIntent.putExtra("room_name", room_name);
            addTenantIntent.putExtra("property_name", property_name);
            startActivity(addTenantIntent);

        });

        // Set Room & Property Name in Past Tenants List Bottom Sheet.
        tvBSRoomName.setText(room_name);
        tvBSPropertyName.setText(property_name);

        // Tenants Recycler View
        LinearLayoutManager layoutTenantManager = new LinearLayoutManager(this);

        rvTenants.setLayoutManager(new LinearLayoutManager(this));
        layoutTenantManager.setReverseLayout(true);    // newest items appear at top
        layoutTenantManager.setStackFromEnd(true);    // optional, usually false
        rvTenants.setLayoutManager(layoutTenantManager);

        FirebaseRecyclerOptions<Tenants> tenants_options = new FirebaseRecyclerOptions.Builder<Tenants>()
                .setQuery(allTenantReference, Tenants.class)
                .build();

        // Bind your data here
        FirebaseRecyclerAdapter<Tenants, TenantsViewHolder> firebaseTenantsRecyclerAdapter = new FirebaseRecyclerAdapter<>(tenants_options) {
            @Override
            protected void onBindViewHolder(@NonNull TenantsViewHolder holder, int position, @NonNull Tenants model) {
                // Bind your data here

                String startDate = model.getTenant_start_date();
                String endDate = model.getTenant_end_date();

                holder.setTenantName(model.getTenant_name());
                holder.setTenantProfileUrl(model.getThumb_tenant_url());
                holder.setTenantPhone(model.getTenant_phone());
                holder.setTenantStartDate(startDate);
                holder.setTenantEndDate(endDate);
                holder.setTenancyDuration(startDate, endDate);

                String tid = getRef(position).getKey();

                holder.itemView.setOnClickListener(view2 -> {

                    Intent tenantProfileIntent = new Intent(RoomDetailsActivity.this, TenantDetailsActivity.class);
                    tenantProfileIntent.putExtra("tenant_id", tid);
                    tenantProfileIntent.putExtra("room_id", room_id);
                    startActivity(tenantProfileIntent);

                });

            }

            @NonNull
            @Override
            public TenantsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.single_tenant_layout, parent, false);
                return new TenantsViewHolder(view);
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                if (getItemCount() > 0) {
                    layoutNoTenantHistory.setVisibility(View.GONE);
                } else {
                    layoutNoTenantHistory.setVisibility(View.VISIBLE);
                }

            }

        };

        rvTenants.setAdapter(firebaseTenantsRecyclerAdapter);
        rvTenants.setAdapter(firebaseTenantsRecyclerAdapter);
        firebaseTenantsRecyclerAdapter.startListening();

        // Show the bottom sheet
        bottomSheetDialog.show();

    }

    public static class TenantsViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public TenantsViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setTenantProfileUrl(String tenantProfileUrl) {
            CircleImageView tenantProfileUrlView = mView.findViewById(R.id.imgItemProfile);

            if (tenantProfileUrl == null || tenantProfileUrl.trim().isEmpty() || tenantProfileUrl.equals("default")) {
                // Show only placeholder
                Glide.with(itemView.getContext())
                        .load(R.drawable.ic_tenant_profile_default)
                        .into(tenantProfileUrlView);
            } else {
                Glide.with(itemView.getContext())
                        .load(tenantProfileUrl)
                        .placeholder(R.drawable.ic_tenant_profile_default)
                        .into(tenantProfileUrlView);
            }
        }

        public void setTenantName(String tenantName) {
            TextView tenantNameView = mView.findViewById(R.id.tvItemTenantName);
            tenantNameView.setText(tenantName);
        }

        public void setTenantPhone(String tenantPhone) {
            TextView tenantPhoneView = mView.findViewById(R.id.tvItemTenantPhone);
            tenantPhoneView.setText(tenantPhone);
        }

        public void setTenantStartDate(String tenantStartDate) {
            TextView tenantStartDateView = mView.findViewById(R.id.tvItemStartDate);
            tenantStartDateView.setText(tenantStartDate);
        }

        public void setTenantEndDate(String tenantEndDate) {
            TextView tenantEndDateView = mView.findViewById(R.id.tvItemEndDate);
            if (tenantEndDate.equals("null")) {
                tenantEndDateView.setTextColor(ContextCompat.getColor(mView.getContext(), R.color.text_amount));
                tenantEndDateView.setTypeface(null, Typeface.BOLD);
                tenantEndDateView.setText(R.string.text_active);
            } else {
                tenantEndDateView.setText(tenantEndDate);
            }

        }

        public void setTenancyDuration(String tenantStartDate, String tenantEndDate) {
            TextView tenancyDurationView = mView.findViewById(R.id.tvItemTenancyDuration);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

                    LocalDate s = LocalDate.parse(tenantStartDate, f);
                    LocalDate e = (tenantEndDate == null || "null".equalsIgnoreCase(tenantEndDate) || tenantEndDate.trim().isEmpty())
                            ? LocalDate.now()
                            : LocalDate.parse(tenantEndDate, f);

                    Period p = Period.between(s, e);

                    String d = (p.getYears() > 0)
                            ? (p.getMonths() > 0 ? p.getYears() + " yr " + p.getMonths() + " mo"
                            : (p.getYears() == 1 ? "1 year" : p.getYears() + " years"))
                            : (p.getMonths() > 0 ? (p.getMonths() == 1 ? "1 month" : p.getMonths() + " months")
                            : (p.getDays() == 1 ? "1 day" : p.getDays() + " days"));

                    tenancyDurationView.setText(d);
                }


            } catch (Exception e) {
                tenancyDurationView.setText("");
            }
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu == null) return false;

        MenuItem item = menu.findItem(R.id.action_edit_room);
        if (item == null) return false;

        if (is_room) {
            item.setTitle(R.string.text_menu_edit_room);
        } else {
            item.setTitle(R.string.text_menu_edit_shop);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (is_occupied) {
            getMenuInflater().inflate(R.menu.room_details_activity_menu, menu);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_edit_tenant) {

            Intent editTenantIntent = new Intent(RoomDetailsActivity.this, EditTenant.class);
            editTenantIntent.putExtra("room_id", room_id);
            editTenantIntent.putExtra("tenant_id", tenant_id);
            editTenantIntent.putExtra("tenant_name", tenant_name);
            editTenantIntent.putExtra("tenant_phone", tenant_phone);
            editTenantIntent.putExtra("tenant_address", tenant_address);
            startActivity(editTenantIntent);

            return true;
        } else if (id == R.id.action_remove_tenant) {
            tenantRemoveFromRoomConfirmation();
            return true;
        } else if (id == R.id.action_view_all_tenant) {
            showBottomSheetTenants();
            return true;
        } else if (id == R.id.action_edit_room) {

            Intent editRoomIntent = new Intent(RoomDetailsActivity.this, EditRoom.class);
            editRoomIntent.putExtra("room_id", room_id);
            editRoomIntent.putExtra("is_room", is_room);
            editRoomIntent.putExtra("room_name", room_name);
            editRoomIntent.putExtra("property_name", property_name);
            startActivity(editRoomIntent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}