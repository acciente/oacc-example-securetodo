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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class SecureTodoConfiguration extends Configuration {
   @Valid
   @NotNull
   private DataSourceFactory oaccdb = new DataSourceFactory();

   @Valid
   @NotNull
   private DataSourceFactory tododb = new DataSourceFactory();

   @Valid
   @NotNull
   private AccessControlContextFactory oaccFactory = new AccessControlContextFactory();

   @JsonProperty("oaccDb")
   public void setOaccDbDataSourceFactory(DataSourceFactory factory) {
      this.oaccdb = factory;
   }

   @JsonProperty("oaccDb")
   public DataSourceFactory getOaccDbDataSourceFactory() {
      return oaccdb;
   }

   @JsonProperty("todoDb")
   public void setTodoDbDataSourceFactory(DataSourceFactory factory) {
      this.tododb = factory;
   }

   @JsonProperty("todoDb")
   public DataSourceFactory getTodoDbDataSourceFactory() {
      return tododb;
   }

   @JsonProperty("oacc")
   public void setAccessControlContextFactory(AccessControlContextFactory factory) {
      this.oaccFactory = factory;
   }

   @JsonProperty("oacc")
   public AccessControlContextFactory getAccessControlContextFactory() {
      return oaccFactory;
   }
}
