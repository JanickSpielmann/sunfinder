package it.spielmann.sunfinder;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_settings);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        View header = findViewById(R.id.settingsHeader);
        int basePaddingPx = (int) (8 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), topInset + basePaddingPx, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        RadioGroup radioGroup = findViewById(R.id.radioGroupPreference);
        boolean preferShade = Prefs.isPreferShade(this);
        radioGroup.check(preferShade ? R.id.radioShade : R.id.radioSun);

        radioGroup.setOnCheckedChangeListener((group, checkedId) ->
                Prefs.setPreferShade(this, checkedId == R.id.radioShade));
    }
}
