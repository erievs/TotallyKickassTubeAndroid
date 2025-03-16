package com.erievs.totallykickasstube;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFERENCES_NAME = "app_preferences";
    private static final String KEY_STREAMING_TYPE = "streaming_type";
    private static final String KEY_YT_DLP_BRANCH = "yt_dlp_branch";

    private RadioGroup streamingTypeGroup;
    private RadioGroup ytDlpBranchGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        streamingTypeGroup = findViewById(R.id.streaming_type_group);
        ytDlpBranchGroup = findViewById(R.id.yt_dlp_branch_group);

        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);

        String savedStreamingType = preferences.getString(KEY_STREAMING_TYPE, "mp4");
        if ("webm".equals(savedStreamingType)) {
            ((RadioButton) findViewById(R.id.option_webm)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.option_mp4)).setChecked(true);
        }

        String savedYtDlpBranch = preferences.getString(KEY_YT_DLP_BRANCH, "NIGHTLY");
        switch (savedYtDlpBranch) {
            case "STABLE":
                ((RadioButton) findViewById(R.id.option_stable)).setChecked(true);
                break;
            case "MASTER":
                ((RadioButton) findViewById(R.id.option_master)).setChecked(true);
                break;
            case "NIGHTLY":
            default:
                ((RadioButton) findViewById(R.id.option_nightly)).setChecked(true);
                break;
        }

        streamingTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedOption = ((RadioButton) findViewById(checkedId)).getText().toString().toLowerCase();
            preferences.edit().putString(KEY_STREAMING_TYPE, selectedOption).apply();
        });

        ytDlpBranchGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedBranch = ((RadioButton) findViewById(checkedId)).getText().toString().toUpperCase();
            preferences.edit().putString(KEY_YT_DLP_BRANCH, selectedBranch).apply();
        });
    }
}