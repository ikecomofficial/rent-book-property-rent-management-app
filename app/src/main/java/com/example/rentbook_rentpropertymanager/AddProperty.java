package com.example.rentbook_rentpropertymanager;

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;

public class AddProperty extends AppCompatActivity {

    private TextInputEditText etPropertyName, etPropertyAddress, etDefaultRentAmount, etUnitRate;
    private TextView textTotalRooms, textTotalShops;
    private int currTotalRooms = 0, currTotalShops = 0;
    private String user_id, pid, currTimestamp;
    private String propertyName, propertyAddress;
    private DatabaseReference databaseReference, propertyReference, activityLogReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_property);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        Window window = getWindow();

        // Set status bar background (example: white)
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.getInsetsController().setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.text_toolbar_add_new_prop);
        }

        // Adjust/Scroll Layout to move view on top of keyboard.
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        NestedScrollView scrollView = findViewById(R.id.main);
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            scrollView.getWindowVisibleDisplayFrame(r);
            int screenHeight = scrollView.getRootView().getHeight();
            int keyboardHeight = screenHeight - r.bottom;
            boolean keyboardVisible = keyboardHeight > screenHeight * 0.15;

            View focused = getCurrentFocus();
            if (keyboardVisible && focused != null) {
                int[] loc = new int[2];
                focused.getLocationOnScreen(loc);
                int offset = (loc[1] + focused.getHeight()) - r.bottom;
                if (offset > 0) scrollView.smoothScrollBy(0, offset + 40);
            } else if (!keyboardVisible) {
                scrollView.smoothScrollTo(0, 0);
            }
        });

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        assert user != null;
        user_id = user.getUid();

        etPropertyName = findViewById(R.id.editTextPropertyName);
        etPropertyAddress = findViewById(R.id.editTextPropertyAddress);
        etDefaultRentAmount = findViewById(R.id.editTextDefaultRent);
        etUnitRate = findViewById(R.id.editTextUnitRate);
        ImageView imgRoomsMinus = findViewById(R.id.imgRoomsMinus);
        textTotalRooms = findViewById(R.id.textTotalRooms);
        ImageView imgRoomsPlus = findViewById(R.id.imgRoomsPlus);
        ImageView imgShopsMinus = findViewById(R.id.imgShopsMinus);
        textTotalShops = findViewById(R.id.textTotalShops);
        ImageView imgShopsPlus = findViewById(R.id.imgShopsPlus);
        MaterialCardView btnCreateProperty = findViewById(R.id.btnCreateProperty);

        textTotalRooms.setText(String.valueOf(currTotalRooms));
        textTotalShops.setText(String.valueOf(currTotalShops));

        btnCreateProperty.setOnClickListener(view -> {
            if (savePropertyToFirebase()){
                createRoomsShopsInFirebase();
                addPropertyActivityLog();
            }
        });

        databaseReference = FirebaseDatabase.getInstance().getReference();
        propertyReference = databaseReference.child("properties");
        activityLogReference = databaseReference.child("activity_log").child(user_id);

        //Minus Buttons Action On click
        imgRoomsMinus.setOnClickListener(view -> {
            if (currTotalRooms > 0){
                currTotalRooms--;
                textTotalRooms.setText(String.valueOf(currTotalRooms));
            }
        });
        imgShopsMinus.setOnClickListener(view -> {
            if (currTotalShops > 0){
                currTotalShops--;
                textTotalShops.setText(String.valueOf(currTotalShops));
            }
        });

        // Plus Buttons Action On click
        imgRoomsPlus.setOnClickListener(view -> {
            currTotalRooms++;
            textTotalRooms.setText(String.valueOf(currTotalRooms));
        });
        imgShopsPlus.setOnClickListener(view -> {
            currTotalShops++;
            textTotalShops.setText(String.valueOf(currTotalShops));
        });

    }

    private boolean savePropertyToFirebase(){

        propertyName = etPropertyName.getText().toString().trim();
        propertyAddress = etPropertyAddress.getText().toString().trim();
        String propertyDefaultRent = etDefaultRentAmount.getText().toString().trim();
        String propertyDefaultUnitRate = etUnitRate.getText().toString().trim();

        currTimestamp = String.valueOf(System.currentTimeMillis());

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
        propertyMap.put("user_id", user_id);
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
        DatabaseReference roomsReference = FirebaseDatabase.getInstance().getReference().child("rooms");

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
                roomsMap.put("cm_rent_paid", false);
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
                roomsMap.put("cm_rent_paid", false);
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

        String finalLogDesc = propertyName + " added at " + propertyAddress + " with "
                + currTotalRooms + " rooms, " + currTotalShops + " shops.";

        // Create unique Activity Log ID
        String log_id = activityLogReference.push().getKey();
        HashMap<String, Object> logMap = new HashMap<>();
        logMap.put("log_title", finalLogTitle);
        logMap.put("log_desc", finalLogDesc);
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