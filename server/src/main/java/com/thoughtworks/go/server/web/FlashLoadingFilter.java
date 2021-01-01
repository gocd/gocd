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

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * @understands loading flash object from session
 */
@Component
public class FlashLoadingFilter extends OncePerRequestFilter {
    public static final String FLASH_SESSION_KEY = "flash_session_key";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            FlashMessageService.useFlash(loadFlash(request));
            filterChain.doFilter(request, response);
        } finally {
            FlashMessageService.useFlash(null);
        }
    }

    private FlashMessageService.Flash loadFlash(HttpServletRequest req) {
        HttpSession session = req.getSession();
        FlashMessageService.Flash flash = (FlashMessageService.Flash) session.getAttribute(FLASH_SESSION_KEY);
        if (flash == null) {
            flash = new FlashMessageService.Flash();
            session.setAttribute(FLASH_SESSION_KEY, flash);
        }
        return flash;
    }
}
