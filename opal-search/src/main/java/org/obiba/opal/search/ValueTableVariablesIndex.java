/*
 * Copyright (c) 2012 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.search;

import java.util.Calendar;

import org.obiba.magma.Attribute;
import org.obiba.magma.Timestamped;
import org.obiba.magma.Variable;

/**
 * An index of a {@code ValueTable} variables.
 */
public interface ValueTableVariablesIndex extends ValueTableIndex {

  /**
   * Name of the field for the given attribute.
   *
   * @param attribute Attribute
   * @return
   */
  String getFieldName(Attribute attribute);

}