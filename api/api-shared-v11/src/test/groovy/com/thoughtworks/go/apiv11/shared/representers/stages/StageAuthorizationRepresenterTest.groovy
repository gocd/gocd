/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv11.shared.representers.stages

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv11.admin.shared.representers.stages.StageAuthorizationRepresenter
import com.thoughtworks.go.config.*
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals

class StageAuthorizationRepresenterTest {

  @Test
  void 'should render stage authorization with hal representation'() {
    def actualJson = toObjectString({ StageAuthorizationRepresenter.toJSON(it, getAdminsConfig() as AuthConfig) })
    assertThatJson(actualJson).isEqualTo(stageAuthorizationHash)
  }

  @Test
  void 'should convert from document to AdminsConfig'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(stageAuthorizationHash)
    def stageAuthorization = StageAuthorizationRepresenter.fromJSON(jsonReader)
    assertEquals(getAdminsConfig(), stageAuthorization)
  }

  @Test
  void 'should render error'() {
    AuthConfig config = getAdminsConfig()
    config.addError("error", "validation failed")
    config.getRoles().get(0).addError("name", "role does not exists")
    config.getUsers().get(0).addError("name", "user does not exists")

    def actualJson = toObjectString({ StageAuthorizationRepresenter.toJSON(it, config) })
    def expectedJson = [
      errors: [users: ["user does not exists"], roles: ["role does not exists"]],
      roles : ['admin_role'],
      users : ['admin_user']
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  static def getAdminsConfig() {
    return new AdminsConfig(new AdminRole(new CaseInsensitiveString("admin_role")), new AdminUser(new CaseInsensitiveString("admin_user")))
  }

  def stageAuthorizationHash =
    [
      roles: ['admin_role'],
      users: ['admin_user']
    ]

}
