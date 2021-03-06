/**
 * Copyright (c) 2019 Enes Altuncu
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.example.altuncu.blocksignal.database;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;

import androidx.annotation.NonNull;

import com.example.altuncu.blocksignal.database.helpers.SQLCipherOpenHelper;
import com.example.altuncu.blocksignal.providers.PhotoProvider;

import java.util.Set;

public abstract class Database {

  protected static final String ID_WHERE              = "_id = ?";
  private   static final String CONVERSATION_URI      = "content://textsecure/thread/";
  private   static final String CONVERSATION_LIST_URI = "content://textsecure/conversation-list";
  private   static final String ATTACHMENT_URI        = "content://textsecure/attachment/";

  protected       SQLCipherOpenHelper databaseHelper;
  protected final Context             context;

  public Database(Context context, SQLCipherOpenHelper databaseHelper) {
    this.context        = context;
    this.databaseHelper = databaseHelper;
  }

  protected void notifyConversationListeners(Set<Long> threadIds) {
    for (long threadId : threadIds)
      notifyConversationListeners(threadId);
  }

  protected void notifyConversationListeners(long threadId) {
    context.getContentResolver().notifyChange(PhotoProvider.parseGetPhotoUri(CONVERSATION_URI + threadId), null);
  }

  protected void notifyConversationListListeners() {
    context.getContentResolver().notifyChange(PhotoProvider.parseGetPhotoUri(CONVERSATION_LIST_URI), null);
  }

  protected void setNotifyConverationListeners(Cursor cursor, long threadId) {
    cursor.setNotificationUri(context.getContentResolver(), PhotoProvider.parseGetPhotoUri(CONVERSATION_URI + threadId));
  }

  protected void setNotifyConverationListListeners(Cursor cursor) {
    cursor.setNotificationUri(context.getContentResolver(), PhotoProvider.parseGetPhotoUri(CONVERSATION_LIST_URI));
  }

  protected void registerAttachmentListeners(@NonNull ContentObserver observer) {
    context.getContentResolver().registerContentObserver(PhotoProvider.parseGetPhotoUri(ATTACHMENT_URI),
                                                         true,
                                                         observer);
  }

  protected void notifyAttachmentListeners() {
    context.getContentResolver().notifyChange(PhotoProvider.parseGetPhotoUri(ATTACHMENT_URI), null);
  }

  public void reset(SQLCipherOpenHelper databaseHelper) {
    this.databaseHelper = databaseHelper;
  }

}
