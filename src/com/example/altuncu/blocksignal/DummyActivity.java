package com.example.altuncu.blocksignal;

import android.app.Activity;
import android.os.Bundle;

/**
 * Copyright (c) 2019 Enes Altuncu
 * Workaround for Android bug:
 * https://code.google.com/p/android/issues/detail?id=53313
 */
public class DummyActivity extends Activity {
  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    finish();
  }
}
