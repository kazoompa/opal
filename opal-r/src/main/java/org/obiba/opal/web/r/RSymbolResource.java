package org.obiba.opal.web.r;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.obiba.opal.core.service.IdentifiersTableService;
import org.obiba.opal.r.service.OpalRSession;

public interface RSymbolResource {

  void setName(String name);

  void setOpalRSession(OpalRSession rSession);

  void setIdentifiersTableService(IdentifiersTableService identifiersTableService);

  String getName();

  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  Response getSymbol();

  @PUT
  @Consumes(MediaType.TEXT_PLAIN)
  Response putString(@Context UriInfo uri, String content, @QueryParam("async") @DefaultValue("false") boolean async);

  /**
   * Push a R data object into the R server: content is expected to be the serialized form of the R object, base64 encoded.
   *
   * @param uri
   * @param content
   * @param async
   * @return
   */
  @POST
  @Consumes("application/x-rdata")
  Response putRData(@Context UriInfo uri, String content, @QueryParam("async") @DefaultValue("false") boolean async);


  @PUT
  @Consumes("application/x-rscript")
  Response putRScript(@Context UriInfo uri, String script, @QueryParam("async") @DefaultValue("false") boolean async);

  @PUT
  @Consumes("application/x-opal")
  Response putMagma(@Context UriInfo uri, String path, @QueryParam("variables") String variableFilter,
      @QueryParam("missings") @DefaultValue("false") Boolean missings, @QueryParam("identifiers") String identifiers,
      @QueryParam("async") @DefaultValue("false") boolean async);

  @DELETE
  Response rm();
}
