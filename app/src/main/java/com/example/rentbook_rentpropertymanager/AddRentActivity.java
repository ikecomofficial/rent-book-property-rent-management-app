package com.example.rentbook_rentpropertymanager;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;
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

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class AddRentActivity extends AppCompatActivity {

    private TextInputEditText etRentAmount;
    private TextView tvCustomDate, tvCustomTime, tvRentMonthYear;
    private Calendar calendar;
    private String room_id, property_id, room_name, property_name;
    private String tenant_id, user_id;
    private String tenant_name;
    private String rent_period_start, rent_period_end, rent_month_year;
    private long rent_timestamp;
    private boolean is_rent_month_custom = false;
    private String paymentMode = "Cash";
    private Integer room_rent = 0, billing_start_day = 1;

    private DatabaseReference roomReference;
    private DatabaseReference tenantReference;
    private DatabaseReference rentReference;
    private DatabaseReference activityLogReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_rent);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        user_id = user.getUid();

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.text_btn_add_rent_record);
        }

        room_id = getIntent().getStringExtra("room_id");
        tenant_id = getIntent().getStringExtra("tenant_id");
        property_id = getIntent().getStringExtra("property_id");
        room_name = getIntent().getStringExtra("room_name");
        property_name = getIntent().getStringExtra("property_name");

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        roomReference = databaseReference.child("rooms").child(room_id);
        tenantReference = databaseReference.child("tenants").child(room_id).child(tenant_id);
        rentReference = databaseReference.child("rents").child(room_id);
        activityLogReference = databaseReference.child("activity_log").child(user_id);

        etRentAmount = findViewById(R.id.etRentAmount);
        MaterialButtonToggleGroup tgPaymentMode = findViewById(R.id.togglePaymentMode);

        tvCustomDate = findViewById(R.id.tvCustomDate);
        tvCustomTime = findViewById(R.id.tvCustomTime);

        tvRentMonthYear = findViewById(R.id.tvRentMonthYear);

        MaterialCardView btnChangeDateTime = findViewById(R.id.btnChangeDateTime);
        MaterialCardView btnChangeRentMY = findViewById(R.id.btnChangeRentMY);

        MaterialCardView btnSaveRent = findViewById(R.id.btnSaveRent);

        fetchRentTenantFirebase();

        calendar = Calendar.getInstance();


        btnChangeRentMY.setOnClickListener(v ->{
            showRentMonthYearPicker();
        });

        tgPaymentMode.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (isChecked){
                    if (checkedId == R.id.btnCashSelection){
                        paymentMode = "Cash";
                    } else if (checkedId == R.id.btnOnlineSelection) {
                        paymentMode = "Online";
                    }
                }
            }
        });

        //setCurrentDateTime();
        setCurrentRentMonthAndTime();

        // 1️⃣ Click to Change Payment Date & Time
        btnChangeDateTime.setOnClickListener(v -> {
            showDateTimePicker();
        });

        btnSaveRent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveRentToFirebase();
            }
        });

    }

    private void fetchRentTenantFirebase(){
        roomReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long rentValue = snapshot.child("room_rent").getValue(Long.class);
                assert rentValue != null;
                room_rent = rentValue.intValue();
                etRentAmount.setText(String.valueOf(room_rent));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        tenantReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tenant_name = snapshot.child("tenant_name").getValue(String.class);
                billing_start_day = snapshot.child("billing_start_day").getValue(Integer.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // Helper method to update TextViews
    private void updateDateTimeViews(Calendar cal) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        tvCustomDate.setText(dateFormat.format(cal.getTime()));
        tvCustomTime.setText(timeFormat.format(cal.getTime()));
    }

    // Helper method to get timestamp
    private long getTimestamp(Calendar cal) {
        return cal.getTimeInMillis();
    }

    // Get current date & time

    private void setCurrentRentMonthAndTime(){
        // Set Current Month Year and the date and time.

        rent_timestamp = System.currentTimeMillis();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        tvCustomDate.setText(dateFormat.format(calendar.getTime()));
        tvCustomTime.setText(timeFormat.format(calendar.getTime()));

        // Set Current Month Year in the top section (rent month & year)

        SimpleDateFormat sdfMonthYear = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String monthYear = sdfMonthYear.format(Calendar.getInstance().getTime());
        tvRentMonthYear.setText(monthYear);
        rent_month_year = monthYear;

        rent_period_start = String.format(Locale.ENGLISH, "%02d", billing_start_day) + " " + monthYear;

        rent_period_end = getRentPeriodEndDate(rent_period_start);


    }

    private void showRentMonthYearPicker() {

        String[] MONTHS = new DateFormatSymbols(Locale.getDefault()).getMonths();

        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_month_year_picker, null);

        NumberPicker npMonth = view.findViewById(R.id.npMonth);
        NumberPicker npYear = view.findViewById(R.id.npYear);

        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH);
        int currentYear = cal.get(Calendar.YEAR);

        // Month Picker
        npMonth.setMinValue(0);
        npMonth.setMaxValue(11);
        npMonth.setDisplayedValues(MONTHS);
        npMonth.setValue(currentMonth);

        // Year Picker (Current & Previous Year)
        npYear.setMinValue(currentYear - 1);
        npYear.setMaxValue(currentYear);
        npYear.setValue(currentYear);

        // Initial restriction
        if (npYear.getValue() == currentYear) {
            npMonth.setMaxValue(currentMonth);
        }

        // Restrict future months dynamically
        npYear.setOnValueChangedListener((picker, oldVal, newVal) -> {

            if (newVal == currentYear) {
                npMonth.setMinValue(0);
                npMonth.setMaxValue(currentMonth);
            } else {
                npMonth.setMinValue(0);
                npMonth.setMaxValue(11);
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("Select Your Rent Month")
                .setView(view)
                .setPositiveButton("OK", (dialog, which) -> {
                    int selectedMonth = npMonth.getValue();
                    int selectedYear = npYear.getValue();

                    String selectedMonthYear = MONTHS[selectedMonth] + " " + selectedYear;
                    tvRentMonthYear.setText(selectedMonthYear);

                    rent_month_year = selectedMonthYear;
                    // Converting Selected Date (DD MM YYYY) to period start and end dates with timestamp.
                    rent_period_start = billing_start_day + " " + selectedMonthYear;

                    rent_period_end = getRentPeriodEndDate(rent_period_start);
                    is_rent_month_custom = true;

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getRentPeriodEndDate(String startDateStr) {

        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);

            Date startDate = sdf.parse(startDateStr.trim());

            Calendar cal = Calendar.getInstance();
            assert startDate != null;
            cal.setTime(startDate);

            // Move to next month
            cal.add(Calendar.MONTH, 1);

            // One day before next cycle
            cal.add(Calendar.DAY_OF_MONTH, -1);

            return sdf.format(cal.getTime());

        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    // Show picker dialogs
    private void showDateTimePicker() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AddRentActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(Calendar.YEAR, selectedYear);
                    calendar.set(Calendar.MONTH, selectedMonth);
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay);

                    // After date, show time picker
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    int minute = calendar.get(Calendar.MINUTE);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            AddRentActivity.this,
                            (timeView, selectedHour, selectedMinute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                                calendar.set(Calendar.MINUTE, selectedMinute);

                                // Update TextViews
                                updateDateTimeViews(calendar);

                                // Get final timestamp
                                rent_timestamp = getTimestamp(calendar);

                            }, hour, minute, false // false for 12-hour format
                    );
                    timePickerDialog.show();

                }, year, month, day
        );
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void saveRentToFirebase(){
        String rentAmount = etRentAmount.getText().toString().trim();

        if (!is_rent_month_custom){
            SimpleDateFormat sdfMonthYear = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            String monthYear = sdfMonthYear.format(Calendar.getInstance().getTime());
            rent_month_year = monthYear;
            rent_period_start = String.format(Locale.ENGLISH, "%02d", billing_start_day) + " " + monthYear;
            rent_period_end = getRentPeriodEndDate(rent_period_start);
        }

        if (rentAmount.isEmpty()) {
            etRentAmount.setError("Enter Rent Amount Paid");
            return;
        }
        // Create unique rent ID
        String rent_id = rentReference.push().getKey();
        HashMap<String, Object> rentMap = new HashMap<>();
        rentMap.put("tenant_name", tenant_name);
        rentMap.put("rent_amount", Integer.parseInt(rentAmount));
        rentMap.put("room_id", room_id);
        rentMap.put("tenant_id", tenant_id);
        rentMap.put("payment_mode", paymentMode);
        rentMap.put("rent_month_year", rent_month_year);
        rentMap.put("rent_period_start", rent_period_start);
        rentMap.put("rent_period_end", rent_period_end);
        rentMap.put("rent_timestamp", rent_timestamp);

        if (rent_id != null){
            rentReference.child(rent_id).setValue(rentMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Rent Added Successfully", Toast.LENGTH_SHORT).show();
                        addRentToCollections(property_id, convertMonthYearToKey(rent_month_year), Integer.parseInt(rentAmount));
                        addRentActivityLog(Integer.parseInt(rentAmount), rent_month_year, rent_timestamp);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    public void addRentActivityLog(int amount, String rentMY, long ts){

        String finalLogTitle = "Rent Recorded";

        String finalLogDesc = "₹" + amount +
                " rent added for " + room_name +
                " at " + property_name +
                " for " + rentMY + ".";

        // Create unique Activity Log ID
        String log_id = activityLogReference.push().getKey();
        HashMap<String, Object> logMap = new HashMap<>();
        logMap.put("log_title", finalLogTitle);
        logMap.put("log_desc", finalLogDesc);
        logMap.put("log_entity", "RENT");
        logMap.put("log_type", "RENT_ADDED");
        logMap.put("log_ts", rent_timestamp);

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

    public static String convertMonthYearToKey(String input) {
        try {
            DateFormat inputFormat =
                    new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);

            DateFormat outputFormat =
                    new SimpleDateFormat("yyyy-MM", Locale.ENGLISH);

            Date date = inputFormat.parse(input);
            return outputFormat.format(date);

        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addRentToCollections(
            String pid,
            String monthYearKey,   // "2025-03"
            int rentToAdd
    ) {

        DatabaseReference rentCollectionReference =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child("collections")
                        .child(pid)
                        .child(monthYearKey)
                        .child("total_rent");

        rentCollectionReference.runTransaction(new Transaction.Handler() {

            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {

                Integer currentRent = currentData.getValue(Integer.class);

                if (currentRent == null) {
                    // 🔹 Month OR rent doesn't exist → create
                    currentData.setValue(rentToAdd);
                } else {
                    // 🔹 Month exists → add
                    currentData.setValue(currentRent + rentToAdd);
                }

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(
                    DatabaseError error,
                    boolean committed,
                    DataSnapshot snapshot
            ) {
                if (error != null) {
                    Log.e("Firebase", "Transaction failed", error.toException());
                    Toast.makeText(AddRentActivity.this, "Transaction failed", Toast.LENGTH_SHORT).show();
                } else if (committed) {
                    Log.d("Firebase", "Rent updated successfully");
                    finish();
                }
            }
        });
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