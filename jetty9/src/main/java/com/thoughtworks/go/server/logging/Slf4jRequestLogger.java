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
package com.thoughtworks.go.server.logging;

import org.eclipse.jetty.server.AbstractNCSARequestLog;
import org.eclipse.jetty.server.RequestLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jRequestLogger extends AbstractNCSARequestLog implements RequestLog {

    private static Logger LOG = LoggerFactory.getLogger("org.eclipse.jetty.server.RequestLog");

    public Slf4jRequestLogger() {
        super(requestEntry -> LOG.info(requestEntry));
    }

    @Override
    protected boolean isEnabled() {
        return LOG.isInfoEnabled();
    }

}
