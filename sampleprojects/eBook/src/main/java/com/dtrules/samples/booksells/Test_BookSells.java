/**
 * Copyright 2004-2009 DTRules.com, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.dtrules.samples.booksells;

import com.dtrules.samples.bookreturns.Test_BookReturns;
import com.dtrules.session.IRSession;
import com.dtrules.testsupport.ATestHarness;
import com.dtrules.xmlparser.XMLPrinter;
import java.io.PrintStream;

public class Test_BookSells extends ATestHarness {

  public static String path = System.getProperty("user.dir") + "/";
  public static String parms = path + "/rs_sells/xml/testParms.xml";

  public Test_BookSells() {
    try {
      load(parms);
    } catch (Exception e) {}
  }

  public static void main(String[] args) throws Exception {
    Test_BookSells t = new Test_BookSells();
    t.runTests();
    t.writeDecisionTables("BookSells", null, true, 10);
  }

  @Override
  public void printReport(int runNumber, IRSession session, PrintStream out)
    throws Exception {
    XMLPrinter xout = new XMLPrinter(out);
    xout.opentag("result");
    session.printEntityReport(
      xout,
      false,
      false,
      session.getState(),
      "access",
      session.getState().find("access")
    );
    session.printEntityReport(
      xout,
      false,
      false,
      session.getState(),
      "note",
      session.getState().find("customer").rEntityValue().get("notes")
    );
    xout.closetag();
  }
}
