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
package com.thoughtworks.go.apiv1.serverhealthmessages.representers

import com.thoughtworks.go.serverhealth.HealthStateType
import com.thoughtworks.go.serverhealth.ServerHealthState
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ServerHealthMessagesRepresenterTest {

  @Test
  void 'should serialize'() {
    def state = ServerHealthState.error("not enough disk space, halting scheduling", "There is not enough disk space on the artifact filesystem", HealthStateType.artifactsDiskFull())
    def actualJson = toArrayString({ ServerHealthMessagesRepresenter.toJSON(it, [state]) })

    assertThatJson(actualJson).isEqualTo([[
                                            message: state.message,
                                            detail : state.description,
                                            level  : state.logLevel.toString(),
                                            time   : jsonDate(state.timestamp)
                                          ]])
  }
}
