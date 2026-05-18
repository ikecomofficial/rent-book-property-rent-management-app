package com.example.rentbook_rentpropertymanager;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import java.util.Objects;

public class AddEbillActivity extends AppCompatActivity {

    // 🏠 IDs
    private String room_id, property_id, tenant_id;

    // 💳 Payment Info
    private String paymentMode = "Cash";
    private String ebill_timestamp;
    private String elc_bill_month_year;

    // 🔢 Electricity Data
    private int last_paid_upto = -1;
    private double elc_unit_rate = 0, elc_bill_amount = 0, units_used = 0, units_paid_upto = 0;
    private boolean isByUnits = true;

    // 📄 Input Fields
    private EditText etCurrentReading, etElcBillAmount;

    // 📊 UI Views
    private TextView tvCustomDate, tvCustomTime, tvPrevPaidTill, tvPrevPaidTillByAmt, tvElcBillMonthYear;
    private LinearLayout layoutLastPaidUnit, layoutElcBillAmount, layoutElcUnitPaid;
    //private MaterialCardView layoutPrevPaidTill, layoutPrevPaidTillByAmt;

    // ⏰ Date & Time
    private Calendar calendar;

    // 🔗 Firebase References
    private DatabaseReference roomReference, elcBillReference, activityLogReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_ebill);
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
            getSupportActionBar().setTitle(R.string.text_btn_add_ebill_record);
        }

        room_id = getIntent().getStringExtra("room_id");
        tenant_id = getIntent().getStringExtra("tenant_id");
        property_id = getIntent().getStringExtra("property_id");

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        roomReference = databaseReference.child("rooms").child(room_id);
        elcBillReference = databaseReference.child("e-bills").child(room_id);
        activityLogReference = databaseReference.child("activity_log").child(user_id);

        // 📄 Input Fields
        etCurrentReading = findViewById(R.id.etCurrentMeterReading);
        etElcBillAmount = findViewById(R.id.etElcBillAmount);
        //etElcLastPaidTill = findViewById(R.id.etElcLastPaidUnit);

        // 📊 UI Layouts
        layoutElcUnitPaid = findViewById(R.id.layoutElcUnitPaid);
        layoutElcBillAmount = findViewById(R.id.layoutElcBillAmount);
        layoutElcBillAmount.setVisibility(View.GONE);

        // 🎯 Actions
        MaterialCardView btnSaveElcBill = findViewById(R.id.btnSaveElcBill);
        MaterialButtonToggleGroup tgElcBillMode = findViewById(R.id.toggleElcBillMode);
        MaterialButtonToggleGroup tgPaymentMode = findViewById(R.id.togglePaymentMode);
        MaterialCardView btnChangeElcBillMY = findViewById(R.id.btnChangeElcBillMY);
        MaterialCardView btnChangeDateTime = findViewById(R.id.btnChangeDateTime);

        // 📅 Date & Time UI
        tvCustomDate = findViewById(R.id.tvCustomDate);
        tvCustomTime = findViewById(R.id.tvCustomTime);
        tvElcBillMonthYear = findViewById(R.id.tvElcBillMonthYear);

        // 📈 Previous Data Display
        tvPrevPaidTill = findViewById(R.id.tvPrevPaidTill);
        tvPrevPaidTillByAmt = findViewById(R.id.tvPrevPaidTillByAmt);

        calendar = Calendar.getInstance();
        if (fetchLastPaidUnitFirebase()){
            fetchElcUnitRate();
        }

        tgElcBillMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked){
                if (checkedId == R.id.btnByUnitsSelection){
                    layoutElcUnitPaid.setVisibility(View.VISIBLE);
                    layoutElcBillAmount.setVisibility(View.GONE);
                    isByUnits = true;
                } else if (checkedId == R.id.btnByAmountSelection) {
                    layoutElcUnitPaid.setVisibility(View.GONE);
                    layoutElcBillAmount.setVisibility(View.VISIBLE);
                    isByUnits = false;
                }
            }
        });

        tgPaymentMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked){
                if (checkedId == R.id.btnCashSelection){
                    paymentMode = "Cash";
                } else if (checkedId == R.id.btnOnlineSelection) {
                    paymentMode = "Online";
                }
            }
        });

        setCurrentElcBillMonthAndTime();

        // 1️⃣ Click on Change Date Time Button Layout (Date + Time)
        btnChangeDateTime.setOnClickListener(v -> {
            showDateTimePicker();
        });

        btnChangeElcBillMY.setOnClickListener(v ->{
            showElcBillMonthYearPicker();
        });

        btnSaveElcBill.setOnClickListener(view -> saveElcBillToFirebase());

    }

    private boolean fetchLastPaidUnitFirebase(){
        roomReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                Long lastPaidUnit = snapshot.child("last_unit_paid").getValue(Long.class);
                last_paid_upto = (lastPaidUnit != null) ? lastPaidUnit.intValue() : 0;

                String tvPrevPaidUpTo = "Paid: " + last_paid_upto + " units";
                tvPrevPaidTill.setText(tvPrevPaidUpTo);
                tvPrevPaidTillByAmt.setText(tvPrevPaidUpTo);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        return true;
    }

    private void fetchElcUnitRate(){
        roomReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double rate = snapshot.child("elc_unit_rate").getValue(Double.class);
                elc_unit_rate = (rate != null) ? rate : 0.0;
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
    private String getTimestamp(Calendar cal) {
        return String.valueOf(cal.getTimeInMillis());
    }

    private void setCurrentElcBillMonthAndTime(){

        ebill_timestamp = String.valueOf(System.currentTimeMillis());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        // 📅 Electricity Bill Meta
        String ebill_date = dateFormat.format(calendar.getTime());
        String ebill_time = timeFormat.format(calendar.getTime());

        tvCustomDate.setText(ebill_date);
        tvCustomTime.setText(ebill_time);

        // Set Current Month Year in Top section (rent month & year)
        Date currentDate = Calendar.getInstance().getTime();
        SimpleDateFormat sdfMonthYear = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String monthYear = sdfMonthYear.format(currentDate);
        tvElcBillMonthYear.setText(monthYear);

        // 🔹 For Firebase / variable → 2026-04
        SimpleDateFormat sdfMonthYearFb = new SimpleDateFormat("yyyy-MM", Locale.ENGLISH);
        elc_bill_month_year = sdfMonthYearFb.format(currentDate);

    }

    private void showElcBillMonthYearPicker() {

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
                    tvElcBillMonthYear.setText(selectedMonthYear);

                    String month = String.format(Locale.getDefault(), "%02d", selectedMonth + 1); //April → 04

                    elc_bill_month_year = selectedYear + "-" + month; // 2026-04

                    //is_elc_bill_month_custom = true;

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Show picker dialogs
    private void showDateTimePicker() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AddEbillActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(Calendar.YEAR, selectedYear);
                    calendar.set(Calendar.MONTH, selectedMonth);
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay);

                    // After date, show time picker
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    int minute = calendar.get(Calendar.MINUTE);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            AddEbillActivity.this,
                            (timeView, selectedHour, selectedMinute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                                calendar.set(Calendar.MINUTE, selectedMinute);

                                // Update TextViews
                                updateDateTimeViews(calendar);

                                // Get final timestamp
                                ebill_timestamp = getTimestamp(calendar);

                            }, hour, minute, false // false for 12-hour format
                    );
                    timePickerDialog.show();

                }, year, month, day
        );
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void saveElcBillToFirebase(){

        if (isByUnits){
            // Unit Mode
            String unitPaidUpTo = Objects.requireNonNull(etCurrentReading.getText()).toString().trim();
            if (unitPaidUpTo.isEmpty()){
                etCurrentReading.setError("Enter Current Meter Reading");
                return;
            }
            units_paid_upto = Integer.parseInt(unitPaidUpTo);
            units_used = units_paid_upto - last_paid_upto;
            elc_bill_amount = units_used * elc_unit_rate;
        }else {
            String elcBillAmount = Objects.requireNonNull(etElcBillAmount.getText()).toString().trim();
            if (elcBillAmount.isEmpty()){
                etElcBillAmount.setError("Enter Electricity Bill Paid Amount");
                return;
            }
            elc_bill_amount = Integer.parseInt(elcBillAmount);
            double unitsUsedDouble = (double) elc_bill_amount / elc_unit_rate;
            units_used = (int) Math.round(unitsUsedDouble);
            units_paid_upto = last_paid_upto + units_used;
        }
        // Create unique rent ID
        String ebill_id = elcBillReference.push().getKey();
        HashMap<String, Object> billMap = new HashMap<>();
        billMap.put("room_id", room_id);
        billMap.put("tenant_id", tenant_id);
        billMap.put("payment_mode", paymentMode);
        billMap.put("ebill_timestamp", ebill_timestamp);
        billMap.put("paid_upto", Math.round(units_paid_upto));
        billMap.put("units_used", Math.round(units_used));
        billMap.put("ebill_amount", Math.round(elc_bill_amount));
        billMap.put("last_paid_upto", last_paid_upto);
        billMap.put("elc_bill_month_year", elc_bill_month_year);

        if (ebill_id != null){
            elcBillReference.child(ebill_id).setValue(billMap)
                    .addOnSuccessListener(aVoid -> {
                        // Update Last Unit Paid in Room id Data
                        roomReference.child("last_unit_paid").setValue(units_paid_upto)
                                .addOnSuccessListener(update -> {
                                    Toast.makeText(this, "Bill Added Successfully", Toast.LENGTH_SHORT).show();
                                    int elcBill = (int) Math.round(elc_bill_amount);
                                    int elcUnitsUsed = (int) Math.round(units_used);
                                    addElcBillToCollections(property_id, elc_bill_month_year, elcBill, elcUnitsUsed);
                                    addElcBillActivityLog();
                                });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    // Convert the month year from 2026-04 → April 2026
    public static String convertMonthYearKey(String input) {
        try {
            DateFormat inputFormat =
                    new SimpleDateFormat("yyyy-MM", Locale.ENGLISH);

            DateFormat outputFormat =
                    new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);

            Date date = inputFormat.parse(input);
            return outputFormat.format(date);

        } catch (ParseException e) {
            Log.e("DateParse", "Error converting key", e);
            return null;
        }
    }

    public void addElcBillToCollections(
            String pid,
            String monthYearKey,   // "2025-03"
            int elcBillToAdd,
            int elcUnitsToAdd
    ) {

        DatabaseReference elcBillCollectionReference =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child("collections")
                        .child(pid)
                        .child(monthYearKey);

        elcBillCollectionReference.runTransaction(new Transaction.Handler() {

            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {

                // Get existing values (if any)
                Integer currentElcBill =
                        currentData.child("total_elc_bill").getValue(Integer.class);

                Integer currentUnitsUsed =
                        currentData.child("total_units_used").getValue(Integer.class);


                if (currentElcBill == null) currentElcBill = 0;
                if (currentUnitsUsed == null) currentUnitsUsed = 0;

                // ✅ Update individual children (NOT the parent)
                currentData.child("total_elc_bill")
                        .setValue(currentElcBill + elcBillToAdd);

                currentData.child("total_units_used")
                        .setValue(currentUnitsUsed + elcUnitsToAdd);


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
                    Toast.makeText(AddEbillActivity.this, "Transaction failed", Toast.LENGTH_SHORT).show();

                } else if (committed) {
                    Log.d("Firebase", "E-Bill updated successfully");
                    finish();
                }
            }
        });
    }

    public void addElcBillActivityLog(){

        String finalLogTitle = "Electricity Bill Recorded";

        String finalLogDesc = "₹" + elc_bill_amount + " • " + convertMonthYearKey(elc_bill_month_year) +
                " • Paid up to " + units_paid_upto + " units.";

        long currTimestamp = System.currentTimeMillis();

        // Create unique Activity Log ID
        String log_id = activityLogReference.push().getKey();
        HashMap<String, Object> logMap = new HashMap<>();
        logMap.put("log_title", finalLogTitle);
        logMap.put("log_desc", finalLogDesc);
        logMap.put("log_entity", "UTILITY");
        logMap.put("log_type", "ELC_BILL_ADDED");
        logMap.put("log_ts", currTimestamp);
        logMap.put("log_primary_value", units_used);

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