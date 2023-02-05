package com.dtrules.samples.bookpreview.datamodel;

import com.dtrules.mapping.DataMap;
import com.dtrules.xmlparser.XMLPrinter;
import java.util.ArrayList;
import java.util.List;

public class Customer extends ABookObj {

  List<Open_Book> open_books = new ArrayList<Open_Book>();
  List<String> notes = new ArrayList<String>();

  public List<Open_Book> getOpen_books() {
    return open_books;
  }

  public void setOpen_books(List<Open_Book> open_books) {
    this.open_books = open_books;
  }

  public List<String> getNotes() {
    return notes;
  }

  public void setNotes(List<String> notes) {
    this.notes = notes;
  }

  public void write2DataMap(DataMap datamap) {
    datamap.opentag(this, "customer");
    if (printed) {
      datamap.closetag();
      return;
    }
    datamap.opentag("open_books");
    for (Open_Book ob : open_books) {
      ob.write2DataMap(datamap);
    }
    datamap.closetag();
    datamap.closetag();
  }
}
