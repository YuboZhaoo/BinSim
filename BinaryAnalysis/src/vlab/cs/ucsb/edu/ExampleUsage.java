package vlab.cs.ucsb.edu;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import generic.stl.Pair;
import vlab.cs.ucsb.edu.DriverProxy;
import vlab.cs.ucsb.edu.DriverProxy.Option;
import vlab.cs.ucsb.edu.ModelCounter;

public class ExampleUsage {

  public static void main(String[] args) {

    DriverProxy abcDriver = new DriverProxy();
    
    abcDriver.setOption(Option.ENABLE_IMPLICATIONS);
    abcDriver.setOption(Option.USE_SIGNED_INTEGERS);
    
    String constraint = "(set-logic QF_S)\n"
        + "(declare-fun var_abc () String)\n"
        + "(assert (not (= var_abc \"abc\")))\n"
        + "(check-sat)";
    
    boolean result = abcDriver.isSatisfiable(constraint);
    
    if (result) {
      System.out.println("Satisfiable");
      long bound = 30;
      BigInteger count = abcDriver.countVariable("var_abc",bound);
      byte[] func = abcDriver.getModelCounterForVariable("var_abc");
      if (count != null) {
        System.out.println("Number of solutions within bound: " + bound + " is " + count.toString());
      } else {
        System.out.println("An error occured during counting, please contact vlab@cs.ucsb.edu");
      }
      
      BigInteger count2 = abcDriver.countVariable("var_abc", bound, func);
      System.out.println("cache count: " + count2);
      
//      abcDriver.printResultAutomaton();
      
      Map<String, String> results = abcDriver.getSatisfyingExamples();
      for (Entry<String, String> var_result : results.entrySet()) {
        System.out.println(var_result.getKey() + " : \"" + var_result.getValue() + "\"");
      }
    } else {
      System.out.println("Unsatisfiable");
    }
    
//    constraint = "(declare-fun x () Int)\n"
//            + "(declare-fun y () Int)\n"
//            + "(assert (= x (* 2 y)))\n"
//            + "(assert (= x (* 2 y)))\n"
//            + "(assert (> x 0))\n"
//            + "(check-sat)";
//    constraint = "(declare-fun x () Int)\n"
//            + "(declare-fun y () Int)\n"
//            + "(assert (> x -3))\n"
//            + "(assert (< x 3))\n"
//            + "(assert (> y 0))\n"
//            + "(assert (< y 6))\n"
//            + "(assert (= x y))\n"
//            + "(check-sat)";
    constraint = "(declare-fun x () Int)\n"
            + "(declare-fun y () Int)\n"
            + "(assert (> x (- 5)))\n"
            + "(assert (< x 0))\n"
            + "(assert (> y (- 3)))\n"
            + "(assert (< y 0))\n"
            + "(assert (= x y))\n";
//            + "(check-sat)";
//    String domain_constraint = "(declare-fun x () Int)\n"
//            + "(declare-fun y () Int)\n"
//            + "(assert (> x (- 5)))\n"
//            + "(assert (< x 0))\n"
//            + "(assert (> y (- 3)))\n"
//            + "(assert (< y 0))\n"
//            + "(check-sat)";
    System.out.println("\nstart");
    ModelCounter modelCounter = new ModelCounter(31, "abc.linear_integer_arithmetic");

    result = abcDriver.isSatisfiable(constraint);
    List<Pair<String,String>> model_counting_vars = new ArrayList<Pair<String,String>>();
    model_counting_vars.add(new Pair<>("x","int"));
    model_counting_vars.add(new Pair<>("y","int"));
//    BigDecimal dom_count = modelCounter.getModelCount(domain_constraint, model_counting_vars, false);
//    System.out.println("Branch domain count:" + dom_count);
    BigDecimal cond_count = modelCounter.getModelCount(constraint, model_counting_vars, false);
    System.out.println("Branch condition count:" + cond_count);
//    String func = abcDriver.getModelCounterForInts();
//    System.out.println("func: " + func);
    if (result) {
      System.out.println("Satisfiable");
          
      Map<String, String> results = abcDriver.getSatisfyingExamples();
      for (Entry<String, String> var_result : results.entrySet()) {
        System.out.println(var_result.getKey() + " : \"" + var_result.getValue() + "\"");
      }
    } else {
      System.out.println("Unsatisfiable");
    }
    
    abcDriver.dispose(); // release resources
  }
}
