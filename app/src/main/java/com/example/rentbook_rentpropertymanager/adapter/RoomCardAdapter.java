package com.example.rentbook_rentpropertymanager.adapter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rentbook_rentpropertymanager.R;
import com.example.rentbook_rentpropertymanager.model.Rooms;
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class RoomCardAdapter extends RecyclerView.Adapter<RoomCardAdapter.RoomViewHolder> {

    private Context context;
    private List<Rooms> roomList;
    private OnItemClickListener listener;

    public RoomCardAdapter(Context context, List<Rooms> roomList) {
        this.context = context;
        this.roomList = roomList;
    }

    // Define the interface for the callback
    public interface OnItemClickListener {
        void onItemClick(Rooms room);
    }

    // Add a public method to set the listener from the Activity
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomName, tvRentAmount, tvTenantName, tvTenantPhone, tvLastPaidUnit, tvRoomStatus, tvRoomRentStatus;
        ImageView imgRentStatus;
        MaterialCardView cardRoomOccupancy, dotRoomOccupancy;
        CircleImageView cimgTenantProfile;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvRentAmount = itemView.findViewById(R.id.tvRoomRent);
            tvTenantName = itemView.findViewById(R.id.tvTenantName);
            tvTenantPhone = itemView.findViewById(R.id.tvTenantNumber);
            cimgTenantProfile = itemView.findViewById(R.id.imgTenantProfile);
            tvRoomStatus = itemView.findViewById(R.id.tvRoomStatus);
            tvLastPaidUnit = itemView.findViewById(R.id.tvLastPaidUnit);
            cardRoomOccupancy = itemView.findViewById(R.id.cardRoomOccupancy);
            dotRoomOccupancy = itemView.findViewById(R.id.dotRoomOccupancy);

            tvRoomRentStatus = itemView.findViewById(R.id.tvRoomRentStatus);
            imgRentStatus = itemView.findViewById(R.id.imgRentStatus);
        }
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.single_room_layout, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        Rooms room = roomList.get(position);

        holder.tvRoomName.setText(room.getRoom_name());
        String rentFormat = NumberFormat.getNumberInstance().format(room.getRoom_rent());
        String monthlyRent = "₹" + rentFormat;
        holder.tvRentAmount.setText(monthlyRent);

        if (room.getTenant_name() != null && room.getTenant_phone() != null && room.getThumb_tenant_url() != null) {
            holder.tvTenantName.setText(room.getTenant_name());
            holder.tvTenantPhone.setText(room.getTenant_phone());
            Glide.with(context).load(room.getThumb_tenant_url())
                    .placeholder(R.drawable.ic_tenant_profile_default)
                    .into(holder.cimgTenantProfile);

            // ❗ IMPORTANT: Remove tint (because of view recycling)
            holder.cimgTenantProfile.clearColorFilter();

        } else {
            holder.tvTenantName.setText(R.string.text_no_tenant_added);   // "No Tenant Added"
            holder.tvTenantPhone.setText(R.string.text_clk_add_tenant);   // "Click (Add Tenant)"
            holder.cimgTenantProfile.setImageResource(R.drawable.ic_no_tenant_profile_default);
            // Apply tint
            holder.cimgTenantProfile.setColorFilter(
                    ContextCompat.getColor(context, R.color.text_heading),
                    PorterDuff.Mode.SRC_IN
            );
        }

        if (room.isIs_occupied()){
            holder.tvRoomStatus.setText(R.string.text_occupied);   // "Occupied"
            holder.tvRoomStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.occ_status_text)); // green text

            holder.cardRoomOccupancy.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.occ_status_bg));

            holder.dotRoomOccupancy.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.occ_status_dot));

            holder.tvRentAmount.setTextColor(ContextCompat.getColor(context, R.color.text_purple_300));

            // Set Rent Status
            String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.ENGLISH)
                    .format(new Date());
            String lastRentMonth = room.getLast_rent_month(); // from Firebase

            if (lastRentMonth != null && lastRentMonth.equals(currentMonth)) {
                // ✅ PAID
                holder.tvRoomRentStatus.setText(R.string.text_paid);
                holder.tvRoomRentStatus.setTextColor(ContextCompat.getColor(context, R.color.occ_status_dot));

                holder.imgRentStatus.setImageResource(R.drawable.ic_verified);
                holder.imgRentStatus.setColorFilter(ContextCompat.getColor(context, R.color.occ_status_dot));

            } else {
                // 🟠 PENDING
                holder.tvRoomRentStatus.setText(R.string.text_due);
                holder.tvRoomRentStatus.setTextColor(ContextCompat.getColor(context, R.color.due_status_text));

                holder.imgRentStatus.setImageResource(R.drawable.ic_clock);
                holder.imgRentStatus.setColorFilter(ContextCompat.getColor(context, R.color.due_status_text));
            }

        } else {
            holder.tvRoomStatus.setText(R.string.text_vacant);   // "Vacant"

            holder.tvRoomStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.vac_status_text));// red text
            holder.cardRoomOccupancy.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.vac_status_bg));

            holder.dotRoomOccupancy.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.vac_status_dot));

            holder.tvRentAmount.setTextColor(ContextCompat.getColor(context, R.color.not_app_status_text));

            // 🟠 Rent Status - Not Applicable
            holder.tvRoomRentStatus.setText(R.string.text_na);
            holder.tvRoomRentStatus.setTextColor(ContextCompat.getColor(context, R.color.not_app_status_text));

            holder.imgRentStatus.setImageResource(R.drawable.ic_dnd_na);
            holder.imgRentStatus.setColorFilter(ContextCompat.getColor(context, R.color.not_app_status_text));

        }
        holder.tvLastPaidUnit.setText(String.valueOf(room.getLast_unit_paid()));

        // Click on room card - goes to Room Details Activity
        // Set the OnClickListener on the entire item view
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(room);
            }
        });
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }
}
