package com.example.rentbook_rentpropertymanager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.rentbook_rentpropertymanager.fragment.AccountFragment;
import com.example.rentbook_rentpropertymanager.fragment.ActivityFragment;
import com.example.rentbook_rentpropertymanager.fragment.CollectionsFragment;
import com.example.rentbook_rentpropertymanager.fragment.PropertiesFragment;

public class BottomNavPagerAdapter extends FragmentStateAdapter {

    public BottomNavPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new PropertiesFragment();
            case 1: return new CollectionsFragment();
            case 2: return new ActivityFragment();
            default: return new AccountFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}

