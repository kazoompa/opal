/*
 * Copyright (c) 2020 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.r.magma;

import org.obiba.opal.spi.r.RRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Convert tibble's labelled vectors to factors, and optionally downgrade to data.frame class. This data structure
 * is more appropriate for running analysis.
 */
class ValueTableDataFrameRConverter extends ValueTableTibbleRConverter {

  private static final Logger log = LoggerFactory.getLogger(ValueTableDataFrameRConverter.class);

  private static final String WITH_FACTORS_FUNC = ".with.factors";

  private static final String WITH_FACTORS_SCRIPT = WITH_FACTORS_FUNC + ".R";

  private final boolean useTibble;

  ValueTableDataFrameRConverter(MagmaAssignROperation magmaAssignROperation) {
    this(magmaAssignROperation, false);
  }

  ValueTableDataFrameRConverter(MagmaAssignROperation magmaAssignROperation, boolean useTibble) {
    super(magmaAssignROperation);
    this.useTibble = useTibble;
  }

  @Override
  public void doAssign(String symbol, String path) {
    super.doAssign(symbol, path);
    try (InputStream is = new ClassPathResource(WITH_FACTORS_SCRIPT).getInputStream();) {
      magmaAssignROperation.doWriteFile(WITH_FACTORS_SCRIPT, is);
    } catch (IOException e) {
      throw new RRuntimeException(e);
    }
    magmaAssignROperation.doEval(String.format("base::source('%s')", WITH_FACTORS_SCRIPT));
    magmaAssignROperation.doEval(String.format("base::is.null(base::assign('%s', %s(`%s`, as.data.frame=%s)))",
        getSymbol(), WITH_FACTORS_FUNC, getSymbol(), useTibble ? "FALSE" : "TRUE"));
    // cleaning
    magmaAssignROperation.doEval(String.format("base::rm(%s)", WITH_FACTORS_FUNC));
    magmaAssignROperation.doEval(String.format("base::unlink('%s')", WITH_FACTORS_SCRIPT));
    if (!useTibble && !withIdColumn()) {
      magmaAssignROperation.doEval(String.format("rownames(`%s`) <- `%s`[['%s']]", getSymbol(), getSymbol(), getIdColumnName()));
      magmaAssignROperation.doEval(String.format("`%s`['%s'] <- NULL", getSymbol(), getIdColumnName()));
    }
  }

}
