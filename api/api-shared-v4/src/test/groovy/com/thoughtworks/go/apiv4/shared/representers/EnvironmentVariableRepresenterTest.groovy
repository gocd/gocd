/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv4.shared.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv4.shared.representers.EnvironmentVariableRepresenter
import com.thoughtworks.go.config.EnvironmentVariableConfig
import com.thoughtworks.go.security.GoCipher
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class EnvironmentVariableRepresenterTest {

  void 'should render plain environment variable with hal representation'() {
    def actualJson = toObjectString({ EnvironmentVariableRepresenter.toJSON(it, getPlainVariable()) })

    assertThatJson(actualJson).isEqualTo(getPlainTextHash)
  }

  @Test
  void 'should render secure environment variable with hal representation'() {
    def actualJson = toObjectString({ EnvironmentVariableRepresenter.toJSON(it, getSecureVariable()) })

    assertThatJson(actualJson).isEqualTo(getSecureHashWithEncryptedValue)
  }

  @Test
  void 'should convert from secure hash with encrypted value to EnvironmentVariableConfig'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(getSecureHashWithEncryptedValue)
    def actualEnvironmentVariableConfig = EnvironmentVariableRepresenter.fromJSON(jsonReader)

    assertThatJson(actualEnvironmentVariableConfig).isEqualTo(getSecureVariable())
  }

  @Test
  void 'should convert from secure hash with clear text value to EnvironmentVariableConfig'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(secureHashWithClearTextValue)
    def actualEnvironmentVariableConfig = EnvironmentVariableRepresenter.fromJSON(jsonReader)

    assertThatJson(actualEnvironmentVariableConfig).isEqualTo(getSecureVariable())
  }

  @Test
  void 'should deserialize an ambiguous encrypted variable (with both value and encrypted_value) to a variable with errors'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      name: 'PASSWORD',
      secure: true,
      value: 'plainText',
      encrypted_value: new GoCipher().encrypt('c!ph3rt3xt')
    ])
    def actualEnvironmentVariableConfig = EnvironmentVariableRepresenter.fromJSON(jsonReader)

    assertEquals(actualEnvironmentVariableConfig.errors().getAllOn("value"), Arrays.asList('You may only specify `value` or `encrypted_value`, not both!'))
    assertEquals(actualEnvironmentVariableConfig.errors().getAllOn("encryptedValue"), Arrays.asList('You may only specify `value` or `encrypted_value`, not both!'))
  }

  @Test
  void 'should deserialize an unambiguous encrypted variable (with either value or encrypted_value) to a variable without errors'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      name: 'PASSWORD',
      secure: true,
      encrypted_value: 'c!ph3rt3xt'
    ])

    def actualEnvironmentVariableConfig = EnvironmentVariableRepresenter.fromJSONArray(jsonReader)

    assertTrue(actualEnvironmentVariableConfig.errors().isEmpty())
  }

  def getSecureVariable()
  {
    return new EnvironmentVariableConfig(new GoCipher(), 'secure', 'confidential', true)
  }

  def getPlainVariable() {
    return new EnvironmentVariableConfig(new GoCipher(), 'plain', 'plain', false)
  }

  def getPlainTextHash =
    [
      name  : 'plain',
      value : 'plain',
      secure: false
    ]

  def getSecureHashWithEncryptedValue =
    [
      secure         : true,
      name           : 'secure',
      encrypted_value: new GoCipher().encrypt('confidential')
    ]

  def secureHashWithClearTextValue =
    [
      secure: true,
      name  : 'secure',
      value : 'confidential'
    ]

}
