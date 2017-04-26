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

import com.acciente.oacc.AccessControlContext;
import com.acciente.oacc.NotAuthorizedException;
import com.acciente.securetodo.api.TodoItem;
import com.acciente.securetodo.auth.OaccBasicAuthenticator;
import com.acciente.securetodo.auth.OaccPrincipal;
import com.acciente.securetodo.core.TodoItemService;
import com.acciente.securetodo.resources.exceptions.AuthorizationExceptionMapper;
import com.acciente.securetodo.resources.exceptions.IllegalArgumentExceptionMapper;
import com.acciente.securetodo.resources.exceptions.InvalidCredentialsExceptionMapper;
import com.acciente.securetodo.resources.exceptions.NotAuthenticatedExceptionMapper;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class TodoItemResourceTest {
   private static final String EMAIL    = "tester@oaccframework.org";
   private static final String PASSWORD = "secret";

   private static final Meter                  meter                  = mock(Meter.class);
   private static final MetricRegistry         environment            = mock(MetricRegistry.class);
   private static final TodoItemService        todoItemService        = mock(TodoItemService.class);
   private static final OaccBasicAuthenticator oaccBasicAuthenticator = mock(OaccBasicAuthenticator.class);
   private static final OaccPrincipal          oaccPrincipal          = mock(OaccPrincipal.class);
   private static final AccessControlContext   oacc                   = mock(AccessControlContext.class);

   static {
      // need to init the environment mock **BEFORE** setting up the ResourceTestRule as a class rule
      when(environment.meter(anyString())).thenReturn(meter);
   }

   @ClassRule
   public static final ResourceTestRule resources = ResourceTestRule.builder()
         .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
         .addProvider(new AuthDynamicFeature(new BasicCredentialAuthFilter.Builder<OaccPrincipal>()
                                                   .setAuthenticator(oaccBasicAuthenticator)
                                                   .setRealm("OACC Basic Authentication")
                                                   .buildAuthFilter()))
         .addProvider(new AuthValueFactoryProvider.Binder<>(OaccPrincipal.class))
         .addProvider(new IllegalArgumentExceptionMapper(environment))
         .addProvider(new NotAuthenticatedExceptionMapper(environment))
         .addProvider(new InvalidCredentialsExceptionMapper(environment))
         .addProvider(new AuthorizationExceptionMapper(environment))
         .addResource(new TodoItemResource(todoItemService))
         .build();

   @Before
   public void setup() {
   }

   @After
   public void tearDown() {
      // we have to reset the mock after each test because of the @ClassRule's injected mocks
      reset(todoItemService, oaccBasicAuthenticator, oaccPrincipal, oacc);
   }

   @Test
   public void postNewTodoItem() throws UnsupportedEncodingException, AuthenticationException {
      final String newTodoItem = fixture("fixtures/todoItem_new.json");
      final TodoItem expectedTodoItem = new TodoItem(1, "make new todo items", true);
      when(oaccBasicAuthenticator.authenticate(any(BasicCredentials.class)))
            .thenReturn(java.util.Optional.ofNullable(oaccPrincipal));
      when(oaccPrincipal.getAccessControlContext()).thenReturn(oacc);
      when(todoItemService.createItem(eq(oacc), any(TodoItem.class)))
            .thenReturn(expectedTodoItem);

      final Response response = resources.getJerseyTest()
            .target("/todos")
            .request()
            .header(Header.Authorization.name(), getBasicAuthHeader(EMAIL, PASSWORD))
            .post(Entity.entity(newTodoItem, MediaType.APPLICATION_JSON));

      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());   // 200 OK
      assertThat(response.readEntity(TodoItem.class)).isEqualTo(expectedTodoItem);
   }

   @Test
   public void postNewTodoItemWithoutAuthentication() {
      final String todoItem = fixture("fixtures/todoItem_new.json");

      final Response response = resources.getJerseyTest().target("/todos").request().post(Entity.entity(todoItem, MediaType.APPLICATION_JSON));

      assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());   // 401 Unauthorized
      verifyZeroInteractions(oaccBasicAuthenticator);
      verifyZeroInteractions(todoItemService);
   }

   @Test
   public void postNewTodoItemThatFailsValidation() throws AuthenticationException, UnsupportedEncodingException {
      final String blankTodoItem = fixture("fixtures/todoItem_blank.json");
      when(oaccBasicAuthenticator.authenticate(any(BasicCredentials.class)))
            .thenReturn(java.util.Optional.ofNullable(oaccPrincipal));
      when(oaccPrincipal.getAccessControlContext()).thenReturn(oacc);
      when(todoItemService.createItem(eq(oacc), any(TodoItem.class)))
            .thenThrow(new IllegalArgumentException("Either title or completed (or both) is required"));

      final Response response = resources.getJerseyTest()
            .target("/todos")
            .request()
            .header(Header.Authorization.name(), getBasicAuthHeader(EMAIL, PASSWORD))
            .post(Entity.entity(blankTodoItem, MediaType.APPLICATION_JSON));

      assertThat(response.getStatus()).isEqualTo(422);   // 422 Unprocessable Entity
      verifyZeroInteractions(oacc);
   }

   @Test
   public void getTodoItems() throws AuthenticationException, UnsupportedEncodingException {
      final List<TodoItem> expectedTodoItems
            = Collections.singletonList(new TodoItem(1, "list all items", true));

      when(oaccBasicAuthenticator.authenticate(any(BasicCredentials.class)))
            .thenReturn(java.util.Optional.ofNullable(oaccPrincipal));
      when(oaccPrincipal.getAccessControlContext()).thenReturn(oacc);
      when(todoItemService.findByAuthenticatedUser(eq(oacc)))
            .thenReturn(expectedTodoItems);

      final Response response = resources.getJerseyTest()
            .target("/todos")
            .request()
            .header(Header.Authorization.name(), getBasicAuthHeader(EMAIL, PASSWORD))
            .get();

      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());   // 200 OK
   }

   @Test
   public void getTodoItemsWithoutAuthentication() {
      final Response response = resources.getJerseyTest().target("/todos").request().get();

      assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());   // 401 Unauthorized
      verifyZeroInteractions(oaccBasicAuthenticator);
      verifyZeroInteractions(todoItemService);
   }

   @Test
   public void patchTodoItem() throws AuthenticationException, UnsupportedEncodingException {
      final long todoItemId = 1;
      final String todoItem = fixture("fixtures/todoItem_new.json");
      final TodoItem expectedTodoItem = new TodoItem(1, "update titles", false);
      when(oaccBasicAuthenticator.authenticate(any(BasicCredentials.class)))
            .thenReturn(java.util.Optional.ofNullable(oaccPrincipal));
      when(oaccPrincipal.getAccessControlContext()).thenReturn(oacc);
      when(todoItemService.updateItem(eq(oacc), any(Long.class), any(TodoItem.class)))
            .thenReturn(expectedTodoItem);

      final Response response = resources.getJerseyTest()
            .target("/todos/" + todoItemId)
            .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true) // to support PATCH
            .request()
            .header(Header.Authorization.name(), getBasicAuthHeader(EMAIL, PASSWORD))
            .build("PATCH", Entity.entity(todoItem, MediaType.APPLICATION_JSON))
            .invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());   // 200 OK
      assertThat(response.readEntity(TodoItem.class)).isEqualTo(expectedTodoItem);
   }

   @Test
   public void patchTodoItemWithoutAuthentication() {
      final long todoItemId = 1;
      final String todoItem = fixture("fixtures/todoItem_new.json");

      final Response response = resources.getJerseyTest()
            .target("/todos/" + todoItemId)
            .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true) // to support PATCH
            .request()
            .build("PATCH", Entity.entity(todoItem, MediaType.APPLICATION_JSON))
            .invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());   // 401 Unauthorized
      verifyZeroInteractions(oaccBasicAuthenticator);
      verifyZeroInteractions(todoItemService);
   }

   @Test
   public void patchTodoItemWithoutAuthorization() throws AuthenticationException, UnsupportedEncodingException {
      final long todoItemId = 1;
      final String todoItem = fixture("fixtures/todoItem_new.json");
      when(oaccBasicAuthenticator.authenticate(any(BasicCredentials.class)))
            .thenReturn(java.util.Optional.ofNullable(oaccPrincipal));
      when(oaccPrincipal.getAccessControlContext()).thenReturn(oacc);
      when(todoItemService.updateItem(eq(oacc), any(Long.class), any(TodoItem.class)))
            .thenThrow(new NotAuthorizedException("not authorized"));

      final Response response = resources.getJerseyTest()
            .target("/todos/" + todoItemId)
            .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true) // to support PATCH
            .request()
            .header(Header.Authorization.name(), getBasicAuthHeader(EMAIL, PASSWORD))
            .build("PATCH", Entity.entity(todoItem, MediaType.APPLICATION_JSON))
            .invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());   // 403 Forbidden
   }

   @Test
   public void patchTodoItemThatDoesNotExist() throws AuthenticationException, UnsupportedEncodingException {
      final long todoItemId = 1;
      final String todoItem = fixture("fixtures/todoItem_new.json");
      when(oaccBasicAuthenticator.authenticate(any(BasicCredentials.class)))
            .thenReturn(java.util.Optional.ofNullable(oaccPrincipal));
      when(oaccPrincipal.getAccessControlContext()).thenReturn(oacc);
      when(todoItemService.updateItem(eq(oacc), any(Long.class), any(TodoItem.class)))
            .thenThrow(new IllegalArgumentException("Resource " + todoItemId + " not found!"));

      final Response response = resources.getJerseyTest()
            .target("/todos/" + todoItemId)
            .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true) // to support PATCH
            .request()
            .header(Header.Authorization.name(), getBasicAuthHeader(EMAIL, PASSWORD))
            .build("PATCH", Entity.entity(todoItem, MediaType.APPLICATION_JSON))
            .invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());   // 404 Not Found
      verifyZeroInteractions(oacc);
   }

   @Test
   public void patchTodoItemThatFailsValidation() throws AuthenticationException, UnsupportedEncodingException {
      final long todoItemId = 1;
      final String blankTodoItem = fixture("fixtures/todoItem_blank.json");
      when(oaccBasicAuthenticator.authenticate(any(BasicCredentials.class)))
            .thenReturn(java.util.Optional.ofNullable(oaccPrincipal));
      when(oaccPrincipal.getAccessControlContext()).thenReturn(oacc);
      when(todoItemService.updateItem(eq(oacc), any(Long.class), any(TodoItem.class)))
            .thenThrow(new IllegalArgumentException("Either title or completed (or both) is required."));

      final Response response = resources.getJerseyTest()
            .target("/todos/" + todoItemId)
            .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true) // to support PATCH
            .request()
            .header(Header.Authorization.name(), getBasicAuthHeader(EMAIL, PASSWORD))
            .build("PATCH", Entity.entity(blankTodoItem, MediaType.APPLICATION_JSON))
            .invoke();

      assertThat(response.getStatus()).isEqualTo(422);   // 422 Unprocessable Entity
      verifyZeroInteractions(oacc);
   }

   @Test
   public void putShareAssociationOnTodoItem() throws AuthenticationException, UnsupportedEncodingException {
      final long todoItemId = 1;
      final String targetEmail = "target@oaccframework.org";
      when(oaccBasicAuthenticator.authenticate(any(BasicCredentials.class)))
            .thenReturn(java.util.Optional.ofNullable(oaccPrincipal));
      when(oaccPrincipal.getAccessControlContext()).thenReturn(oacc);

      final Response response = resources.getJerseyTest()
            .target("/todos/" + todoItemId)
            .queryParam("share_with", targetEmail)
            .request()
            .header(Header.Authorization.name(), getBasicAuthHeader(EMAIL, PASSWORD))
            .put(Entity.json(""));

      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());   // 204 No Content
      assertThat(response.readEntity(Object.class)).isNull();
   }

   @Test
   public void putShareAssociationOnTodoItemWithoutAuthentication() {
      final long todoItemId = 1;
      final String targetEmail = "target@oaccframework.org";
      final Response response = resources.getJerseyTest()
            .target("/todos/" + todoItemId)
            .queryParam("share_with", targetEmail)
            .request()
            .put(Entity.json(""));

      assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());   // 401 Unauthorized
      verifyZeroInteractions(oaccBasicAuthenticator);
      verifyZeroInteractions(todoItemService);
   }

   @Test
   public void putShareAssociationOnTodoItemWithoutAuthorization() throws AuthenticationException, UnsupportedEncodingException {
      final long todoItemId = 1;
      final String targetEmail = "target@oaccframework.org";
      when(oaccBasicAuthenticator.authenticate(any(BasicCredentials.class)))
            .thenReturn(java.util.Optional.ofNullable(oaccPrincipal));
      when(oaccPrincipal.getAccessControlContext()).thenReturn(oacc);
      doThrow(new NotAuthorizedException("not authorized"))
            .when(todoItemService).shareItem(eq(oacc), eq(todoItemId), eq(targetEmail));

      final Response response = resources.getJerseyTest()
            .target("/todos/" + todoItemId)
            .queryParam("share_with", targetEmail)
            .request()
            .header(Header.Authorization.name(), getBasicAuthHeader(EMAIL, PASSWORD))
            .put(Entity.json(""));

      assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());   // 403 Forbidden
      verifyZeroInteractions(oacc);
   }

   @Test
   public void putShareAssociationOnTodoItemOrUserThatDoesNotExist() throws AuthenticationException, UnsupportedEncodingException {
      final long todoItemId = 1;
      final String targetEmail = "target@oaccframework.org";
      when(oaccBasicAuthenticator.authenticate(any(BasicCredentials.class)))
            .thenReturn(java.util.Optional.ofNullable(oaccPrincipal));
      when(oaccPrincipal.getAccessControlContext()).thenReturn(oacc);
      doThrow(new IllegalArgumentException("Resource " + todoItemId + " not found!"))
            .when(todoItemService).shareItem(eq(oacc), eq(todoItemId), eq(targetEmail));

      final Response response = resources.getJerseyTest()
            .target("/todos/" + todoItemId)
            .queryParam("share_with", targetEmail)
            .request()
            .header(Header.Authorization.name(), getBasicAuthHeader(EMAIL, PASSWORD))
            .put(Entity.json(""));

      assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());   // 404 Not Found
      verifyZeroInteractions(oacc);
   }

   private static String getBasicAuthHeader(String username, String password) throws UnsupportedEncodingException {
      return "Basic " + DatatypeConverter.printBase64Binary((username + ":" + password).getBytes("UTF-8"));
   }
}
