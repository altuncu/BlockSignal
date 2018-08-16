package com.example.altuncu.blocksignal.giph.ui;


import android.os.Bundle;
import android.support.v4.content.Loader;

import com.example.altuncu.blocksignal.giph.model.GiphyImage;
import com.example.altuncu.blocksignal.giph.net.GiphyGifLoader;

import java.util.List;

public class GiphyGifFragment extends GiphyFragment {

  @Override
  public Loader<List<GiphyImage>> onCreateLoader(int id, Bundle args) {
    return new GiphyGifLoader(getActivity(), searchString);
  }

}
