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
package com.thoughtworks.go.spark

import org.junit.jupiter.api.Test

trait NonAnonymousUserSecurity {
  @Test
  void 'should allow nobody with security disabled'() {
    disableSecurity()

    makeHttpCall()
    assertRequestForbidden()
  }

  @Test
  void 'should not allow anonymous with security enabled'() {
    enableSecurity()
    loginAsAnonymous()

    makeHttpCall()
    assertRequestForbidden()
  }

  @Test
  void 'should allow admin users with security enabled'() {
    enableSecurity()
    loginAsAdmin()

    makeHttpCall()
    assertRequestAllowed()
  }

  @Test
  void 'should allow normal users with security enabled'() {
    enableSecurity()
    loginAsUser()

    makeHttpCall()
    assertRequestAllowed()
  }

}
