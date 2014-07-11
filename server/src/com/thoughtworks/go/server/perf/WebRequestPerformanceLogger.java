package com.thoughtworks.go.server.perf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WebRequestPerformanceLogger {
    private PerformanceLogger performanceLogger;

    @Autowired
    public WebRequestPerformanceLogger(PerformanceLogger performanceLogger) {
        this.performanceLogger = performanceLogger;
    }

    public void logRequest(String uri, String requestor, int status, long contentCount, long amountOfTimeItTookInMilliseconds) {
        performanceLogger.log("WEB-REQUEST {} {} {} {} {}", status, uri, requestor, contentCount, amountOfTimeItTookInMilliseconds);
    }
}
