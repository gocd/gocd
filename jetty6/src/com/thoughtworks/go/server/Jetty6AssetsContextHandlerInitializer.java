package com.thoughtworks.go.server;

import org.mortbay.component.LifeCycle;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.IOException;

public class Jetty6AssetsContextHandlerInitializer implements LifeCycle.Listener{
    private Jetty6AssetsContextHandler assetsContextHandler;
    private WebAppContext webAppContext;

    public Jetty6AssetsContextHandlerInitializer(Jetty6AssetsContextHandler assetsContextHandler, WebAppContext webAppContext){
        this.assetsContextHandler = assetsContextHandler;
        this.webAppContext = webAppContext;
    }

    @Override
    public void lifeCycleStarting(LifeCycle event) {
    }

    @Override
    public void lifeCycleStarted(LifeCycle event) {
        try {
            assetsContextHandler.init(webAppContext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void lifeCycleFailure(LifeCycle event, Throwable cause) {
    }

    @Override
    public void lifeCycleStopping(LifeCycle event) {
    }

    @Override
    public void lifeCycleStopped(LifeCycle event) {
    }
}
