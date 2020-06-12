package com.asiainfo.cem.common.utils;

public class Pairs<T1, T2> {

  public Pairs(T1 item1, T2 item2) {
    fst = item1;
    snd = item2;
  }

  public final T1 fst;
  public final T2 snd;
}
