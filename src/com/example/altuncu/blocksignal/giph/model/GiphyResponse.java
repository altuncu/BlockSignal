package com.example.altuncu.blocksignal.giph.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GiphyResponse {

  @JsonProperty
  private List<GiphyImage> data;

  public List<GiphyImage> getData() {
    return data;
  }

}
