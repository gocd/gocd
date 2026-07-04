/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.spark

import groovy.transform.SelfType
import org.junit.jupiter.api.Test

@SelfType([SecurityServiceTrait, ControllerTrait, SecurityTestTraitBasics])
trait PipelineAccessSecurity {

  @Test
  void 'should allow all with security disabled'() {
    disableSecurity()

    makeHttpCall()
    assertRequestAllowed()
  }

  @Test
  void "should disallow anonymous users, with security enabled"() {
    enableSecurity()
    loginAsAnonymous()

    makeHttpCall()

    assertRequestForbidden()
  }

  @Test
  void 'should disallow normal users, with security enabled'() {
    loginAsUser()

    makeHttpCall()
    assertRequestForbidden()
  }

  @Test
  void 'should allow admin, with security enabled'() {
    loginAsAdmin()

    makeHttpCall()
    assertRequestAllowed()
  }

  @Test
  void "should allow pipeline view users, with security enabled"() {
    loginAsPipelineViewUser(pipelineSpecifier)

    makeHttpCall()

    assertRequestAllowed()
  }

  abstract SecurityServiceTrait.PipelineSpecifier getPipelineSpecifier()

  String getGroupName() {
    return pipelineSpecifier.groupName()
  }

  String getPipelineName() {
    return pipelineSpecifier.pipelineName()
  }
}
