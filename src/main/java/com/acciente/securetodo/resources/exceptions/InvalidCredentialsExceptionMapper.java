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

package com.acciente.securetodo.resources.exceptions;

import com.acciente.oacc.InvalidCredentialsException;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import io.dropwizard.jersey.errors.ErrorMessage;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class InvalidCredentialsExceptionMapper implements ExceptionMapper<InvalidCredentialsException> {
   private final Meter exceptions;

   public InvalidCredentialsExceptionMapper(MetricRegistry metrics) {
      exceptions = metrics.meter(getClass().getCanonicalName() + " exceptions");
   }

   @Override
   public Response toResponse(InvalidCredentialsException e) {
      exceptions.mark();
      return Response
            .status(422) // UNPROCESSABLE_ENTITY
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(new ErrorMessage(422, e.getMessage()))
            .build();
   }
}