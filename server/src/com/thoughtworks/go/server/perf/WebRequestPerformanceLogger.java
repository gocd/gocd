package com.thoughtworks.go.server.perf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WebRequestPerformanceLogger {
    private final boolean shouldLog;
    private PerformanceLogger performanceLogger;

    @Autowired
    public WebRequestPerformanceLogger(PerformanceLogger performanceLogger) {
        this.performanceLogger = performanceLogger;
        this.shouldLog = performanceLogger.isLoggingTurnedOn();
    }

    public void logRequest(String uri, String requestor, int status, long contentCount, long requestStartTime, long requestEndTime) {
        performanceLogger.log("WEB-REQUEST {} {} {} {} {} {}", status, uri, requestor, contentCount, requestStartTime, requestEndTime);
    }

    public boolean isLoggingTurnedOn() {
        return shouldLog;
    }
}
