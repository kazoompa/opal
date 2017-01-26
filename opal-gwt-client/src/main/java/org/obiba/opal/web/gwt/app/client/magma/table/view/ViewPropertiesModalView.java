/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.web.gwt.app.client.magma.table.view;

import com.github.gwtbootstrap.client.ui.Alert;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import org.obiba.opal.web.gwt.app.client.i18n.TranslationMessages;
import org.obiba.opal.web.gwt.app.client.i18n.Translations;
import org.obiba.opal.web.gwt.app.client.js.JsArrays;
import org.obiba.opal.web.gwt.app.client.magma.table.presenter.ViewPropertiesModalPresenter;
import org.obiba.opal.web.gwt.app.client.magma.table.presenter.ViewPropertiesModalUiHandlers;
import org.obiba.opal.web.gwt.app.client.magma.view.TableReferenceColumn;
import org.obiba.opal.web.gwt.app.client.magma.view.TableReferencesTable;
import org.obiba.opal.web.gwt.app.client.ui.*;
import org.obiba.opal.web.gwt.app.client.ui.celltable.CheckboxColumn;
import org.obiba.opal.web.model.client.magma.TableDto;
import org.obiba.opal.web.model.client.magma.ViewDto;
import org.obiba.opal.web.model.client.ws.ClientErrorDto;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ViewPropertiesModalView extends ModalPopupViewWithUiHandlers<ViewPropertiesModalUiHandlers>
    implements ViewPropertiesModalPresenter.Display {

  interface Binder extends UiBinder<Widget, ViewPropertiesModalView> {}

  private static final int DEFAULT_PAGE_SIZE = 10;

  @UiField
  Modal dialog;

  @UiField
  ControlGroup nameGroup;

  @UiField
  TextBox name;

  @UiField
  Button closeButton;

  @UiField
  Button saveButton;

  @UiField
  ControlGroup tablesGroup;

  @UiField
  TableChooser tableChooser;

  @UiField
  OpalSimplePager pager;

  @UiField
  TableReferencesTable table;

  @UiField
  Alert selectAllItemsAlert;

  @UiField
  Alert selectItemTipsAlert;

  @UiField
  Label selectAllStatus;

  @UiField
  IconAnchor selectAllAnchor;

  @UiField
  IconAnchor clearSelectionAnchor;

  @UiField
  IconAnchor deleteLink;

  @UiField
  IconAnchor moveUpLink;

  @UiField
  IconAnchor moveDownLink;

  private Translations translations;

  private TranslationMessages translationMessages;

  private final ListDataProvider<TableDto> dataProvider = new ListDataProvider<TableDto>();

  private CheckboxColumn<TableDto> checkActionCol;

  private List<String> innerTableReferences = new ArrayList<String>();

  @Inject
  public ViewPropertiesModalView(Binder uiBinder, EventBus eventBus, Translations translations, TranslationMessages translationMessages) {
    super(eventBus);
    this.translations = translations;
    this.translationMessages = translationMessages;
    initWidget(uiBinder.createAndBindUi(this));
    dialog.setTitle(translations.editProperties());
    //tableChooser.addStyleName("table-chooser-large");

    table.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);
    table.setSelectionModel(new SingleSelectionModel<TableDto>());
    table.setEmptyTableWidget(new Label(translations.noTablesLabel()));
    table.setPageSize(DEFAULT_PAGE_SIZE);

    pager.setDisplay(table);
    dataProvider.addDataDisplay(table);
  }

  @UiHandler("addButton")
  void onAddButton(ClickEvent event) {
    List<TableDto> existingTables = new ArrayList<TableDto>(dataProvider.getList());
    TableDto tableToAdd = tableChooser.getSelectedTable();
    if(!validateTableAddition(existingTables, tableToAdd)) return;
    existingTables.add(tableToAdd);
    dataProvider.setList(existingTables);
    dataProvider.refresh();
    pager.setPagerVisible(dataProvider.getList().size() >= DEFAULT_PAGE_SIZE);
    pager.setPage(dataProvider.getList().size() / table.getPageSize());
  }


  @UiHandler("moveUpLink")
  void onMoveUp(ClickEvent event) {
    List<TableDto> tables = new ArrayList<TableDto>();
    Collection<TableDto> reordered = new ArrayList<TableDto>();

    int i = 0;
    int pos = 0;
    for(TableDto tableDto : dataProvider.getList()) {
      if(checkActionCol.getSelectionModel().isSelected(tableDto)) {
        if(reordered.isEmpty()) {
          pos = i - 1;
        }
        reordered.add(tableDto);
      } else {
        tables.add(tableDto);
      }
      i++;
    }
    tables.addAll(pos >= 0 ? pos : 0, reordered);

    dataProvider.setList(tables);
    dataProvider.refresh();
  }

  @UiHandler("moveDownLink")
  void onMoveDown(ClickEvent event) {
    List<TableDto> tables = new ArrayList<TableDto>();
    Collection<TableDto> reordered = new ArrayList<TableDto>();

    int i = 0;
    int pos = 0;
    for(TableDto tableDto : dataProvider.getList()) {
      if(checkActionCol.getSelectionModel().isSelected(tableDto)) {
        if(reordered.isEmpty()) {
          pos = i + 1;
        }
        reordered.add(tableDto);
      } else {
        tables.add(tableDto);
        i++;
      }
    }
    tables.addAll(pos, reordered);

    dataProvider.setList(tables);
    dataProvider.refresh();
  }

  @UiHandler("deleteLink")
  void onDelete(ClickEvent event) {
    // Remove selected items from table
    List<TableDto> tables = new ArrayList<TableDto>();
    for(TableDto tableDto : dataProvider.getList()) {
      if(!checkActionCol.getSelectionModel().isSelected(tableDto)) {
        tables.add(tableDto);
      } else {
        innerTableReferences.remove(toReference(tableDto));
      }
    }
    checkActionCol.clearSelection();

    dataProvider.setList(tables);
    dataProvider.refresh();
    pager.setPagerVisible(dataProvider.getList().size() >= DEFAULT_PAGE_SIZE);
  }

  @UiHandler("closeButton")
  void onClose(ClickEvent event) {
    dialog.hide();
  }

  @UiHandler("saveButton")
  void onSave(ClickEvent event) {
    getUiHandlers().onSave(getName().getText(), dataProvider.getList(), innerTableReferences);
  }

  @Override
  public void renderProperties(ViewDto view) {
    name.setText(view.getName());
  }

  @Override
  public void prepareTables(JsArray<TableDto> tables, JsArrayString froms, JsArrayString innerFroms) {
    tableChooser.addTableSelections(tables);
    renderTableReferencesRows(tables, froms, innerFroms);
  }

  @Override
  public void showError(String message, @Nullable FormField group) {
    if(Strings.isNullOrEmpty(message)) return;

    dialog.closeAlerts();
    String msg = message;
    try {
      ClientErrorDto errorDto = JsonUtils.unsafeEval(message);
      msg = errorDto.getStatus();
    } catch(Exception ignored) {
    }

    if(group == null) {
      dialog.addAlert(msg, AlertType.ERROR);
    } else if(group.equals(FormField.NAME)) dialog.addAlert(msg, AlertType.ERROR, nameGroup);
    else dialog.addAlert(msg, AlertType.ERROR, tablesGroup);
  }

  @Override
  public HasText getName() {
    return name;
  }

  @Override
  public HasCollection<TableDto> getSelectedTables() {
    return new HasCollection<TableDto>() {
      @Override
      public Collection<TableDto> getCollection() {
        return dataProvider.getList();
      }
    };
  }

  private String toReference(TableDto tableDto) {
    return tableDto.getDatasourceName() + "." + tableDto.getName();
  }

  private void renderTableReferencesRows(JsArray<TableDto> tables, JsArrayString froms, JsArrayString innerFroms) {
    addColumns();

    Map<String, TableDto> tableReferencesMap = Maps.newHashMap();
    for (TableDto tableDto : JsArrays.toIterable(tables)) {
      tableReferencesMap.put(toReference(tableDto), tableDto);
    }
    List<TableDto> selectedTables = new ArrayList<TableDto>();
    for (String selection : JsArrays.toIterable(froms)) {
      if (tableReferencesMap.containsKey(selection)) {
        selectedTables.add(tableReferencesMap.get(selection));
      }
    }
    innerTableReferences = new ArrayList<String>(JsArrays.toList(innerFroms));

    dataProvider.setList(selectedTables);
    dataProvider.refresh();
    pager.setPagerVisible(dataProvider.getList().size() >= DEFAULT_PAGE_SIZE);


  }

  private void addColumns() {
    checkActionCol = new CheckboxColumn<TableDto>(new TableReferenceDisplay());
    table.addColumn(checkActionCol, checkActionCol.getCheckColumnHeader());
    table.setColumnWidth(checkActionCol, 1, Style.Unit.PX);
    table.addColumn(new TableReferenceColumn(), translations.nameLabel());
    InnerJoinColumn innerCol = new InnerJoinColumn();
    table.addColumn(innerCol, translations.innerJoinLabel());
    table.setColumnWidth(innerCol, 1, Style.Unit.PX);
  }

  /**
   * Validate that the table is not already present.
   *
   * @param existingTables
   * @param tableToAdd
   * @return
   */
  private boolean validateTableAddition(Iterable<TableDto> existingTables, TableDto tableToAdd) {
    String ref = toReference(tableToAdd);
    for (TableDto dto : existingTables) {
      if (toReference(dto).equals(ref)) return false;
    }
    return true;
  }

  private class InnerJoinColumn extends Column<TableDto, Boolean> {


    private InnerJoinColumn() {
      super(new CheckboxCell(true, false));
      setFieldUpdater(new FieldUpdater<TableDto, Boolean>() {
        @Override
        public void update(int index, TableDto object, Boolean value) {
          String ref = toReference(object);
          if (value) {
            if (!innerTableReferences.contains(ref)) innerTableReferences.add(ref);
          } else if (innerTableReferences.contains(ref)) {
            innerTableReferences.remove(ref);
          }
        }
      });
    }

    @Override
    public Boolean getValue(TableDto object) {
      return innerTableReferences.contains(toReference(object));
    }
  }

  private class TableReferenceDisplay implements CheckboxColumn.Display<TableDto> {

    @Override
    public Table<TableDto> getTable() {
      return table;
    }

    @Override
    public Object getItemKey(TableDto item) {
      return toReference(item);
    }

    @Override
    public IconAnchor getClearSelection() {
      return clearSelectionAnchor;
    }

    @Override
    public IconAnchor getSelectAll() {
      return selectAllAnchor;
    }

    @Override
    public HasText getSelectAllStatus() {
      return selectAllStatus;
    }

    @Override
    public ListDataProvider<TableDto> getDataProvider() {
      return dataProvider;
    }

    @Override
    public String getNItemLabel(int nb) {
      return translationMessages.nTablesLabel(nb).toLowerCase();
    }

    @Override
    public Alert getSelectActionsAlert() {
      return selectAllItemsAlert;
    }

    @Override
    public Alert getSelectTipsAlert() {
      return selectItemTipsAlert;
    }
  }
}
