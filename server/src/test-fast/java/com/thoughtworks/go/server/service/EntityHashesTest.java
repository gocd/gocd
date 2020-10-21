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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.AdminUser;
import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.MagicalGoConfigXmlWriter;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Collections;

import static com.thoughtworks.go.util.CachedDigestUtils.sha512_256Hex;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntityHashesTest {
    private EntityHashes hashes;

    @BeforeEach
    void setup() {
        hashes = new EntityHashes(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins());
    }

    @Test
    void digest_Strings() {
        assertEquals(sha512_256Hex("a/b/c"), hashes.digest("a", "b", "c"));
    }

    @Test
    void digestDomainNonConfigEntity() {
        assertEquals(sha512_256Hex("{\"hello\":\"world\"}"), hashes.digestDomainNonConfigEntity(Collections.singletonMap("hello", "world")));
    }

    @Nested
    class ConfigEntities {
        @BeforeEach
        void setup() {
            final MagicalGoConfigXmlWriter writer = mock(MagicalGoConfigXmlWriter.class);

            hashes = new EntityHashes(mock(ConfigCache.class), mock(ConfigElementImplementationRegistry.class)) {
                @Override
                protected String serializeDomainEntity(Object domainObject) {
                    return writer.toXmlPartial(domainObject);
                }
            };

            // mocking this as we aren't testing the XML serialization; that's supposed to be an abstraction!
            when(writer.toXmlPartial(any(AdminUser.class))).thenAnswer((Answer<String>) invocation -> {
                final AdminUser user = invocation.getArgument(0);
                return user.getName().toString();
            });
        }

        @Test
        void digest_Entities() {
            final String expected = sha512_256Hex(format("%s/%s",
                    sha512_256Hex("bilbo"),
                    sha512_256Hex("baggins")
            ));

            assertEquals(expected, hashes.digest(
                    Arrays.asList(
                            new AdminUser("bilbo"),
                            new AdminUser("baggins")
                    )
            ));
        }

        @Test
        void digestDomainConfigEntity() {
            assertEquals(sha512_256Hex("bilbo"), hashes.digestDomainConfigEntity(new AdminUser("bilbo")));
        }
    }
}