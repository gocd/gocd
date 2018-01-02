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

import java.lang.reflect.InvocationTargetException;

public abstract class ServletHelper {
    public abstract ServletRequest getRequest(javax.servlet.ServletRequest servletRequest);

    public abstract ServletResponse getResponse(javax.servlet.ServletResponse servletResponse);

    private static ServletHelper instance;

    public static void init() {
        try {
            instance = getAppServerHelper("com.thoughtworks.go.server.util.Jetty9ServletHelper");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ServletHelper getInstance() {
        return instance;
    }

    private static com.thoughtworks.go.server.util.ServletHelper getAppServerHelper(String className) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        return (com.thoughtworks.go.server.util.ServletHelper) Class.forName(className).getConstructor().newInstance();
    }
}

