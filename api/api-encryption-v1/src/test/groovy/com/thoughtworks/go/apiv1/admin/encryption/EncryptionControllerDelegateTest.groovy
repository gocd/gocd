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
package com.thoughtworks.go.apiv1.admin.encryption

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.admin.encryption.representers.EncryptedValueRepresenter
import com.thoughtworks.go.security.CryptoException
import com.thoughtworks.go.security.GoCipher
import com.thoughtworks.go.spark.AnyAdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import java.util.concurrent.TimeUnit

import static com.thoughtworks.go.api.util.HaltApiMessages.errorWhileEncryptingMessage
import static com.thoughtworks.go.api.util.HaltApiMessages.rateLimitExceeded
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.spy

class EncryptionControllerDelegateTest implements SecurityServiceTrait, ControllerTrait<EncryptionControllerDelegate> {
  public static final int REQUESTS_PER_MINUTE = 10
  private GoCipher cipher = spy(new GoCipher())
  TestingTicker ticker = new TestingTicker().useSystemClock()

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, AnyAdminUserSecurity {

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
          .hasBodyWithJsonObject(cipher.encrypt("foo"), EncryptedValueRepresenter.class)
      }

      @Test
      void 'it should handle cipher exception while decryption'() {
        doThrow(new CryptoException("boom!")).when(cipher).encrypt("foo")
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
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Json `{\\\"foo\\\":\\\"bar\\\"}` does not contain property 'value'")
      }

      @Nested
      class RateLimit {
        @BeforeEach
        void setUp() {
          ticker.freeze()
          ticker.time = 0
        }

        @Test
        void "should rate limit if requests per minute exceeds limit"() {
          loginAsAdmin()

          REQUESTS_PER_MINUTE.times { i ->
            postWithApiHeader(controller.controllerBasePath(), [value: 'foo'])
            ticker.forward(interval(REQUESTS_PER_MINUTE) - 1, TimeUnit.MILLISECONDS)

            assertThatResponse()
              .isOk()
              .hasContentType(controller.mimeType)
              .hasHeader("X-RateLimit-Limit", REQUESTS_PER_MINUTE.toString())
              .hasHeader("X-RateLimit-Remaining", (REQUESTS_PER_MINUTE - i).toString())
              .hasBodyWithJsonObject(cipher.encrypt("foo"), EncryptedValueRepresenter.class)
          }

          postWithApiHeader(controller.controllerBasePath(), [value: 'foo'])

          assertThatResponse()
            .isTooManyRequests()
            .hasHeader("X-RateLimit-Limit", REQUESTS_PER_MINUTE.toString())
            .hasHeader("X-RateLimit-Remaining", "0")
            .hasContentType(controller.mimeType)
            .hasJsonMessage(rateLimitExceeded())
        }

        @Test
        void "should service all requests when request rate is within limit"() {
          loginAsAdmin()

          (REQUESTS_PER_MINUTE).times { i ->
            postWithApiHeader(controller.controllerBasePath(), [value: 'foo'])
            ticker.forward(interval(REQUESTS_PER_MINUTE) - 1, TimeUnit.MILLISECONDS)

            assertThatResponse()
              .isOk()
              .hasContentType(controller.mimeType)
              .hasHeader("X-RateLimit-Limit", REQUESTS_PER_MINUTE.toString())
              .hasHeader("X-RateLimit-Remaining", (REQUESTS_PER_MINUTE - i).toString())
              .hasBodyWithJsonObject(cipher.encrypt("foo"), EncryptedValueRepresenter.class)
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
              .hasHeader("X-RateLimit-Limit", REQUESTS_PER_MINUTE.toString())
              .hasHeader("X-RateLimit-Remaining", REQUESTS_PER_MINUTE.toString())
              .hasBodyWithJsonObject(cipher.encrypt("foo"), EncryptedValueRepresenter.class)
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
    return new EncryptionControllerDelegate(new ApiAuthenticationHelper(securityService, goConfigService), cipher, REQUESTS_PER_MINUTE, ticker)
  }
}
