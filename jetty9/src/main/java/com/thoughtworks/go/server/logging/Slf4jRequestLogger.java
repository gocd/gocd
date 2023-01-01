/*
 * Copyright 2023 Thoughtworks, Inc.
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

import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jRequestLogger extends CustomRequestLog implements RequestLog {

    private static final Logger LOG = LoggerFactory.getLogger("org.eclipse.jetty.server.RequestLog");
    private static final String NCSA_FORMAT_WITH_LATENCY_MS = NCSA_FORMAT + " %{ms}T";

    public Slf4jRequestLogger() {
        super(LOG::info, NCSA_FORMAT_WITH_LATENCY_MS);
    }

    @Override
    public void log(Request request, Response response) {
        if (LOG.isInfoEnabled()) {
            super.log(request, response);
        }
    }
}
