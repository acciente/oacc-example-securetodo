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

package com.acciente.securetodo.resources;

import com.acciente.securetodo.api.TodoUser;
import com.acciente.securetodo.core.TodoUserService;
import com.acciente.securetodo.resources.exceptions.IllegalArgumentExceptionMapper;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TodoUserResourceTest {
   private static final String EMAIL             = "tester@oaccframework.org";
   private static final String PASSWORD          = "secret";
   private static final char[] PASSWORD_AS_CHARS = PASSWORD.toCharArray();

   private static final Meter           meter           = mock(Meter.class);
   private static final MetricRegistry  environment     = mock(MetricRegistry.class);
   private static final TodoUserService todoUserService = mock(TodoUserService.class);

   static {
      // need to init the environment mock **BEFORE** setting up the ResourceTestRule as a class rule
      when(environment.meter(anyString())).thenReturn(meter);
   }

   @ClassRule
   public static final ResourceTestRule resources = ResourceTestRule.builder()
         .addResource(new TodoUserResource(todoUserService))
         .addProvider(new IllegalArgumentExceptionMapper(environment))
         .build();

   @Before
   public void setup() {
   }

   @After
   public void tearDown() {
      // we have to reset the mock after each test because of the @ClassRule's injected mocks
      reset(todoUserService);
   }

   @Test
   public void postUserThatFailsValidation() {
      final String todoUser_noPwd = fixture("fixtures/todoUser_noPwd.json");
      when(todoUserService.createUser(any(TodoUser.class)))
            .thenThrow(new IllegalArgumentException("Password is required."));

      final Response response = resources.client().target("/users").request().post(Entity.entity(todoUser_noPwd, MediaType.APPLICATION_JSON));

      assertThat(response.getStatus()).isEqualTo(422);   // 422 Unprocessable Entity
   }

   @Test
   public void postNewUser() throws IOException {
      final String postedTodoUserJson = fixture("fixtures/todoUser.json");
      final TodoUser expectedTodoUser = new TodoUser(EMAIL);
      when(todoUserService.createUser(any(TodoUser.class))).thenReturn(expectedTodoUser);

      final Response response = resources.client().target("/users")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.entity(postedTodoUserJson, MediaType.APPLICATION_JSON));

      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      final TodoUser responseTodoUser = response.readEntity(TodoUser.class);
      assertThat(responseTodoUser).isEqualTo(expectedTodoUser);
      assertThat(responseTodoUser.getPassword()).isNull();
      verify(todoUserService).createUser(new TodoUser(EMAIL, PASSWORD_AS_CHARS));
   }

   @Test
   public void postNewUserWithMixedCaseEmail() throws IOException {
      final String postedTodoUserJson = fixture("fixtures/todoUser_mixedCaseEmail.json");
      final String email_lowercase = EMAIL.toLowerCase();
      final TodoUser expectedTodoUser = new TodoUser(email_lowercase);
      when(todoUserService.createUser(any(TodoUser.class))).thenReturn(expectedTodoUser);

      final Response response = resources.client().target("/users")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.entity(postedTodoUserJson, MediaType.APPLICATION_JSON));

      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      final TodoUser responseTodoUser = response.readEntity(TodoUser.class);
      assertThat(responseTodoUser).isEqualTo(expectedTodoUser);
      assertThat(responseTodoUser.getPassword()).isNull();
      final ArgumentCaptor<TodoUser> todoUserCaptor = ArgumentCaptor.forClass(TodoUser.class);
      verify(todoUserService).createUser(todoUserCaptor.capture());
      final TodoUser todoUserArg = todoUserCaptor.getValue();
      assertThat(todoUserArg).isNotNull();
      assertThat(todoUserArg.getEmail()).isNotEqualTo(email_lowercase);
      assertThat(todoUserArg.getEmail()).isEqualToIgnoringCase(EMAIL);
   }

   @Test
   public void postExistingUser() throws IOException {
      final String postedTodoUserJson = fixture("fixtures/todoUser.json");
      when(todoUserService.createUser(any(TodoUser.class)))
            .thenThrow(new IllegalArgumentException("External id is not unique"));

      final Response response = resources.client().target("/users")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.entity(postedTodoUserJson, MediaType.APPLICATION_JSON));

      assertThat(response.getStatus()).isEqualTo(422);   // 422 Unprocessable Entity
   }
}
