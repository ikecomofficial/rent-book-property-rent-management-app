package com.example.rentbook_rentpropertymanager.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class CollectionsFragment extends Fragment {

    private String user_id, property_id;
    private TextView tvTotalPropAmount, tvTotalPropRent, tvTotalPropElcBill;
    private int propTotalAmount, propTotalRent, propTotalElcBill;
    private RecyclerView rvCollectionsList;
    private ChipGroup chipGroupProperties;
    private View viewNoChipsGap;
    private String preSelectedPropertyId;
    private ProgressBar progressBarCollections;
    private HorizontalScrollView hsvPropertiesChips;
    private LinearLayout layoutNoCollection;
    private FirebaseRecyclerAdapter<MonthlyCollections, CollectionsViewHolder> firebaseRecyclerAdapter;
    private MaterialCardView mcvYearSelector;

    private MaterialAutoCompleteTextView dropdownYear;

    private final List<String> yearList = new ArrayList<>();

    private String selectedYear = "All Time";

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
        progressBarCollections = view.findViewById(R.id.progressBarCollections);

        chipGroupProperties = view.findViewById(R.id.chipGroupProperties);
        hsvPropertiesChips = view.findViewById(R.id.hsvPropertiesChips);

        viewNoChipsGap = view.findViewById(R.id.viewNoChipsGap);

        dropdownYear = view.findViewById(R.id.dropdownYear);
        mcvYearSelector = view.findViewById(R.id.mcvYearSelector);

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

        DatabaseReference propertiesReference = FirebaseDatabase.getInstance().getReference().child("properties")
                .child(user_id);

        //Query userProperties = propertiesReference.orderByChild("user_id").equalTo(user_id);

        propertiesReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                chipGroupProperties.removeAllViews();

                // 🚫 No properties at all
                if (!snapshot.hasChildren()) {

                    hsvPropertiesChips.setVisibility(View.GONE);
                    viewNoChipsGap.setVisibility(View.VISIBLE);

                    // 🔥 THIS IS WHAT YOU WERE MISSING
                    showNoCollectionUI();

                    return;
                }

                // ✅ Properties exist
                hsvPropertiesChips.setVisibility(View.VISIBLE);
                viewNoChipsGap.setVisibility(View.GONE);

                Chip chipToSelect = null;

                for (DataSnapshot property : snapshot.getChildren()) {

                    String propertyId = property.getKey();
                    String propertyName = property.child("property_name").getValue(String.class);

                    if (propertyName == null) continue;

                    Chip chip = createChip(propertyId, propertyName);
                    chipGroupProperties.addView(chip);

                    if (preSelectedPropertyId != null &&
                            preSelectedPropertyId.equals(propertyId)) {
                        chipToSelect = chip;
                    }

                    if (preSelectedPropertyId == null && chipToSelect == null) {
                        chipToSelect = chip;
                    }
                }

                // ✅ Auto select first chip → THIS triggers collection load
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

    private void showNoCollectionUI() {
        layoutNoCollection.setVisibility(View.VISIBLE);
        rvCollectionsList.setVisibility(View.GONE);
    }

    private void showCollectionList() {
        layoutNoCollection.setVisibility(View.GONE);
        rvCollectionsList.setVisibility(View.VISIBLE);
    }

    private void loadAvailableYears(String pid){

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("collections").child(pid);

        reference.addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        yearList.clear();

                        yearList.add("All Time");

                        Set<String> uniqueYears = new HashSet<>();

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                            String key = dataSnapshot.getKey();
                            if (key != null && key.contains("-")) {
                                String year = key.split("-")[0];
                                uniqueYears.add(year);
                            }
                        }

                        List<String> sortedYears = new ArrayList<>(uniqueYears);

                        Collections.sort(sortedYears, Collections.reverseOrder());

                        yearList.addAll(sortedYears);

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                        requireContext(), R.layout.item_collection_year_dropdown, yearList);

                        dropdownYear.setAdapter(adapter);

                        dropdownYear.setText(selectedYear, false);

                        mcvYearSelector.setOnClickListener(v -> dropdownYear.showDropDown());

                        dropdownYear.setOnClickListener(v -> dropdownYear.showDropDown());

                        dropdownYear.setOnItemClickListener((parent, view, position, id) -> {
                                    selectedYear = yearList.get(position);
                                    loadMonthlyCollectionsFromFirebase(pid);
                                });
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {

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
        chip.setChipBackgroundColorResource(R.color.bg_list_layout_primary);
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

                selectedYear = "All Time";
                loadAvailableYears(propertyId);
                loadMonthlyCollectionsFromFirebase(propertyId);

            } else {

                // 🚫 If user tapped the already-selected chip, ignore unselect
                if (buttonView.isPressed()) {
                    chip.setChecked(true);
                    return;
                }

                // ✅ This un-check happened because another chip was selected
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
                ContextCompat.getColor(requireContext(), R.color.bg_list_layout_primary)));
        chip.setTextColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.text_muted)));
    }

    // Demo loader (replace later with real logic)

    private void loadMonthlyCollectionsFromFirebase(String pid){

        DatabaseReference collectionsReference = FirebaseDatabase.getInstance().getReference().child("collections").child(pid);

        Query propertyCollections;

        if (selectedYear.equals("All Time")) {
            propertyCollections = collectionsReference.orderByKey();
        } else {
            propertyCollections = collectionsReference.orderByKey()
                    .startAt(selectedYear + "-01")
                    .endAt(selectedYear + "-12");
        }

        FirebaseRecyclerOptions<MonthlyCollections> options = new FirebaseRecyclerOptions.Builder<MonthlyCollections>()
                .setQuery(propertyCollections, MonthlyCollections.class).build();

        if (firebaseRecyclerAdapter != null){
            firebaseRecyclerAdapter.stopListening();
        }

        firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<MonthlyCollections, CollectionsViewHolder>(options) {
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

                        progressBarCollections.setVisibility(View.GONE);
                        if (getItemCount() == 0){
                            tvTotalPropAmount.setText(formatAmount(0));
                            tvTotalPropRent.setText(formatAmount(0));
                            tvTotalPropElcBill.setText(formatAmount(0));
                            layoutNoCollection.setVisibility(View.VISIBLE);
                            rvCollectionsList.setVisibility(View.GONE);
                            //progressBarCollections.setVisibility(View.GONE);
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

                            //progressBarCollections.setVisibility(View.GONE);
                            layoutNoCollection.setVisibility(View.GONE);
                            rvCollectionsList.setVisibility(View.VISIBLE);
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

    @Override
    public void onStop() {
        super.onStop();

        if (firebaseRecyclerAdapter != null){
            firebaseRecyclerAdapter.stopListening();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (firebaseRecyclerAdapter != null){
            firebaseRecyclerAdapter.startListening();
        }
    }

    public static class CollectionsViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public CollectionsViewHolder(View itemView) {
            super(itemView);
            mView = itemView;

        }


        public void setCollectionMonthYear(String collectionMonthYear){

            TextView tvCollectionMonth = mView.findViewById(R.id.tvCollectionMonth);
            TextView tvCollectionYear = mView.findViewById(R.id.tvCollectionYear);
            //tvCollectionMY.setText(convertKeyToMonthYear(collectionMonthYear));

            // Extract year
            String year = collectionMonthYear.substring(0, 4);

            // Extract month number
            int monthIndex = Integer.parseInt(collectionMonthYear.substring(5, 7));

            // Get month name
            String[] months = new DateFormatSymbols(Locale.ENGLISH).getShortMonths();
            String month = months[monthIndex - 1].toUpperCase();

            // Results
            tvCollectionMonth.setText(month); // MAR
            tvCollectionYear.setText(year);   // 2025


        }

        public void setTotalUnitsUsed(int totalUnitsUsed){
            TextView tvTotalUnitsUsed = mView.findViewById(R.id.tvTotalUnitsUsed);
            String totalConsumedUnits = totalUnitsUsed + " units";
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
    }

}

