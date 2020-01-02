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
package com.thoughtworks.go.apiv2.adminsconfig.representers

import com.thoughtworks.go.config.AdminRole
import com.thoughtworks.go.config.AdminUser
import com.thoughtworks.go.config.AdminsConfig
import com.thoughtworks.go.server.service.result.BulkUpdateAdminsResult
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.api.base.JsonUtils.toObjectWithoutLinks
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class BulkUpdateFailureResultRepresenterTest {

  @Test
  void shouldNotSerializeNonExistentUsersAndRolesWhenAbsent() {
    def result = new BulkUpdateAdminsResult()
    result.unprocessableEntity("Validation Failed")
    result.adminsConfig = new AdminsConfig(new AdminUser("adminUser"), new AdminRole("adminRole"))
    def json = toObjectString { BulkUpdateFailureResultRepresenter.toJSON(it, result) }
    def expected = [message: "Validation Failed", data: [roles: ["adminRole"], users: ["adminUser"]]]

    assertThatJson(json).isEqualTo(expected)
  }

}
