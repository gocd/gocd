package com.thoughtworks.go.server.perf;

import com.thoughtworks.go.domain.JobIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AgentCommunicationPerformanceLogger {
    private PerformanceLogger performanceLogger;

    @Autowired
    public AgentCommunicationPerformanceLogger(PerformanceLogger performanceLogger) {
        this.performanceLogger = performanceLogger;
    }

    public void logPing(String agentUUID, long startTime, long endTime) {
        performanceLogger.log("AGENT-PING {} {} {}", agentUUID, startTime, endTime);
    }

    public void logReporting(String agentUUID, JobIdentifier jobIdentifier, String thingBeingReported, long startTime, long endTime) {
        performanceLogger.log("AGENT-REPORT {} {} {} {} {} {}", agentUUID, thingBeingReported, startTime, endTime, jobIdentifier);
    }

    public void logIsIgnoredCheck(JobIdentifier jobIdentifier, long startTime, long endTime) {
        performanceLogger.log("AGENT-IGN-CHECK {} {} {}", startTime, endTime, jobIdentifier);
    }
}
