package com.erievs.totallykickasstube;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFERENCES_NAME = "app_preferences";
    private static final String KEY_STREAMING_TYPE = "streaming_type";

    private RadioGroup streamingTypeGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        streamingTypeGroup = findViewById(R.id.streaming_type_group);

        // they're two types webm with vp9/opus and mp4 with avc1/acc
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        String savedStreamingType = preferences.getString(KEY_STREAMING_TYPE, "mp4");

        if ("webm".equals(savedStreamingType)) {
            RadioButton webmRadioButton = findViewById(R.id.option_webm);
            webmRadioButton.setChecked(true);
        } else {
            RadioButton mp4RadioButton = findViewById(R.id.option_mp4);
            mp4RadioButton.setChecked(true);
        }

        streamingTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedRadioButton = findViewById(checkedId);
            String selectedOption = selectedRadioButton.getText().toString().toLowerCase();

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_STREAMING_TYPE, selectedOption);
            editor.apply();
        });
    }
}