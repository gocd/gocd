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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class EphemeralAutoRegisterKeyServiceTest {
    @Mock
    SystemEnvironment systemEnvironment;
    EphemeralAutoRegisterKeyService service;

    @BeforeEach
    void setUp() {
        initMocks(this);
        service = new EphemeralAutoRegisterKeyService(systemEnvironment);
    }

    @Nested
    class autoRegisterKey {
        @Test
        void shouldGenerateANewAutoRegisterKey() {
            String autoRegisterKey = service.autoRegisterKey();

            assertThat(autoRegisterKey).isNotBlank();
        }
    }

    @Nested
    class validateAndRevoke {
        @Test
        void shouldBeAValidKeyIfCheckingForValidityForTheFirstTime() {
            when(systemEnvironment.getEphemeralAutoRegisterKeyExpiryInMillis()).thenReturn(10000L);

            service = new EphemeralAutoRegisterKeyService(systemEnvironment);

            String autoRegisterKey = service.autoRegisterKey();

            assertThat(service.validateAndRevoke(autoRegisterKey)).isTrue();
        }

        @Test
        void shouldRevokeKeyOnceValidated() {
            when(systemEnvironment.getEphemeralAutoRegisterKeyExpiryInMillis()).thenReturn(10000L);

            service = new EphemeralAutoRegisterKeyService(systemEnvironment);

            String autoRegisterKey = service.autoRegisterKey();

            assertThat(service.validateAndRevoke(autoRegisterKey)).isTrue();
            assertThat(service.validateAndRevoke(autoRegisterKey)).isFalse();
        }

        @Test
        void shouldNotBeValidIfKeyIsExpired() throws InterruptedException {
            when(systemEnvironment.getEphemeralAutoRegisterKeyExpiryInMillis()).thenReturn(1L);

            service = new EphemeralAutoRegisterKeyService(systemEnvironment);

            String autoRegisterKey = service.autoRegisterKey();

            sleep(10L);
            assertThat(service.validateAndRevoke(autoRegisterKey)).isFalse();
        }
    }
}
