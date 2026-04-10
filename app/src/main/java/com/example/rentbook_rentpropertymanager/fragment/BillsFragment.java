package com.example.rentbook_rentpropertymanager.fragment;

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

import com.example.rentbook_rentpropertymanager.model.Bills;
import com.example.rentbook_rentpropertymanager.R;
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

public class BillsFragment extends Fragment {

    private LinearLayout layoutNoBillRecord;
    private RecyclerView rvElcBillList;
    private String room_id, property_id;
    private DatabaseReference databaseReference, ebillsReference;
    private FirebaseRecyclerAdapter<Bills, BillsFragment.BillsViewHolder> firebaseBillsRecyclerAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_bills, container, false);

        // Get roomId from arguments

        room_id = getArguments() != null ? getArguments().getString("room_id") : null;
        property_id = getArguments() != null ? getArguments().getString("property_id") : null;

        databaseReference = FirebaseDatabase.getInstance().getReference();
        assert room_id != null;
        ebillsReference = databaseReference.child("e-bills").child(room_id);

        // e-bill Recycler View
        rvElcBillList = view.findViewById(R.id.rvElcBillRecord);
        LinearLayoutManager layoutBillManager = new LinearLayoutManager(getContext());
        layoutBillManager.setReverseLayout(true);    // newest items appear at top
        layoutBillManager.setStackFromEnd(true);    // optional, usually false
        rvElcBillList.setLayoutManager(layoutBillManager);

        layoutNoBillRecord = view.findViewById(R.id.layoutNoBillRecord);

        loadElcBillRecyclerList();

        if (firebaseBillsRecyclerAdapter != null) {
            rvElcBillList.setAdapter(firebaseBillsRecyclerAdapter);
            firebaseBillsRecyclerAdapter.startListening();
        }


        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (firebaseBillsRecyclerAdapter != null && firebaseBillsRecyclerAdapter.getSnapshots().isListening()) {
            firebaseBillsRecyclerAdapter.stopListening();
        }
    }

    private void loadElcBillRecyclerList(){

        FirebaseRecyclerOptions<Bills> bill_options = new FirebaseRecyclerOptions.Builder<Bills>()
                .setQuery(ebillsReference, Bills.class)
                .build();

        firebaseBillsRecyclerAdapter = new FirebaseRecyclerAdapter<Bills, BillsFragment.BillsViewHolder>(bill_options) {
            @Override
            protected void onBindViewHolder(@NonNull BillsFragment.BillsViewHolder holder, int position, @NonNull Bills model) {
                // Bind your data here

                String elcBillMonthYear = model.getElc_bill_month_year();
                int elcBillAmount = model.getEbill_amount();
                int elcUnitsUsed = model.getUnits_used();
                holder.setBillAmount(elcBillAmount);
                holder.setBillPaymentMode(model.getPayment_mode());
                holder.setBillUnitPaidTill(model.getPaid_upto());
                holder.setBillUnitUsed(elcUnitsUsed);
                holder.setElcBillMonthYear(elcBillMonthYear);
                // etc.

                // Format timestamp into date & time
                long timestamp = Long.parseLong(model.getEbill_timestamp());
                Date date = new Date(timestamp);

                String dateOnly = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date);
                String timeOnly = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);

                holder.setBillDateTime(dateOnly, timeOnly);

                // Long press to delete
                holder.itemView.setOnLongClickListener(v -> {
                    new MaterialAlertDialogBuilder(v.getContext())
                            .setTitle("Delete Electricity Bill?")
                            .setMessage("Are you sure you want to delete this bill record?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                // 🔑 Call your delete function here
                                deleteElcBillRecord(getRef(position).getKey(), property_id, convertMonthYearToKey(elcBillMonthYear), elcBillAmount, elcUnitsUsed);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .show();

                    return true; // ✅ consume the long press
                });

            }

            @NonNull
            @Override
            public BillsFragment.BillsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.single_ebill_layout_v2, parent, false);
                return new BillsFragment.BillsViewHolder(view);
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                int itemCount = getItemCount();
                if (itemCount == 0) {
                    layoutNoBillRecord.setVisibility(View.VISIBLE);
                } else {
                    layoutNoBillRecord.setVisibility(View.GONE);
                }
            }

        };

        rvElcBillList.setAdapter(firebaseBillsRecyclerAdapter);

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

    private void deleteElcBillRecord(String elc_bill_id, String pid, String elcBillMonthYear,
                                     int elcBillAmount, int elcUnitsUsed) {
        ebillsReference.child(elc_bill_id).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // ✅ Record deleted successfully
                    Toast.makeText(getContext(), "E-bill Deleted", Toast.LENGTH_SHORT).show();
                    subtractElcBillFromCollections(pid, elcBillMonthYear, elcBillAmount, elcUnitsUsed);
                    subtractUnitsPaidFromRooms(elcUnitsUsed);
                })
                .addOnFailureListener(e -> {
                    // ❌ Handle failure
                    Toast.makeText(getContext(), "E-bill Not Deleted", Toast.LENGTH_SHORT).show();
                });
    }

    public void subtractElcBillFromCollections(
            String pid,
            String monthYearKey,   // "2025-03"
            int elcBillToSubtract,
            int elcUnitsToSubtract
    ) {

        DatabaseReference elcBillCollectionReference =
                databaseReference.child("collections").child(pid).child(monthYearKey);

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

                int updatedElcBill = currentElcBill - elcBillToSubtract;
                int updatedUnitsUsed = currentUnitsUsed - elcUnitsToSubtract;
                currentData.child("total_elc_bill")
                        .setValue(Math.max(updatedElcBill, 0));

                currentData.child("total_units_used")
                        .setValue(Math.max(updatedUnitsUsed, 0));


                return Transaction.success(currentData);
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
                    Toast.makeText(requireContext(), "E-bill & Units Minus", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void subtractUnitsPaidFromRooms(
            int elcUnitsToSubtract
    ) {

        DatabaseReference roomsReference =
                databaseReference.child("rooms").child(room_id).child("last_unit_paid");

        roomsReference.runTransaction(new Transaction.Handler() {

            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {

                Integer currentLastPaid = data.getValue(Integer.class);

                if (currentLastPaid == null) {
                    // Nothing to subtract from
                    return Transaction.success(data);
                }

                int updatedLastPaid = currentLastPaid - elcUnitsToSubtract;

                // 🔒 Prevent negative values
                data.setValue(Math.max(updatedLastPaid, 0));

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
                    Toast.makeText(requireContext(), "Last Paid Updated", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    public static class BillsViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public BillsViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setBillDateTime(String billDate, String billTime){
            TextView billDateView = mView.findViewById(R.id.tvBillDateTime);
            String finalDateTime = billDate + ", " + billTime;
            billDateView.setText(finalDateTime);

        }

        public  void setBillUnitPaidTill(int billUnitPaidTill){
            TextView billUnitPaidTillView = mView.findViewById(R.id.tvBillPaidTill);
            billUnitPaidTillView.setText(String.valueOf(billUnitPaidTill));
        }

        public  void setBillUnitUsed(int billUnitUsed){
            TextView billUnitUsedView = mView.findViewById(R.id.tvBillUsedUnit);
            billUnitUsedView.setText(String.valueOf(billUnitUsed));
        }

        public void setBillAmount(int billAmount) {
            TextView billAmountView = mView.findViewById(R.id.tvBillAmount);
            String displayBill = "+ ₹" + NumberFormat.getInstance(new Locale("en", "IN")).format(billAmount);
            billAmountView.setText(displayBill);
        }

        public void setBillPaymentMode(String billPaymentMode) {
            TextView billPaymentModeView = mView.findViewById(R.id.tvBillPaymentMode);
            ImageView imgBillPaymentMode = mView.findViewById(R.id.imgBillPaymentMode);

            MaterialCardView mcvElcBillPaymentMode = mView.findViewById(R.id.mcvElcBillPaymentMode);

            billPaymentModeView.setText(String.valueOf(billPaymentMode));
            if (billPaymentMode.equals(("Online"))){
                imgBillPaymentMode.setImageResource(R.drawable.ic_bank_online);

                // Apply payment mode card background
                mcvElcBillPaymentMode.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.payment_mode_online_bg));

            }else {
                imgBillPaymentMode.setImageResource(R.drawable.ic_cash_payment);

                mcvElcBillPaymentMode.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.payment_mode_cash_bg));

            }
        }

        public void setElcBillMonthYear(String elcBillMonthYear){

            TextView tvElcBillMonth = mView.findViewById(R.id.tvElcBillMonth);
            TextView tvElcBillYear = mView.findViewById(R.id.tvElcBillYear);

            String month = elcBillMonthYear.substring(0,3).toUpperCase();
            String year = elcBillMonthYear.substring(elcBillMonthYear.lastIndexOf(" ") + 1);

            tvElcBillMonth.setText(month); // MAR
            tvElcBillYear.setText(year);   // 2025

        }

    }
}