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

package com.acciente.securetodo.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import org.junit.Test;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;

public class TodoUserTest {
   private static final ObjectMapper MAPPER                  = Jackson.newObjectMapper();
   private static final String       EMAIL_VALUE             = "tester@oaccframework.org";
   private static final String       PASSWORD_VALUE          = "secret";
   private static final char[]       PASSWORD_VALUE_AS_CHARS = PASSWORD_VALUE.toCharArray();

   @Test
   public void equalsIgnoringPassword() throws Exception {
      final TodoUser todoUser_withPwd = new TodoUser(EMAIL_VALUE, PASSWORD_VALUE_AS_CHARS);
      final TodoUser todoUser_withoutPwd = new TodoUser(EMAIL_VALUE);

      assertThat(todoUser_withPwd.getPassword()).isNotNull();
      assertThat(todoUser_withoutPwd.getPassword()).isNull();
      assertThat(todoUser_withPwd).isEqualTo(todoUser_withoutPwd);
   }

   @Test
   public void equalsIsCaseSensitive() throws Exception {
      final TodoUser todoUserA = new TodoUser(EMAIL_VALUE.toLowerCase());
      final TodoUser todoUserB = new TodoUser(EMAIL_VALUE.toUpperCase());

      assertThat(todoUserA).isNotEqualTo(todoUserB);
   }

   @Test
   public void serializeToJson() throws Exception {
      final TodoUser todoUser = new TodoUser(EMAIL_VALUE, PASSWORD_VALUE_AS_CHARS);
      final String serializedTodoUser = MAPPER.writeValueAsString(todoUser);

      // note that writeValueAsString() would strip password from the expected json anyway
      // so this test only asserts that the non-ignored fields were serialized correctly
      final String expectedSerialization = MAPPER.writeValueAsString(MAPPER.readValue(fixture("fixtures/todoUser_noPwd.json"), TodoUser.class));

      assertThat(serializedTodoUser).isEqualTo(expectedSerialization);
   }

   @Test
   public void serializeToJsonIgnoresPassword() throws Exception {
      final TodoUser todoUser = new TodoUser(EMAIL_VALUE, PASSWORD_VALUE_AS_CHARS);
      final String serializedTodoUser = MAPPER.writeValueAsString(todoUser);

      assertThat(serializedTodoUser).doesNotContain("password");
      assertThat(serializedTodoUser).doesNotContain(PASSWORD_VALUE);
   }

   @Test
   public void deserializeFromJson() throws Exception {
      final TodoUser expectedTodoUser = new TodoUser(EMAIL_VALUE, PASSWORD_VALUE_AS_CHARS);
      final TodoUser deserializedTodoUser = MAPPER.readValue(fixture("fixtures/todoUser.json"), TodoUser.class);

      assertThat(deserializedTodoUser).isEqualTo(expectedTodoUser);
   }

   @Test
   public void deserializeFromJsonIncludesPassword() throws Exception {
      final TodoUser expectedTodoUser = new TodoUser(EMAIL_VALUE, PASSWORD_VALUE_AS_CHARS);
      final TodoUser deserializedTodoUser = MAPPER.readValue(fixture("fixtures/todoUser.json"), TodoUser.class);

      assertThat(deserializedTodoUser.getPassword()).isNotNull();
      assertThat(deserializedTodoUser.getPassword()).isEqualTo(expectedTodoUser.getPassword());
   }
}
