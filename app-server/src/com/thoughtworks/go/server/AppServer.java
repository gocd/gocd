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

    abstract void addStopServlet();

    abstract Throwable getUnavailableException();

    abstract void configure() throws Exception;

    abstract void start() throws Exception;

    abstract void stop() throws Exception;
}
