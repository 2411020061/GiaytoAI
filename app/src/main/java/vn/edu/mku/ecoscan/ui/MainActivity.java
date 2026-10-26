package vn.edu.mku.ecoscan.ui;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import vn.edu.mku.ecoscan.R;
import vn.edu.mku.ecoscan.ui.alerts.AlertsFragment;
import vn.edu.mku.ecoscan.ui.home.HomeFragment;
import vn.edu.mku.ecoscan.ui.scan.ScanFragment;
import vn.edu.mku.ecoscan.ui.vault.VaultFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        bottomNav = findViewById(R.id.bottomNav);
        setupListeners();

        // Default tab: Trang chủ
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
        }
    }

    private void setupListeners() {
        bottomNav.setOnItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.nav_home) {
                replaceFragment(new HomeFragment());
            } else if (id == R.id.nav_vault) {
                replaceFragment(new VaultFragment());
            } else if (id == R.id.nav_scan) {
                replaceFragment(new ScanFragment());
            } else if (id == R.id.nav_alerts) {
                replaceFragment(new AlertsFragment());
            }
            return true;
        });
    }

    public void switchToTab(int navId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(navId);
        }
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
