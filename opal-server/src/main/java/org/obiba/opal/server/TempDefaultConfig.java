package org.obiba.opal.server;

import org.obiba.magma.Datasource;
import org.obiba.magma.MagmaEngine;
import org.obiba.magma.datasource.hibernate.support.HibernateDatasourceFactory;
import org.obiba.opal.core.cfg.OpalConfiguration;
import org.obiba.opal.core.cfg.OpalConfigurationService;
import org.obiba.opal.core.domain.database.Database;
import org.obiba.opal.core.domain.database.SqlDatabase;
import org.obiba.opal.core.runtime.database.DatabaseRegistry;
import org.obiba.opal.core.runtime.jdbc.DatabaseSessionFactoryProvider;
import org.obiba.opal.project.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

//TODO to be deleted once UI is fully working
@Transactional
@Component
@Deprecated
public class TempDefaultConfig {

  public static final String USERNAME = "root";

  public static final String PASSWORD = "1234";

  @Autowired
  private DatabaseRegistry databaseRegistry;

  @Autowired
  private ProjectService projectService;

  @Autowired
  private OpalConfigurationService configService;

  public void createDefaultConfig() {

    if(databaseRegistry.getDatabase("opal-data") != null) return;

    System.out.println("Create default config...");

    SqlDatabase opalData = new SqlDatabase.Builder() //
        .name("opal-data") //
        .url("jdbc:mysql://localhost:3306/opal?characterEncoding=UTF-8") //
        .driverClass("com.mysql.jdbc.Driver") //
        .username(USERNAME) //
        .password(PASSWORD) //
        .editable(false) //
        .type(Database.Type.STORAGE) //
        .magmaDatasourceType(SqlDatabase.MAGMA_HIBERNATE_DATASOURCE) //
        .build();
    databaseRegistry.addOrReplaceDatabase(opalData);

    SqlDatabase opalKey = new SqlDatabase.Builder() //
        .name("opal-key") //
        .url("jdbc:mysql://localhost:3306/key?characterEncoding=UTF-8") //
        .driverClass("com.mysql.jdbc.Driver") //
        .username(USERNAME) //
        .password(PASSWORD) //
        .editable(false) //
        .usedForIdentifiers(true) //
        .type(Database.Type.STORAGE) //
        .magmaDatasourceType(SqlDatabase.MAGMA_HIBERNATE_DATASOURCE) //
        .build();
    databaseRegistry.addOrReplaceDatabase(opalKey);

    createDatasourceAndProject("opal-data", opalData);
    createDatasourceAndProject("opal-data2", opalData);
  }

  private void createDatasourceAndProject(String datasourceName, final SqlDatabase opalData) {
    final Datasource datasource = databaseRegistry.createStorageMagmaDatasource(datasourceName, opalData);
    MagmaEngine.get().addDatasource(datasource);
    configService.modifyConfiguration(new OpalConfigurationService.ConfigModificationTask() {

      @Override
      public void doWithConfig(OpalConfiguration config) {
        config.getMagmaEngineFactory().withFactory(new HibernateDatasourceFactory(datasource.getName(),
            new DatabaseSessionFactoryProvider(datasource.getName(), databaseRegistry, opalData.getName())));
      }
    });
    projectService.getOrCreateProject(datasource);
  }

}