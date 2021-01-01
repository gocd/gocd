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
package com.thoughtworks.go.api.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.domain.config.ConfigurationProperty
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.security.GoCipher
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

class ConfigurationPropertyRepresenterTest {

  @Nested
  class Serialize {
    @Test
    void 'it should serialize an unencrypted value if property type is unsecure'() {
      def configProperty = ConfigurationPropertyMother.create("user", false, "bob")
      def actualJson = toObjectString({
        ConfigurationPropertyRepresenter.toJSON(it, configProperty)
      })
      assertThatJson(actualJson).isEqualTo([key: 'user', value: 'bob'])
    }

    @Test
    void 'it should serialize an encrypted value if property type is secure'() {
      def configProperty = ConfigurationPropertyMother.create("password", true, "p@ssw0rd")
      def actualJson = toObjectString({
        ConfigurationPropertyRepresenter.toJSON(it, configProperty)
      })
      assertThatJson(actualJson).isEqualTo([key: 'password', encrypted_value: configProperty.getEncryptedValue()])
    }

    @Test
    void 'it should serialize property with null value'() {
      def configProperty = ConfigurationPropertyMother.createKeyOnly("Username")
      def actualJson = toObjectString({
        ConfigurationPropertyRepresenter.toJSON(it, configProperty)
      })
      assertThatJson(actualJson).isEqualTo([key: 'Username'])
    }

    @Test
    void 'it should serialize with errors'() {
      def configProperty = ConfigurationPropertyMother.create("user", false, "bob")
      configProperty.addError("encryptedValue", "blah")
      configProperty.addError("encryptedValue", "boo")

      def actualJson = toObjectString({ ConfigurationPropertyRepresenter.toJSON(it, configProperty) })
      assertThatJson(actualJson).isEqualTo([key: 'user', value: 'bob', "errors": ["encrypted_value": ["blah", "boo"]]])
    }
  }

  @Nested
  class Deserialize {
    @Test
    void 'it should deserialize to encrypted property if encrypted value is submitted'() {
      def encryptedValue = new GoCipher().encrypt('password')
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([key: 'user', encrypted_value: encryptedValue])
      def property = ConfigurationPropertyRepresenter.fromJSON(jsonReader)
      assertThat(property).isEqualTo(ConfigurationPropertyMother.create("user", true, 'password'))
    }

    @Test
    void 'fromJSONHandlingEncryption() deserializes to encrypted property if the secure flag is set'() {
      String plainText = 'password'
      JsonReader jsonReader = GsonTransformer.instance.jsonReaderFrom([key: 'user', value: plainText, secure: true])
      ConfigurationProperty property = ConfigurationPropertyRepresenter.fromJSONHandlingEncryption(jsonReader)
      assertThat(property).isEqualTo(ConfigurationPropertyMother.create("user", true, plainText))
    }

    @Test
    void 'fromJSONHandlingEncryption() ignores secure flag if encrypted_value is set'() {
      String encryptedValue = new GoCipher().encrypt('password')
      JsonReader jsonReader = GsonTransformer.instance.jsonReaderFrom([key: 'user', encrypted_value: encryptedValue, secure: false])
      ConfigurationProperty property = ConfigurationPropertyRepresenter.fromJSONHandlingEncryption(jsonReader)
      assertThat(property).isEqualTo(ConfigurationPropertyMother.create("user", true, 'password'))
    }

    @Test
    void 'it should deserialize to simple property if plain-text value is submitted'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([key: 'user', value: 'bob'])
      def property = ConfigurationPropertyRepresenter.fromJSON(jsonReader)
      assertThat(property).isEqualTo(ConfigurationPropertyMother.create("user", false, "bob"))
    }

    @Test
    void 'it should deserialize with errors'() {
      def encryptedValue = new GoCipher().encrypt('p@ssword')
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([key: 'password', value: 'p@ssword', encrypted_value: encryptedValue])
      def property = ConfigurationPropertyRepresenter.fromJSON(jsonReader)

      assertThat(property.hasErrors()).isTrue()
      assertThat(property.errors()).hasSize(2)
      assertThat(property.errors().getAllOn("value")).isEqualTo(["You may only specify `value` or `encrypted_value`, not both!"])
      assertThat(property.errors().getAllOn("encryptedValue")).isEqualTo(["You may only specify `value` or `encrypted_value`, not both!"])
    }
  }
}
