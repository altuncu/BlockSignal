package com.example.altuncu.blocksignal;

import androidx.annotation.NonNull;

import com.example.altuncu.blocksignal.database.model.ThreadRecord;
import com.example.altuncu.blocksignal.mms.GlideRequests;

import java.util.Locale;
import java.util.Set;

public interface BindableConversationListItem extends Unbindable {

  public void bind(@NonNull ThreadRecord thread,
                   @NonNull GlideRequests glideRequests, @NonNull Locale locale,
                   @NonNull Set<Long> selectedThreads, boolean batchMode);
}
