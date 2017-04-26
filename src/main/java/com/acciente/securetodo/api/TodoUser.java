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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect
public class TodoUser {
   private final String email;

   @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)   // don't serialize the password
   private final char[] password;

   private TodoUser() {
      email = null;
      password = null;
   }

   public TodoUser(String email) {
      this.email = email;
      password = null;
   }

   public TodoUser(String email, char[] password) {
      this.email = email;
      this.password = password;
   }

   public String getEmail() {
      return email;
   }


   public char[] getPassword() {
      return password;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      TodoUser todoUser = (TodoUser) o;

      return email != null ? email.equals(todoUser.email) : todoUser.email == null;
   }

   @Override
   public int hashCode() {
      return email != null ? email.hashCode() : 0;
   }
}
