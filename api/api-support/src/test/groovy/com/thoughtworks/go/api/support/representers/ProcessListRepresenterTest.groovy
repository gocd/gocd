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

package com.thoughtworks.go.api.support.representers

import com.thoughtworks.go.util.ProcessWrapper
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class ProcessListRepresenterTest {
  @Test
  void 'should return empty list when no process running'() {
    def json = toObjectString({
      ProcessListRepresenter.toJSON(it, Collections.emptyList())
    })

    def expected = ["process-list": []]

    assertThatJson(json).isEqualTo(expected)
  }

  @Test
  void 'should return process list as json'() {
    def processOne = mockProcess("ls", "20/12/19 - 12:23:16:624", 2)
    def processTwo = mockProcess("git", "20/12/19 - 12:23:15:634", 3)

    def json = toObjectString({
      ProcessListRepresenter.toJSON(it, Arrays.asList(processOne, processTwo))
    })

    def expected = [
      "process-list": [
        ["command": "ls", "start_time": "20/12/19 - 12:23:16:624", "idle_time": "2 minutes"],
        ["command": "git", "start_time": "20/12/19 - 12:23:15:634", "idle_time": "3 minutes"]
      ]
    ]

    assertThatJson(json).isEqualTo(expected)
  }

  private static ProcessWrapper mockProcess(String command, String startTime, long idleTimeInMinutes) {
    def process = mock(ProcessWrapper)
    when(process.getCommand()).thenReturn(command)
    when(process.getStartTimeForDisplay()).thenReturn(startTime)
    when(process.getIdleTime()).thenReturn(idleTimeInMinutes * 60000)
    return process
  }
}
