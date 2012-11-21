/*******************************************************************************
 * Copyright (c) 2011 OBiBa. All rights reserved.
 *  
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.r;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.obiba.magma.Category;
import org.obiba.magma.Value;
import org.obiba.magma.ValueSequence;
import org.obiba.magma.ValueType;
import org.obiba.magma.Variable;
import org.obiba.magma.VariableEntity;
import org.obiba.magma.VariableValueSource;
import org.obiba.magma.type.BinaryType;
import org.obiba.magma.type.BooleanType;
import org.obiba.magma.type.DateTimeType;
import org.obiba.magma.type.DateType;
import org.obiba.magma.type.DecimalType;
import org.obiba.magma.type.IntegerType;
import org.obiba.magma.type.LocaleType;
import org.obiba.magma.type.TextType;
import org.obiba.opal.core.domain.VariableNature;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPFactor;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPRaw;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A utility class for mapping {@code ValueType} to R {@code REXP}.
 */
public enum VectorType {

  booleans(BooleanType.get()) {
    @Override
    protected REXP asContinuousVector(Variable variable, int size, Iterable<Value> values) {
      byte booleans[] = new byte[size];
      int i = 0;
      for(Value value : values) {
        if(value.isNull()) {
          booleans[i++] = REXPLogical.NA;
        } else if((Boolean) value.getValue()) {
          booleans[i++] = REXPLogical.TRUE;
        } else {
          booleans[i++] = REXPLogical.FALSE;
        }
      }
      return new REXPLogical(booleans);
    }

  },

  ints(IntegerType.get()) {
    @Override
    protected REXP asContinuousVector(Variable variable, int size, Iterable<Value> values) {
      List<Long> missings = getMissings(variable);
      int ints[] = new int[size];
      int i = 0;
      for(Value value : values) {
        if(value.isNull()) {
          ints[i++] = REXPInteger.NA;
        } else {
          Long val = (Long) value.getValue();
          // OPAL-1536 do not push missings
          if(missings.contains(val) == false) {
            ints[i++] = val.intValue();
          }
        }
      }
      return new REXPInteger(ints);
    }

    private List<Long> getMissings(Variable variable) {
      List<Long> missings = Lists.newArrayList();
      if(variable.hasCategories()) {
        for(Category cat : variable.getCategories()) {
          if(cat.isMissing()) {
            try {
              missings.add(Long.parseLong(cat.getName()));
            } catch(NumberFormatException e) {
              // ignore
            }
          }
        }
      }
      return missings;
    }

  },

  doubles(DecimalType.get()) {
    @Override
    protected REXP asContinuousVector(Variable variable, int size, Iterable<Value> values) {
      List<Double> missings = getMissings(variable);
      double doubles[] = new double[size];
      int i = 0;
      for(Value value : values) {
        if(value.isNull()) {
          doubles[i++] = REXPDouble.NA;
        } else {
          Double val = (Double) value.getValue();
          // OPAL-1536 do not push missings
          if(missings.contains(val) == false) {
            doubles[i++] = val.doubleValue();
          }
        }
      }
      return new REXPDouble(doubles);
    }

    private List<Double> getMissings(Variable variable) {
      List<Double> missings = Lists.newArrayList();
      if(variable.hasCategories()) {
        for(Category cat : variable.getCategories()) {
          if(cat.isMissing()) {
            try {
              missings.add(Double.parseDouble(cat.getName()));
            } catch(NumberFormatException e) {
              // ignore
            }
          }
        }
      }
      return missings;
    }
  },

  datetimes(DateTimeType.get()),

  dates(DateType.get()),

  locales(LocaleType.get()),

  strings(TextType.get()),

  binaries(BinaryType.get()) {
    @Override
    protected REXP asContinuousVector(Variable variable, int size, Iterable<Value> values) {
      REXPRaw raws[] = new REXPRaw[size];
      int i = 0;
      for(Value value : values) {
        raws[i++] = new REXPRaw((byte[]) value.getValue());
      }
      return new REXPList(new RList(raws));
    }
  };

  private ValueType type;

  private VectorType(ValueType type) {
    this.type = type;
  }

  public static VectorType forValueType(ValueType type) {
    for(VectorType v : VectorType.values()) {
      if(v.type == type) {
        return v;
      }
    }
    throw new MagmaRRuntimeException("No VectorType for ValueType " + type);
  }

  /**
   * Build R vector.
   * @param vvs
   * @param entities
   * @return
   */
  public REXP asVector(VariableValueSource vvs, SortedSet<VariableEntity> entities) {
    Variable variable = vvs.getVariable();
    int size = entities.size();
    Iterable<Value> values = vvs.asVectorSource().getValues(entities);
    if(variable.isRepeatable()) {
      return asValueSequencesVector(variable, size, values);
    } else
      return asValuesVector(variable, size, values);
  }

  /**
   * Build a type specific R vector. Default behaviour is to check the variable if it defines categories
   * 
   * @param size
   * @param values
   * @return
   */
  protected REXP asValuesVector(Variable variable, int size, Iterable<Value> values) {
    switch(VariableNature.getNature(variable)) {
    case CATEGORICAL:
      return asFactors(variable, size, values);
    }
    return asContinuousVector(variable, size, values);
  }

  protected REXP asContinuousVector(Variable variable, int size, Iterable<Value> values) {
    return asStringValuesVector(variable, size, values);
  }

  /**
   * Build a list of R vectors.
   * @param size
   * @param values
   * @return
   */
  private REXP asValueSequencesVector(Variable variable, int size, Iterable<Value> values) {
    REXP sequences[] = new REXP[size];
    int i = 0;
    for(Value value : values) {
      ValueSequence seq = value.asSequence();
      sequences[i++] = seq.isNull() ? null : asValuesVector(variable, seq.getSize(), seq.getValue());
    }
    return new REXPList(new RList(sequences));
  }

  protected REXP asFactors(Variable variable, int size, Iterable<Value> values) {
    String[] levels = new String[variable.getCategories().size()];
    Map<String, Integer> codes = Maps.newHashMap();
    // REXPFactor is one-based. That is, the ID the first level, is 1.
    int i = 1;
    for(Category c : variable.getCategories()) {
      levels[i - 1] = c.getName();
      codes.put(c.getName(), i++);
    }

    int ints[] = new int[size];

    i = 0;
    for(Value value : values) {
      if(i >= size) {
        throw new IllegalStateException("unexpected value");
      }

      String str = value.toString();
      Integer code = codes.get(str);
      ints[i] = code != null ? code : REXPInteger.NA;
      i++;
    }
    return new REXPFactor(ints, levels);
  }

  /**
   * Build a R vector of strings.
   * @param size
   * @param values
   * @return
   */
  protected REXP asStringValuesVector(Variable variable, int size, Iterable<Value> values) {
    String strings[] = new String[size];
    int i = 0;
    for(Value value : values) {
      if(i >= size) {
        throw new IllegalStateException("unexpected value");
      }

      String str = value.toString();
      strings[i] = (str != null && str.length() > 0) ? str : null;
      i++;
    }

    return new REXPString(strings);
  }
}
