package com.example.altuncu.blocksignal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.example.altuncu.blocksignal.preferences.MmsPreferencesActivity;

public class PromptMmsActivity extends PassphraseRequiredActionBarActivity {

  @Override
  protected void onCreate(Bundle bundle, boolean ready) {
    setContentView(com.example.altuncu.blocksignal.R.layout.prompt_apn_activity);
    initializeResources();
  }

  private void initializeResources() {
    Button okButton = findViewById(com.example.altuncu.blocksignal.R.id.ok_button);
    Button cancelButton = findViewById(com.example.altuncu.blocksignal.R.id.cancel_button);

    okButton.setOnClickListener(v -> {
      Intent intent = new Intent(PromptMmsActivity.this, MmsPreferencesActivity.class);
      intent.putExtras(PromptMmsActivity.this.getIntent().getExtras());
      startActivity(intent);
      finish();
    });

    cancelButton.setOnClickListener(v -> finish());
  }

}
