package com.example.altuncu.blocksignal.search.model;

import androidx.annotation.NonNull;

import com.example.altuncu.blocksignal.recipients.Recipient;

/**
 * Represents a search result for a message.
 */
public class MessageResult {

  public final Recipient recipient;
  public final String    bodySnippet;
  public final long      threadId;
  public final long      receivedTimestampMs;

  public MessageResult(@NonNull Recipient recipient,
                       @NonNull String bodySnippet,
                       long threadId,
                       long receivedTimestampMs)
  {
    this.recipient           = recipient;
    this.bodySnippet         = bodySnippet;
    this.threadId            = threadId;
    this.receivedTimestampMs = receivedTimestampMs;
  }
}
