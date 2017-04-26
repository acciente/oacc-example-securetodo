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

public class TodoItemTest {
   private static final ObjectMapper MAPPER          = Jackson.newObjectMapper();
   private static final int          ID_VALUE        = 1;
   private static final String       TITLE_VALUE     = "write tests";
   private static final boolean      COMPLETED_VALUE = true;
//   private static final String       URL_VALUE       = "/todos/" + ID_VALUE;

   @Test
   public void equals() throws Exception {
      final TodoItem todoItemA = new TodoItem(ID_VALUE, TITLE_VALUE, COMPLETED_VALUE);
      final TodoItem todoItemACopy = new TodoItem(todoItemA.getId(),
                                                  todoItemA.getTitle(),
                                                  todoItemA.getCompleted());
      final TodoItem todoItemB = new TodoItem(ID_VALUE + 1,
                                              TITLE_VALUE + " again",
                                              !COMPLETED_VALUE);

      assertThat(todoItemA).isEqualTo(todoItemACopy);
      assertThat(todoItemA).isNotEqualTo(todoItemB);
   }

   @Test
   public void equalsIsCaseSensitive() throws Exception {
      final TodoItem todoItemA = new TodoItem(ID_VALUE, TITLE_VALUE.toLowerCase(), COMPLETED_VALUE);
      final TodoItem todoItemB = new TodoItem(ID_VALUE, TITLE_VALUE.toUpperCase(), COMPLETED_VALUE);

      assertThat(todoItemA).isNotEqualTo(todoItemB);
   }

   @Test
   public void serializeToJson() throws Exception {
      final TodoItem todoItem = new TodoItem(ID_VALUE, TITLE_VALUE, COMPLETED_VALUE);
      final String serializedTodoItem = MAPPER.writeValueAsString(todoItem);
      final String expectedSerialization = MAPPER.writeValueAsString(MAPPER.readValue(fixture("fixtures/todoItem.json"), TodoItem.class));

      assertThat(serializedTodoItem).isEqualTo(expectedSerialization);
   }

   @Test
   public void deserializeFromJson() throws Exception {
      final TodoItem expectedTodoItem = new TodoItem(ID_VALUE, TITLE_VALUE, COMPLETED_VALUE);
      final TodoItem deserializedTodoItem = MAPPER.readValue(fixture("fixtures/todoItem.json"), TodoItem.class);

      assertThat(deserializedTodoItem).isEqualTo(expectedTodoItem);
   }

   @Test
   public void patchWithAnotherTodoItem() throws Exception {
      final TodoItem todoItemA = new TodoItem(ID_VALUE, TITLE_VALUE, COMPLETED_VALUE);
      final TodoItem todoItemB = new TodoItem(ID_VALUE + 1,
                                              TITLE_VALUE + " again",
                                              !COMPLETED_VALUE);
      final TodoItem expectedInstance = new TodoItem(ID_VALUE,
                                                     todoItemB.getTitle(),
                                                     todoItemB.getCompleted());

      final TodoItem patchedInstance = todoItemA.getPatchedInstance(todoItemB);

      assertThat(patchedInstance).isEqualTo(expectedInstance);
   }
}
