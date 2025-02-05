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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the interface to EvaluateJob.  This class must be stateless in
 * order to support multiple threads using it to evaluate jobs for CHIP.
 * @author paul
 *
 */
public class EvaluateJobJava implements EvaluateJob {

  int FPL_Base = 867;
  int FPL_PerAdditionalPerson = 300;

  @Override
  public String getName() {
    return "java";
  }

  String[] excludedIncomes = { "CS", "SS" };

  String[] coveredCounties = {
    "AA",
    "AK",
    "BC",
    "BK",
    "BT",
    "CR",
    "CO",
    "TX",
    "LO",
    "AR",
    "XR",
    "XO",
    "XX",
    "XO",
    "XR",
  };

  /**
   * This is our state class for collecting various intermediate results needed
   * as we process a job for CHIP.
   *
   * @author paul
   *
   */
  class JState {

    Map<Integer, Boolean> clientEligibility = new HashMap<Integer, Boolean>();
    Map<Integer, String[]> clientNotes = new HashMap<Integer, String[]>();
    Map<Integer, Integer> clientIncome = new HashMap<Integer, Integer>();
    Map<Integer, Integer> clientGroupCnt = new HashMap<Integer, Integer>();
    Map<Integer, Integer> clientTotalIncome = new HashMap<Integer, Integer>();
  }

  @Override
  /**
   * Process the given job for CHIP eligibility for the clients in the
   * Job that are applying for CHIP.  Also takes a thread number for
   * reporting purposes.
   *
   * @param threadnum
   * @param app
   * @param job
   * @return
   */
  public String evaluate(int threadnum, ChipApp app, Job job) {
    JState jstate = new JState();

    if (job.getProgram() == "CHIP") {
      Calculate_Individual_Income(job, jstate);
      Calculate_Group_Size(job, jstate);
      Evaluate_Chip_Eligibility(job, jstate);
      Evaluate_Results(threadnum, app, job, jstate);
    }

    return null;
  }

  /**
   * Go through the income records for each individual in the case,
   * and add up their countable income.
   * @param job
   * @param jstate
   */
  public void Calculate_Individual_Income(Job job, JState jstate) {
    for (Client client : job.getCase().getClients()) {
      Integer amount = 0;
      for (Income income : client.getIncomes()) {
        if (income.getEarned()) {
          amount += income.getAmount();
        } else {
          boolean include = true;
          for (String ex : excludedIncomes) {
            if (ex.equals(income.getType())) {
              include = false;
              break;
            }
          }
          if (include) {
            amount += income.getAmount();
          }
        }
      }
      jstate.clientIncome.put(client.getId(), amount);
    }
  }

  /**
   * Calculate the size of the eligibility group centered on each individual
   * applying for CHIP.  The incomes of these individuals are also summed as
   * appropriate to arrive at the income of the group.  That is what we look
   * at in determining the means of the group around an applicant.
   * @param job
   * @param jstate
   */
  public void Calculate_Group_Size(Job job, JState jstate) {
    for (Client thisClient : job.getCase().getClients()) if (
      thisClient.getApplying()
    ) {
      int totalgroupincome = 0;
      int groupcnt = 0;
      for (Client client : job.getCase().getClients()) {
        if (thisClient == client) {
          groupcnt++;
          if (client.getPregnant()) {
            groupcnt += client.getExpectedChildren();
          }

          if (client.getAge() > 18) {
            totalgroupincome += jstate.clientIncome.get(client.getId());
          }
        }

        if (is("parent", job.getCase(), client, thisClient)) {
          groupcnt++;
          if (client.getPregnant()) {
            groupcnt += client.getExpectedChildren();
          }

          if (client.getAge() > 18) {
            totalgroupincome += jstate.clientIncome.get(client.getId());
          }
        }

        if (is("sibling", job.getCase(), client, thisClient)) {
          groupcnt++;
          if (client.getPregnant()) {
            groupcnt += client.getExpectedChildren();
          }

          if (client.getAge() > 18) {
            totalgroupincome += jstate.clientIncome.get(client.getId());
          }
        }

        if (is("child", job.getCase(), client, thisClient)) {
          groupcnt++;

          if (client.getAge() > 18) {
            totalgroupincome += jstate.clientIncome.get(client.getId());
          }
        }
      }
      jstate.clientGroupCnt.put(thisClient.getId(), groupcnt);
      jstate.clientTotalIncome.put(thisClient.getId(), totalgroupincome);
    }
  }

