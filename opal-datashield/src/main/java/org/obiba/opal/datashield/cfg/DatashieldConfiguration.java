/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.datashield.cfg;

import java.util.List;

import org.obiba.opal.core.cfg.OpalConfigurationExtension;
import org.obiba.opal.datashield.DataShieldMethod;

public class DatashieldConfiguration implements OpalConfigurationExtension {

  private List<DataShieldMethod> aggregatingMethods;

  public List<DataShieldMethod> getAggregatingMethods() {
    return aggregatingMethods;
  }
}
