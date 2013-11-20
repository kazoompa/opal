/*******************************************************************************
 * Copyright (c) 2012 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.magma;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.obiba.magma.Timestamps;
import org.obiba.magma.Value;
import org.obiba.magma.ValueSet;
import org.obiba.magma.Variable;
import org.obiba.magma.VariableEntity;
import org.obiba.magma.VariableValueSource;
import org.obiba.magma.VectorSource;
import org.obiba.opal.web.TimestampedResponses;
import org.obiba.opal.web.model.Magma.ValueSetsDto;
import org.obiba.opal.web.model.Magma.ValueSetsDto.ValueSetDto;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Transactional
public class ValueSetsResourceImpl extends AbstractValueTableResource implements ValueSetsResource {

  @Nullable
  private VariableValueSource variableValueSource;

  @Override
  public void setVariableValueSource(@Nullable VariableValueSource variableValueSource) {
    this.variableValueSource = variableValueSource;
  }

  @Override
  public Response getValueSets(UriInfo uriInfo, String select, int offset, int limit, Boolean filterBinary) {
    // filter entities
    Iterable<VariableEntity> variableEntities = filterEntities(offset, limit);
    ValueSetsDto vs = variableValueSource == null
        ? getValueSetsDto(uriInfo, select, variableEntities, filterBinary)
        : getValueSetsDto(uriInfo, variableEntities, filterBinary);
    return TimestampedResponses.ok(getValueTable(), vs).build();
  }

  @Override
  public Response getValueSetsTimestamps(int offset, int limit) {

    // filter entities
    Iterable<VariableEntity> variableEntities = filterEntities(offset, limit);

    ValueSetsDto.Builder builder = ValueSetsDto.newBuilder().setEntityType(getValueTable().getEntityType());
    builder.addAllValueSets(Iterables.transform(variableEntities, new Function<VariableEntity, ValueSetDto>() {
      @Override
      public ValueSetDto apply(VariableEntity fromEntity) {
        Timestamps timestamps = getValueTable().getValueSetTimestamps(fromEntity);
        return ValueSetsDto.ValueSetDto.newBuilder().setIdentifier(fromEntity.getIdentifier())
            .setTimestamps(Dtos.asDto(timestamps)).build();
      }
    }));

    return TimestampedResponses.ok(getValueTable(), builder.build()).build();
  }

  private ValueSetsDto getValueSetsDto(UriInfo uriInfo, String select, Iterable<VariableEntity> variableEntities,
      boolean filterBinary) {
    Iterable<Variable> variables = filterVariables(select, 0, null);

    ValueSetsDto.Builder builder = ValueSetsDto.newBuilder().setEntityType(getValueTable().getEntityType());

    builder.addAllVariables(Iterables.transform(variables, new Function<Variable, String>() {

      @Override
      public String apply(Variable from) {
        return from.getName();
      }

    }));

    ImmutableList.Builder<ValueSetDto> valueSetDtoBuilder = ImmutableList.builder();
    for(ValueSetDto dto : Iterables
        .transform(variableEntities, new VariableEntityValueSetDtoFunction(variables, uriInfo, filterBinary))) {
      valueSetDtoBuilder.add(dto);
    }

    builder.addAllValueSets(valueSetDtoBuilder.build());

    return builder.build();
  }

  @SuppressWarnings("ConstantConditions")
  private ValueSetsDto getValueSetsDto(final UriInfo uriInfo, Iterable<VariableEntity> variableEntities,
      final boolean filterBinary) {
    final Variable variable = variableValueSource.getVariable();
    ValueSetsDto.Builder builder = ValueSetsDto.newBuilder().setEntityType(variable.getEntityType())
        .addVariables(variable.getName());

    VectorSource vector = variableValueSource.asVectorSource();
    if(vector == null) {
      builder.addAllValueSets(Iterables.transform(variableEntities, new Function<VariableEntity, ValueSetDto>() {
        @Override
        public ValueSetDto apply(VariableEntity fromEntity) {
          ValueSet valueSet = getValueTable().getValueSet(fromEntity);
          Value value = variableValueSource.getValue(valueSet);
          return getValueSetDto(uriInfo, fromEntity, variable, filterBinary, value);
        }
      }));
    } else {
      addValueSetDtosFromVectorSource(uriInfo, variableEntities, variable, filterBinary, vector, builder);
    }

    return builder.build();
  }

  private void addValueSetDtosFromVectorSource(UriInfo uriInfo, Iterable<VariableEntity> variableEntities,
      Variable variable, boolean filterBinary, VectorSource vector, ValueSetsDto.Builder builder) {
    ImmutableSortedSet<VariableEntity> sortedEntities = ImmutableSortedSet.<VariableEntity>naturalOrder()
        .addAll(variableEntities).build();
    Iterable<Value> values = vector.getValues(sortedEntities);

    HashMap<VariableEntity, Value> results = new LinkedHashMap<VariableEntity, Value>();
    Iterator<VariableEntity> entitiesIterator = sortedEntities.iterator();
    for(Value value : values) {
      VariableEntity entity = entitiesIterator.next();
      results.put(entity, value);
    }

    for(VariableEntity entity : variableEntities) {
      builder.addValueSets(getValueSetDto(uriInfo, entity, variable, filterBinary, results.get(entity)));
    }
  }

  private ValueSetDto getValueSetDto(UriInfo uriInfo, VariableEntity fromEntity, Variable variable,
      boolean filterBinary, Value value) {
    String link = uriInfo.getPath().replace("valueSets",
        "valueSet/entity/" + fromEntity.getIdentifier() + "/variable/" + variable.getName() + "/value");
    return ValueSetsDto.ValueSetDto.newBuilder().setIdentifier(fromEntity.getIdentifier())
        .addValues(Dtos.asDto(link, value, filterBinary)).build();
  }

  private class VariableEntityValueSetDtoFunction implements Function<VariableEntity, ValueSetDto> {

    private final Iterable<Variable> variables;

    private final UriInfo uriInfo;

    private final boolean filterBinary;

    private VariableEntityValueSetDtoFunction(Iterable<Variable> variables, UriInfo uriInfo, boolean filterBinary) {
      this.variables = variables;
      this.uriInfo = uriInfo;
      this.filterBinary = filterBinary;
    }

    @Override
    public ValueSetDto apply(final VariableEntity fromEntity) {
      final ValueSet valueSet = getValueTable().getValueSet(fromEntity);

      Iterable<ValueSetsDto.ValueDto> valueDtosIter = Iterables
          .transform(variables, new Function<Variable, ValueSetsDto.ValueDto>() {

            @Override
            public ValueSetsDto.ValueDto apply(Variable fromVariable) {
              String link = uriInfo.getPath().replace("valueSets",
                  "valueSet/entity/" + fromEntity.getIdentifier() + "/variable/" + fromVariable.getName() +
                      "/value");
              Value value = getValueTable().getVariableValueSource(fromVariable.getName()).getValue(valueSet);
              return Dtos.asDto(link, value, filterBinary).build();
            }
          });

      // Do not add iterable directly otherwise the values will be fetched as many times it is iterated
      // (i.e. 2 times, see AbstractMessageLite.addAll()).
      ImmutableList.Builder<ValueSetsDto.ValueDto> valueDtos = ImmutableList.builder();
      for(ValueSetsDto.ValueDto dto : valueDtosIter) {
        valueDtos.add(dto);
      }

      return Dtos.asDto(valueSet).addAllValues(valueDtos.build()) //
          .setTimestamps(Dtos.asDto(valueSet.getTimestamps())).build();
    }
  }
}