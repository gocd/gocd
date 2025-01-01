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
package com.thoughtworks.go.server.web;

import com.thoughtworks.go.presentation.FlashMessageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class FlashLoadingFilterTest {
    private FlashLoadingFilter filter;
    private FlashMessageModel flash;
    private FlashMessageService service;
    private String messageKey;

    @BeforeEach
    public void setUp() {
        service = new FlashMessageService();
        filter = new FlashLoadingFilter();
        flash = null;
        messageKey = null;
    }

    @Test
    public void shouldInitializeFlashIfNotPresent() throws IOException, ServletException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) {
                messageKey = service.add(new FlashMessageModel("my message", "error"));
                flash = service.get(messageKey);
            }
        };
        assertThat(messageKey).isNull();
        filter.doFilter(req, res, filterChain);
        assertThat(messageKey).isNotNull();
        assertThat(flash.toString()).isEqualTo("my message");
        assertThat(flash.getFlashClass()).isEqualTo("error");
    }

    @Test
    public void shouldLoadExistingFlashFromSession() throws IOException, ServletException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();

        FlashMessageService.Flash oldFlash = new FlashMessageService.Flash();
        oldFlash.put("my_key", new FlashMessageModel("my other message", "warning"));

        session.putValue(FlashLoadingFilter.FLASH_SESSION_KEY, oldFlash);
        req.setSession(session);

        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) {
                flash = service.get("my_key");
            }
        };

        filter.doFilter(req, res, filterChain);
        assertThat(flash.toString()).isEqualTo("my other message");
        assertThat(flash.getFlashClass()).isEqualTo("warning");
    }

    @Test
    public void shouldClearThreadContext() throws IOException, ServletException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) {
                messageKey = service.add(new FlashMessageModel("my message", "error"));
                flash = service.get(messageKey);
            }
        };
        filter.doFilter(req, res, filterChain);
        assertThat(flash.toString()).isEqualTo("my message");
        try {
            service.get(messageKey);
            fail("attempt to load flash message should fail, as no thread local is cleared out");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("No flash context found, this call should only be made within a request.");
        }
    }

    @Test
    public void shouldClearThreadContextInCaseOfExceptionAsWell() throws IOException, ServletException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) {
                messageKey = service.add(new FlashMessageModel("my message", "error"));
                flash = service.get(messageKey);
                throw new RuntimeException("exception here");
            }
        };
        try {
            filter.doFilter(req, res, filterChain);
            fail("exception gobbled");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("exception here");
        }
        assertThat(flash.toString()).isEqualTo("my message");

        try {
            service.get(messageKey);
            fail("attempt to load flash message should fail, as no thread local is cleared out");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("No flash context found, this call should only be made within a request.");
        }
    }

    @Test
    public void shouldFailForNonHttpReqeusts() throws IOException, ServletException {
        try {
            filter.doFilter(mock(ServletRequest.class), mock(ServletResponse.class), new MockFilterChain());
            fail("should not process non HTTP requests");
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("OncePerRequestFilter just supports HTTP requests");
        }
    }
}
