package com.example.rentbook_rentpropertymanager.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rentbook_rentpropertymanager.R;
import com.example.rentbook_rentpropertymanager.model.ActivityLog;
import com.google.android.material.card.MaterialCardView;
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

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ActivityFragment extends Fragment {

    private ChipGroup chipGroup;
    private ActivityLogAdapter activityLogAdapter;
    private final List<ActivityLog> fullList = new ArrayList<>();
    private final List<ActivityLog> filteredList = new ArrayList<>();
    private RecyclerView rvActivityLogs;
    private HorizontalScrollView hsvLogEntityChips;
    private DatabaseReference activityLogsReference;
    private String user_id;
    private LinearLayout layoutNoActivity;
    private ProgressBar pbActivityLog;
    private static final int ITEMS_TO_LOAD = 40;
    private boolean isLoading = false;
    private String lastKey = null, selectedEntity = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        assert user != null;
        user_id = user.getUid();

        rvActivityLogs = view.findViewById(R.id.rvActivityLogs);
        chipGroup = view.findViewById(R.id.chipGroupActivityLogs);

        pbActivityLog = view.findViewById(R.id.pbActivityLog);
        layoutNoActivity = view.findViewById(R.id.layoutNoActivity);
        hsvLogEntityChips = view.findViewById(R.id.hsvLogEntityChips);

        rvActivityLogs.setLayoutManager(new LinearLayoutManager(getContext()));

        activityLogAdapter = new ActivityLogAdapter();
        rvActivityLogs.setAdapter(activityLogAdapter);

        activityLogsReference = FirebaseDatabase.getInstance()
                .getReference("activity_log")
                .child(user_id);

        loadActivityLogs();

        rvActivityLogs.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView,
                                   int dx, int dy) {

                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager =
                        (LinearLayoutManager) recyclerView.getLayoutManager();

                if (!isLoading && layoutManager != null) {

                    int totalItemCount = layoutManager.getItemCount();
                    int lastVisibleItem = layoutManager.findLastVisibleItemPosition();

                    if (lastVisibleItem >= totalItemCount - 3) {
                        loadActivityLogs();
                    }
                }
            }
        });
        return view;
    }

    // Load & Reverse Logs
    private void loadActivityLogs() {

        if (isLoading) return;

        isLoading = true;

        Query query;

        if (lastKey == null) {
            query = activityLogsReference
                    .orderByKey()
                    .limitToLast(ITEMS_TO_LOAD);
        } else {
            query = activityLogsReference
                    .orderByKey()
                    .endBefore(lastKey)   // 🔥 important change
                    .limitToLast(ITEMS_TO_LOAD);
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // ✅ Always hide loader once Firebase responds
                pbActivityLog.setVisibility(View.GONE);

                List<ActivityLog> tempList = new ArrayList<>();

                String firstKeyInBatch = null;

                for (DataSnapshot ds : snapshot.getChildren()) {

                    if (firstKeyInBatch == null) {
                        firstKeyInBatch = ds.getKey();  // save old key
                    }

                    ActivityLog log = ds.getValue(ActivityLog.class);

                    if (log != null) {
                        tempList.add(log);
                    }
                }
                // 🔥 HANDLE EMPTY STATE HERE
                if (tempList.isEmpty() && fullList.isEmpty()) {

                    showNoActivityUI();   // 👈 ADD THIS
                    isLoading = false;
                    return;
                }

                // ✅ DATA EXISTS
                hideNoActivityUI();  // 👈 ADD THIS
                //pbActivityLog.setVisibility(View.GONE);

                if (!tempList.isEmpty()) {
                    lastKey = firstKeyInBatch;  // update for next page
                    Collections.reverse(tempList);
                    fullList.addAll(tempList);

                    // Generate chips only first time
                    if (chipGroup.getChildCount() == 0) {
                        generateDynamicChips();
                    }

                    // Restore current filter
                    if (selectedEntity == null) {
                        activityLogAdapter.updateList(fullList);
                    } else {
                        applyFilter(selectedEntity);
                    }
                }
                isLoading = false;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isLoading = false;
            }
        });
    }

    private void showNoActivityUI() {
        layoutNoActivity.setVisibility(View.VISIBLE);
        rvActivityLogs.setVisibility(View.GONE);
        hsvLogEntityChips.setVisibility(View.GONE); // optional
    }

    private void hideNoActivityUI() {
        layoutNoActivity.setVisibility(View.GONE);
        rvActivityLogs.setVisibility(View.VISIBLE);
        hsvLogEntityChips.setVisibility(View.VISIBLE);
    }


    // Dynamic Chip Creation
    private void generateDynamicChips() {

        chipGroup.removeAllViews();

        Set<String> entitySet = new HashSet<>();
        for (ActivityLog log : fullList) {
            entitySet.add(log.getLog_entity());
        }

        // Always add "All" first
        Chip allChip = createChip("All", null);
        allChip.setChecked(true);
        chipGroup.addView(allChip);

        for (String entity : entitySet) {
            chipGroup.addView(createChip(formatEntity(entity), entity));
        }

        chipGroup.setSingleSelection(true);
    }

    private Chip createChip(String text, String entityValue) {

        Chip chip = new Chip(getContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setClickable(true);

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

        chip.setOnClickListener(v -> applyFilter(entityValue));

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

    // Filtering
    private void applyFilter(String entity) {

        selectedEntity = entity;

        filteredList.clear();

        if (entity == null) {

            filteredList.addAll(fullList);

        } else {

            for (ActivityLog log : fullList) {

                if (entity.equals(log.getLog_entity())) {
                    filteredList.add(log);
                }
            }
        }

        activityLogAdapter.updateList(filteredList);
    }

    // Format ENTITY_NAME → Entity Name
    private String formatEntity(String entity) {

        if (entity == null || entity.isEmpty()) return "";

        return String.valueOf(entity.charAt(0)).toUpperCase() +
                entity.substring(1).toLowerCase();
    }

    // Adapter Inside Activity
    private static class ActivityLogAdapter
            extends RecyclerView.Adapter<ActivityLogAdapter.ActivityLogViewHolder> {

        private List<ActivityLog> list = new ArrayList<>();

        @SuppressLint("NotifyDataSetChanged")
        void updateList(List<ActivityLog> newList) {
            list = newList;
            notifyDataSetChanged();
        }

        static class ActivityLogViewHolder extends RecyclerView.ViewHolder {

            TextView logTitle, logDesc, logDateTime, tvLogPrimaryValue;
            MaterialCardView mcvEntityDesc, mcvEntityIconBG, mcvLogPrimaryValueBG;
            ImageView imgEntityIcon;

            ActivityLogViewHolder(View itemView) {
                super(itemView);
                logTitle = itemView.findViewById(R.id.tvActivityLogTitle);
                logDesc = itemView.findViewById(R.id.tvActivityLogDesc);
                logDateTime = itemView.findViewById(R.id.tvActivityLogDateTime);
                tvLogPrimaryValue = itemView.findViewById(R.id.tvLogPrimaryValue);
                mcvEntityIconBG = itemView.findViewById(R.id.mcvEntityIconBG);
                imgEntityIcon = itemView.findViewById(R.id.imgEntityIcon);
                mcvEntityDesc = itemView.findViewById(R.id.mcvEntityDesc);
                mcvLogPrimaryValueBG = itemView.findViewById(R.id.mcvLogPrimaryValueBG);

            }
        }

        @NonNull
        @Override
        public ActivityLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                        int viewType) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.single_activity_log_layout, parent, false);

            return new ActivityLogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ActivityLogViewHolder holder,
                                     int position) {

            ActivityLog log = list.get(position);

            holder.logTitle.setText(log.getLog_title());
            holder.logDesc.setText(log.getLog_desc());

            // Format timestamp into date & time
            long log_ts = log.getLog_ts();
            Date date = new Date(log_ts);

            String finalDate = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date);

            holder.logDateTime.setText(finalDate);

            applyColorsByEntity(holder, log.getLog_entity(), log.getLog_primary_value());

        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    private static void applyColorsByEntity(ActivityLogAdapter.ActivityLogViewHolder holder, String entity, Object value) {

        Context context = holder.itemView.getContext();

        switch (entity) {

            case LogEntity.PROPERTY:

                // Change icon drawable
                holder.imgEntityIcon.setImageResource(R.drawable.ic_properties);

                // Apply icon card background
                holder.mcvEntityIconBG.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.entity_prop_bg));

                // Change icon tint
                holder.imgEntityIcon.setColorFilter(
                        ContextCompat.getColor(context, R.color.entity_prop_icon),
                        PorterDuff.Mode.SRC_IN);

                // Apply description card background
                holder.mcvEntityDesc.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.entity_prop_bg));

                // Apply stroke
                holder.mcvEntityDesc.setStrokeColor(
                        ContextCompat.getColor(context, R.color.entity_prop_bg_border));

                // Apply primary value card background
                holder.mcvLogPrimaryValueBG.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.entity_prop_bg));

                // Apply primary value text color
                holder.tvLogPrimaryValue.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.entity_prop_icon));

                if (value instanceof String) {
                    holder.tvLogPrimaryValue.setText((String) value);
                }else {
                    holder.tvLogPrimaryValue.setText("N/A");
                }

                break;


            case LogEntity.RENT:

                // Change icon drawable
                holder.imgEntityIcon.setImageResource(R.drawable.ic_receipt);

                // Apply icon card background
                holder.mcvEntityIconBG.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.entity_rent_bg));

                // Change icon tint
                holder.imgEntityIcon.setColorFilter(
                        ContextCompat.getColor(context, R.color.entity_rent_icon),
                        PorterDuff.Mode.SRC_IN);

                // Apply description card background
                holder.mcvEntityDesc.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.entity_rent_bg));

                // Apply stroke
                holder.mcvEntityDesc.setStrokeColor(
                        ContextCompat.getColor(context, R.color.entity_rent_bg_border));

                // Apply primary value card background
                holder.mcvLogPrimaryValueBG.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.entity_rent_bg));

                // Apply primary value text color
                holder.tvLogPrimaryValue.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.entity_rent_icon));

                if (value instanceof Long) {
                    long amount = (Long) value;
                    String finalValue = "+ " + formatAmount((int) amount);
                    holder.tvLogPrimaryValue.setText(finalValue);
                }else {
                    holder.tvLogPrimaryValue.setText("N/A");
                }

                break;


            case LogEntity.TENANT:

                // Change icon drawable
                holder.imgEntityIcon.setImageResource(R.drawable.ic_add_user);

                // Apply icon card background
                holder.mcvEntityIconBG.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.entity_tenant_bg));

                // Change icon tint
                holder.imgEntityIcon.setColorFilter(
                        ContextCompat.getColor(context, R.color.entity_tenant_icon),
                        PorterDuff.Mode.SRC_IN);

                // Apply description card background
                holder.mcvEntityDesc.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.entity_tenant_bg));

                // Apply stroke
                holder.mcvEntityDesc.setStrokeColor(
                        ContextCompat.getColor(context, R.color.entity_tenant_bg_border));

                // Apply primary value card background
                holder.mcvLogPrimaryValueBG.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.entity_tenant_bg));

                // Apply primary value text color
                holder.tvLogPrimaryValue.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.entity_tenant_icon));

                if (value instanceof String) {
                    holder.tvLogPrimaryValue.setText((String) value);
                }else {
                    holder.tvLogPrimaryValue.setText("N/A");
                }

                break;


            case LogEntity.UTILITY:

                // Change icon drawable
                holder.imgEntityIcon.setImageResource(R.drawable.ic_meter_reading);

                // Apply icon card background
                holder.mcvEntityIconBG.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.entity_elc_bill_bg));

                // Change icon tint
                holder.imgEntityIcon.setColorFilter(
                        ContextCompat.getColor(context, R.color.entity_elc_bill_icon),
                        PorterDuff.Mode.SRC_IN);

                // Apply description card background
                holder.mcvEntityDesc.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.entity_elc_bill_bg));

                // Apply stroke
                holder.mcvEntityDesc.setStrokeColor(
                        ContextCompat.getColor(context, R.color.entity_elc_bill_bg_border));

                // Apply primary value card background
                holder.mcvLogPrimaryValueBG.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.entity_elc_bill_bg));

                // Apply primary value text color
                holder.tvLogPrimaryValue.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.entity_elc_bill_icon));

                if (value instanceof Long) {
                    long amount = (Long) value;
                    String finalValue = "+" + amount + " units";
                    holder.tvLogPrimaryValue.setText(finalValue);
                }else {
                    holder.tvLogPrimaryValue.setText("N/A");
                }

                break;
        }
    }

    public static class LogEntity {
        public static final String PROPERTY = "PROPERTY";
        public static final String RENT = "RENT";
        public static final String TENANT = "TENANT";
        public static final String UTILITY = "UTILITY";
    }

    public static String formatAmount(int amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        format.setMaximumFractionDigits(0);
        format.setMinimumFractionDigits(0);
        return format.format(amount);
    }

    private void deleteOldActivityLogs() {

        DatabaseReference logsRef = FirebaseDatabase.getInstance()
                .getReference("activity_log")
                .child(user_id);

        long currentTime = System.currentTimeMillis();

        long twoYearsMillis =
                1000L * 60 * 60 * 24 * 365 * 2;

        long cutoffTime = currentTime - twoYearsMillis;

        Query query = logsRef
                .orderByChild("log_ts")
                .endAt(cutoffTime);

        query.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot ds : snapshot.getChildren()) {

                    ds.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void refreshLogsFromStart() {

        lastKey = null;
        isLoading = false;

        fullList.clear();
        activityLogAdapter.notifyDataSetChanged();

        loadActivityLogs();
        deleteOldActivityLogs();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);   // 🔥 VERY IMPORTANT
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem refreshItem = menu.add(Menu.NONE, 1001, Menu.NONE, "Refresh");
        refreshItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_sync);

        if (icon != null) {
            icon = DrawableCompat.wrap(icon);
            DrawableCompat.setTint(icon, ContextCompat.getColor(requireContext(), R.color.white));
            refreshItem.setIcon(icon);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == 1001) {

            refreshLogsFromStart();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}

