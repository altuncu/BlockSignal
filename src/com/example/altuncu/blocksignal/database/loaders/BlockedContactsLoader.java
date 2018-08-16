package com.example.altuncu.blocksignal.database.loaders;

import android.content.Context;
import android.database.Cursor;

import com.example.altuncu.blocksignal.database.DatabaseFactory;
import com.example.altuncu.blocksignal.util.AbstractCursorLoader;

public class BlockedContactsLoader extends AbstractCursorLoader {

  public BlockedContactsLoader(Context context) {
    super(context);
  }

  @Override
  public Cursor getCursor() {
    return DatabaseFactory.getRecipientDatabase(getContext())
                          .getBlocked();
  }

}
