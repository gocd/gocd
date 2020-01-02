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
package com.thoughtworks.go.server;

import com.thoughtworks.go.server.service.support.toggle.Toggles;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class SparkOrSpringToggle {

    @SuppressWarnings({"PMD.UnusedPrivateMethod", "unused"})
    private void basedOnToggle(String toggle, HttpServletRequest request) throws IOException {
        if (Toggles.isToggleOn(toggle)) {
            request.setAttribute("sparkOrSpring", "spring");
        } else {
            request.setAttribute("sparkOrSpring", "spark");
        }
    }
}
