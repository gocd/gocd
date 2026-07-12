/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.activation.test;

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.net.Socket;
import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "ResultOfMethodCallIgnored", "removal", "ThrowableNotThrown"})
@Extension
public class ClassWhichUsesVariousJdkPackages implements GoPlugin {
    public ClassWhichUsesVariousJdkPackages() {
        // jdk.net
        System.out.println(jdk.net.Sockets.supportedOptions(Socket.class));

        // javax
        var documentBuilderFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        documentBuilderFactory.getClass();

        // org.w3c
        var domException = new org.w3c.dom.DOMException((short) 1, "abc");
        domException.getClass();

        // org.xml.sax
        org.xml.sax.Attributes locator = new org.xml.sax.helpers.AttributesImpl();
        locator.getClass();

        // Double-check the logger
        Logger.getLoggerFor(ClassWhichUsesVariousJdkPackages.class).info("ClassWhichUsesVariousJdkPackages initialized");
    }

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest requestMessage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("some-extension-type", List.of("1.0"));
    }
}
