/*
 * Copyright (c) 2021 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.security;

import com.google.common.base.Strings;
import org.apache.http.HttpStatus;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.HttpRequest;
import org.obiba.opal.audit.OpalUserProvider;
import org.obiba.opal.web.ws.cfg.OpalWsConfig;
import org.obiba.opal.web.ws.intercept.RequestCyclePostProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;

@Component
public class AuditInterceptor implements RequestCyclePostProcess {

  private static final Logger log = LoggerFactory.getLogger(AuditInterceptor.class);

  private static final String LOG_FORMAT = "{}";

  private static final String[] VALID_IP_HEADER_CANDIDATES = {
      "X-Forwarded-For",
      "Proxy-Client-IP",
      "WL-Proxy-Client-IP",
      "HTTP_X_FORWARDED_FOR",
      "HTTP_X_FORWARDED",
      "HTTP_X_CLUSTER_CLIENT_IP",
      "HTTP_CLIENT_IP",
      "HTTP_FORWARDED_FOR",
      "HTTP_FORWARDED",
      "HTTP_VIA",
      "REMOTE_ADDR"};

  @Autowired
  private OpalUserProvider opalUserProvider;

  @Override
  public void postProcess(HttpServletRequest servletRequest, HttpRequest request, ResourceMethodInvoker resourceMethod, ServerResponse response) {
    logServerError(servletRequest, request, response);
    logClientError(servletRequest, request, response);
    logInfo(servletRequest, request, response);
  }

  private String getArguments(HttpServletRequest servletRequest, HttpRequest request, ServerResponse response) {
    MDC.put("username", opalUserProvider.getUsername());
    MDC.put("status", response.getStatus() + "");
    MDC.put("method", request.getHttpMethod());
    MDC.put("ip", getClientIP(servletRequest, request));

    StringBuilder sb = new StringBuilder(request.getUri().getPath(true));
    MultivaluedMap<String, String> params = request.getUri().getQueryParameters();
    if (!params.isEmpty()) {
      sb.append(" queryParams:").append(params);
    }

    return sb.toString();
  }

  private String getClientIP(HttpServletRequest servletRequest, HttpRequest request) {
    String ip = "";

    for (String ipHeader : VALID_IP_HEADER_CANDIDATES) {
      ip = request.getHttpHeaders().getRequestHeaders().keySet().stream()
          .filter(ipHeader::equalsIgnoreCase)
          .map(h -> request.getHttpHeaders().getHeaderString(h))
          .findFirst().orElse("");
      if (!Strings.isNullOrEmpty(ip)) break;
    }

    if (Strings.isNullOrEmpty(ip))
      ip = servletRequest.getRemoteAddr();

    return ip;
  }

  private void logServerError(HttpServletRequest servletRequest, HttpRequest request, ServerResponse response) {
    if (!log.isErrorEnabled()) return;
    if (response.getStatus() < HttpStatus.SC_INTERNAL_SERVER_ERROR) return;

    log.error(LOG_FORMAT, getArguments(servletRequest, request, response));
  }

  private void logClientError(HttpServletRequest servletRequest, HttpRequest request, ServerResponse response) {
    if (!log.isWarnEnabled()) return;
    if (response.getStatus() < HttpStatus.SC_BAD_REQUEST) return;
    if (response.getStatus() >= HttpStatus.SC_INTERNAL_SERVER_ERROR) return;

    log.warn(LOG_FORMAT, getArguments(servletRequest, request, response));
  }

  private void logInfo(HttpServletRequest servletRequest, HttpRequest request, ServerResponse response) {
    if (!log.isInfoEnabled()) return;
    if (response.getStatus() >= HttpStatus.SC_BAD_REQUEST) return;

    boolean logged = false;
    if (response.getStatus() == HttpStatus.SC_CREATED) {
      URI resourceUri = (URI) response.getMetadata().getFirst(HttpHeaders.LOCATION);
      if (resourceUri != null) {
        String path = resourceUri.getPath().substring(OpalWsConfig.WS_ROOT.length());
        MDC.put("created", path);
        log.info(LOG_FORMAT, getArguments(servletRequest, request, response));
        logged = true;
      }
    }

    if (!logged) {
      log.info(LOG_FORMAT, getArguments(servletRequest, request, response));
    }
  }

}
