package com.linkedin.searchperf.query.sensei;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.senseidb.search.client.json.req.Selection;
import com.senseidb.search.client.json.req.SenseiClientRequest;
import com.senseidb.search.client.json.req.SenseiClientRequest.Builder;

public class CampaignQueryProducer extends SenseiQueryProducer {
  private static final String _NULL = "null";
  private List<String> jsonRequests = new ArrayList<String>();
  int i = 0;

  @Override
  public synchronized SenseiClientRequest createQuery(boolean includeFacets, int simpleSelections, int rangeSelections,
      int pathSelections) {
    if (jsonRequests.size() == 0) {
      throw new IllegalStateException("There are no requests");
    }
    if (i >= jsonRequests.size()) {
      i = 0;
    }
    return createRequest(jsonRequests.get(i++));

  }

  private SenseiClientRequest createRequest(String string) {
    try {
      JSONObject json = new JSONObject(string);
      Builder builder = SenseiClientRequest.builder();
      builder.paging(100, 0);
      for (String key : JSONObject.getNames(json)) {
        if (key.equals("id")) {
          continue;
        }
        if (!key.equals("reg")) {
          List<String> values = new ArrayList<String>(Arrays.asList(json.getString(key).split(",")));
          if (!values.contains(_NULL)) values.add(_NULL);
          if (values.size() > 1) {
            builder.addSelection(Selection.terms(key, values.toArray(new String[values.size()])));
          }
        } else {
          String geo = json.getString("reg");
          List<String> partialGeos = new ArrayList<String>();
          int index = -1;
          do  {
            index = geo.indexOf(".", index + 1);
            if (index > 0) {
              partialGeos.add(geo.substring(0, index));
            } else {
              partialGeos.add(geo);
              partialGeos.add("null");
            }
            
          } while (index >= 0);
         // builder.addSelection(Selection.terms("reg", partialGeos.toArray(new String[partialGeos.size()])));
        } 
      }
      return builder.build();
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
    
  }

  @Override
  public synchronized void init(InputStream schema, InputStream data) {
    try {
      for (String line : (Iterable<String>) IOUtils.readLines(data)) {
        if (line.contains("{")) {
          jsonRequests.add(line);
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
