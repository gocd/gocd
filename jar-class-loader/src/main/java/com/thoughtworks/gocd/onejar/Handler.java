/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.gocd.onejar;

import com.thoughtworks.gocd.Boot;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {

    private static final String PROTOCOL = "onejar";

    private static final String JAVA_PROTOCOL_HANDLER = "java.protocol.handler.pkgs";

    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        // Add our 'onejar:' protocol handler, but leave open the
        // possibility of a subsequent class taking over the
        // factory.
        String handlerPackage = System.getProperty(JAVA_PROTOCOL_HANDLER);
        if (handlerPackage == null || handlerPackage.trim().isEmpty()) {
            handlerPackage = "";
        }
        if (handlerPackage.length() > 0) {
            handlerPackage = "|" + handlerPackage;
        }
        handlerPackage = Boot.class.getPackage().getName() + handlerPackage;
        System.setProperty(JAVA_PROTOCOL_HANDLER, handlerPackage);
        initialized = true;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        URLConnection urlConnection = new URLConnection(u) {

            @Override
            public void connect() throws IOException {

            }

            @Override
            public InputStream getInputStream() throws IOException {
                String file = u.getFile();
                return Boot.class.getResourceAsStream("/" + file);
            }
        };
        urlConnection.setUseCaches(false);
        return urlConnection;
    }

    public static URL toOneJarUrl(String name) {
        try {
            return new URL(PROTOCOL, null, -1, name);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
