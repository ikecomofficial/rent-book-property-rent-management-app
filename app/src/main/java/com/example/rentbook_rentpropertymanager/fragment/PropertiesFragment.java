package com.example.rentbook_rentpropertymanager.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rentbook_rentpropertymanager.AddProperty;
import com.example.rentbook_rentpropertymanager.MainActivity;
import com.example.rentbook_rentpropertymanager.PropertyDetailsActivity;
import com.example.rentbook_rentpropertymanager.R;
import com.example.rentbook_rentpropertymanager.model.Properties;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class PropertiesFragment extends Fragment {

    // 📊 Property List & Empty State
    private RecyclerView propertyList;
    private LinearLayout layoutNoPropertyList;
    private MaterialCardView btnAddProperty;

    // 👤 User Info
    private String user_id;
    private TextView tvUserName;
    private CircleImageView cimgUserProfile;

    // 📈 Overall Property Stats
    private int occupiedSum, totalSum, propertiesItemCount;
    private TextView tvProgressLabel;
    private TextView tvPropertiesCount;
    private ProgressBar progressBarProperties;
    private CircularProgressIndicator occupancyProgressBar;

    // 🏠 Room & Shop Occupancy Stats
    private int roomOccupiedSum, totalRoomSum, shopOccupiedSum, totalShopSum;
    private TextView tvCombinedRoomOcc, tvCombinedRoomTotal, tvCombinedShopOcc, tvCombinedShopTotal;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_properties, container, false);

        // 📊 Property List & Empty State
        layoutNoPropertyList = view.findViewById(R.id.layoutNoPropertyList);

        // 👤 User Info
        tvUserName = view.findViewById(R.id.tvUserName);
        cimgUserProfile = view.findViewById(R.id.cimgUserAccount);

        // 📈 Overall Property Stats
        progressBarProperties = view.findViewById(R.id.progressBarProperties);
        tvProgressLabel = view.findViewById(R.id.tvProgressLabel);
        tvPropertiesCount = view.findViewById(R.id.tvPropertiesCount);
        occupancyProgressBar = view.findViewById(R.id.occupancyProgressBar);

        // 🏠 Room & Shop Occupancy Stats
        tvCombinedRoomOcc = view.findViewById(R.id.tvCombinedRoomOcc);
        tvCombinedRoomTotal = view.findViewById(R.id.tvCombinedRoomTotal);
        tvCombinedShopOcc = view.findViewById(R.id.tvCombinedShopOcc);
        tvCombinedShopTotal = view.findViewById(R.id.tvCombinedShopTotal);

        // 🎯 Actions
        btnAddProperty = view.findViewById(R.id.btnAddProperty);
        MaterialCardView btnAddFirstProperty = view.findViewById(R.id.btnAddFirstProperty);
        propertyList = view.findViewById(R.id.propertyListRecycleView);
        propertyList.setHasFixedSize(true);
        propertyList.setLayoutManager(new LinearLayoutManager(getContext()));

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        assert user != null;
        user_id = user.getUid();

        String providerId = user.getProviderId();
        if (providerId.equals("google.com")) {
            // Set UserName on top
            tvUserName.setText(user.getDisplayName());

        } else {
            fetchUserNameFromFirebase();
        }

        cimgUserProfile.setOnClickListener(v -> {
            ((MainActivity) requireActivity())
                    .goToTab(3, R.id.nav_settings);

        });


        btnAddProperty.setOnClickListener(v -> {

            Intent intent = new Intent(getContext(), AddProperty.class);
            startActivity(intent);

        });

        btnAddFirstProperty.setOnClickListener(v -> {

            Intent intent = new Intent(getContext(), AddProperty.class);
            startActivity(intent);

        });

        return view;
    }

    private void fetchUserNameFromFirebase(){
        DatabaseReference userReference = FirebaseDatabase.getInstance().getReference().child("users").child(user_id);
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    String user_name = snapshot.child("name").getValue(String.class);
                    String user_thumb_prof = snapshot.child("thumb_profile_url").getValue(String.class);

                    if (user_name == null || user_name.trim().isEmpty()) {
                        user_name = getString(R.string.text_user);
                    }

                    tvUserName.setText(user_name);

                    Glide.with(requireContext())
                            .load((user_thumb_prof == null || user_thumb_prof.trim().isEmpty() || "default".equals(user_thumb_prof))
                                    ? R.drawable.ic_tenant_profile_default
                                    : user_thumb_prof)
                            .placeholder(R.drawable.ic_tenant_profile_default)
                            .error(R.drawable.ic_tenant_profile_default)
                            .into(cimgUserProfile);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadPropertiesFromFirebase() {
        DatabaseReference propertiesReference = FirebaseDatabase.getInstance().getReference().child("properties");

        Query userProperties = propertiesReference.orderByChild("user_id").equalTo(user_id);
        FirebaseRecyclerOptions<Properties> options = new FirebaseRecyclerOptions.Builder<Properties>()
                .setQuery(userProperties, Properties.class)
                .build();

        FirebaseRecyclerAdapter<Properties, PropertiesViewHolder> firebaseRecyclerAdapter =
                new FirebaseRecyclerAdapter<>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull PropertiesViewHolder holder, int position, @NonNull Properties model) {

                        // Bind your data here
                        String property_name = model.getProperty_name();
                        String property_address = model.getProperty_address();

                        holder.setPropertyName(property_name);
                        holder.setPropertyAddress(property_address);

                        holder.setPropertyOccupancy(model.getRooms_occupied(), model.getTotal_rooms(), model.getShops_occupied(),
                                model.getTotal_shops());
                        // etc.

                        // Send pid to PropertyDetails Activity
                        String property_id = getRef(position).getKey();

                        holder.mView.setOnClickListener(view -> {

                            Intent propertyDetailIntent = new Intent(getContext(), PropertyDetailsActivity.class);
                            propertyDetailIntent.putExtra("property_id", property_id);
                            propertyDetailIntent.putExtra("property_name", property_name);
                            propertyDetailIntent.putExtra("property_address", property_address);
                            startActivity(propertyDetailIntent);
                        });
                    }


                    @NonNull
                    @Override
                    public PropertiesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.single_property_layout, parent, false);
                        return new PropertiesViewHolder(view);
                    }

                    @Override
                    public void onDataChanged() {
                        super.onDataChanged();

                        roomOccupiedSum = 0;
                        totalRoomSum = 0;
                        shopOccupiedSum = 0;
                        totalShopSum = 0;
                        occupiedSum = 0;
                        totalSum = 0;

                        for (int i = 0; i < getItemCount(); i++) {
                            Properties model = getItem(i);

                            int propertyOccSum = model.getRooms_occupied() + model.getShops_occupied();
                            int propertyTotalSum = model.getTotal_rooms() + model.getTotal_shops();

                            occupiedSum += propertyOccSum;
                            totalSum += propertyTotalSum;

                            // Get Total & Occupied Rooms/Shops combined of all properties

                            roomOccupiedSum += model.getRooms_occupied();
                            totalRoomSum += model.getTotal_rooms();
                            shopOccupiedSum += model.getShops_occupied();
                            totalShopSum += model.getTotal_shops();

                        }

                        // Update the progress bar with cumulative totals
                        setProgressBarData(occupiedSum, totalSum);

                        // Update Total & Occupied Rooms/Shops combined of all properties
                        setAllPropertiesRoomShopData(roomOccupiedSum, totalRoomSum, shopOccupiedSum, totalShopSum);

                        progressBarProperties.setVisibility(View.GONE);
                        propertyList.setVisibility(View.VISIBLE);
                        propertiesItemCount = getItemCount();
                        if (propertiesItemCount == 0) {
                            layoutNoPropertyList.setVisibility(View.VISIBLE);
                            btnAddProperty.setVisibility(View.GONE);
                        } else {
                            layoutNoPropertyList.setVisibility(View.GONE);
                            btnAddProperty.setVisibility(View.VISIBLE);
                            String p_count = "Properties" + " (" + propertiesItemCount + ")";
                            tvPropertiesCount.setText(p_count);
                        }
                    }
                };

        firebaseRecyclerAdapter.startListening();
        propertyList.setAdapter(firebaseRecyclerAdapter);

    }

    public void setProgressBarData(int occupied, int total) {
        if (total > 0) {
            int bar_percentage = (occupied * 100) / total;

            occupancyProgressBar.setMax(100); // make sure max is 100
            occupancyProgressBar.setProgress(bar_percentage);

            String progressPercentLabel = bar_percentage + "%";

            // Update label
            tvProgressLabel.setText(progressPercentLabel);
        } else {
            occupancyProgressBar.setProgress(0);
            tvProgressLabel.setText("N/A");
        }
    }

    public void setAllPropertiesRoomShopData(int roomOccupied, int totalRoom, int shopOccupied, int totalShop) {
        if (totalRoom > 0 && totalShop > 0) {
            tvCombinedRoomOcc.setText(String.valueOf(roomOccupied));
            tvCombinedRoomTotal.setText(String.valueOf(totalRoom));
            tvCombinedShopOcc.setText(String.valueOf(shopOccupied));
            tvCombinedShopTotal.setText(String.valueOf(totalShop));
        } else {
            tvCombinedRoomOcc.setText(String.valueOf(0));
            tvCombinedRoomTotal.setText(String.valueOf(0));
            tvCombinedShopOcc.setText(String.valueOf(0));
            tvCombinedShopTotal.setText(String.valueOf(0));
        }

    }

    public static class PropertiesViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public PropertiesViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setPropertyName(String propertyName) {
            TextView propertyNameView = mView.findViewById(R.id.tvPropertyName);
            propertyNameView.setText(propertyName);
        }

        public void setPropertyAddress(String propertyAddress) {
            TextView propertyNameView = mView.findViewById(R.id.tvPropertyAddress);
            propertyNameView.setText(propertyAddress);
        }

        public void setPropertyOccupancy(int occupiedRooms, int totalRooms, int occupiedShops, int totalShops) {
            TextView tvOccupiedRooms = mView.findViewById(R.id.tvOccupiedRooms);
            TextView tvTotalRooms = mView.findViewById(R.id.tvTotalRooms);
            TextView tvOccupiedShops = mView.findViewById(R.id.tvOccupiedShops);
            TextView tvTotalShops = mView.findViewById(R.id.tvTotalShops);

            MaterialCardView dotPropertyOcc = mView.findViewById(R.id.dotPropertyOcc);
            MaterialCardView dotPropertyOccRing = mView.findViewById(R.id.dotPropertyOccRing);

            tvOccupiedRooms.setText(String.valueOf(occupiedRooms));
            tvTotalRooms.setText(String.valueOf(totalRooms));
            tvOccupiedShops.setText(String.valueOf(occupiedShops));
            tvTotalShops.setText(String.valueOf(totalShops));

            int totalOcc = occupiedRooms + occupiedShops;
            int totalRoomShops = totalRooms + totalShops;

            if (totalOcc == totalRoomShops) {
                dotPropertyOcc.setCardBackgroundColor(
                        ContextCompat.getColor(mView.getContext(), R.color.occ_status_dot));

                dotPropertyOccRing.setStrokeColor(
                        ContextCompat.getColor(mView.getContext(), R.color.occ_status_bg));
            } else {
                dotPropertyOcc.setCardBackgroundColor(
                        ContextCompat.getColor(mView.getContext(), R.color.partial_status_dot));

                dotPropertyOccRing.setStrokeColor(
                        ContextCompat.getColor(mView.getContext(), R.color.partial_status_bg));
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        loadPropertiesFromFirebase();
    }

}

