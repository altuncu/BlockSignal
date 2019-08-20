package com.example.altuncu.blocksignal.mms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import com.example.altuncu.blocksignal.database.Address;
import com.example.altuncu.blocksignal.database.model.MessageRecord;

/**
 * Copyright (c) 2019 Enes Altuncu
 * Represents the information required to find the {@link MessageRecord} pointed to by a quote.
 */
public class QuoteId {

  private static final String TAG = QuoteId.class.getSimpleName();

  private static final String ID      = "id";
  private static final String AUTHOR  = "author";

  private final long    id;
  private final Address author;

  public QuoteId(long id, @NonNull Address author) {
    this.id     = id;
    this.author = author;
  }

  public long getId() {
    return id;
  }

  public @NonNull Address getAuthor() {
    return author;
  }

  public @NonNull String serialize() {
    try {
      JSONObject object = new JSONObject();
      object.put(ID, id);
      object.put(AUTHOR, author.serialize());
      return object.toString();
    } catch (JSONException e) {
      Log.e(TAG, "Failed to serialize to json", e);
      return "";
    }
  }

  public static @Nullable QuoteId deserialize(@NonNull String serialized) {
    try {
      JSONObject json = new JSONObject(serialized);
      return new QuoteId(json.getLong(ID), Address.fromSerialized(json.getString(AUTHOR)));
    } catch (JSONException e) {
      Log.e(TAG, "Failed to deserialize from json", e);
      return null;
    }
  }
}
