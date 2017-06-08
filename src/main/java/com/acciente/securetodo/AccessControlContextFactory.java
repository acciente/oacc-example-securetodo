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

import com.acciente.oacc.AccessControlContext;
import com.acciente.oacc.encryptor.bcrypt.BCryptPasswordEncryptor;
import com.acciente.oacc.sql.SQLAccessControlContextFactory;
import com.acciente.oacc.sql.SQLProfile;
import com.acciente.securetodo.health.DataSourceHealthCheck;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import org.hibernate.validator.constraints.NotEmpty;

public class AccessControlContextFactory {
   @NotEmpty
   private String schemaName;

   @NotEmpty
   private String sqlProfile;

   private ManagedDataSource dataSource;
   private BCryptPasswordEncryptor bCryptPasswordEncryptor;

   @JsonProperty
   public String getSchemaName() {
      return schemaName;
   }

   @JsonProperty
   public void setSchemaName(String schemaName) {
      this.schemaName = schemaName;
   }

   @JsonProperty
   public String getSqlProfile() {
      return sqlProfile;
   }

   @JsonProperty
   public void setSqlProfile(String sqlProfile) {
      this.sqlProfile = sqlProfile;
   }

   public void initialize(Environment environment, PooledDataSourceFactory dataSourceFactory, String name) {
      dataSource = dataSourceFactory.build(environment.metrics(), name);
      bCryptPasswordEncryptor = BCryptPasswordEncryptor.newInstance(12);
      environment.lifecycle().manage(dataSource);
      environment.healthChecks().register(name,
                                          new DataSourceHealthCheck(environment.getHealthCheckExecutorService(),
                                                                    dataSourceFactory.getValidationQueryTimeout()
                                                                          .orElse(Duration.seconds(5)),
                                                                    dataSource,
                                                                    dataSourceFactory.getValidationQuery()));
   }

   public AccessControlContext build() {
      return SQLAccessControlContextFactory.getAccessControlContext(dataSource,
                                                                    getSchemaName(),
                                                                    SQLProfile.valueOf(getSqlProfile()),
                                                                    bCryptPasswordEncryptor);
   }
}
