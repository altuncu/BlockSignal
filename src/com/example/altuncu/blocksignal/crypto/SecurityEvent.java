package com.example.altuncu.blocksignal.crypto;

import android.content.Context;
import android.content.Intent;

import com.example.altuncu.blocksignal.recipients.Recipient;
import com.example.altuncu.blocksignal.service.KeyCachingService;

/**
 * Copyright (c) 2019 Enes Altuncu
 * This class processes key exchange interactions.
 *
 * @author Moxie Marlinspike
 */

public class SecurityEvent {

  public static final String SECURITY_UPDATE_EVENT = "com.example.altuncu.blocksignal.KEY_EXCHANGE_UPDATE";

  public static void broadcastSecurityUpdateEvent(Context context) {
    Intent intent = new Intent(SECURITY_UPDATE_EVENT);
    intent.setPackage(context.getPackageName());
    context.sendBroadcast(intent, KeyCachingService.KEY_PERMISSION);
  }

}
