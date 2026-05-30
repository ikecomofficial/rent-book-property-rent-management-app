package com.example.rentbook_rentpropertymanager.adapter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.rentbook_rentpropertymanager.fragment.BillsFragment;
import com.example.rentbook_rentpropertymanager.fragment.RentsFragment;

public class RentBillPagerAdapter extends FragmentStateAdapter {

    private final String room_id, property_id;
    public RentBillPagerAdapter(@NonNull FragmentActivity fragmentActivity, String room_id, String property_id) {
        super(fragmentActivity);
        this.room_id = room_id;
        this.property_id = property_id;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        Bundle bundle = new Bundle();
        bundle.putString("room_id", room_id);
        bundle.putString("property_id", property_id);
        if (position == 0) {
            fragment = new RentsFragment();
        } else {
            fragment = new BillsFragment();
        }
        fragment.setArguments(bundle); // pass the roomId to fragment
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
