package com.example.altuncu.blocksignal.util.concurrent;

import com.example.altuncu.blocksignal.util.concurrent.ListenableFuture.Listener;

import java.util.concurrent.ExecutionException;

public abstract class AssertedSuccessListener<T> implements Listener<T> {
  @Override
  public void onFailure(ExecutionException e) {
    throw new AssertionError(e);
  }
}
