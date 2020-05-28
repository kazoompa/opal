/*
 * Copyright (c) 2020 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.gwt.app.client.ui;

import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.github.gwtbootstrap.client.ui.event.HideHandler;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.PopupView;
import com.gwtplatform.mvp.client.PopupViewCloseHandler;
import com.gwtplatform.mvp.client.ViewImpl;
import com.gwtplatform.mvp.client.proxy.NavigationEvent;
import com.gwtplatform.mvp.client.proxy.NavigationHandler;
import com.gwtplatform.mvp.client.view.PopupPositioner;

/**
 * A simple implementation of {@link com.gwtplatform.mvp.client.PopupView} that can be used when the widget
 * returned by {@link #asWidget()} inherits from {@link com.google.gwt.user.client.ui.PopupPanel}.
 * <p/>
 * Also, this implementation simply disregards every call to
 * {@link #setInSlot(Object, com.google.gwt.user.client.ui.IsWidget)}, {@link #addToSlot(Object,
 * com.google.gwt.user.client.ui.IsWidget)}, and
 * {@link #removeFromSlot(Object, com.google.gwt.user.client.ui.IsWidget)}.
 */
public abstract class ModalViewImpl extends ViewImpl implements PopupView {

  private HandlerRegistration autoHideHandler;

  private HandlerRegistration closeHandlerRegistration;

  private final EventBus eventBus;

  /**
   * The {@link ModalViewImpl} class uses the {@link com.google.web.bindery.event.shared.EventBus} to listen to
   * {@link com.gwtplatform.mvp.client.proxy.NavigationEvent} in order to automatically close when this event is
   * fired, if desired. See
   * {@link #setAutoHideOnNavigationEventEnabled(boolean)} for details.
   *
   * @param eventBus The {@link com.google.web.bindery.event.shared.EventBus}.
   */
  protected ModalViewImpl(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  protected EventBus getEventBus() {
    return eventBus;
  }

  @Override
  public void center() {
  }

  @Override
  public void hide() {
    asModal().hide();
  }

  public void addHideHandler(HideHandler handler) {
    asModal().addHideHandler(handler);
  }

  public void addHiddneHandler(HiddenHandler handler) {
    asModal().addHiddenHandler(handler);
  }

  @Override
  public void setAutoHideOnNavigationEventEnabled(boolean autoHide) {
    if(autoHide) {
      if(autoHideHandler != null) {
        return;
      }
      autoHideHandler = eventBus.addHandler(NavigationEvent.getType(), new NavigationHandler() {
        @Override
        public void onNavigation(NavigationEvent navigationEvent) {
          hide();
        }
      });
    } else {
      if(autoHideHandler != null) {
        autoHideHandler.removeHandler();
      }
    }
  }

  @Override
  public void setCloseHandler(final PopupViewCloseHandler popupViewCloseHandler) {
    if(closeHandlerRegistration != null) {
      closeHandlerRegistration.removeHandler();
    }
    if(popupViewCloseHandler == null) {
      closeHandlerRegistration = null;
    } else {
//            closeHandlerRegistration = asModal().addCloseHandler(new CloseHandler<PopupPanel>() {
//              @Override
//              public void onClose(CloseEvent<PopupPanel> event) {
//                popupViewCloseHandler.onClose();
//              }
//            });
    }
  }

  @Override
  public void setPosition(int left, int top) {
  }

  @Override
  public void setPopupPositioner(PopupPositioner popupPositioner) {

  }

  @Override
  public final void show() {
  }

  @Override
  public final void showAndReposition() {
    onShow();
    asModal().show();
  }

  /**
   * Called before the modal is shown.
   */
  public void onShow() {

  }

  /**
   * Retrieves this view as a {@link com.google.gwt.user.client.ui.PopupPanel}. See {@link #asWidget()}.
   *
   * @return This view as a {@link com.google.gwt.user.client.ui.PopupPanel} object.
   */
  protected Modal asModal() {
    return (Modal) asWidget();
  }
}
