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
package com.thoughtworks.go.apiv1.pipelineoperations.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.security.GoCipher
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class PipelineScheduleOptionsRepresenterTest {
  @Test
  void 'should deserialize'() {
    def encryptedValue = new GoCipher().encrypt("encrypted_secval2")
    def scheduleOptionsJson = [
      update_materials_before_scheduling: true,
      materials                         : [[fingerprint: "fingerprint1", revision: "revision1"], [fingerprint: "fingerprint2", revision: "revision2"]],
      environment_variables             : [[name: "VAR1", value: "val1"],
                                           [name: "VAR2", value: "val2"],
                                           [name: "SEC_VAR1", value: "secval1", secure: true],
                                           [name: "SEC_VAR2", encrypted_value: encryptedValue, secure: true]]
    ]

    def jsonReader = GsonTransformer.instance.jsonReaderFrom(scheduleOptionsJson)
    def pipelineScheduleOptions = PipelineScheduleOptionsRepresenter.fromJSON(jsonReader)

    assertThat(pipelineScheduleOptions.shouldPerformMDUBeforeScheduling()).isTrue()

    assertThat(pipelineScheduleOptions.getPlainTextEnvironmentVariables()).hasSize(2)
    assertThat(pipelineScheduleOptions.getPlainTextEnvironmentVariables().getVariable("VAR1").getValue()).isEqualTo("val1")
    assertThat(pipelineScheduleOptions.getPlainTextEnvironmentVariables().getVariable("VAR2").getValue()).isEqualTo("val2")

    assertThat(pipelineScheduleOptions.getSecureEnvironmentVariables()).hasSize(2)
    assertThat(pipelineScheduleOptions.getSecureEnvironmentVariables().getVariable("SEC_VAR1").getValue()).isEqualTo("secval1")
    assertThat(pipelineScheduleOptions.getSecureEnvironmentVariables().getVariable("SEC_VAR2").getEncryptedValue()).isEqualTo(encryptedValue)

    assertThat(pipelineScheduleOptions.getMaterials()).hasSize(2)
    assertThat(pipelineScheduleOptions.getMaterials().find { material -> material.getFingerprint().equals("fingerprint1") }.getRevision()).isEqualTo("revision1")
    assertThat(pipelineScheduleOptions.getMaterials().find { material -> material.getFingerprint().equals("fingerprint2") }.getRevision()).isEqualTo("revision2")
  }

  @Test
  void 'should handle invalid encrypted value in json during deserialization'() {
    def encryptedValue = new GoCipher().encrypt("encrypted_secval")
    def scheduleOptionsJson = [
      environment_variables: [[name: "SEC_VAR", encrypted_value: encryptedValue, secure: true]]
    ]

    def jsonReader = GsonTransformer.instance.jsonReaderFrom(scheduleOptionsJson)
    def pipelineScheduleOptions = PipelineScheduleOptionsRepresenter.fromJSON(jsonReader)

    assertThat(pipelineScheduleOptions.getSecureEnvironmentVariables().size(), is(1))
    assertThat(pipelineScheduleOptions.getSecureEnvironmentVariables().find { var -> var.getName().equals("SEC_VAR") }.getEncryptedValue())
      .isEqualTo(encryptedValue)
  }
}
