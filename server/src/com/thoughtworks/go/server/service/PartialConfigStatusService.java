package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.ChangedRepoConfigWatchListListener;
import com.thoughtworks.go.config.GoConfigWatchList;
import com.thoughtworks.go.config.PartialConfigUpdateCompletedListener;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.server.materials.MaterialUpdateMessage;
import com.thoughtworks.go.server.messaging.GoMessageListener;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @understands current status of a partial configuration
 *
 * This service always has a current status of each configuration material
 */
public class PartialConfigStatusService implements PartialConfigUpdateCompletedListener,
        GoMessageListener<MaterialUpdateMessage>, ChangedRepoConfigWatchListListener {

    private GoConfigWatchList configWatchList;
    // key is fingerprint, value is trackingId
    private ConcurrentHashMap<String, ConfigStatus> activeParts = new ConcurrentHashMap<String, ConfigStatus>();

    //TODO talk to server health service

    /**
     * Waits until all configuration sources are polled and successfully parsed.
     */
    public void waitAllReady()
    {
        throw new RuntimeException("TODO: Not implemented yet");
    }

    public boolean isReady(String fingerprint){
        ConfigStatus status = activeParts.get(activeParts);
        if(status == null)
            return  false;
        if(status.isNotBeingUpdated() && status.isLastParseSuccess())
        {
            return  true;
        }
        return false;
    }

    public boolean isInProgress(String fingerprint){
        throw new RuntimeException("TODO: Not implemented yet");
    }

    @Override
    public void onMessage(MaterialUpdateMessage message) {
        String fingerprint = message.getMaterial().getFingerprint();
        if(this.configWatchList.hasConfigRepoWithFingerprint(fingerprint))
        {
            activeParts.putIfAbsent(fingerprint, new ConfigStatus());

            ConfigStatus status = activeParts.get(fingerprint);
            status.setLastStartedTrackingId(message.trackingId());
        }
    }

    @Override
    public void onChangedRepoConfigWatchList(ConfigReposConfig newConfigRepos) {
        throw new RuntimeException("TODO: Not implemented yet");
    }

    @Override
    public void onFailedPartialConfig(ConfigRepoConfig repoConfig, Exception ex) {
        String fingerprint = repoConfig.getMaterialConfig().getFingerprint();
        if(this.configWatchList.hasConfigRepoWithFingerprint(fingerprint))
        {
            activeParts.putIfAbsent(fingerprint, new ConfigStatus());

            ConfigStatus status = activeParts.get(fingerprint);
            status.setLastParseSuccess(false);
        }
        else
        {
            // parsed something not on repo list, why?
        }
    }

    @Override
    public void onSuccessPartialConfig(ConfigRepoConfig repoConfig, PartialConfig newPart) {
        String fingerprint = repoConfig.getMaterialConfig().getFingerprint();
        if(this.configWatchList.hasConfigRepoWithFingerprint(fingerprint))
        {
            activeParts.putIfAbsent(fingerprint, new ConfigStatus());

            ConfigStatus status = activeParts.get(fingerprint);
            status.setLastParseSuccess(true);
        }
        else
        {
            // parsed something not on repo list, why?
        }
    }

    private class ConfigStatus
    {
        private long lastStartedTrackingId = -1;
        private long lastCompletedTrackingId = -1;
        private boolean lastParseSuccess = false;

        public long getLastStartedTrackingId() {
            return lastStartedTrackingId;
        }

        public void setLastStartedTrackingId(long lastStartedTrackingId) {
            this.lastStartedTrackingId = lastStartedTrackingId;
        }

        public long getLastCompletedTrackingId() {
            return lastCompletedTrackingId;
        }

        public void setLastCompletedTrackingId(long lastCompletedTrackingId) {
            this.lastCompletedTrackingId = lastCompletedTrackingId;
        }

        public boolean isLastParseSuccess() {
            return lastParseSuccess;
        }

        public void setLastParseSuccess(boolean lastParseSuccess) {
            this.lastParseSuccess = lastParseSuccess;
        }

        public boolean isNotBeingUpdated() {
            return lastStartedTrackingId == lastCompletedTrackingId;
        }
        public boolean isBeingUpdated() {
            return lastStartedTrackingId > lastCompletedTrackingId;
        }
    }

}
