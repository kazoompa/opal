/*
 * Copyright (c) 2020 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.system.taxonomy;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

import org.obiba.opal.web.model.Opal;

public interface VocabulariesResource {

  void setTaxonomyName(String taxonomyName);

  @GET
  List<Opal.VocabularyDto> getVocabularies();

  @POST
  Response createVocabulary(Opal.VocabularyDto vocabulary);
}
