package com.dtrules.samples.bookpreview.app;

import com.dtrules.xmlparser.AGenericXMLParser;
import com.dtrules.xmlparser.GenericXMLParser;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

public class LoadSettings extends AGenericXMLParser {

  BookPreviewApp app;

  LoadSettings(BookPreviewApp app) {
    this.app = app;
  }

  boolean isTrue(String body) {
    return (
      body.equalsIgnoreCase("true") ||
      body.equals("1") ||
      body.equalsIgnoreCase("yes") ||
      body.equalsIgnoreCase("y")
    );
  }

  @Override
  public void endTag(
    String[] tagstk,
    int tagstkptr,
    String tag,
    String body,
    HashMap<String, String> attribs
  ) throws Exception, IOException {
    if (tag.equals("threads")) {
      app.threads = Integer.parseInt(body);
    } else if (tag.equals("db_overhead")) {
      app.db_delay = Integer.parseInt(body);
    } else if (tag.equals("numCases")) {
      app.numCases = Integer.parseInt(body);
    } else if (tag.equals("save")) {
      app.save = Integer.parseInt(body);
    } else if (tag.equals("console")) {
      app.console = isTrue(body);
    } else if (tag.equals("update")) {
      app.update = Integer.parseInt(body);
    } else if (tag.equals("dtrules")) {
      if (body.equals("dtrules")) {
        app.ejob = new EvaluateJobDTRules();
      } else if (body.equals("java")) {
        //app.ejob = new EvaluateJobJava();
      } else {
        app.ejob = new EvaluateJobNone();
      }
    } else if (tag.equals("printresults")) {
      app.printresults = isTrue(body);
    }
  }

  public static void loadSettings(BookPreviewApp app) throws Exception {
    LoadSettings ls = new LoadSettings(app);
    FileInputStream settings = new FileInputStream(
      app.getRulesDirectoryPath() + "settings.xml"
    );

    GenericXMLParser.load(settings, ls);
  }
}
