package com.linkedin.searchperf.common.query;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.linkedin.searchperf.common.launcher.GenericPerformanceLauncher;
import com.linkedin.searchperf.common.launcher.GenericPerformanceLauncher.PerformanceResult;
import com.linkedin.searchperf.query.sensei.SenseiQueryProducer;
import com.linkedin.searchperf.runner.PerfRunnerConfig;
import com.linkedin.searchperf.runner.impl.SenseiConcurrentRunner;
import com.senseidb.search.client.SenseiServiceProxy;
import com.senseidb.search.client.req.SenseiClientRequest;
import com.yammer.metrics.reporting.ConsoleReporter;

public class BaSenseiPerformanceLauncher {
  public static AtomicLong counter = new AtomicLong();
  public static void main(String[] args) throws Exception {
    if (args == null || args.length == 0) {
      args = new String[] {"configs/test-sensei.properties"};
    }
    PropertiesConfiguration config = GenericPerformanceLauncher.extractPropertyConfig(args);
    List<Integer> threads = GenericPerformanceLauncher.extractNumberOfThreads(config);
    PerfRunnerConfig senseiRunnerConfig = PerfRunnerConfig.build(config);
    SenseiQueryProducer queryProducer = new SenseiQueryProducer() {
      @Override
      public synchronized com.senseidb.search.client.req.SenseiClientRequest createQuery(boolean includeFacets, int simpleSelections, int rangeSelections,
          int pathSelections) {
        // TODO Auto-generated method stub
        return SenseiClientRequest.builder().build();
      }
    };
    final int[] vieweeId = new int[] {1732168, 2137965,
        925742,
        1799411,
        384905
        };
    SenseiServiceProxy senseiServiceProxy = new SenseiServiceProxy(senseiRunnerConfig.getUrl()) {
    
    @Override
    public String sendPostRaw(String urlStr, String requestStr) {
       final int index =  (int)counter.incrementAndGet() %  vieweeId.length;
       String bql =  "select count(*) where vieweeId = " + vieweeId[index] + " group by industryCode";
       JSONObject bqlJson;
      try {
        bqlJson = new JSONObject().put("bql", bql);
        String output = super.sendPostRaw(getSearchUrl(), bqlJson.toString());
        //System.out.println("!!!" + output);
        return output;
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }      
      
    };
    List<PerformanceResult> ret = new ArrayList<PerformanceResult>();
    FileUtils.deleteQuietly(new File(GenericPerformanceLauncher.RESULT_FILE));
    File resultFile = new File(GenericPerformanceLauncher.RESULT_FILE);
    FileWriter fileWriter = new FileWriter(resultFile, true);
    fileWriter.append(PerformanceResult.getMetadata() + "\n");
    System.gc();
    System.out.println(PerformanceResult.getMetadata());
    for (int i : threads) {
      senseiRunnerConfig.setNumThreads(i);
      SenseiConcurrentRunner senseiConcurrentRunner = new SenseiConcurrentRunner();
      PerformanceResult result = run(senseiRunnerConfig, queryProducer, senseiServiceProxy, senseiConcurrentRunner);
      fileWriter.append(result.toString() + "\n");
      System.out.println(result);
    }
    fileWriter.close();
    System.exit(0);
  }

  private static PerformanceResult run(PerfRunnerConfig senseiRunnerConfig, SenseiQueryProducer queryProducer,
      SenseiServiceProxy senseiServiceProxy, SenseiConcurrentRunner senseiConcurrentRunner)
      throws UnsupportedEncodingException {
    senseiConcurrentRunner.init(senseiRunnerConfig, senseiServiceProxy, queryProducer);
    senseiConcurrentRunner.run();
    senseiConcurrentRunner.getExecutors().shutdown();
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(byteArrayOutputStream);
    ConsoleReporter consoleReporter = new ConsoleReporter(printStream);
    consoleReporter.run();
    consoleReporter.shutdown();
    String output = byteArrayOutputStream.toString("UTF-8");
    //System.out.println(((List)senseiConcurrentRunner.createRequest()).get(0).toString());
    PerformanceResult performanceResult = GenericPerformanceLauncher.process(output, senseiRunnerConfig);
    return performanceResult;
  } 
}
