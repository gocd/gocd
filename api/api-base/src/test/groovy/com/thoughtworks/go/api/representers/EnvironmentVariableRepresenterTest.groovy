/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.api.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.EnvironmentVariableConfig
import com.thoughtworks.go.security.GoCipher
import org.junit.jupiter.api.Test
import spark.HaltException

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import static org.assertj.core.api.Assertions.assertThat
import static org.junit.jupiter.api.Assertions.*

class EnvironmentVariableRepresenterTest {

  @Test
  void 'should serialize plain text variable to JSON'() {
    EnvironmentVariableConfig config = new EnvironmentVariableConfig("JAVA_HOME", "/bin/java")

    def json = toObjectString({ EnvironmentVariableRepresenter.toJSON(it, config) })

    assertThatJson(json).isEqualTo([
      "name"  : "JAVA_HOME",
      "secure": false,
      "value" : "/bin/java"
    ])
  }

  @Test
  void 'should serialize secure environment variable to JSON'() {
    EnvironmentVariableConfig config = new EnvironmentVariableConfig(new GoCipher(), "secret", "password", true)

    def json = toObjectString({ EnvironmentVariableRepresenter.toJSON(it, config) })

    assertThatJson(json).isEqualTo([
      "encrypted_value": config.getEncryptedValue(),
      "name"           : "secret",
      "secure"         : true
    ])
  }

  @Test
  void 'should serialize errors in environment variable to JSON'() {
    EnvironmentVariableConfig config = new EnvironmentVariableConfig("JAVA_HOME", "/bin/java")
    config.addError("value", 'You may only specify `value` or `encrypted_value`, not both!')
    config.addError("encryptedValue", 'You may only specify `value` or `encrypted_value`, not both!')

    def json = toObjectString({ EnvironmentVariableRepresenter.toJSON(it, config) })

    assertThatJson(json).isEqualTo([
      "name"  : "JAVA_HOME",
      "secure": false,
      "value" : "/bin/java",
      "errors": ["encrypted_value": ['You may only specify `value` or `encrypted_value`, not both!'], "value": ['You may only specify `value` or `encrypted_value`, not both!']]
    ])
  }

  @Test
  void 'should serialize collection of environment config to JSON'() {
    List<EnvironmentVariableConfig> configs = List.of(
      new EnvironmentVariableConfig("JAVA_HOME", "/bin/java"),
      new EnvironmentVariableConfig("GROOVY_HOME", "/bin/groovy")
    )

    def json = toObjectString({ EnvironmentVariableRepresenter.toJSONArray(it, "environment_variables", configs) })

    assertThatJson(json).isEqualTo([
      "environment_variables": [
        [
          "name"  : "JAVA_HOME",
          "secure": false,
          "value" : "/bin/java"
        ],
        [
          "name"  : "GROOVY_HOME",
          "secure": false,
          "value" : "/bin/groovy"
        ]
      ]
    ])
  }

  @Test
  void 'should de-serialize from JSON array of environment variables'() {
    def encryptedSecret = new GoCipher().encrypt("confidential")
    def json = [
      "environment_variables": [
        [
          "name"           : "secured",
          "encrypted_value": encryptedSecret,
          "secure"         : true
        ],
        [
          "name"  : "plain",
          "value" : "plaint text value",
          "secure": false
        ]
      ]
    ]

    def jsonReader = GsonTransformer.instance.jsonReaderFrom(json)
    def environmentVariableConfig = EnvironmentVariableRepresenter.fromJSONArray(jsonReader)
    def secureVariable = environmentVariableConfig.get(0)
    def plainVariable = environmentVariableConfig.get(1)

    assertThat(secureVariable.secure).isEqualTo(true)
    assertThat(secureVariable.name).isEqualTo("secured")
    assertThat(secureVariable.value).isEqualTo("confidential")
    assertThat(secureVariable.errors().isEmpty()).isEqualTo(true)

    assertThat(plainVariable.secure).isEqualTo(false)
    assertThat(plainVariable.name).isEqualTo("plain")
    assertThat(plainVariable.value).isEqualTo("plaint text value")
    assertThat(plainVariable.errors().isEmpty()).isEqualTo(true)
  }

  @Test
  void 'should de-serialize an ambiguous encrypted variable (with both value and encrypted_value) to a variable with errors'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      name           : 'PASSWORD',
      secure         : true,
      value          : 'plainText',
      encrypted_value: new GoCipher().encrypt('c!ph3rt3xt')
    ])
    def actualEnvironmentVariableConfig = EnvironmentVariableRepresenter.fromJSON(jsonReader)

    assertEquals(actualEnvironmentVariableConfig.errors().getAllOn("value"), List.of('You may only specify `value` or `encrypted_value`, not both!'))
    assertEquals(actualEnvironmentVariableConfig.errors().getAllOn("encryptedValue"), List.of('You may only specify `value` or `encrypted_value`, not both!'))
  }

  @Test
  void 'de-serialize should error out when both value or encrypted_value not specified'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      name  : 'PASSWORD',
      secure: true,
    ])

    def haltException = (HaltException) assertThrows(HaltException.class, {
      EnvironmentVariableRepresenter.fromJSON(jsonReader)
    })

    assertThatJson(haltException.body())isEqualTo("{\"message\" : \"Environment variable must contain either 'value' or 'encrypted_value'\"}")
  }

  @Test
  void 'should deserialize an unambiguous encrypted variable (with encrypted_value) to a variable without errors'() {
    def encryptedPassword = new GoCipher().encrypt("c!ph3rt3xt")
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      name           : 'PASSWORD',
      secure         : true,
      encrypted_value: encryptedPassword
    ])
    def actualEnvironmentVariableConfig = EnvironmentVariableRepresenter.fromJSON(jsonReader)

    assertTrue(actualEnvironmentVariableConfig.errors().isEmpty())
    assertEquals(encryptedPassword, actualEnvironmentVariableConfig.encryptedValue)
    assertEquals("PASSWORD", actualEnvironmentVariableConfig.name)
    assertEquals(true, actualEnvironmentVariableConfig.isSecure)
  }

  @Test
  void 'should deserialize an unambiguous encrypted variable (with value) to a variable without errors'() {
    def encryptedPassword = new GoCipher().encrypt("c!ph3rt3xt")
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      name  : 'PASSWORD',
      secure: true,
      value : 'c!ph3rt3xt'
    ])
    def actualEnvironmentVariableConfig = EnvironmentVariableRepresenter.fromJSON(jsonReader)

    assertTrue(actualEnvironmentVariableConfig.errors().isEmpty())
    assertEquals(encryptedPassword, actualEnvironmentVariableConfig.encryptedValue)
    assertEquals("PASSWORD", actualEnvironmentVariableConfig.name)
    assertEquals(true, actualEnvironmentVariableConfig.isSecure)
  }
}
