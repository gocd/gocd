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
package com.thoughtworks.go.apiv10.shared.representers.stages

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv10.admin.shared.representers.stages.ApprovalRepresenter
import com.thoughtworks.go.config.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

class ApprovalRepresenterTest {

  @Test
  void 'renders approval with hal representation'() {
    def actualJson = toObjectString({ ApprovalRepresenter.toJSON(it, getApproval()) })

    assertThatJson(actualJson).isEqualTo(approvalHash)
  }

  @Test
  void 'should convert basic hash to Approval object'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(approvalHash)
    def approval = ApprovalRepresenter.fromJSON(jsonReader)

    assertThat(getApproval().getType()).isEqualTo(approval.getType())
  }

  @Test
  void 'should render error'() {
    def approval = new Approval()
    approval.setType("junk")
    def authConfig = new AuthConfig(new AdminRole(new CaseInsensitiveString("role1")), new AdminUser(new CaseInsensitiveString("user1")))
    approval.setAuthConfig(authConfig)
    authConfig.addError("name", "error")
    approval.addError("type", "You have defined approval type as 'junk'. Approval can only be of the type 'manual' or 'success'.")

    def actualJson = toObjectString({ ApprovalRepresenter.toJSON(it, approval) })
    assertThatJson(actualJson).isEqualTo(approvalHashWithErrors)
  }

  @Test
  void 'should deserialize'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(approvalHash)
    def approval = ApprovalRepresenter.fromJSON(jsonReader)

    assertThat(getApproval()).isEqualTo(approval)
  }

  @Nested
  class AllowOnlyOnSuccess {
    @Test
    void 'should convert hash with allow_only_on_success'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom(inputHash)
      def actualObject = ApprovalRepresenter.fromJSON(jsonReader)

      assertThat(approvalObject().getType()).isEqualTo(actualObject.getType())
      assertThat(actualObject.allowOnlyOnSuccess).isTrue()
    }

    @Test
    void 'should serialize approval object'() {
      def actualJson = toObjectString({ ApprovalRepresenter.toJSON(it, approvalObject()) })

      assertThatJson(actualJson).isEqualTo(inputHash)
    }

    @Test
    void 'should always deserialize allow_only_on_success'() {
      def inputJSON = inputHash
      inputJSON.replace("type", "success")

      def jsonReader = GsonTransformer.instance.jsonReaderFrom(inputJSON)
      def actualObject = ApprovalRepresenter.fromJSON(jsonReader)

      assertThat(actualObject.getType()).isEqualTo("success")
      assertThat(actualObject.isAllowOnlyOnSuccess()).isTrue()
    }

    def inputHash = [
      type                 : "manual",
      allow_only_on_success: true,
      authorization        : [
        roles: [],
        users: []
      ]
    ]

    static def approvalObject() {
      def manualApproval = Approval.manualApproval()
      manualApproval.allowOnlyOnSuccess = true
      return manualApproval;
    }
  }

  def approvalHash =
    [
      type                 : "manual",
      allow_only_on_success: false,
      authorization        : [
        roles: ["role1", "role2"],
        users: ["user1", "user2"]
      ]
    ]

  static def getApproval() {
    def authConfig = new AuthConfig(new AdminRole(new CaseInsensitiveString("role1")), new AdminRole(new CaseInsensitiveString("role2")),
      new AdminUser(new CaseInsensitiveString("user1")), new AdminUser(new CaseInsensitiveString("user2")))
    return new Approval(authConfig)
  }

  def approvalHashWithErrors =
    [
      type                 : "junk",
      allow_only_on_success: false,
      authorization        : [
        roles : ["role1"],
        users : ["user1"],
      ],
      errors               : [
        type: ["You have defined approval type as 'junk'. Approval can only be of the type 'manual' or 'success'."]
      ]
    ]
}
