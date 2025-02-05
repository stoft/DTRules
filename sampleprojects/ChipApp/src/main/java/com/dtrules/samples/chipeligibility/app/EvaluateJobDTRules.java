package com.dtrules.samples.chipeligibility.app;

import com.dtrules.entity.IREntity;
import com.dtrules.infrastructure.RulesException;
import com.dtrules.interpreter.IRObject;
import com.dtrules.interpreter.RArray;
import com.dtrules.mapping.DataMap;
import com.dtrules.mapping.Mapping;
import com.dtrules.samples.chipeligibility.app.dataobjects.Case;
import com.dtrules.samples.chipeligibility.app.dataobjects.Client;
import com.dtrules.samples.chipeligibility.app.dataobjects.Income;
import com.dtrules.samples.chipeligibility.app.dataobjects.Job;
import com.dtrules.samples.chipeligibility.app.dataobjects.Relationship;
import com.dtrules.samples.chipeligibility.app.dataobjects.Result;
import com.dtrules.session.DTState;
import com.dtrules.session.IRSession;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class EvaluateJobDTRules implements EvaluateJob {

  @Override
  public String getName() {
    return "dtrules";
  }

  String getJobName(Job job) {
    int id = job.getId();
    String cnt = "" + id;
    for (; id < 100000; id *= 10) cnt = "0" + cnt;
    return "Job_" + cnt;
  }

  public String evaluate(int threadnum, ChipApp app, Job job) {
    try {
      IRSession session = app.rs.newSession();

      if (app.trace && (job.getId() % app.save == 0)) {
        OutputStream out = new FileOutputStream(
          app.getOutputDirectory() + getJobName(job) + "_trace.xml"
        );
        session.getState().setOutput(out, System.out);
        session.getState().setState(DTState.TRACE | DTState.DEBUG);
        session.getState().traceStart();
      }

      // Map the data from the job into the Rules Engine

      // First create a data map.
      Mapping mapping = session.getMapping();
      DataMap datamap = session.getDataMap(mapping, null);

      datamap.opentag(job, "job");
      datamap.readDO(job, "job");

      Case c = job.getCase();
      datamap.opentag(c, "case");
      datamap.readDO(c, "case");
      for (Client client : c.getClients()) {
        datamap.opentag(client, "client");
        datamap.readDO(client, "client");
        for (Income income : client.getIncomes()) {
          datamap.readDO(income, "income");
        }
        datamap.closetag();
      }
      for (Relationship r : c.getRelationships()) {
        datamap.opentag("relationship");
        datamap.printdata("type", r.getType());
        datamap.readDO(r.getSource(), "source");
        datamap.readDO(r.getTarget(), "target");
        datamap.closetag();
      }
      datamap.closetag();

      datamap.closetag();

      int id = job.getId();

      if (app.save > 0 && id % app.save == 0) {
        String cnt = "" + id;
        for (; id < 100000; id *= 10) cnt = "0" + cnt;
        try {
          datamap.print(
            new FileOutputStream(
              app.getOutputDirectory() + "job_" + cnt + ".xml"
            )
          );
        } catch (Exception e) {
          System.out.println(
            "Couldn't write to '" +
            app.getOutputDirectory() +
            "job_" +
            cnt +
            ".xml'"
          );
        }
      }

      mapping.loadData(session, datamap);

      // Once the data is loaded, execute the rules.
      session.execute(app.getDecisionTableName());

      if (app.trace && (job.getId() % app.save == 0)) {
        session.getState().traceEnd();
      }

      printReport(threadnum, app, session);
    } catch (Exception ex) {
      System.out.print("<-ERR  ");
      ex.printStackTrace();
      return "\nAn Error occurred while running the example:\n" + ex + "\n";
    }
    return null;
  }

  public void printReport(int threadnum, ChipApp app, IRSession session)
    throws RulesException {
    IREntity job = session.getState().find("job.job").rEntityValue();
    RArray results = job.get("job.results").rArrayValue();
    String jobId = job.get("id").stringValue();
    RArray clients = session.getState().find("case.clients").rArrayValue();

    if (results.size() == 0 && app.console) {
      System.out.println("No results for job " + jobId);
    }

    int approved = 0;
    int denied = 0;
    for (IRObject r : results) {
      IREntity result = r.rEntityValue();

      IREntity client = result.get("client").rEntityValue();
      String clinetId = client.get("id").stringValue();
      String answer = "";
      if (result.get("eligible").booleanValue()) {
        answer = "Approved";
        approved++;
        app.getApprovedClients().add(Integer.parseInt(clinetId));
      } else {
        answer = "Denied";
        denied++;
        app.getDeniedClients().add(Integer.parseInt(clinetId));
      }

      if (app.console) {
        String notes = "";
        RArray ns = result.get("notes").rArrayValue();
        for (IRObject n : ns) {
          notes += "   " + n.stringValue() + "\n";
        }
        System.out.println();
        System.out.println(
          "Job " +
          jobId +
          " Client " +
          clinetId +
          " was " +
          answer +
          "\n" +
          notes
        );
      }

      RArray notes = result.get("notes").rArrayValue();
      synchronized (app) {
        for (IRObject n : notes) {
          String note = n.stringValue();
          Integer v = app.results.get(note);
          if (v == null) v = 0;
          app.results.put(note, v + 1);
        }
      }
    }

    app.update(threadnum, clients.size(), approved, denied);
  }
}
