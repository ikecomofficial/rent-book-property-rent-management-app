package com.example.rentbook_rentpropertymanager;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.rentbook_rentpropertymanager.fragment.CollectionsFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;
    private long backPressedTime = 0;
    private String preSelectedPropertyId;
    private MaterialToolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        // Check if the user is already signed in. If not, redirect to LoginScreen.
        if (user == null) {
            goToLoginScreen();
            return;
        }

        toolbar = findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbar);

        // Set your ONE fixed background color here
        toolbar.setBackground(
                new ColorDrawable(ContextCompat.getColor(this, R.color.primary_main))
        );

        int openTab = getIntent().getIntExtra("open_tab", -1);
        preSelectedPropertyId = getIntent().getStringExtra("property_id");

        if (openTab == 1) {
            bottomNav.setSelectedItemId(R.id.nav_collections); // your 3rd item id
        }

        viewPager = findViewById(R.id.view_pager);
        bottomNav = findViewById(R.id.bottom_nav);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            return insets; // consume nothing → removes extra padding
        });

        viewPager.setAdapter(new BottomNavPagerAdapter(this));

        // 🚫 Disable swipe
        viewPager.setUserInputEnabled(false);

        // Bottom nav → swipe
        bottomNav.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_properties) {
                viewPager.setCurrentItem(0, false);
                // Hide Toolbar

            } else if (item.getItemId() == R.id.nav_collections) {
                viewPager.setCurrentItem(1, false);

                CollectionsFragment fragment = new CollectionsFragment();
                Bundle bundle = new Bundle();
                bundle.putString("property_id", preSelectedPropertyId);
                fragment.setArguments(bundle);

            } else if (item.getItemId() == R.id.nav_activity) {
                viewPager.setCurrentItem(2, false);

            } else {
                viewPager.setCurrentItem(3, false);
            }
            return true;
        });

        // Swipe → bottom nav
        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        switch (position) {
                            case 0:
                                bottomNav.setSelectedItemId(R.id.nav_properties);
                                toolbar.setVisibility(View.GONE);
                                break;
                            case 1:
                                bottomNav.setSelectedItemId(R.id.nav_collections);
                                toolbar.setVisibility(View.VISIBLE);
                                toolbar.setTitle("Collection Overview");
                                toolbar.setBackgroundColor(
                                        ContextCompat.getColor(MainActivity.this, R.color.toolbar_bg)
                                );
                                break;
                            case 2:
                                bottomNav.setSelectedItemId(R.id.nav_activity);
                                toolbar.setVisibility(View.VISIBLE);
                                toolbar.setTitle("Recent Activities");
                                toolbar.setBackgroundColor(
                                        ContextCompat.getColor(MainActivity.this, R.color.toolbar_bg)
                                );
                                break;
                            case 3:
                                bottomNav.setSelectedItemId(R.id.nav_settings);
                                toolbar.setVisibility(View.VISIBLE);
                                toolbar.setTitle("Account Setting");
                                toolbar.setBackgroundColor(
                                        ContextCompat.getColor(MainActivity.this, R.color.toolbar_bg)
                                );
                                break;
                        }
                    }
                });
    }

    private void goToLoginScreen() {
        Intent intent = new Intent(MainActivity.this, LoginScreen.class);
        startActivity(intent);
        finish(); // End MainActivity so the user can't press back to it
    }

    public void goToTab(int position, int menuId) {
        viewPager.setCurrentItem(position, false);
        bottomNav.setSelectedItemId(menuId);
    }

    @Override
    public void onBackPressed() {

        // If NOT on first fragment
        if (viewPager.getCurrentItem() != 0) {

            // Go to first fragment
            viewPager.setCurrentItem(0, false);
            bottomNav.setSelectedItemId(R.id.nav_properties); // your first tab id
            return;
        }

        // If already on first fragment
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed(); // exit app
            return;
        } else {
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        }

        backPressedTime = System.currentTimeMillis();
    }


}