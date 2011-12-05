/*******************************************************************************
 * Copyright (c) 2011 OBiBa. All rights reserved.
 *  
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.wizard.derive.presenter;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.obiba.opal.web.gwt.app.client.widgets.presenter.SummaryTabPresenter;
import org.obiba.opal.web.gwt.app.client.wizard.derive.util.Variables;
import org.obiba.opal.web.gwt.app.client.wizard.derive.util.Variables.ValueType;
import org.obiba.opal.web.gwt.rest.client.ResourceCallback;
import org.obiba.opal.web.gwt.rest.client.ResourceRequestBuilder;
import org.obiba.opal.web.gwt.rest.client.ResourceRequestBuilderFactory;
import org.obiba.opal.web.model.client.magma.CategoryDto;
import org.obiba.opal.web.model.client.magma.TableDto;
import org.obiba.opal.web.model.client.magma.ValueDto;
import org.obiba.opal.web.model.client.magma.VariableDto;
import org.obiba.opal.web.model.client.math.SummaryStatisticsDto;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Response;
import com.google.inject.Inject;

/**
 *
 */
public class ScriptEvaluationPresenter extends WidgetPresenter<ScriptEvaluationPresenter.Display> {

  public static final int PAGE_SIZE = 20;

  @Inject
  private SummaryTabPresenter summaryTabPresenter;

  private VariableDto variable;

  private String script;

  private String valueType;

  private TableDto table;

  private int currentOffset;

  private boolean repeatable;

  //
  // Constructors
  //

  @Inject
  public ScriptEvaluationPresenter(final Display display, final EventBus eventBus) {
    super(display, eventBus);
  }

  public void setTable(TableDto table) {
    this.table = table;
  }

  /**
   * Set the variable to be evaluated. Value type and script are extracted from the variable dto.
   * @param variable
   */
  public void setVariable(VariableDto variable) {
    this.variable = variable;
    this.valueType = variable.getValueType();
    this.repeatable = variable.getIsRepeatable();
    this.script = Variables.getScript(variable);
  }

  private void populateValues(final int offset) {
    getDisplay().setValueType(valueType);
    getDisplay().setScript(script);
    getDisplay().populateValues(null);
    currentOffset = offset;
    StringBuilder link = new StringBuilder(table.getLink())//
    .append("/variable/_transient/values?limit=").append(PAGE_SIZE)//
    .append("&offset=").append(offset).append("&");
    appendVariableLimitArguments(link);
    ResourceRequestBuilder<JsArray<ValueDto>> requestBuilder = ResourceRequestBuilderFactory.<JsArray<ValueDto>> newBuilder() //
    .forResource(link.toString()).post().withFormBody("script", script) //
    .withCallback(new ResourceCallback<JsArray<ValueDto>>() {

      @Override
      public void onResource(Response response, JsArray<ValueDto> resource) {
        int high = offset + PAGE_SIZE;
        if(resource != null && resource.length() < high) {
          high = offset + resource.length();
        }
        getDisplay().setPageLimits(offset + 1, high, table.getValueSetCount());
        getDisplay().populateValues(resource);
      }

    });
    requestBuilder.send();
  }

  private void requestSummary() {
    StringBuilder link = new StringBuilder(table.getLink()).append("/variable/_transient/summary?");
    appendVariableSummaryArguments(link);

    ResourceRequestBuilder<SummaryStatisticsDto> requestBuilder = ResourceRequestBuilderFactory.<SummaryStatisticsDto> newBuilder()//
    .forResource(link.toString()).withFormBody("script", script).post();

    if(variable != null) {
      JsArray<CategoryDto> cats = variable.getCategoriesArray();
      if(cats != null) {
        for(int i = 0; i < cats.length(); i++) {
          requestBuilder.withFormBody("category", cats.get(i).getName());
        }
      }
    }

    summaryTabPresenter.setRequestBuilder(requestBuilder);
    summaryTabPresenter.forgetSummary();
    summaryTabPresenter.refreshDisplay();
  }

  private void appendVariableSummaryArguments(StringBuilder link) {
    appendVariableLimitArguments(link);

    if(ValueType.TEXT.is(variable.getValueType()) && Variables.allCategoriesMissing(variable)) {
      link.append("&nature=categorical")//
      .append("&distinct=true");
    }
  }

  private void appendVariableLimitArguments(StringBuilder link) {
    link.append("valueType=" + valueType) //
    .append("&repeatable=" + repeatable); //
  }

  //
  // WidgetPresenter Methods
  //

  @Override
  public void refreshDisplay() {
    requestSummary();
    populateValues(0);
  }

  @Override
  public void revealDisplay() {
  }

  @Override
  protected void onBind() {
    summaryTabPresenter.bind();
    getDisplay().setSummaryTabWidget(summaryTabPresenter.getDisplay());
    addEventHandlers();
  }

  @Override
  protected void onUnbind() {
    summaryTabPresenter.unbind();
  }

  @Override
  public Place getPlace() {
    return null;
  }

  @Override
  protected void onPlaceRequest(PlaceRequest request) {
  }

  protected void addEventHandlers() {
    super.registerHandler(getDisplay().addNextPageClickHandler(new NextPageClickHandler()));
    super.registerHandler(getDisplay().addPreviousPageClickHandler(new PreviousPageClickHandler()));
  }

  //
  // Inner classes and Interfaces
  //

  public class PreviousPageClickHandler implements ClickHandler {

    @Override
    public void onClick(ClickEvent event) {
      if(currentOffset > 0) {
        populateValues(currentOffset - PAGE_SIZE);
      }
    }

  }

  public class NextPageClickHandler implements ClickHandler {

    @Override
    public void onClick(ClickEvent event) {
      if(currentOffset + PAGE_SIZE < table.getValueSetCount()) {
        populateValues(currentOffset + PAGE_SIZE);
      }
    }

  }

  public interface Display extends WidgetDisplay {

    void setSummaryTabWidget(WidgetDisplay widget);

    void populateValues(JsArray<ValueDto> values);

    void setValueType(String type);

    void setScript(String text);

    HandlerRegistration addNextPageClickHandler(ClickHandler handler);

    HandlerRegistration addPreviousPageClickHandler(ClickHandler handler);

    void setPageLimits(int low, int high, int count);
  }

}
