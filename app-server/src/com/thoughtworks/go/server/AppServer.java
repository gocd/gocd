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

package com.thoughtworks.go.server;

import com.thoughtworks.go.util.SystemEnvironment;

import javax.net.ssl.SSLSocketFactory;

public abstract class AppServer {
    protected SystemEnvironment systemEnvironment;
    protected String password;
    protected SSLSocketFactory sslSocketFactory;

    public AppServer(SystemEnvironment systemEnvironment, String password, SSLSocketFactory sslSocketFactory){
        this.systemEnvironment = systemEnvironment;
        this.password = password;
        this.sslSocketFactory = sslSocketFactory;
    }

    abstract void addExtraJarsToClasspath(String extraClasspath);

    abstract void setCookieExpirePeriod(int cookieExpirePeriod);

    abstract void setInitParameter(String name, String value);

    abstract Throwable getUnavailableException();

    abstract void configure() throws Exception;

    abstract void start() throws Exception;

    abstract void stop() throws Exception;
}
