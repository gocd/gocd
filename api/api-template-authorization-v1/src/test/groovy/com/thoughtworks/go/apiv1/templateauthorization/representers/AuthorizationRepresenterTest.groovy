/*
 * Copyright 2020 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.templateauthorization.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

class AuthorizationRepresenterTest {
  @Nested
  class Serialize {

    @Nested
    class allowGroupAdmins {
      @Test
      void "should represent an authorization with allowGroupAdmins details"() {
        def actualJson = toObjectString({
          AuthorizationRepresenter.toJSON(it, new Authorization())
        })

        assertThatJson(actualJson).isEqualTo(["all_group_admins_are_view_users": true])
      }
    }

    @Nested
    class View {

      @Test
      void "should represent an authorization with view users"() {
        def actualJson = toObjectString({
          AuthorizationRepresenter.toJSON(it, new Authorization(new ViewConfig(user("user1"), user("user2"))))
        })

        assertThatJson(actualJson).isEqualTo([
          "all_group_admins_are_view_users": true,
          "view": ["roles": [], "users": ["user1", "user2"]]])
      }

      @Test
      void "should represent an authorization with view roles"() {
        def actualJson = toObjectString({
          AuthorizationRepresenter.toJSON(it, new Authorization(new ViewConfig(role("role1"), role("role2"))))
        })

        assertThatJson(actualJson).isEqualTo([
          "all_group_admins_are_view_users": true,
          "view": ["roles": ["role1", "role2"], "users": []]])
      }

      @Test
      void "should represent an authorization with view roles and users"() {
        def actualJson = toObjectString({
          AuthorizationRepresenter.toJSON(it, new Authorization(new ViewConfig(role("role1"), user("user1"))))
        })

        assertThatJson(actualJson).isEqualTo([
          "all_group_admins_are_view_users": true,
          "view": ["roles": ["role1"], "users": ["user1"]]])
      }

      @Test
      void "should represent an authorization with errors on both roles and users"() {
        def actualJson = toObjectString({
          def role = role("role1")
          role.addError("roles", "Role \"role1\" does not exist.")
          def user = user("user1")
          user.addError("users", "User \"user1\" does not exist.")

          AuthorizationRepresenter.toJSON(it, new Authorization(new ViewConfig(role, user)))
        })

        def expected = [
          "all_group_admins_are_view_users": true,
          "view": [
            "errors": [
              "roles": ["Role \"role1\" does not exist."],
              "users": ["User \"user1\" does not exist."]
            ],
            "roles" : ["role1"],
            "users" : ["user1"]
          ]
        ]

        assertThatJson(actualJson).isEqualTo(expected)
      }
    }

    @Nested
    class Admin {
      @Test
      void "should represent an authorization with admin users"() {
        def actualJson = toObjectString({
          AuthorizationRepresenter.toJSON(it, new Authorization(new AdminsConfig(user("user1"), user("user2"))))
        })

        assertThatJson(actualJson).isEqualTo([
          "all_group_admins_are_view_users": true,
          "admin": ["roles": [], "users": ["user1", "user2"]]])
      }

      @Test
      void "should represent an authorization with admin roles"() {
        def actualJson = toObjectString({
          AuthorizationRepresenter.toJSON(it, new Authorization(new AdminsConfig(role("role1"), role("role2"))))
        })

        assertThatJson(actualJson).isEqualTo([
          "all_group_admins_are_view_users": true,
          "admin": ["roles": ["role1", "role2"], "users": []]])
      }

      @Test
      void "should represent an authorization with admin roles and users"() {
        def actualJson = toObjectString({
          AuthorizationRepresenter.toJSON(it, new Authorization(new AdminsConfig(role("role1"), user("user1"))))
        })

        assertThatJson(actualJson).isEqualTo([
          "all_group_admins_are_view_users": true,
          "admin": ["roles": ["role1"], "users": ["user1"]]])
      }

      @Test
      void "should represent an authorization with errors on both roles and users"() {
        def actualJson = toObjectString({
          def role = role("role1")
          role.addError("roles", "Role \"role1\" does not exist.")
          def user = user("user1")
          user.addError("users", "User \"user1\" does not exist.")

          AuthorizationRepresenter.toJSON(it, new Authorization(new AdminsConfig(role, user)))
        })

        def expected = [
          "all_group_admins_are_view_users": true,
          "admin": [
            "errors": [
              "roles": ["Role \"role1\" does not exist."],
              "users": ["User \"user1\" does not exist."]
            ],
            "roles" : ["role1"],
            "users" : ["user1"]
          ]
        ]

        assertThatJson(actualJson).isEqualTo(expected)
      }
    }
  }

  @Nested
  class Deserialize {

    @Nested
    class allowGroupAdmins {
      @Test
      void 'should convert from json to authorization with allowGroupAdmins'() {
        def jsonReader = GsonTransformer.instance.jsonReaderFrom(["all_group_admins_are_view_users": false])
        def authorization = AuthorizationRepresenter.fromJSON(jsonReader)

        assertFalse(authorization.isAllowGroupAdmins())
      }
    }

    @Nested
    class View {
      @Test
      void 'should convert from json to authorization with view users'() {
        def jsonReader = GsonTransformer.instance.jsonReaderFrom([
          "all_group_admins_are_view_users": true,
          "view": ["users": ["user1", "user2"]]])
        def authorization = AuthorizationRepresenter.fromJSON(jsonReader)

        assertTrue(authorization.isAllowGroupAdmins())
        assertTrue(authorization.getViewConfig().contains(user("user1")))
        assertTrue(authorization.getViewConfig().contains(user("user2")))
      }

      @Test
      void 'should convert from json to authorization with view roles'() {
        def jsonReader = GsonTransformer.instance.jsonReaderFrom(["view": ["roles": ["role1", "role2"]]])
        def authorization = AuthorizationRepresenter.fromJSON(jsonReader)

        assertTrue(authorization.getViewConfig().contains(role("role1")))
        assertTrue(authorization.getViewConfig().contains(role("role2")))
      }

      @Test
      void 'should convert from json to authorization with view users and roles'() {
        def jsonReader = GsonTransformer.instance.jsonReaderFrom(["view": ["users": ["user1"], "roles": ["role1"]]])
        def authorization = AuthorizationRepresenter.fromJSON(jsonReader)

        assertTrue(authorization.getViewConfig().contains(role("role1")))
        assertTrue(authorization.getViewConfig().contains(user("user1")))
      }
    }

    @Nested
    class Admins {
      @Test
      void 'should convert from json to authorization with admin users'() {
        def jsonReader = GsonTransformer.instance.jsonReaderFrom(["admin": ["users": ["user1", "user2"]]])
        def authorization = AuthorizationRepresenter.fromJSON(jsonReader)

        assertTrue(authorization.getAdminsConfig().contains(user("user1")))
        assertTrue(authorization.getAdminsConfig().contains(user("user2")))
      }

      @Test
      void 'should convert from json to authorization with admin roles'() {
        def jsonReader = GsonTransformer.instance.jsonReaderFrom(["admin": ["roles": ["role1", "role2"]]])
        def authorization = AuthorizationRepresenter.fromJSON(jsonReader)

        assertTrue(authorization.getAdminsConfig().contains(role("role1")))
        assertTrue(authorization.getAdminsConfig().contains(role("role2")))
      }

      @Test
      void 'should convert from json to authorization with admin users and roles'() {
        def jsonReader = GsonTransformer.instance.jsonReaderFrom(["admin": ["users": ["user1"], "roles": ["role1"]]])
        def authorization = AuthorizationRepresenter.fromJSON(jsonReader)

        assertTrue(authorization.getAdminsConfig().contains(role("role1")))
        assertTrue(authorization.getAdminsConfig().contains(user("user1")))
      }
    }
  }

  static def user(name) {
    new AdminUser(new CaseInsensitiveString(name))
  }

  static def role(name) {
    new AdminRole(new CaseInsensitiveString(name))
  }
}