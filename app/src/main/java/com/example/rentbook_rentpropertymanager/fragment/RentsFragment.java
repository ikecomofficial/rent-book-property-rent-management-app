package com.example.rentbook_rentpropertymanager.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rentbook_rentpropertymanager.R;
import com.example.rentbook_rentpropertymanager.model.Rents;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RentsFragment extends Fragment {

    private LinearLayout layoutNoRentRecord;
    private RecyclerView rvRentList;
    private String property_id;
    private DatabaseReference rentsReference;
    private FirebaseRecyclerAdapter<Rents, RentsFragment.RentsViewHolder> firebaseRecyclerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_rents, container, false);

        // Get roomId from arguments
        String room_id = getArguments() != null ? getArguments().getString("room_id") : null;
        property_id = getArguments() != null ? getArguments().getString("property_id") : null;

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        assert room_id != null;
        rentsReference = databaseReference.child("rents").child(room_id);

        // Rent Recycler View
        rvRentList = view.findViewById(R.id.rvRentRecord);
        LinearLayoutManager layoutRentManager = new LinearLayoutManager(getContext());
        layoutRentManager.setReverseLayout(true);    // newest items appear at top
        layoutRentManager.setStackFromEnd(true);    // optional, usually false
        rvRentList.setLayoutManager(layoutRentManager);

        layoutNoRentRecord = view.findViewById(R.id.layoutNoRentRecord);

        loadRentRecyclerList();

        if (firebaseRecyclerAdapter != null) {
            rvRentList.setAdapter(firebaseRecyclerAdapter);
            firebaseRecyclerAdapter.startListening();
        }

        return view;  // Return the view **after** initializing RecyclerView
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (firebaseRecyclerAdapter != null) {
            firebaseRecyclerAdapter.stopListening(); // stop only when view is destroyed
        }
    }

    private void loadRentRecyclerList() {

        FirebaseRecyclerOptions<Rents> rent_options = new FirebaseRecyclerOptions.Builder<Rents>()
                .setQuery(rentsReference, Rents.class)
                .build();

        firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Rents, RentsFragment.RentsViewHolder>(rent_options) {
            @Override
            protected void onBindViewHolder(@NonNull RentsFragment.RentsViewHolder holder, int position, @NonNull Rents model) {
                // Bind your data here

                String rentMonthYear = model.getRent_month_year();
                int rentAmount = model.getRent_amount();
                holder.setRentAmount(rentAmount);
                holder.setRentPaymentMode(model.getPayment_mode());
                //holder.setRentTenantName(model.getTenant_name());

                holder.setRentMonthYear(rentMonthYear);
                holder.setRentPeriodStartEnd(model.getRent_period_start(), model.getRent_period_end());

                // Group Rent Records by Tenant Name

                String currentTenant = model.getTenant_name();

                boolean showHeader = false;


                // If not last item
                if (position < getItemCount() - 1) {

                    String nextTenant = getItem(position + 1).getTenant_name();

                    if (!currentTenant.equals(nextTenant)) {
                        showHeader = true;
                    }
                }

                holder.setTenantHeader(currentTenant, showHeader);

                // Format timestamp into date & time
                long timestamp = model.getRent_timestamp();
                Date date = new Date(timestamp);

                String dateOnly = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date);
                String timeOnly = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);

                holder.setRentDateTime(dateOnly, timeOnly);

                // Long press to delete
                holder.itemView.setOnLongClickListener(v -> {
                    new MaterialAlertDialogBuilder(v.getContext())
                            .setTitle("Delete Rent Record?")
                            .setMessage("Are you sure you want to delete this rent record?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                // 🔑 Call your delete function here
                                deleteRentRecord(getRef(position).getKey(), property_id, convertMonthYearToKey(rentMonthYear), rentAmount);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .show();

                    return true; // ✅ consume the long press
                });

            }

            @NonNull
            @Override
            public RentsFragment.RentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.single_rent_layout, parent, false);
                return new RentsViewHolder(view);
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                rvRentList.post(() -> firebaseRecyclerAdapter.notifyDataSetChanged());

                int itemCount = getItemCount();
                if (itemCount == 0) {
                    layoutNoRentRecord.setVisibility(View.VISIBLE);
                } else {
                    layoutNoRentRecord.setVisibility(View.GONE);
                }
            }

        };

        rvRentList.setAdapter(firebaseRecyclerAdapter);
    }

    public static String convertMonthYearToKey(String input) {
        if (input == null || input.trim().isEmpty()) return null;

        try {
            SimpleDateFormat inputFormat =
                    new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);

            SimpleDateFormat outputFormat =
                    new SimpleDateFormat("yyyy-MM", Locale.ENGLISH);

            Date date = inputFormat.parse(input.trim());
            return outputFormat.format(date);

        } catch (ParseException e) {
            return null;
        }
    }

    private String getMonthYear(String timestampStr) {
        try {
            long timestamp = Long.parseLong(timestampStr); // convert string → long
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (NumberFormatException e) {
            Log.e("ParseError", "Invalid number format", e);
            return ""; // fallback if string is invalid
        }
    }

    private void deleteRentRecord(String rent_id, String pid, String rentMonthYear, int rentAmount) {
        rentsReference.child(rent_id).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // ✅ Record deleted successfully
                    Toast.makeText(getContext(), "Rent Deleted", Toast.LENGTH_SHORT).show();
                    subtractRentFromCollections(pid, rentMonthYear, rentAmount);
                })
                .addOnFailureListener(e -> {
                    // ❌ Handle failure
                    Toast.makeText(getContext(), "Rent Not Deleted", Toast.LENGTH_SHORT).show();
                });
    }

    public void subtractRentFromCollections(
            String pid,
            String monthYearKey,   // "2025-03"
            int rentToSubtract
    ) {

        DatabaseReference rentCollectionReference =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child("collections")
                        .child(pid)
                        .child(monthYearKey)
                        .child("total_rent");

        Toast.makeText(requireContext(), "Rent Amount:" + rentToSubtract + monthYearKey, Toast.LENGTH_SHORT).show();

        rentCollectionReference.runTransaction(new Transaction.Handler() {

            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {

                Integer currentTotalRent = data.getValue(Integer.class);

                if (currentTotalRent == null) {
                    // Nothing to subtract from
                    return Transaction.success(data);
                }

                int updatedTotal = currentTotalRent - rentToSubtract;

                // 🔒 Prevent negative values
                data.setValue(Math.max(updatedTotal, 0));

                return Transaction.success(data);
            }

            @Override
            public void onComplete(
                    DatabaseError error,
                    boolean committed,
                    DataSnapshot snapshot
            ) {
                if (error != null) {
                    Log.e("Firebase", "Failed to subtract rent", error.toException());
                } else if (committed) {
                    Log.d("Firebase", "Rent subtracted successfully");
                    Toast.makeText(requireContext(), "Rent Minus", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    public static class RentsViewHolder extends RecyclerView.ViewHolder {

        View mView;
        MaterialCardView mcvRentPaymentMode;

        public RentsViewHolder(View itemView) {
            super(itemView);
            mView = itemView;

        }


        public void setRentDateTime(String rentDate, String rentTime){
            TextView rentDateView = mView.findViewById(R.id.tvRentDateTime);
            String finalDateTime = rentDate + ", " + rentTime;
            rentDateView.setText(finalDateTime);

        }

        public void setRentMonthYear(String rentMonthYear){
            TextView tvRentMonth = mView.findViewById(R.id.tvRentMonth);
            TextView tvRentYear = mView.findViewById(R.id.tvRentYear);

            //TextView tvRentMY = mView.findViewById(R.id.tvRentMonthYear);
            //tvRentMY.setText(rentMonthYear);

            String month = rentMonthYear.substring(0,3).toUpperCase();
            String year = rentMonthYear.substring(rentMonthYear.lastIndexOf(" ") + 1);

            tvRentMonth.setText(month); // MAR
            tvRentYear.setText(year);   // 2025

        }

        public void setRentPeriodStartEnd(String rentPeriodStart, String rentPeriodEnd){
            TextView tvRentPeriod = mView.findViewById(R.id.tvRentPeriod);
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);

                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM", Locale.ENGLISH);

                Date startDate = inputFormat.parse(rentPeriodStart);
                Date endDate   = inputFormat.parse(rentPeriodEnd);

                String finalRentPeriod = outputFormat.format(startDate) + " - " + outputFormat.format(endDate);

                tvRentPeriod.setText(finalRentPeriod);

            } catch (ParseException e) {
                Log.e("DateParse", "Error setting rent period", e);
            }
        }

        public void setRentAmount(int rentAmount) {
            TextView rentAmountView = mView.findViewById(R.id.tvRentAmount);
            //String rent_amount = "₹" + String.valueOf(rentAmount);
            String displayRent = "+ ₹" + NumberFormat.getInstance(new Locale("en", "IN")).format(rentAmount);
            rentAmountView.setText(displayRent);
        }

        public void setRentPaymentMode(String rentPaymentMode) {
            TextView rentPaymentModeView = mView.findViewById(R.id.tvRentPaymentMode);
            ImageView imgRentPaymentMode = mView.findViewById(R.id.imgRentPaymentMode);

            MaterialCardView mcvRentPaymentMode = mView.findViewById(R.id.mcvRentPaymentMode);

            if (rentPaymentMode.equals(("Online"))){
                imgRentPaymentMode.setImageResource(R.drawable.ic_bank_online);

                // Apply payment mode card background
                mcvRentPaymentMode.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.payment_mode_online_bg));
            }else {
                imgRentPaymentMode.setImageResource(R.drawable.ic_cash_payment);
                mcvRentPaymentMode.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.payment_mode_cash_bg));
            }
            rentPaymentModeView.setText(rentPaymentMode);
        }

        public void setTenantHeader(String rentTenantName, boolean showHeader) {

            LinearLayout layoutHeaderTenantName = mView.findViewById(R.id.layoutHeaderTenantName);
            TextView tvHeaderTenantName = mView.findViewById(R.id.tvHeaderTenantName);

            if (showHeader) {
                String tenantHeader = rentTenantName.toUpperCase() + "'S RECORDS";
                tvHeaderTenantName.setText(tenantHeader);
                layoutHeaderTenantName.setVisibility(View.VISIBLE);
            } else {
                layoutHeaderTenantName.setVisibility(View.GONE);
            }
        }
    }

}