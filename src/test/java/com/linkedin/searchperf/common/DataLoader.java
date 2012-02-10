package com.linkedin.searchperf.common;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

public class DataLoader {
  public static void main(String[] args) throws Exception {
    Set<String> properties = new LinkedHashSet<String>();
    for (Object strObj : FileUtils.readLines(new File("data/campaigns.json"))) {
      String str = (String) strObj;
      if (!str.contains("{")) {
        continue;
      }
      //System.out.println(str);
      JSONObject campaign = new JSONObject(str);
      properties.addAll(Arrays.asList(JSONObject.getNames(campaign)));
    }
    System.out.println(properties);
    FileUtils.deleteQuietly(new File("data/modifiedCampaigns.json"));
    FileWriter fileWriter = new FileWriter("data/modifiedCampaigns.json", true);
    for (Object strObj : FileUtils.readLines(new File("data/campaigns.json"))) {
      String str = (String) strObj;
      if (!str.contains("{")) {
        continue;
      }
      JSONObject campaign = new JSONObject(str);
      for (String property : properties) {
        if (campaign.opt(property) == null || "_null".equals(campaign.opt(property))) {
          campaign.put(property, "null");
        }
      }
      fileWriter.append(campaign.toString() + "\n");
    }
    fileWriter.close();
  }
}
