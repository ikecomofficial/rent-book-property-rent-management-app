package com.example.rentbook_rentpropertymanager.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rentbook_rentpropertymanager.MainActivity;
import com.example.rentbook_rentpropertymanager.R;
import com.example.rentbook_rentpropertymanager.model.MonthlyCollections;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class CollectionsFragment extends Fragment {

    private String user_id, property_id;
    private TextView tvTotalPropAmount, tvTotalPropRent, tvTotalPropElcBill;
    private int propTotalAmount, propTotalRent, propTotalElcBill;
    private RecyclerView rvCollectionsList;
    private ChipGroup chipGroupProperties;
    private String preSelectedPropertyId;
    private HorizontalScrollView hsvPropertiesChips;
    private LinearLayout layoutNoCollection;
    private FirebaseRecyclerAdapter<MonthlyCollections, CollectionsViewHolder> firebaseRecyclerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collections, container, false);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        assert user != null;
        user_id = user.getUid();

        if (getArguments() != null) {
            preSelectedPropertyId = getArguments().getString("property_id");
        }

        tvTotalPropAmount = view.findViewById(R.id.tvTotalPropAmount);
        tvTotalPropRent = view.findViewById(R.id.tvTotalPropRent);
        tvTotalPropElcBill = view.findViewById(R.id.tvTotalPropElcBill);

        layoutNoCollection = view.findViewById(R.id.layoutNoCollection);

        chipGroupProperties = view.findViewById(R.id.chipGroupProperties);
        hsvPropertiesChips = view.findViewById(R.id.hsvPropertiesChips);

        loadPropertyChips();

        // Rent Recycler View
        rvCollectionsList = view.findViewById(R.id.collectionListRecyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);    // newest items appear at top
        linearLayoutManager.setStackFromEnd(true);    // optional, usually false
        rvCollectionsList.setHasFixedSize(true);
        rvCollectionsList.setLayoutManager(linearLayoutManager);

        return view;
    }

    private void loadPropertyChips() {

        DatabaseReference propertiesReference = FirebaseDatabase.getInstance().getReference().child("properties");

        Query userProperties = propertiesReference.orderByChild("user_id").equalTo(user_id);

        userProperties.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                chipGroupProperties.removeAllViews();

                Chip chipToSelect = null;

                for (DataSnapshot property : snapshot.getChildren()) {

                    String propertyId = property.getKey();
                    String propertyName = property.child("property_name").getValue(String.class);

                    if (propertyName == null) continue;

                    Chip chip = createChip(propertyId, propertyName);
                    chipGroupProperties.addView(chip);

                    // If coming from a specific property
                    if (preSelectedPropertyId != null &&
                            preSelectedPropertyId.equals(propertyId)) {
                        chipToSelect = chip;
                    }

                    // Otherwise, select first property
                    if (preSelectedPropertyId == null && chipToSelect == null) {
                        chipToSelect = chip;
                    }
                }

                // Auto-select correct chip
                if (chipToSelect != null) {
                    chipToSelect.setChecked(true);
                    property_id = (String) chipToSelect.getTag();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Failed to load properties", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Chip createChip(String propertyId, String propertyName) {

        Chip chip = new Chip(getContext());
        chip.setText(propertyName);
        chip.setTag(propertyId);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setCheckedIconVisible(false);

        // Minimal styling (no extra XML)
        chip.setChipCornerRadius(50f);
        chip.setChipStrokeWidth(1f);
        chip.setChipStrokeColorResource(R.color.primary_main);
        chip.setChipBackgroundColorResource(R.color.white);
        chip.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_heading)
        );

        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {

                // ✅ Chip just became selected (either first time or switched)
                setChipSelected(chip);

                hsvPropertiesChips.post(() ->
                        hsvPropertiesChips.smoothScrollTo(
                                buttonView.getLeft() - 32, 0
                        )
                );

                loadMonthlyCollectionsFromFirebase(propertyId);

                if (firebaseRecyclerAdapter != null) {
                    rvCollectionsList.setAdapter(firebaseRecyclerAdapter);
                    firebaseRecyclerAdapter.startListening();
                }

            } else {

                // 🚫 If user tapped the already-selected chip, ignore unselect
                if (buttonView.isPressed()) {
                    chip.setChecked(true);
                    return;
                }

                // ✅ This uncheck happened because another chip was selected
                setChipUnselected(chip);
            }
        });

        return chip;
    }

    private void setChipSelected(Chip chip) {
        chip.setChipBackgroundColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primary_main)));
        chip.setTextColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.white)));
    }

    private void setChipUnselected(Chip chip) {
        chip.setChipBackgroundColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.white)));
        chip.setTextColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.text_muted)));
    }

    // Demo loader (replace later with real logic)

    private void loadMonthlyCollectionsFromFirebase(String pid){
        DatabaseReference collectionsReference = FirebaseDatabase.getInstance().getReference().child("collections").child(pid);

        Query propertyCollections = collectionsReference.orderByKey();
        FirebaseRecyclerOptions<MonthlyCollections> options = new FirebaseRecyclerOptions.Builder<MonthlyCollections>()
                .setQuery(propertyCollections, MonthlyCollections.class)
                .build();

        FirebaseRecyclerAdapter<MonthlyCollections, CollectionsViewHolder> firebaseRecyclerAdapter =
                new FirebaseRecyclerAdapter<MonthlyCollections, CollectionsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull CollectionsViewHolder holder, int position, @NonNull MonthlyCollections model) {

                        // Bind your data here
                        String monthYearKey = getRef(position).getKey(); // "2025-02"

                        int totalRent = model.getTotal_rent();
                        int totalElcBill = model.getTotal_elc_bill();
                        int totalUnitsUsed = model.getTotal_units_used();
                        int totalCollection = totalRent + totalElcBill;
                        if (monthYearKey != null){
                            model.setCollection_month_year(monthYearKey);
                            holder.setCollectionMonthYear(monthYearKey);
                        }else {
                            model.setCollection_month_year("N/A");
                            holder.setCollectionMonthYear("N/A");
                        }
                        holder.setCollectionTotalAmount(Objects.requireNonNullElse(totalCollection, 0));
                        holder.setCollectionTotalRent(Objects.requireNonNullElse(totalRent, 0));
                        holder.setCollectionTotalElcBill(Objects.requireNonNullElse(totalElcBill, 0));
                        holder.setTotalUnitsUsed(Objects.requireNonNullElse(totalUnitsUsed, 0));

                    }

                    @NonNull
                    @Override
                    public CollectionsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.single_month_collection_layout, parent, false);
                        return new CollectionsViewHolder(view);
                    }

                    @Override
                    public void onDataChanged(){
                        super.onDataChanged();

                        if (getItemCount() == 0){
                            tvTotalPropAmount.setText("N/A");
                            tvTotalPropRent.setText("N/A");
                            tvTotalPropElcBill.setText("N/A");
                            layoutNoCollection.setVisibility(View.VISIBLE);
                        }else {

                            propTotalAmount = 0;
                            propTotalRent = 0;
                            propTotalElcBill = 0;

                            for (int i = 0; i < getItemCount(); i++) {
                                MonthlyCollections model = getItem(i);

                                int propertyTotalRentSum = model.getTotal_rent();
                                int propertyTotalElcBillSum = model.getTotal_elc_bill();


                                propTotalRent += propertyTotalRentSum;
                                propTotalElcBill += propertyTotalElcBillSum;


                            }

                            propTotalAmount = propTotalRent + propTotalElcBill;
                            tvTotalPropAmount.setText(formatAmount(propTotalAmount));
                            tvTotalPropRent.setText(formatAmount(propTotalRent));
                            tvTotalPropElcBill.setText(formatAmount(propTotalElcBill));

                            layoutNoCollection.setVisibility(View.GONE);
                        }

                    }
                };

        firebaseRecyclerAdapter.startListening();
        rvCollectionsList.setAdapter(firebaseRecyclerAdapter);

    }

    public static String formatAmount(int amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        format.setMaximumFractionDigits(0);
        format.setMinimumFractionDigits(0);
        return format.format(amount);
    }

    public static class CollectionsViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public CollectionsViewHolder(View itemView) {
            super(itemView);
            mView = itemView;

        }


        public void setCollectionMonthYear(String collectionMonthYear){

            TextView tvCollectionMY = mView.findViewById(R.id.tvCollectionMY);
            tvCollectionMY.setText(convertKeyToMonthYear(collectionMonthYear));

        }

        public void setTotalUnitsUsed(int totalUnitsUsed){
            TextView tvTotalUnitsUsed = mView.findViewById(R.id.tvTotalUnitsUsed);
            String totalConsumedUnits = "Power Units: " + totalUnitsUsed;
            tvTotalUnitsUsed.setText(totalConsumedUnits);
        }

        public void setCollectionTotalAmount(int collectionTotalAmount){

            TextView tvTotalAmount = mView.findViewById(R.id.tvTotalAmount);
            tvTotalAmount.setText(formatAmount(collectionTotalAmount));

        }
        public void setCollectionTotalRent(int collectionTotalRent){

            TextView tvTotalRent = mView.findViewById(R.id.tvTotalRent);
            tvTotalRent.setText(formatAmount(collectionTotalRent));

        }
        public void setCollectionTotalElcBill(int collectionTotalElcBill){

            TextView tvTotalElcBill = mView.findViewById(R.id.tvTotalElcBill);
            tvTotalElcBill.setText(formatAmount(collectionTotalElcBill));

        }
        /*public void setRentDateTime(String rentDate, String rentTime){
            TextView rentDateView = mView.findViewById(R.id.tvRentDateTime);
            String finalDateTime = rentDate + ", " + rentTime;
            rentDateView.setText(finalDateTime);

        }

         */
    }

    public static String convertKeyToMonthYear(String input) {
        if (input == null || input.trim().isEmpty()) return "N/A";

        try {
            DateFormat inputFormat =
                    new SimpleDateFormat("yyyy-MM", Locale.ENGLISH);

            DateFormat outputFormat =
                    new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);

            Date date = inputFormat.parse(input.trim());
            assert date != null;
            return outputFormat.format(date);

        } catch (ParseException e) {
            return "N/A";
        }
    }
/*
    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) requireActivity()).showToolbar(true, "Collections");
    }

 */

}

