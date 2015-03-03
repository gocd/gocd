package com.thoughtworks.go.server;

import com.thoughtworks.go.util.SystemEnvironment;

import javax.net.ssl.SSLSocketFactory;
import java.util.HashMap;

public class AppServerStub extends AppServer {

    public HashMap<String, Object> calls = new HashMap<String, Object>();
    public HashMap<String, String> initparams = new HashMap<String, String>();

    public AppServerStub(SystemEnvironment systemEnvironment, String password, SSLSocketFactory sslSocketFactory) {
        super(systemEnvironment, password, sslSocketFactory);
    }

    @Override
    void addExtraJarsToClasspath(String extraClasspath) {
        calls.put("addExtraJarsToClasspath", extraClasspath);
    }

    @Override
    void setCookieExpirePeriod(int cookieExpirePeriod) {
        calls.put("setCookieExpirePeriod", cookieExpirePeriod);
    }

    @Override
    void setInitParameter(String name, String value) {
        initparams.put(name, value);
    }

    @Override
    void addStopServlet() {
        calls.put("addStopServlet", true);
    }

    @Override
    Throwable getUnavailableException() {
        calls.put("getUnavailableException", true);
        return null;
    }

    @Override
    void configure() throws Exception {
        calls.put("configure", true);
    }

    @Override
    void start() throws Exception {
        calls.put("start", true);
    }

    @Override
    void stop() throws Exception {
        calls.put("stop", true);

    }
}
