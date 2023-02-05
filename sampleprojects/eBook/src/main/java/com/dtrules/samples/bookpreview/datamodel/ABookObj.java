package com.dtrules.samples.bookpreview.datamodel;

import com.dtrules.mapping.DataMap;

public abstract class ABookObj implements DataObj {

  static int sid = 1;
  int id = sid++;
  boolean printed = false;

  public abstract void write2DataMap(DataMap datamap);

  public int getId() {
    return id;
  }

  public boolean getPrinted() {
    if (!printed) {
      printed = true;
      return false;
    }
    return true;
  }
}
