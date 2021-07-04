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
package com.thoughtworks.go.server.web;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HstsHeaderTest {

    private SystemEnvironment systemEnvironment;

    @BeforeEach
    public void setUp() throws Exception {
        systemEnvironment = new SystemEnvironment();
    }

    @AfterEach
    public void tearDown() throws Exception {
        systemEnvironment.clearProperty("gocd.enable.hsts.header");
        systemEnvironment.clearProperty("gocd.hsts.header.max.age");
        systemEnvironment.clearProperty("gocd.hsts.header.include.subdomains");
        systemEnvironment.clearProperty("gocd.hsts.header.preload");
    }

    @Test
    public void shouldReturnEmptyWhenHstsIsNotEnabled() {
        Optional<HstsHeader> hstsHeader = HstsHeader.fromSystemEnvironment(systemEnvironment);
        assertThat(hstsHeader.isPresent(), is(false));
    }

    @Test
    public void shouldGenerateHstsHeaderWithDefaultMaxAge() {
        systemEnvironment.setProperty("gocd.enable.hsts.header", "true");
        Optional<HstsHeader> hstsHeader = HstsHeader.fromSystemEnvironment(systemEnvironment);
        assertThat(hstsHeader.isPresent(), is(true));
        assertThat(hstsHeader.get().headerName(), is("Strict-Transport-Security"));
        assertThat(hstsHeader.get().headerValue(), is("max-age=31536000"));
    }

    @Test
    public void shouldGenerateHstsHeaderWithSpecifiedMaxAge() {
        systemEnvironment.setProperty("gocd.enable.hsts.header", "true");
        systemEnvironment.setProperty("gocd.hsts.header.max.age", "12345");
        Optional<HstsHeader> hstsHeader = HstsHeader.fromSystemEnvironment(systemEnvironment);
        assertThat(hstsHeader.isPresent(), is(true));
        assertThat(hstsHeader.get().headerName(), is("Strict-Transport-Security"));
        assertThat(hstsHeader.get().headerValue(), is("max-age=12345"));
    }

    @Test
    public void shouldGenerateHstsHeaderWithSubdomainsOption() {
        systemEnvironment.setProperty("gocd.enable.hsts.header", "true");
        systemEnvironment.setProperty("gocd.hsts.header.include.subdomains", "true");
        Optional<HstsHeader> hstsHeader = HstsHeader.fromSystemEnvironment(systemEnvironment);
        assertThat(hstsHeader.isPresent(), is(true));
        assertThat(hstsHeader.get().headerName(), is("Strict-Transport-Security"));
        assertThat(hstsHeader.get().headerValue(), is("max-age=31536000; includeSubDomains"));
    }

    @Test
    public void shouldGenerateHstsHeaderWithPreloadOption() {
        systemEnvironment.setProperty("gocd.enable.hsts.header", "true");
        systemEnvironment.setProperty("gocd.hsts.header.preload", "true");
        Optional<HstsHeader> hstsHeader = HstsHeader.fromSystemEnvironment(systemEnvironment);
        assertThat(hstsHeader.isPresent(), is(true));
        assertThat(hstsHeader.get().headerName(), is("Strict-Transport-Security"));
        assertThat(hstsHeader.get().headerValue(), is("max-age=31536000; preload"));
    }

    @Test
    public void shouldGenerateHstsHeaderWithAllOptions() {
        systemEnvironment.setProperty("gocd.enable.hsts.header", "true");
        systemEnvironment.setProperty("gocd.hsts.header.max.age", "12345");
        systemEnvironment.setProperty("gocd.hsts.header.include.subdomains", "true");
        systemEnvironment.setProperty("gocd.hsts.header.preload", "true");
        Optional<HstsHeader> hstsHeader = HstsHeader.fromSystemEnvironment(systemEnvironment);
        assertThat(hstsHeader.isPresent(), is(true));
        assertThat(hstsHeader.get().headerName(), is("Strict-Transport-Security"));
        assertThat(hstsHeader.get().headerValue(), is("max-age=12345; includeSubDomains; preload"));
    }

}
