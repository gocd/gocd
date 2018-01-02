/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.domain.oauth;

import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

public class OauthDomainEntityTest {
    class Foo {}

    private OauthDomainEntity<Foo> entity;

    @Before public void setUp() throws Exception {
        entity = new OauthDomainEntity<Foo>() {
            public Foo getDTO() {
                return null;
            }
        };
    }

    @Test
    public void shouldSetIdIfAvailable() {
        entity.setIdIfAvailable(Collections.singletonMap("id", 102L));
        assertThat(entity.getId(), is(102l));
    }

    @Test
    public void shouldNotSetIdIfBlank() {
        entity.setIdIfAvailable(Collections.singletonMap("id", -1L));
        assertThat(entity.getId(), is(-1l));
    }

    @Test
    public void shouldNotSetIdIfNull() {
        entity.setIdIfAvailable(new HashMap<String, String>());
        assertThat(entity.getId(), is(-1l));
    }

}
