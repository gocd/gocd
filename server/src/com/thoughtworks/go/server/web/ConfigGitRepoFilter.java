/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.service.ConfigRepository;
import org.eclipse.jgit.http.server.GitFilter;
import org.eclipse.jgit.transport.resolver.FileResolver;
import org.springframework.web.context.ContextLoader;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class ConfigGitRepoFilter extends GitFilter {
    public void destroy() {
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws ServletException, IOException {
        super.doFilter(req, resp, chain);
    }

    public void init(FilterConfig config) throws ServletException {
        ConfigRepository configRepository = ContextLoader.getCurrentWebApplicationContext().getBean(ConfigRepository.class);
        FileResolver<HttpServletRequest> resolver = new FileResolver<>();
        resolver.exportRepository("config-repository", configRepository.getGitRepo());
        setRepositoryResolver(resolver);
        setReceivePackFactory(null);

        super.init(config);

    }

}
