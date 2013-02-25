package com.linkedin.searchperf.query.sensei;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.linkedin.searchperf.runner.PerfRunnerConfig;
import com.senseidb.search.client.req.Facet;
import com.senseidb.search.client.req.Facet.Builder;
import com.senseidb.search.client.req.Selection;
import com.senseidb.search.client.req.SenseiClientRequest;
import com.senseidb.search.client.req.Terms;

public class SenseiQueryProducer {
  private DataCollector collector;
  private SchemaParser parser;
  private SchemaMetadata queryMetadata;
  private Map<String, Set<Object>> fieldValues;
  private SelectionGenerator selectionGenerator;

  public SenseiQueryProducer() {

  }

  public synchronized void init(InputStream schema, InputStream data) {
    try {
      collector = new DataCollector();
      parser = new SchemaParser();
      queryMetadata = parser.parse(schema);
      fieldValues = collector.collectValues(data);
      for (String facet : queryMetadata.getFacetsPerType().get(FacetType.multi)) {
        collector.postProcessMultiValues(facet, fieldValues);
      }
      selectionGenerator = new SelectionGenerator(fieldValues, queryMetadata);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    } finally {
      IOUtils.closeQuietly(schema);
      IOUtils.closeQuietly(data);
    }
  }

  public synchronized SenseiClientRequest createQuery(boolean includeFacets, int simpleSelections, int rangeSelections, int pathSelections) {
    List<Selection> simpleSelectionsList = new ArrayList<Selection>();
    List<Selection> rangeSelectionsList = new ArrayList<Selection>();
    List<Selection> pathSelectionsList = new ArrayList<Selection>();
    com.senseidb.search.client.req.SenseiClientRequest.Builder builder = SenseiClientRequest.builder();
    Facet builtFacet = Facet.builder().expand(true).max(100000).minHit(0).orderByHits().build();
    for (FacetType facetType : queryMetadata.getFacetsPerType().keySet()) {
      for (String facetName : queryMetadata.getFacetsPerType().get(facetType)) {
        switch (facetType) {
        case simple:
          simpleSelectionsList.add(selectionGenerator.createSimpleSelection(facetName));
          break;
        case multi:
          simpleSelectionsList.add(selectionGenerator.createSimpleSelection(facetName));
          break;
        case range:
          rangeSelectionsList.add(selectionGenerator.createRangeSelection(facetName));
          break;
        case path:
          pathSelectionsList.add(selectionGenerator.createPathSelection(facetName));
          break;
        default:
          throw new IllegalStateException("Unknown enum value found." + facetType);
        }
      }
    }
    adjustQuantityAndShuffle(simpleSelectionsList, simpleSelections);
    adjustQuantityAndShuffle(rangeSelectionsList, rangeSelections);
    adjustQuantityAndShuffle(pathSelectionsList, pathSelections);
    List<Selection> selections = simpleSelectionsList;
    selections.addAll(rangeSelectionsList);
    selections.addAll(pathSelectionsList);
    for (Selection selection : selections) {
      builder.addSelection(selection);
      if (includeFacets) {
       if (selection instanceof Terms)
        builder.addFacet(selection.getField(), builtFacet);
      }
    }
    SenseiClientRequest clientRequest = builder.build();
    return clientRequest;
  }

  private void adjustQuantityAndShuffle(List<?> list, int count) {
    if (count < list.size()) {
      Collections.shuffle(list);
      int size = list.size();
      for (int i = count; i < size; i++) {
        list.remove(0);
      }
    }

  }

  public DataCollector getCollector() {
    return collector;
  }

  public void setCollector(DataCollector collector) {
    this.collector = collector;
  }

  public SchemaParser getParser() {
    return parser;
  }

  public void setParser(SchemaParser parser) {
    this.parser = parser;
  }

  public SchemaMetadata getQueryMetadata() {
    return queryMetadata;
  }

  public void setQueryMetadata(SchemaMetadata queryMetadata) {
    this.queryMetadata = queryMetadata;
  }

  public Map<String, Set<Object>> getFieldValues() {
    return fieldValues;
  }

  public void setFieldValues(Map<String, Set<Object>> fieldValues) {
    this.fieldValues = fieldValues;
  }

  public SelectionGenerator getFilterGenerator() {
    return selectionGenerator;
  }

  public void setFilterGenerator(SelectionGenerator filterGenerator) {
    this.selectionGenerator = filterGenerator;
  }

  @Override
  public SenseiQueryProducer clone() {
    SenseiQueryProducer ret = new SenseiQueryProducer();
    ret.collector = collector;
    ret.fieldValues = fieldValues;
    ret.queryMetadata = queryMetadata;
    ret.parser = parser;
    ret.selectionGenerator = new SelectionGenerator(fieldValues, queryMetadata);
    return ret;
  }
  public static SenseiQueryProducer build(PerfRunnerConfig perfRunnerConfig) {
    SenseiQueryProducer queryProducer = new SenseiQueryProducer();
    queryProducer.init(PerfRunnerConfig.getResource(perfRunnerConfig.getSchemaPath()),
        PerfRunnerConfig.getResource(perfRunnerConfig.getDataFilePath()));
    return queryProducer;
  }
  
}
