package com.example.altuncu.blocksignal.contacts.avatars;

import androidx.annotation.NonNull;

import com.example.altuncu.blocksignal.color.MaterialColor;
import com.example.altuncu.blocksignal.color.MaterialColors;

public class ContactColors {

  public static final MaterialColor UNKNOWN_COLOR = MaterialColor.GREY;

  public static MaterialColor generateFor(@NonNull String name) {
    return MaterialColors.CONVERSATION_PALETTE.get(Math.abs(name.hashCode()) % MaterialColors.CONVERSATION_PALETTE.size());
  }

}
