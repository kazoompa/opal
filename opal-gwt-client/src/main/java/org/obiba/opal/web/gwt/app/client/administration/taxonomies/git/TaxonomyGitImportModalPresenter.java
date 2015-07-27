package org.obiba.opal.web.gwt.app.client.administration.taxonomies.git;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.obiba.opal.web.gwt.app.client.administration.taxonomies.event.TaxonomyImportedEvent;
import org.obiba.opal.web.gwt.app.client.presenter.ModalPresenterWidget;
import org.obiba.opal.web.gwt.app.client.validator.FieldValidator;
import org.obiba.opal.web.gwt.app.client.validator.RequiredTextValidator;
import org.obiba.opal.web.gwt.app.client.validator.ValidationHandler;
import org.obiba.opal.web.gwt.app.client.validator.ViewValidationHandler;
import org.obiba.opal.web.gwt.rest.client.ResourceRequestBuilderFactory;
import org.obiba.opal.web.gwt.rest.client.ResponseCodeCallback;
import org.obiba.opal.web.gwt.rest.client.UriBuilders;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.ui.HasText;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.PopupView;

public class TaxonomyGitImportModalPresenter extends ModalPresenterWidget<TaxonomyGitImportModalPresenter.Display>
    implements TaxonomyGitImportModalUiHandlers {

  protected ValidationHandler validationHandler;

  @Inject
  public TaxonomyGitImportModalPresenter(EventBus eventBus, Display display) {
    super(eventBus, display);
    getView().setUiHandlers(this);
  }

  @Override
  public void onBind() {
    validationHandler = new TaxonomyGitImportValidationHandler();
  }

  @Override
  public void onImport(String name, String repository, String reference, String file) {
    if(!validationHandler.validate()) return;
    
    ResourceRequestBuilderFactory.newBuilder()
        .forResource(
            UriBuilders.SYSTEM_CONF_TAXONOMIES_IMPORT_GITHUB.create().query("user", name).query("repo", repository)
                .query("ref", reference).query("file", file).build())
        .withCallback(new ResponseCodeCallback() {
          @Override
          public void onResponseCode(Request request, Response response) {
            getView().hideDialog();
            getEventBus().fireEvent(new TaxonomyImportedEvent());
          }
        }, Response.SC_OK, Response.SC_CREATED) //
        .withCallback(new ResponseCodeCallback() {
          @Override
          public void onResponseCode(Request request, Response response) {
            if(response.getText() != null && response.getText().length() != 0) {
              getView().showError(null, response.getText());
            } else {
              getView().showError("TaxonomyGitImportFailed");
            }

          }
        }, Response.SC_BAD_REQUEST, Response.SC_INTERNAL_SERVER_ERROR) //
        .post().send();
  }

  public interface Display extends PopupView, HasUiHandlers<TaxonomyGitImportModalUiHandlers> {
    HasText getUser();
    HasText getRepository();

    enum FormField {
      USER, REPOSITORY
    }

    void hideDialog();

    void showError(String messageKey);

    void showError(@Nullable FormField formField, String message);

    void clearErrors();
  }

  private class TaxonomyGitImportValidationHandler extends ViewValidationHandler {

    private Set<FieldValidator> validators;

    @Override
    protected Set<FieldValidator> getValidators() {
      if(validators != null) {
        return validators;
      }

      validators = new LinkedHashSet<FieldValidator>();
      validators.add(new RequiredTextValidator(getView().getUser(), "TaxonomyGitUserRequired",
          Display.FormField.USER.name()));
      validators.add(new RequiredTextValidator(getView().getRepository(), "TaxonomyGitRepositoryRequired", Display.FormField.REPOSITORY.name()));
      return validators;
    }

    @Override
    protected void showMessage(String id, String message) {
      getView().showError(Display.FormField.valueOf(id), message);
    }
  }
}