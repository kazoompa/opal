package org.obiba.opal.web.system.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.obiba.magma.datasource.mongodb.MongoDBDatasourceFactory;
import org.obiba.magma.datasource.mongodb.MongoDBFactory;
import org.obiba.opal.core.domain.database.Database;
import org.obiba.opal.core.domain.database.MongoDbSettings;
import org.obiba.opal.core.service.database.DatabaseRegistry;
import org.obiba.opal.core.service.database.MultipleIdentifiersDatabaseException;
import org.obiba.opal.core.service.database.NoSuchDatabaseException;
import org.obiba.opal.web.database.Dtos;
import org.obiba.opal.web.magma.ClientErrorDtos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mongodb.DB;
import com.wordnik.swagger.annotations.ApiOperation;

import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.obiba.opal.web.model.Database.DatabaseDto;

/**
 * Class is not transactional because of delete method
 */
@Component
@Scope("request")
@Path("/system/database/{name}")
public class DatabaseResource {

  @Autowired
  private DatabaseRegistry databaseRegistry;

  @PathParam("name")
  private String name;

  @GET
  public DatabaseDto get() {
    Database database = getDatabase();
    return Dtos.asDto(database, databaseRegistry.hasDatasource(database));
  }

  @DELETE
  public Response delete() {
    Database database = getDatabase();
    databaseRegistry.delete(database);
    return Response.ok().build();
  }

  @PUT
  public Response update(DatabaseDto dto) throws MultipleIdentifiersDatabaseException {
    Database database = Dtos.fromDto(dto);
    try {
      // Allow edition of all fields except database name
      database.setName(databaseRegistry.getDatabase(name).getName());
    } catch(NoSuchDatabaseException ignored) {
      // do nothing if it's a new database
    }
    databaseRegistry.update(database);
    return Response.ok().build();
  }

  @POST
  @Path("/connections")
  @Transactional(readOnly = true)
  public Response testConnection() {
    Database database = getDatabase();
    if(database.hasSqlSettings()) {
      return testSqlConnection();
    }
    if(database.hasMongoDbSettings()) {
      return testMongoConnection(database.getMongoDbSettings());
    }
    throw new RuntimeException("Connection test not yet implemented for database " + database.getClass());
  }

  @GET
  @Path("/hasEntities")
  @ApiOperation(value = "Returns true if the database has entities")
  public Response getHasEntities() {
    Database database = databaseRegistry.getDatabase(name);
    return Response.ok().entity(String.valueOf(databaseRegistry.hasEntities(database))).build();
  }

  private Database getDatabase() {
    return databaseRegistry.getDatabase(name);
  }

  private Response testSqlConnection() {
    try {
      JdbcOperations jdbcTemplate = new JdbcTemplate(databaseRegistry.getDataSource(name, null));
      return jdbcTemplate.execute(new ConnectionCallback<Response>() {
        @Override
        public Response doInConnection(Connection con) throws SQLException, DataAccessException {
          return con.isValid(1) ? Response.ok().build() : databaseConnectionFail();
        }
      });
    } catch(RuntimeException e) {
      return databaseConnectionFail();
    }
  }

  private Response testMongoConnection(MongoDbSettings mongoDbSettings) {
    try {
      MongoDBDatasourceFactory datasourceFactory = mongoDbSettings.createMongoDBDatasourceFactory("_test");
      List<String> dbs = datasourceFactory.getMongoDBFactory().getMongoClient().getDatabaseNames();
      if(dbs.contains(datasourceFactory.getMongoDbDatabaseName())) {
        return Response.ok().build();
      }
      return datasourceFactory.getMongoDBFactory().execute(new MongoDBFactory.MongoDBCallback<Response>() {
        @Override
        public Response doWithDB(DB db) {
          db.getCollection("_coll_test").drop();
          return Response.ok().build();
        }
      });
    } catch(RuntimeException e) {
      return databaseConnectionFail();
    }
  }

  private Response databaseConnectionFail() {
    return Response.status(SERVICE_UNAVAILABLE)
        .entity(ClientErrorDtos.getErrorMessage(SERVICE_UNAVAILABLE, "DatabaseConnectionFailed").build()).build();
  }

}
