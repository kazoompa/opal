/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.wizard.createdatasource.presenter;

import java.util.HashSet;
import java.util.Set;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.obiba.opal.web.gwt.app.client.event.WorkbenchChangeEvent;
import org.obiba.opal.web.gwt.app.client.presenter.ApplicationPresenter;
import org.obiba.opal.web.gwt.app.client.presenter.NavigatorPresenter;
import org.obiba.opal.web.model.client.magma.DatasourceFactoryDto;
import org.obiba.opal.web.model.client.magma.ExcelDatasourceFactoryDto;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class CreateDatasourceStepPresenter extends WidgetPresenter<CreateDatasourceStepPresenter.Display> {
  //
  // Instance Variables
  //

  @Inject
  private Provider<ApplicationPresenter> applicationPresenter;

  @Inject
  private Provider<NavigatorPresenter> navigatorPresenter;

  @Inject
  private Provider<CreateDatasourceConclusionStepPresenter> createDatasourceConclusionStepPresenter;

  @Inject
  private HibernateDatasourceFormPresenter hibernateDatasourceFormPresenter;

  @Inject
  private ExcelDatasourceFormPresenter excelDatasourceFormPresenter;

  private Set<DatasourceFormPresenter> datasourceFormPresenters = new HashSet<DatasourceFormPresenter>();

  //
  // Constructors
  //

  @Inject
  public CreateDatasourceStepPresenter(final Display display, final EventBus eventBus) {
    super(display, eventBus);
  }

  //
  // WidgetPresenter Methods
  //

  @Override
  protected void onBind() {
    addEventHandlers();

    // FIXME: Is there a way of registering these automatically ? Injecting the set fails.
    datasourceFormPresenters.add(hibernateDatasourceFormPresenter);
    datasourceFormPresenters.add(excelDatasourceFormPresenter);

    getDisplay().setDatasourceForm(hibernateDatasourceFormPresenter);
  }

  @Override
  protected void onUnbind() {
  }

  protected void addEventHandlers() {
    super.registerHandler(getDisplay().addCancelClickHandler(new CancelClickHandler()));
    super.registerHandler(getDisplay().addCreateClickHandler(new CreateClickHandler()));
    super.registerHandler(getDisplay().addDatasourceTypeChangeHandler(new ChangeHandler() {

      @Override
      public void onChange(ChangeEvent evt) {
        GWT.log(getDisplay().getDatasourceType());
        for(DatasourceFormPresenter formPresenter : datasourceFormPresenters) {
          if(formPresenter.isForType(getDisplay().getDatasourceType())) {
            getDisplay().setDatasourceForm(formPresenter);
            break;
          }
        }
      }
    }));
  }

  @Override
  public void revealDisplay() {
  }

  @Override
  public void refreshDisplay() {
  }

  @Override
  public Place getPlace() {
    return null;
  }

  @Override
  protected void onPlaceRequest(PlaceRequest request) {
  }

  //
  // Methods
  //

  //
  // Inner Classes / Interfaces
  //

  public interface Display extends WidgetDisplay {

    String getDatasourceName();

    String getDatasourceType();

    HandlerRegistration addCancelClickHandler(ClickHandler handler);

    HandlerRegistration addCreateClickHandler(ClickHandler handler);

    HandlerRegistration addDatasourceTypeChangeHandler(ChangeHandler handler);

    void setDatasourceForm(DatasourceFormPresenter formPresenter);

    DatasourceFormPresenter getDatasourceForm();
  }

  class CancelClickHandler implements ClickHandler {

    public void onClick(ClickEvent arg0) {
      eventBus.fireEvent(new WorkbenchChangeEvent(navigatorPresenter.get()));
    }
  }

  class CreateClickHandler implements ClickHandler {

    public void onClick(ClickEvent arg0) {
      DatasourceFactoryDto dto = createDatasourceFactoryDto();
      if(dto.getName().length() > 0) {
        CreateDatasourceConclusionStepPresenter presenter = createDatasourceConclusionStepPresenter.get();
        presenter.setDatasourceFactory(dto);
        eventBus.fireEvent(new WorkbenchChangeEvent(presenter));
      } else {
        // TODO: error message
      }
    }

    private DatasourceFactoryDto createDatasourceFactoryDto() {
      DatasourceFactoryDto dto = getDisplay().getDatasourceForm().getDatasourceFactory();
      dto.setName(getDisplay().getDatasourceName().trim());
      return dto;
    }

    private DatasourceFactoryDto createExcelDatasourceFactoryDto(String filePath) {
      ExcelDatasourceFactoryDto extensionDto = ExcelDatasourceFactoryDto.create();
      extensionDto.setFile(filePath);
      extensionDto.setReadOnly(false);

      DatasourceFactoryDto dto = DatasourceFactoryDto.create();
      dto.setExtension(ExcelDatasourceFactoryDto.DatasourceFactoryDtoExtensions.params, extensionDto);

      return dto;
    }
  }
}
