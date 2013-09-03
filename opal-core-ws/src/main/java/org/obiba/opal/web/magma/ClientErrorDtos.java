/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.magma;

import java.util.Arrays;
import java.util.Collections;

import javax.ws.rs.core.Response;

import org.mozilla.javascript.RhinoException;
import org.obiba.magma.support.DatasourceParsingException;
import org.obiba.opal.web.model.Magma.DatasourceParsingErrorDto;
import org.obiba.opal.web.model.Magma.JavaScriptErrorDto;
import org.obiba.opal.web.model.Ws.ClientErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for handling ClientError Dtos.
 */
public class ClientErrorDtos {

  private static final Logger log = LoggerFactory.getLogger(ClientErrorDtos.class);

  private ClientErrorDtos() {}

  public static ClientErrorDto.Builder getErrorMessage(Response.StatusType responseStatus, String errorStatus,
      String... args) {
    return ClientErrorDto.newBuilder() //
        .setCode(responseStatus.getStatusCode()) //
        .setStatus(errorStatus == null ? "" : errorStatus) //
        .addAllArguments(args == null ? Collections.<String>emptyList() : Arrays.asList(args));
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public static ClientErrorDto.Builder getErrorMessage(Response.StatusType responseStatus, String errorStatus,
      Exception e) {
    ClientErrorDto.Builder builder = getErrorMessage(responseStatus, errorStatus);
    Throwable cause = getRootCause(e);
    builder.addArguments(cause.getMessage() == null ? cause.getClass().getName() : cause.getMessage());
    return builder;
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public static ClientErrorDto.Builder getErrorMessage(Response.StatusType responseStatus, String errorStatus,
      RuntimeException e) {
    log.warn(errorStatus, e);
    Throwable cause = getRootCause(e);
    return getErrorMessage(responseStatus, errorStatus)
        .addArguments(cause.getMessage() == null ? cause.getClass().getName() : cause.getMessage());
  }

  private static Throwable getRootCause(Exception e) {
    Throwable cause = e;
    while(cause.getCause() != null) {
      cause = cause.getCause();
    }
    return cause;
  }

  public static ClientErrorDto.Builder getErrorMessage(Response.StatusType responseStatus, String errorStatus,
      DatasourceParsingException pe) {
    ClientErrorDto.Builder builder = getErrorMessage(responseStatus, errorStatus);
    builder.addArguments(pe.getMessage());
    // build a parsing error dto list
    if(pe.getChildren().isEmpty()) {
      builder.addExtension(DatasourceParsingErrorDto.errors, newDatasourceParsingErrorDto(pe).build());
    } else {
      for(DatasourceParsingException child : pe.getChildrenAsList()) {
        builder.addExtension(DatasourceParsingErrorDto.errors, newDatasourceParsingErrorDto(child).build());
      }
    }
    return builder;
  }

  public static ClientErrorDto.Builder getErrorMessage(Response.StatusType responseStatus, String errorStatus,
      RhinoException exception) {
    return getErrorMessage(responseStatus, errorStatus) //
        .addArguments(exception.getMessage()) //
        .addExtension(JavaScriptErrorDto.errors, newJavaScriptErrorDto(exception).build());
  }

  private static DatasourceParsingErrorDto.Builder newDatasourceParsingErrorDto(DatasourceParsingException pe) {
    DatasourceParsingErrorDto.Builder builder = DatasourceParsingErrorDto.newBuilder() //
        .setDefaultMessage(pe.getMessage()) //
        .setKey(pe.getKey());
    for(Object arg : pe.getParameters()) {
      builder.addArguments(arg.toString());
    }
    return builder;
  }

  private static JavaScriptErrorDto.Builder newJavaScriptErrorDto(RhinoException exception) {
    JavaScriptErrorDto.Builder builder = JavaScriptErrorDto.newBuilder() //
        .setMessage(exception.details()) //
        .setSourceName(exception.sourceName()) //
        .setLineNumber(exception.lineNumber()); //
    if(exception.lineSource() != null) {
      builder.setLineSource(exception.lineSource());
    }
    if(exception.columnNumber() != 0) { // column number is 0 if unknown
      builder.setColumnNumber(exception.columnNumber());
    }
    return builder;
  }
}
