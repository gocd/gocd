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
package com.thoughtworks.go.apiv3.rolesconfig.representers

import com.google.gson.JsonParseException
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.policy.Allow
import com.thoughtworks.go.config.policy.Deny
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.assertThatExceptionOfType

class DirectiveRepresenterTest {
  def allow_json = [
    permission: 'allow',
    action    : 'view',
    type      : 'environment',
    resource  : '*'
  ]

  def deny_json = [
    permission: 'deny',
    action    : 'view',
    type      : 'environment',
    resource  : 'blah_*'
  ]

  def invalid_permission_json = [
    permission: 'blah',
    action    : 'view',
    type      : 'environment',
    resource  : '*'
  ]

  def allowDirective = new Allow("view", "environment", "*")

  def denyDirective = new Deny("view", "environment", "blah_*")

  @Test
  void allowDirective_shouldGenerateJSON() {
    def actualJson = toObjectString({
      DirectiveRepresenter.toJSON(it, this.allowDirective)
    })

    assertThatJson(actualJson).isEqualTo(this.allow_json)
  }

  @Test
  void allowDirective_shouldBuildObjectFromJson() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(this.allow_json)
    def roleConfig = DirectiveRepresenter.fromJSON(jsonReader)
    assertThat(roleConfig).isEqualTo(this.allowDirective)
  }

  @Test
  void denyDirective_shouldGenerateJSON() {
    def actualJson = toObjectString({
      DirectiveRepresenter.toJSON(it, this.denyDirective)
    })

    assertThatJson(actualJson).isEqualTo(this.deny_json)
  }

  @Test
  void denyDirective_shouldBuildObjectFromJson() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(this.deny_json)
    def roleConfig = DirectiveRepresenter.fromJSON(jsonReader)
    assertThat(roleConfig).isEqualTo(this.denyDirective)
  }

  @Test
  void invalidPermissionJSON_shouldFailToBuildObjectWhenInvalidPermissionsAreSpecified() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(this.invalid_permission_json)
    def roleConfig = DirectiveRepresenter.fromJSON(jsonReader)
    assertThat(roleConfig).isEqualTo(new DirectiveRepresenter.Unknown("blah", "view", "environment", "*"))
    assertThat(roleConfig.errors().on("permission")).isEqualTo("Invalid permission, must be either 'allow' or 'deny'.")
  }
}
