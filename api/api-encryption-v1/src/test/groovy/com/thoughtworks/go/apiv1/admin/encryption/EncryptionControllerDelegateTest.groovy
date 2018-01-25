/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.admin.encryption

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.admin.encryption.representers.EncryptedValueRepresenter
import com.thoughtworks.go.security.GoCipher
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.bouncycastle.crypto.InvalidCipherTextException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.util.HaltApiMessages.errorWhileEncryptingMessage
import static com.thoughtworks.go.api.util.HaltApiMessages.missingJsonProperty
import static com.thoughtworks.go.api.util.HaltApiMessages.rateLimitExceeded
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.spy

class EncryptionControllerDelegateTest implements SecurityServiceTrait, ControllerTrait<EncryptionControllerDelegate> {
  public static final int REQUESTS_PER_MINUTE = 10
  private GoCipher cipher = spy(new GoCipher())

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait {

      @Test
      void 'should allow all with security disabled'() {
        disableSecurity()

        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void "should disallow anonymous users, with security enabled"() {
        enableSecurity()
        loginAsAnonymous()

        makeHttpCall()

        assertRequestNotAuthorized()
      }

      @Test
      void 'should disallow normal users, with security enabled'() {
        enableSecurity()
        loginAsUser()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

      @Test
      void 'should allow admin, with security enabled'() {
        enableSecurity()
        loginAsAdmin()

        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void 'should allow pipeline group admin users, with security enabled'() {
        enableSecurity()
        loginAsGroupAdmin()

        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void 'should allow template admin users, with security enabled'() {
        enableSecurity()
        loginAsTemplateAdmin()

        makeHttpCall()
        assertRequestAuthorized()
      }

      @Override
      String getControllerMethodUnderTest() {
        return "encrypt"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), [:])
      }
    }

    @Nested
    class AsAuthorizedUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
      }

      @Test
      void 'it should return encrypted value of submitted plain text passed'() {
        loginAsAdmin()
        postWithApiHeader(controller.controllerBasePath(), [value: 'foo'])

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBodySerializedWith(cipher.encrypt("foo"), EncryptedValueRepresenter.class)
      }

      @Test
      void 'it should handle cipher exception while decryption'() {
        doThrow(new InvalidCipherTextException("boom!")).when(cipher).encrypt("foo")
        loginAsAdmin()
        postWithApiHeader(controller.controllerBasePath(), [value: 'foo'])

        assertThatResponse()
          .isInternalServerError()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(errorWhileEncryptingMessage())
      }

      @Test
      void 'should handle bad input JSON'() {
        loginAsAdmin()
        postWithApiHeader(controller.controllerBasePath(), [foo: 'bar'])

        assertThatResponse()
          .isUnprocessibleEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(missingJsonProperty("value"))
      }

      @Nested
      class RateLimit {
        @Test
        void "should rate limit if requests per minute exceeds limit"() {
          loginAsAdmin()
          REQUESTS_PER_MINUTE.times { i ->
            postWithApiHeader(controller.controllerBasePath(), [value: 'foo'])
            sleep(interval(REQUESTS_PER_MINUTE))
            assertThatResponse()
              .isOk()
              .hasContentType(controller.mimeType)
              .hasHeader("X-RateLimit-Limit", "10")
              .hasHeader("X-RateLimit-Remaining", (REQUESTS_PER_MINUTE - i).toString())
              .hasJsonBodySerializedWith(cipher.encrypt("foo"), EncryptedValueRepresenter.class)
          }

          postWithApiHeader(controller.controllerBasePath(), [value: 'foo'])

          assertThatResponse()
            .isTooManyRequests()
            .hasHeader("X-RateLimit-Limit", "10")
            .hasHeader("X-RateLimit-Remaining", "0")
            .hasContentType(controller.mimeType)
            .hasJsonMessage(rateLimitExceeded())
        }

        @Test
        void "should service all requests when request rate is within limit"() {
          loginAsAdmin()
          (REQUESTS_PER_MINUTE - 1).times { i ->
            postWithApiHeader(controller.controllerBasePath(), [value: 'foo'])
            sleep(interval(REQUESTS_PER_MINUTE - 1))

            assertThatResponse()
              .isOk()
              .hasContentType(controller.mimeType)
              .hasHeader("X-RateLimit-Limit", "10")
              .hasHeader("X-RateLimit-Remaining", (REQUESTS_PER_MINUTE - i).toString())
              .hasJsonBodySerializedWith(cipher.encrypt("foo"), EncryptedValueRepresenter.class)
          }
        }

        @Test
        void 'should service all requests when made for different users'() {
          (REQUESTS_PER_MINUTE * 10).times { i ->
            loginAsAdmin()
            postWithApiHeader(controller.controllerBasePath(), [value: 'foo'])

            assertThatResponse()
              .isOk()
              .hasContentType(controller.mimeType)
              .hasHeader("X-RateLimit-Limit", "10")
              .hasHeader("X-RateLimit-Remaining", "10")
              .hasJsonBodySerializedWith(cipher.encrypt("foo"), EncryptedValueRepresenter.class)
          }
        }

        private long interval(int requestsPerMinute) {
          (long) requestsPerMinute / 60 * 1000
        }
      }
    }
  }

  @Override
  EncryptionControllerDelegate createControllerInstance() {
    return new EncryptionControllerDelegate(new ApiAuthenticationHelper(securityService, goConfigService), cipher, REQUESTS_PER_MINUTE)
  }
}
