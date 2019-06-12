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

package com.thoughtworks.go.apiv8.shared.representers.stages

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv8.admin.shared.representers.stages.ApprovalRepresenter
import com.thoughtworks.go.config.*
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals


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

    assertEquals(getApproval().getType(), approval.getType())
  }

  @Test
  void 'should render error'() {
    def approval = new Approval()
    approval.setType("junk")
    def authConfig = new AuthConfig(new AdminRole(new CaseInsensitiveString("role1")), new AdminUser(new CaseInsensitiveString("user1")))
    approval.setAuthConfig(authConfig)
    authConfig.addError("name", "error")
    approval.addError("type", "You have defined approval type as 'junk'. Approval can only be of the type 'manual' or 'success'.")

    def actualJson = toObjectString({ ApprovalRepresenter.toJSON(it, approval)})
    assertThatJson(actualJson).isEqualTo(approvalHashWithErrors)
  }

  @Test
  void 'should deserialize'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(approvalHash)
    def approval = ApprovalRepresenter.fromJSON(jsonReader)

    assertEquals(getApproval(), approval)
  }

  def approvalHash =
  [
    type: "manual",
    authorization: [
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
    type: "junk",
    authorization: [
      roles: ["role1"],
      users: ["user1"],
      errors: [
        name: ["error"]
      ]
    ],
    errors: [
      type: ["You have defined approval type as 'junk'. Approval can only be of the type 'manual' or 'success'."]
    ]
  ]
}
