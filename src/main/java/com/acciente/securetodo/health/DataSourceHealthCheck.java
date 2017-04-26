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

package com.acciente.securetodo.health;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.db.TimeBoundHealthCheck;
import io.dropwizard.util.Duration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;

public class DataSourceHealthCheck extends HealthCheck {
   private final DataSource           dataSource;
   private final String               validationQuery;
   private final TimeBoundHealthCheck timeBoundHealthCheck;

   public DataSourceHealthCheck(ExecutorService executorService,
                                Duration duration,
                                DataSource dataSource,
                                String validationQuery) {
      this.dataSource = dataSource;
      this.validationQuery = validationQuery;
      timeBoundHealthCheck = new TimeBoundHealthCheck(executorService, duration);
   }

   @Override
   protected Result check() throws Exception {
      return timeBoundHealthCheck.check(() -> {
         try (final Connection connection = dataSource.getConnection();
              final PreparedStatement statement = connection.prepareStatement(validationQuery);
              final ResultSet ignored = statement.executeQuery();) {
            return Result.healthy();
         }
      });
   }
}
