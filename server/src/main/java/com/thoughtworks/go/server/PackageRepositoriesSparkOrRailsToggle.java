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
import javax.servlet.http.HttpServletResponse;

public class PackageRepositoriesSparkOrRailsToggle {

    public void redirect(HttpServletRequest request, HttpServletResponse response) {
        if (Toggles.isToggleOn(Toggles.SHOW_OLD_PKG_REPOS_SPA)) {
            String requestURI = request.getRequestURI().replace("go/", "");
            request.setAttribute("url", "rails/" + requestURI);
        } else {
            request.setAttribute("url", "spark/admin/package_repositories");
        }
    }
}
