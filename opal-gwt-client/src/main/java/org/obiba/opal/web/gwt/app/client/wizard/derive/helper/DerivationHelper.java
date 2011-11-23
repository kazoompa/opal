/*******************************************************************************
 * Copyright (c) 2011 OBiBa. All rights reserved.
 *  
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.wizard.derive.helper;

import java.util.List;

import org.obiba.opal.web.gwt.app.client.i18n.Translations;
import org.obiba.opal.web.gwt.app.client.wizard.derive.view.ValueMapEntry;
import org.obiba.opal.web.model.client.magma.VariableDto;

import com.google.gwt.core.client.GWT;

/**
 *
 */
public abstract class DerivationHelper {

  protected Translations translations = GWT.create(Translations.class);

  protected List<ValueMapEntry> valueMapEntries;

  protected VariableDto originalVariable;

  public DerivationHelper(VariableDto originalVariable) {
    this.originalVariable = originalVariable;
  }

  protected abstract void initializeValueMapEntries();

  protected abstract DerivedVariableGenerator getDerivedVariableGenerator();

  public VariableDto getDerivedVariable() {
    return getDerivedVariableGenerator().generate();
  }

  public List<ValueMapEntry> getValueMapEntries() {
    return valueMapEntries;
  }

  public boolean addEntry(ValueMapEntry entryArg) {
    for(ValueMapEntry entry : valueMapEntries) {
      if(entry.getValue().equals(entryArg.getValue())) {
        return false;
      }
    }
    valueMapEntries.add(entryArg);
    return true;
  }

  public boolean hasValueMapEntryWithValue(String value) {
    for(ValueMapEntry entry : valueMapEntries) {
      if(entry.getValue().equals(value)) {
        return true;
      }
    }
    return false;
  }

  public VariableDto getOriginalVariable() {
    return originalVariable;
  }

}
