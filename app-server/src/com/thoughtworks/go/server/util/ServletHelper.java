package com.thoughtworks.go.server.util;

import com.thoughtworks.go.util.SystemEnvironment;

import java.lang.reflect.InvocationTargetException;

public abstract class ServletHelper {
    public abstract ServletRequest getRequest(javax.servlet.ServletRequest servletRequest);

    public abstract ServletResponse getResponse(javax.servlet.ServletResponse servletResponse);
    public abstract String encodeString(String string);

    public static ServletHelper getServerHelper(Boolean usingJetty9) {
        try {
            if (usingJetty9) {
                return getAppServerHelper("com.thoughtworks.go.server.util.Jetty9ServletHelper");
            }
            return getAppServerHelper("com.thoughtworks.go.server.util.Jetty6ServletHelper");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static com.thoughtworks.go.server.util.ServletHelper getAppServerHelper(String className) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        return (com.thoughtworks.go.server.util.ServletHelper) Class.forName(className).getConstructor().newInstance();
    }

}

