/*
 * Copyright 2016 - 2017, Acciente LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acciente.securetodo;

import com.acciente.securetodo.auth.OaccBasicAuthenticator;
import com.acciente.securetodo.auth.OaccPrincipal;
import com.acciente.securetodo.core.TodoItemService;
import com.acciente.securetodo.core.TodoUserService;
import com.acciente.securetodo.db.TodoItemDAO;
import com.acciente.securetodo.db.TodoUserDAO;
import com.acciente.securetodo.resources.TodoItemResource;
import com.acciente.securetodo.resources.TodoUserResource;
import com.acciente.securetodo.resources.exceptions.AuthorizationExceptionMapper;
import com.acciente.securetodo.resources.exceptions.IllegalArgumentExceptionMapper;
import com.acciente.securetodo.resources.exceptions.InvalidCredentialsExceptionMapper;
import com.acciente.securetodo.resources.exceptions.NotAuthenticatedExceptionMapper;
import io.dropwizard.Application;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.DatabaseConfiguration;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.migrations.CloseableLiquibase;
import io.dropwizard.migrations.CloseableLiquibaseWithClassPathMigrationsFile;
import io.dropwizard.migrations.DbCommand;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.skife.jdbi.v2.DBI;

public class SecureTodoApplication extends Application<SecureTodoConfiguration> {

   public static void main(final String[] args) throws Exception {
      new SecureTodoApplication().run(args);
   }

   @Override
   public String getName() {
      return "SecureTodo";
   }

   @Override
   public void initialize(final Bootstrap<SecureTodoConfiguration> bootstrap) {
      bootstrap.addBundle(new InitializingMigrationsBundle<SecureTodoConfiguration>() {
         @Override
         public DataSourceFactory getDataSourceFactory(SecureTodoConfiguration configuration) {
            return configuration.getOaccDbDataSourceFactory();
         }

         @Override
         public String name() {
            return "oaccdb";
         }

         @Override
         public String getMigrationsFileName() {
            return "migrations_oaccdb.xml";
         }
      });

      bootstrap.addBundle(new InitializingMigrationsBundle<SecureTodoConfiguration>() {
         @Override
         public DataSourceFactory getDataSourceFactory(SecureTodoConfiguration configuration) {
            return configuration.getTodoDbDataSourceFactory();
         }

         @Override
         public String name() {
            return "tododb";
         }

         @Override
         public String getMigrationsFileName() {
            return "migrations_tododb.xml";
         }
      });
   }

   @Override
   public void run(final SecureTodoConfiguration configuration,
                   final Environment environment) {
      final DBIFactory dbiFactory = new DBIFactory();
      final DBI todoJdbi = dbiFactory.build(environment, configuration.getTodoDbDataSourceFactory(), "todoDb");
      final TodoUserDAO todoUserDao = todoJdbi.onDemand(TodoUserDAO.class);
      final TodoItemDAO todoItemDao = todoJdbi.onDemand(TodoItemDAO.class);

      final AccessControlContextFactory accessControlContextFactory = configuration.getAccessControlContextFactory();
      accessControlContextFactory.initialize(environment, configuration.getOaccDbDataSourceFactory(), "oacc");

      environment.jersey().register(new TodoUserResource(new TodoUserService(todoUserDao, accessControlContextFactory)));
      environment.jersey().register(new TodoItemResource(new TodoItemService(todoItemDao)));

      environment.jersey().register(new AuthDynamicFeature(
            new BasicCredentialAuthFilter.Builder<OaccPrincipal>()
                  .setAuthenticator(new OaccBasicAuthenticator(accessControlContextFactory))
                  .setRealm("OACC Basic Authentication")
                  .buildAuthFilter()));
      // to use @Auth to inject a custom Principal type into a resource:
      environment.jersey().register(new AuthValueFactoryProvider.Binder<>(OaccPrincipal.class));

      environment.jersey().register(new AuthorizationExceptionMapper(environment.metrics()));
      environment.jersey().register(new IllegalArgumentExceptionMapper(environment.metrics()));
      environment.jersey().register(new InvalidCredentialsExceptionMapper(environment.metrics()));
      environment.jersey().register(new NotAuthenticatedExceptionMapper(environment.metrics()));
   }

   /*
    * Custom migrations bundle implementation that runs the respective liquibase migration.
    * Based on io.dropwizard.migrations.MigrationsBundle
    */
   private abstract class InitializingMigrationsBundle<T extends SecureTodoConfiguration> implements ConfiguredBundle<T>, DatabaseConfiguration<T> {
      private static final String DEFAULT_NAME = "db";
      private static final String DEFAULT_MIGRATIONS_FILE = "migrations.xml";

      @Override
      @SuppressWarnings("unchecked")
      public final void initialize(Bootstrap<?> bootstrap) {
         final Class<T> klass = (Class<T>) bootstrap.getApplication().getConfigurationClass();
         bootstrap.addCommand(new DbCommand<>(name(), this, klass, getMigrationsFileName()));
      }

      public String getMigrationsFileName() {
         return DEFAULT_MIGRATIONS_FILE;
      }

      public String name() {
         return DEFAULT_NAME;
      }

      @Override
      public void run(T configuration, Environment environment) throws Exception {
         CloseableLiquibase liquibase
               = new CloseableLiquibaseWithClassPathMigrationsFile(getDataSourceFactory(configuration)
                                                                         .build(environment.metrics(), name()),
                                                                   getMigrationsFileName());
         liquibase.update("");
      }
   }
}
