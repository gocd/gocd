package com.thoughtworks.go.server.util;

import com.thoughtworks.go.util.SystemEnvironment;

import java.lang.reflect.InvocationTargetException;

public abstract class ServletHelper {
    public abstract ServletRequest getRequest(javax.servlet.ServletRequest servletRequest);

    public abstract ServletResponse getResponse(javax.servlet.ServletResponse servletResponse);

    public abstract String encodeString(String string);

    private static ServletHelper instance;

    public static void init(Boolean usingJetty9) {
        try {
            if (usingJetty9) {
                instance = getAppServerHelper("com.thoughtworks.go.server.util.Jetty9ServletHelper");
            } else {
                instance = getAppServerHelper("com.thoughtworks.go.server.util.Jetty6ServletHelper");
            }
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

