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
package com.thoughtworks.go.apiv3.users.representers


import com.thoughtworks.go.server.service.result.BulkUpdateUsersOperationResult
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class BulkUpdateUsersOperationResultRepresenterTest {

  @Test
  void 'render the bulk deletion failure result'() {
    def nonExistingUsers = ['Bob', 'John']
    def enabledUsers = ['Tina']
    def result = new HttpLocalizedOperationResult()
    result.unprocessableEntity("Deletion failed because some users were either enabled or do not exist.")

    def bulkDeletionFailureResult = new BulkUpdateUsersOperationResult(nonExistingUsers, enabledUsers)
    bulkDeletionFailureResult.badRequest("Deletion failed because some users were either enabled or do not exist.")

    def expectedJson = [
      message           : "Deletion failed because some users were either enabled or do not exist.",
      non_existent_users: nonExistingUsers,
      enabled_users     : enabledUsers
    ]

    def json = toObject({ BulkDeletionFailureResultRepresenter.toJSON(it, bulkDeletionFailureResult) })
    assertThatJson(json).isEqualTo(expectedJson)
  }

  @Test
  void 'should not render non existing users if there are none'() {
    def nonExistingUsers = []
    def enabledUsers = ['Tina']

    def bulkDeletionFailureResult = new BulkUpdateUsersOperationResult(nonExistingUsers, enabledUsers)
    bulkDeletionFailureResult.unprocessableEntity("Deletion failed because some users were either enabled or do not exist.")

    def expectedJson = [
      message      : "Deletion failed because some users were either enabled or do not exist.",
      enabled_users: enabledUsers
    ]

    def json = toObject({ BulkDeletionFailureResultRepresenter.toJSON(it, bulkDeletionFailureResult) })
    assertThatJson(json).isEqualTo(expectedJson)
  }

  @Test
  void 'should not render enabled users if there are none'() {
    def nonExistingUsers = ['Bob', 'John']
    def enabledUsers = []

    def bulkDeletionFailureResult = new BulkUpdateUsersOperationResult(nonExistingUsers, enabledUsers)
    bulkDeletionFailureResult.unprocessableEntity("Deletion failed because some users were either enabled or do not exist.")

    def expectedJson = [
      message           : "Deletion failed because some users were either enabled or do not exist.",
      non_existent_users: nonExistingUsers
    ]

    def json = toObject({ BulkDeletionFailureResultRepresenter.toJSON(it, bulkDeletionFailureResult) })
    assertThatJson(json).isEqualTo(expectedJson)
  }
}
