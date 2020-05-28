/*
 * Copyright (c) 2020 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.spi.r.datasource;

import org.obiba.opal.spi.datasource.DatasourceService;

public interface RDatasourceService extends DatasourceService {

  /**
   * Set the accessor to the R session for executing operations.
   *
   * @param sessionHandler
   */
  void setRSessionHandler(RSessionHandler sessionHandler);

}
