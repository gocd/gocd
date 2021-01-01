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
package com.thoughtworks.go.apiv1.pipelineoperations.representers

import com.thoughtworks.go.api.util.GsonTransformer
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class MaterialRevisionRepresenterTest {
  @Test
  void 'should deserialize json'() {
    def inputJson = [
      fingerprint: "some-random-fingerprint",
      revision   : "some-revision"
    ]
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(inputJson)

    def materialForScheduling = MaterialRevisionRepresenter.fromJSON(jsonReader)

    assertThat(materialForScheduling.fingerprint).isEqualTo("some-random-fingerprint")
    assertThat(materialForScheduling.revision).isEqualTo("some-revision")
  }
}
