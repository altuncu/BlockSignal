package com.example.altuncu.blocksignal.crypto;


import android.content.Context;
import androidx.annotation.NonNull;

import com.example.altuncu.blocksignal.util.Base64;
import com.example.altuncu.blocksignal.util.TextSecurePreferences;
import com.example.altuncu.blocksignal.util.Util;

import java.io.IOException;

public class ProfileKeyUtil {

  public static synchronized boolean hasProfileKey(@NonNull Context context) {
    return TextSecurePreferences.getProfileKey(context) != null;
  }

  public static synchronized @NonNull byte[] getProfileKey(@NonNull Context context) {
    try {
      String encodedProfileKey = TextSecurePreferences.getProfileKey(context);

      if (encodedProfileKey == null) {
        encodedProfileKey = Util.getSecret(32);
        TextSecurePreferences.setProfileKey(context, encodedProfileKey);
      }

      return Base64.decode(encodedProfileKey);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

}
