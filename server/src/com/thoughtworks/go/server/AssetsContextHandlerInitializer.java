package com.thoughtworks.go.server;


import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.IOException;

public class AssetsContextHandlerInitializer implements LifeCycle.Listener{
    private AssetsContextHandler assetsContextHandler;
    private WebAppContext webAppContext;

    public AssetsContextHandlerInitializer(AssetsContextHandler assetsContextHandler, WebAppContext webAppContext){
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
