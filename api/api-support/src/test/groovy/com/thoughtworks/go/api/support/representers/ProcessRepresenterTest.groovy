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
package com.thoughtworks.go.api.support.representers

import com.thoughtworks.go.util.ProcessWrapper
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class ProcessRepresenterTest {
  @Test
  void 'should serialize to json'() {
    def process = mock(ProcessWrapper)
    when(process.getCommand()).thenReturn("ls")
    when(process.getStartTimeForDisplay()).thenReturn("20/12/19 - 12:23:16:624")
    when(process.getIdleTime()).thenReturn(2 * 60000L)

    def json = toObjectString({
      ProcessRepresenter.toJSON(it, process)
    })

    def expected = ["command": "ls", "start_time": "20/12/19 - 12:23:16:624", "idle_time": "2 minutes"]

    assertThatJson(json).isEqualTo(expected)
  }
}
