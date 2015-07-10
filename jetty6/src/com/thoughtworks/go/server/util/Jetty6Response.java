/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.util;

import org.mortbay.jetty.Response;

import javax.servlet.ServletResponseWrapper;

public class Jetty6Response implements ServletResponse {
    private javax.servlet.ServletResponse servletResponse;

    public Jetty6Response(javax.servlet.ServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }

    @Override
    public int getStatus() {
        return response().getStatus();
    }

    @Override
    public long getContentCount() {
        return response().getContentCount();
    }

    /* Handle single-level of wrapping in response (usually for gzip filtered responses). */
    private Response response() {
        if (servletResponse instanceof ServletResponseWrapper) {
            return (Response) ((ServletResponseWrapper) servletResponse).getResponse();
        }
        return (Response) servletResponse;
    }
}
