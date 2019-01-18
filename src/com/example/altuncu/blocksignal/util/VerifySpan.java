package com.example.altuncu.blocksignal.util;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.style.ClickableSpan;
import android.view.View;

import com.example.altuncu.blocksignal.crypto.IdentityKeyParcelable;
import com.example.altuncu.blocksignal.database.Address;
import com.example.altuncu.blocksignal.database.documents.IdentityKeyMismatch;
import org.whispersystems.libsignal.IdentityKey;

public class VerifySpan extends ClickableSpan {

    private final Context     context;
    private final Address     address;
    private final IdentityKey identityKey;

    public VerifySpan(@NonNull Context context, @NonNull IdentityKeyMismatch mismatch) {
        this.context     = context;
        this.address     = mismatch.getAddress();
        this.identityKey = mismatch.getIdentityKey();
    }

    public VerifySpan(@NonNull Context context, @NonNull Address address, @NonNull IdentityKey identityKey) {
        this.context     = context;
        this.address     = address;
        this.identityKey = identityKey;
    }

    @Override
    public void onClick(View widget) {
        // TODO => Implement verifyKeys() && put out of onClick()
    }
}