  /**
   * Once we have the incomes of the individuals, and the size of the group
   * around each applicant, and the income of that group, we can evaluate the
   * eligibility of each applicant.
   *
   * @param job
   * @param jstate
   */
  public void Evaluate_Chip_Eligibility(Job job, JState jstate) {
    // For all the clients whose applying == true.
    for (Client client : job.getCase().getClients()) if (client.getApplying()) {
      int incomeGroupCnt = jstate.clientGroupCnt.get(client.getId());
      int FPL = FPL_Base + FPL_PerAdditionalPerson * (incomeGroupCnt - 1);
      int FPL200 = FPL * 2;
      List<String> notes = new ArrayList<String>();
      client.setNotes(notes);
      boolean eligible = true;

      if (
        !client.getValidatedCitizenship() &&
        !client.getValidatedImmigrationStatus()
      ) {
        notes.add(
          "Ineligible: The Client is not a validated Citizen, nor a validated immigrant"
        );
        eligible = false;
      }

      if (jstate.clientTotalIncome.get(client.getId()) > FPL200) {
        notes.add(
          "Ineligible: The Client's total group income is greater than 200 percent of the FPL "
        );
        eligible = false;
      }
      if (!client.getUninsured()) {
        notes.add("Ineligible: The Applying Client must not have insurance");
        eligible = false;
      }

      if (client.getUninsured() && client.getLostInsuranceDate() != null) {
        long delta =
          job.getCurrentdate().getTime() -
          client.getLostInsuranceDate().getTime();
        long days = delta / (1000 * 60 * 60 * 24);

        if (days <= 90) {
          notes.add(
            "Ineligible: The Client must be uninsured for 90 days or more to qualify for CHIP"
          );
          eligible = false;
        }
      }

      boolean goodCounty = false;
      for (String county : coveredCounties) {
        if (job.getCase().getCounty_cd().equals(county)) {
          goodCounty = true;
          break;
        }
      }

      if (!goodCounty) {
        notes.add("Ineligible: The Client is not in a covered county");
        eligible = false;
      }

      if (client.getAge() > 18) {
        notes.add(
          "Ineligible: The Client must be 18 or younger to qualify for CHIP"
        );
        eligible = false;
      }

      if (client.getEligibleForMedicaid()) {
        notes.add(
          "Ineligible: The Client is eligible for Medicaid, so they are not eligible for CHIP"
        );
        eligible = false;
      }

      jstate.clientEligibility.put(client.getId(), eligible);
      notes.add(
        "Client Income = " + jstate.clientTotalIncome.get(client.getId())
      );
    }
  }

  /**
   * Look through the list of relationships, and answer the question, is this
   * source individual the [relationship] of the target.
   * @param relationship
   * @param thecase
   * @param source
   * @param target
   * @return
   */
  public boolean is(
    String relationship,
    Case thecase,
    Client source,
    Client target
  ) {
    for (Relationship r : thecase.getRelationships()) {
      if (
        r.getSource() == source &&
        r.getTarget() == target &&
        r.getType().equals(relationship)
      ) {
        return true;
      }
    }

    return false;
  }

  /**
   * Sum up the results and report to the app.
   * @param threadnum
   * @param app
   * @param job
   * @param jstate
   */
  public void Evaluate_Results(
    int threadnum,
    ChipApp app,
    Job job,
    JState jstate
  ) {
    if (jstate.clientEligibility.size() == 0 && app.console) {
      System.out.println("No results for job " + job.getId());
    }
    int approved = 0;
    int denied = 0;
    for (Client client : job.getCase().getClients()) if (client.getApplying()) {
      Boolean e = jstate.clientEligibility.get(client.getId());
      synchronized (app) {
        if (e == null) {
          System.out.println("Client " + client.getId() + " is null");
        } else if (e) {
          app.getApprovedClients().add(client.getId());
          approved++;
        } else {
          app.getDeniedClients().add(client.getId());
          denied++;
        }
      }
      Result r = new Result();
      r.setClient(client);
      r.setClient_id("" + client.getId());
      r.setNotes(client.getNotes());
      job.getResults().add(r);
    }

    for (Result result : job.getResults()) {
      for (String note : result.getNotes()) {
        if (note.startsWith("Ineligible:")) {
          Integer v = app.results.get(note);
          if (v == null) v = 0;
          app.results.put(note, v + 1);
        }
      }
    }

    app.update(threadnum, job.getCase().getClients().size(), approved, denied);
  }
}
