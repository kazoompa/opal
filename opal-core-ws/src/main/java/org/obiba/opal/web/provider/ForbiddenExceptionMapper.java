/*
 * Copyright (c) 2021 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.provider;

import com.google.protobuf.GeneratedMessage;
import org.obiba.opal.web.magma.ClientErrorDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

@Component
@Provider
public class ForbiddenExceptionMapper extends ErrorDtoExceptionMapper<ForbiddenException> {

  private static final Logger log = LoggerFactory.getLogger(ForbiddenExceptionMapper.class);

  @Override
  protected Response.Status getStatus() {
    return FORBIDDEN;
  }

  @Override
  protected GeneratedMessage.ExtendableMessage<?> getErrorDto(ForbiddenException exception) {
    log.warn("Forbidden exception", exception);
    return ClientErrorDtos.getErrorMessage(getStatus(), "Forbidden", exception);
  }

}
