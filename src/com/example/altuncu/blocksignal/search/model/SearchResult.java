package com.example.altuncu.blocksignal.search.model;

import androidx.annotation.NonNull;

import com.example.altuncu.blocksignal.database.CursorList;
import com.example.altuncu.blocksignal.database.model.ThreadRecord;
import com.example.altuncu.blocksignal.recipients.Recipient;

import java.util.List;

/**
 * Copyright (c) 2019 Enes Altuncu
 * Represents an all-encompassing search result that can contain various result for different
 * subcategories.
 */
public class SearchResult {

  public static final SearchResult EMPTY = new SearchResult("", CursorList.emptyList(), CursorList.emptyList(), CursorList.emptyList());

  private final String                    query;
  private final CursorList<Recipient>     contacts;
  private final CursorList<ThreadRecord>  conversations;
  private final CursorList<MessageResult> messages;

  public SearchResult(@NonNull String                    query,
                      @NonNull CursorList<Recipient>     contacts,
                      @NonNull CursorList<ThreadRecord>  conversations,
                      @NonNull CursorList<MessageResult> messages)
  {
    this.query         = query;
    this.contacts      = contacts;
    this.conversations = conversations;
    this.messages      = messages;
  }

  public List<Recipient> getContacts() {
    return contacts;
  }

  public List<ThreadRecord> getConversations() {
    return conversations;
  }

  public List<MessageResult> getMessages() {
    return messages;
  }

  public String getQuery() {
    return query;
  }

  public int size() {
    return contacts.size() + conversations.size() + messages.size();
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public void close() {
    contacts.close();
    conversations.close();
    messages.close();
  }
}
