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

package com.thoughtworks.go.apiv1.versioninfos.representers

import com.thoughtworks.go.api.util.GsonTransformer
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class GoLatestVersionRepresenterTest {
  @Test
  void 'should return object with correct values'() {
    def latestVersion = [
      "message"                     : "{\"latest-version\":\"20.5.0-11820\",\"release-time\":\"2020-06-22T05:35:21Z\"}",
      "message_signature"           : "some_msg_signature",
      "signing_public_key"          : "some_public_key",
      "signing_public_key_signature": "some_public_key_signature"
    ]
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(latestVersion)

    def actualObject = GoLatestVersionRepresenter.fromJSON(jsonReader)

    assertThat(actualObject.message).isEqualTo("{\"latest-version\":\"20.5.0-11820\",\"release-time\":\"2020-06-22T05:35:21Z\"}")
    assertThat(actualObject.messageSignature).isEqualTo("some_msg_signature")
    assertThat(actualObject.signingPublicKey).isEqualTo("some_public_key")
    assertThat(actualObject.signingPublicKeySignature).isEqualTo("some_public_key_signature")
  }
}
