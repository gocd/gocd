/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.web;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.UUID;

import static com.thoughtworks.go.util.StringUtil.isBlank;

@Component
public class TokenManager {

    private static final String TOKEN = "post.token";

    void create(HttpSession session) {
        synchronized (session) {
            if (null == session.getAttribute(TOKEN)) {
                session.setAttribute(TOKEN, UUID.randomUUID().toString());
            }
        }
    }

    public boolean verify(HttpServletRequest request) {
        String postedToken = request.getParameter(TOKEN);
        String expectedToken = (String) request.getSession().getAttribute(TOKEN);
        return !isBlank(postedToken) && !isBlank(expectedToken) && postedToken.equals(expectedToken);
    }
}
