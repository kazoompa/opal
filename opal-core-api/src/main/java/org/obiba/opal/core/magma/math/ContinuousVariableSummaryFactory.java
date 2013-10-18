package org.obiba.opal.core.magma.math;

import java.util.List;

import org.obiba.magma.ValueTable;
import org.obiba.magma.Variable;

import static org.obiba.opal.core.magma.math.ContinuousVariableSummary.Distribution;

public class ContinuousVariableSummaryFactory extends AbstractVariableSummaryFactory {

  private Distribution distribution;

  private List<Double> percentiles;

  private int intervals;

  private Integer offset;

  private Integer limit;

  @Override
  public String getCacheKey() {
    return getCacheKey(getVariable(), getTable(), distribution, percentiles, intervals, offset, limit);
  }

  public static String getCacheKey(Variable variable, ValueTable table, Distribution distribution,
      List<Double> percentiles, int intervals, Integer offset, Integer limit) {
    String key = variable.getVariableReference(table) + "." + distribution + "." + intervals;
    if(percentiles != null) key += "." + percentiles.hashCode();
    if(offset != null) key += "." + offset;
    if(limit != null) key += "." + limit;
    return key;
  }

  public ContinuousVariableSummary getSummary() {
    return new ContinuousVariableSummary.Builder(getVariable(), distribution) //
        .defaultPercentiles(percentiles) //
        .intervals(intervals) //
        .filter(offset, limit) //
        .addTable(getTable(), getValueSource()) //
        .build();
  }

  public Distribution getDistribution() {
    return distribution;
  }

  public void setDistribution(Distribution distribution) {
    this.distribution = distribution;
  }

  public List<Double> getPercentiles() {
    return percentiles;
  }

  public void setPercentiles(List<Double> percentiles) {
    this.percentiles = percentiles;
  }

  public int getIntervals() {
    return intervals;
  }

  public void setIntervals(int intervals) {
    this.intervals = intervals;
  }

  public Integer getOffset() {
    return offset;
  }

  public void setOffset(Integer offset) {
    this.offset = offset;
  }

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  @SuppressWarnings("ParameterHidesMemberVariable")
  public static class Builder
      extends AbstractVariableSummaryFactory.Builder<ContinuousVariableSummaryFactory, Builder> {

    @Override
    protected ContinuousVariableSummaryFactory createFactory() {
      return new ContinuousVariableSummaryFactory();
    }

    @Override
    protected Builder createBuilder() {
      return this;
    }

    public Builder distribution(Distribution distribution) {
      factory.distribution = distribution;
      return this;
    }

    public Builder percentiles(List<Double> percentiles) {
      factory.percentiles = percentiles;
      return this;
    }

    public Builder intervals(int intervals) {
      factory.intervals = intervals;
      return this;
    }

    public Builder offset(Integer offset) {
      factory.offset = offset;
      return this;
    }

    public Builder limit(Integer limit) {
      factory.limit = limit;
      return this;
    }

    public ContinuousVariableSummaryFactory build() {
      return factory;
    }
  }

}