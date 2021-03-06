/*
 * Copyright (c) 2021 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.gwt.app.client.project.resources;

import com.google.common.base.Strings;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.PopupView;
import org.obiba.opal.web.gwt.app.client.i18n.Translations;
import org.obiba.opal.web.gwt.app.client.presenter.ModalPresenterWidget;
import org.obiba.opal.web.gwt.app.client.project.resources.event.ResourceCreatedEvent;
import org.obiba.opal.web.gwt.app.client.project.resources.event.ResourceUpdatedEvent;
import org.obiba.opal.web.gwt.rest.client.ResourceRequestBuilderFactory;
import org.obiba.opal.web.gwt.rest.client.ResponseCodeCallback;
import org.obiba.opal.web.gwt.rest.client.UriBuilders;
import org.obiba.opal.web.model.client.opal.ResourceReferenceDto;

import javax.inject.Inject;

public class ProjectResourceModalPresenter extends ModalPresenterWidget<ProjectResourceModalPresenter.Display>
    implements ProjectResourceModalUiHandlers {

  private final Translations translations;

  private String projectName;

  private ResourceReferenceDto originalResource;

  private ResourceCreatedEvent.ResourceCreatedHandler resourceCreatedHandler;

  @Inject
  public ProjectResourceModalPresenter(EventBus eventBus,
                                       Display view,
                                       Translations translations) {
    super(eventBus, view);
    getView().setUiHandlers(this);
    this.translations = translations;
  }

  public void initialize(String projectName, ResourceReferenceDto resource, boolean readOnly) {
    this.projectName = projectName;
    this.originalResource = resource;
    getView().initialize(resource, readOnly);
  }

  @Override
  public void onSave(final String name, String description, String factoryKey, JSONObject parameters, JSONObject credentials) {
    String[] token = ResourceProvidersService.splitResourceFactoryKey(factoryKey);
    ResourceReferenceDto resource = ResourceReferenceDto.create();
    resource.setName(name);
    if (!Strings.isNullOrEmpty(description)) resource.setDescription(description);
    resource.setProject(projectName);
    resource.setProvider(token[0]);
    resource.setFactory(token[1]);
    resource.setParameters(parameters.toString());
    resource.setCredentials(credentials == null ? "{}" : credentials.toString());
    if (originalResource == null || !originalResource.hasName())
      onCreate(resource);
    else
      onUpdate(resource);
  }

  private void onUpdate(final ResourceReferenceDto resource) {
    ResourceRequestBuilderFactory.newBuilder()
        .forResource(UriBuilders.PROJECT_RESOURCE.create().build(projectName, originalResource.getName()))
        .withResourceBody(ResourceReferenceDto.stringify(resource))
        .withCallback(new ResponseCodeCallback() {
          @Override
          public void onResponseCode(Request request, Response response) {
            if (response.getStatusCode() == Response.SC_OK) {
              getView().hideDialog();
              fireEvent(new ResourceUpdatedEvent(projectName, resource.getName()));
            }
          }
        }, Response.SC_OK, Response.SC_INTERNAL_SERVER_ERROR, Response.SC_FORBIDDEN, Response.SC_BAD_REQUEST)
        .put().send();
  }

  private void onCreate(final ResourceReferenceDto resource) {
    ResourceRequestBuilderFactory.newBuilder()
        .forResource(UriBuilders.PROJECT_RESOURCES.create().build(projectName))
        .withResourceBody(ResourceReferenceDto.stringify(resource))
        .withCallback(new ResponseCodeCallback() {
          @Override
          public void onResponseCode(Request request, Response response) {
            if (response.getStatusCode() == Response.SC_CREATED) {
              getView().hideDialog();
              ResourceCreatedEvent event = new ResourceCreatedEvent(projectName, resource.getName());
              fireEvent(event);
              if (resourceCreatedHandler != null)
                resourceCreatedHandler.onResourceCreated(event);
            }
          }
        }, Response.SC_CREATED, Response.SC_INTERNAL_SERVER_ERROR, Response.SC_FORBIDDEN, Response.SC_BAD_REQUEST)
        .post().send();
  }

  public void setResourceCreatedHandler(ResourceCreatedEvent.ResourceCreatedHandler resourceCreatedHandler) {
    this.resourceCreatedHandler = resourceCreatedHandler;
  }

  public interface Display extends PopupView, HasUiHandlers<ProjectResourceModalUiHandlers> {

    void initialize(ResourceReferenceDto resource, boolean readOnly);

    void hideDialog();
  }
}
