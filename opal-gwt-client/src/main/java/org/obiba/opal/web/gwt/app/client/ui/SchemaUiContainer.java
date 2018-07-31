package org.obiba.opal.web.gwt.app.client.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.obiba.opal.web.gwt.app.client.support.jsonschema.JsonSchemaGWT;

import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.TakesValue;
import com.google.gwt.user.client.ui.DoubleBox;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class SchemaUiContainer extends com.github.gwtbootstrap.client.ui.ControlGroup {

  private JSONObject schema;
  private String key;

  private boolean required;

  private String type;
  private boolean isSingleValueType;

  public SchemaUiContainer(JSONObject schema, String key, boolean required) {
    this.schema = schema;
    this.key = key;
    this.required = required;

    type = JsonSchemaGWT.getType(schema);
    isSingleValueType = "integer".equals(type) || "number".equals(type) || "string".equals(type);

    setUp();
  }

  public Object getValue() {
    Iterator<Widget> iterator = getChildren().iterator();
    List<Object> values = new ArrayList<>();

    boolean found = false;

    while(!found || iterator.hasNext()) {
      Widget widget = iterator.next();

      if (widget instanceof TakesValue || widget instanceof ListBox) {
        if (isSingleValueType) found = true;
        values.add(widget instanceof TakesValue ? ((TakesValue) widget).getValue() : ((ListBox) widget).getSelectedValue());
      }
    }

    return isSingleValueType ? values.get(0) : values;
  }

  public boolean isValid() {
    // create a schema validator? is this useful if the validation is done through the browser (HTML5)?
    boolean valid = true;
    Object value = getValue();

    switch(type) {
      case "string": {
        if (required) {
          valid = value != null && ((String) value).trim().length() > 0;
        }

        valid = valid && JsonSchemaGWT.valueForStringSchemaIsValid(value instanceof String ? (String) value : null, schema);
        break;
      }
      case "integer":
      case "number": {
        if (required) {
          valid = value != null;
        }

        valid = valid && JsonSchemaGWT.valueForNumericSchemaIsValid(value instanceof Number ? (Number) value : null, schema);
        break;
      }
    }

    return valid;
  }

  public JSONObject getSchema() {
    return schema;
  }

  public String getKey() {
    return key;
  }

  public boolean isRequired() {
    return required;
  }

  private void setUp() {
    Widget widget = buildInputWidget();

    if (widget != null) {
      JSONValue title = schema.get("title");
      if(title != null && title.isString() != null) {
        String titleStringValue = title.isString().stringValue();
        add(new ControlLabel(titleStringValue));
        setTitle(titleStringValue);
      }

      // find out what to do with type
      if (required) widget.getElement().setAttribute("required", "required");
      add(widget);

      JSONValue description = schema.get("description");
      if(description != null && description.isString() != null) {
        add(new ControlLabel(description.isString().stringValue()));
      }
    }
  }

  private Widget buildInputWidget() {
    JSONValue anEnum = schema.get("enum");
    boolean hasEnum = anEnum != null && anEnum.isArray() != null;
    // validation for enum, must create a ListBox for those, currently easy to implement for type == string

    JSONValue aDefault = schema.get("default");

    // for now 3 cases: number, integer and string
    switch(type) {
      case "number": {
        DoubleBox input = new DoubleBox();
        input.setName(key);
        input.getElement().setAttribute("type", "number");
        input.getElement().setAttribute("step", "0.001");
        setNumericSchemaValidations(input);

        if (aDefault != null && aDefault.isNumber() != null) {
          double defaultDoubleValue = aDefault.isNumber().doubleValue();
          input.setValue(defaultDoubleValue);
        }

        return input;
      }
      case "integer": {
        IntegerBox input = new IntegerBox();
        input.setName(key);
        input.getElement().setAttribute("type", "number");
        input.getElement().setAttribute("step", "1");
        setNumericSchemaValidations(input);

        if (aDefault != null && aDefault.isNumber() != null) {
          double defaultDoubleValue = aDefault.isNumber().doubleValue();
          input.setValue(Double.valueOf(defaultDoubleValue).intValue());
        }

        return input;
      }
      case "string": {
        if (hasEnum) {
          JSONArray enumArray = anEnum.isArray();

          ListBox listBox = new ListBox();
          listBox.setName(key);

          for(int i = 0; i < enumArray.size(); i++) {
            listBox.addItem(enumArray.get(i).isString().stringValue());
          }

          return listBox;
        }

        TextBox input = new TextBox();
        input.setName(key);
        setStringSchemaValidations(input);

        if (aDefault != null && aDefault.isString() != null) {
          String defaultStringValue = aDefault.isString().stringValue();
          input.setValue(defaultStringValue);
        }

        return input;
      }
    }

    return null;
  }

  private void setStringSchemaValidations(Widget widget) {
    JSONValue maxLength = schema.get("maxLength");
    if (maxLength != null && maxLength.isNumber() != null) {
      if (maxLength.isNumber().doubleValue() > 0) widget.getElement().setAttribute("max", maxLength.isNumber().toString());
    }

    JSONValue minLength = schema.get("minLength");
    if (minLength != null && minLength.isNumber() != null) {
      if (minLength.isNumber().doubleValue() > 0) widget.getElement().setAttribute("min", minLength.isNumber().toString());
    }

    JSONValue pattern = schema.get("pattern");
    if (pattern != null && pattern.isString() != null) {
      widget.getElement().setAttribute("pattern", pattern.isString().stringValue());
    }
  }

  private void setNumericSchemaValidations(Widget widget) {
    JSONValue maximum = schema.get("maximum");
    if (maximum != null && maximum.isNumber() != null) {
      widget.getElement().setAttribute("max", maximum.isNumber().toString());
    }

    JSONValue minimum = schema.get("minimum");
    if (minimum != null && minimum.isNumber() != null) {
      widget.getElement().setAttribute("min", minimum.isNumber().toString());
    }
  }
}
