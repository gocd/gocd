/*
 * Copyright 2019 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.SecureSiteUrl;
import com.thoughtworks.go.domain.ServerSiteUrlConfig;
import com.thoughtworks.go.domain.SiteUrl;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.UUID;

@ConfigTag("server")
public class ServerConfig implements Validatable {
    public static final String SERVER_BACKUPS = "serverBackups";
    @ConfigAttribute(value = "jobTimeout", optional = true)
    private String jobTimeout = "0";
    @ConfigAttribute(value = "agentAutoRegisterKey", optional = true, allowNull = true)
    private String agentAutoRegisterKey;
    @ConfigAttribute(value = "webhookSecret", optional = true, allowNull = true)
    private String webhookSecret;
    @ConfigAttribute(value = "commandRepositoryLocation", alwaysWrite = true)
    private String commandRepositoryLocation = "default";

    @SkipParameterResolution
    @ConfigAttribute(value = "serverId", optional = true, allowNull = true)
    private String serverId;

    @ConfigSubtag
    private SiteUrls siteUrls;
    @ConfigSubtag
    private SecurityConfig securityConfig = new SecurityConfig();
    @ConfigSubtag
    private MailHost mailHost;
    @ConfigSubtag
    private BackupConfig backupConfig;

    public ArtifactConfig getArtifactConfig() {
        return artifactConfig;
    }

    @ConfigSubtag
    private ArtifactConfig artifactConfig = new ArtifactConfig();

    @ConfigAttribute(value = "tokenGenerationKey", allowNull = true)
    private String tokenGenerationKey;

    private ConfigErrors errors = new ConfigErrors();

    public static final String JOB_TIMEOUT = "JOB_TIMEOUT";

    public static final String NEVER_TIMEOUT = "neverTimeout";
    public static final String OVERRIDE_TIMEOUT = "overrideTimeout";
    public static final String PURGE_START = "purgeStart";
    public static final String ARTIFACT_DIR = "artifactsDir";


    public ServerConfig() {
    }

    public ServerConfig(SecurityConfig securityConfig, MailHost mailHost, SiteUrl siteUrl, SecureSiteUrl secureSiteUrl) {
        this.securityConfig = securityConfig;
        this.mailHost = mailHost;
        this.siteUrls = new SiteUrls(siteUrl, secureSiteUrl);
    }

    @PostConstruct
    public void ensureServerIdExists() {
        if (serverId == null) {
            serverId = UUID.randomUUID().toString();
        }
    }

    @PostConstruct
    public void ensureAgentAutoregisterKeyExists() {
        if (StringUtils.isBlank(agentAutoRegisterKey)) {
            agentAutoRegisterKey = UUID.randomUUID().toString();
        }
    }

    @PostConstruct
    public void ensureWebhookSecretExists() {
        if (StringUtils.isBlank(webhookSecret)) {
            webhookSecret = UUID.randomUUID().toString();
        }
    }

    @PostConstruct
    public void ensureTokenGenerationKeyExists() {
        if (StringUtils.isBlank(tokenGenerationKey)) {
            tokenGenerationKey = UUID.randomUUID().toString();
        }
    }

    @PostConstruct
    public void ensureArtifactConfigExists() {
        artifactConfig.ensureThatArtifactDirectoryExists();
    }

    public ServerConfig(SecurityConfig securityConfig, MailHost mailHost) {
        this(securityConfig, mailHost, new SiteUrl(), new SecureSiteUrl());
    }

    public ServerConfig(String artifactsDir, SecurityConfig securityConfig) {
        this.artifactConfig.setArtifactsDir(new ArtifactDirectory(artifactsDir));
        this.securityConfig = securityConfig;
    }

    public ServerConfig(String artifactsDir, SecurityConfig securityConfig, Double purgeStart, Double purgeUpto) {
        this(artifactsDir, securityConfig);
        this.setPurgeLimits(purgeStart, purgeUpto);
    }

    public ServerConfig(String artifacts, SecurityConfig securityConfig, double purgeStart, double purgeUpto, String jobTimeout) {
        this(artifacts, securityConfig, purgeStart, purgeUpto);
        this.jobTimeout = jobTimeout;
    }

    public ServerConfig(String artifacts, SecurityConfig securityConfig, int purgeStart, int purgeUpto, String jobTimeout, String agentAutoRegisterKey) {
        this(artifacts, securityConfig, purgeStart, purgeUpto, jobTimeout);
        this.agentAutoRegisterKey = agentAutoRegisterKey;
    }

    public String artifactsDir() {
        return artifactConfig.getArtifactsDir().getArtifactDir();
    }

    public boolean isSecurityEnabled() {
        return securityConfig.isSecurityEnabled();
    }

    public void useSecurity(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    public SecurityConfig security() {
        return this.securityConfig;
    }

    public MailHost mailHost() {
        return mailHost;
    }

    public void setArtifactConfig(ArtifactConfig artifactConfig) {
        this.artifactConfig = artifactConfig;
    }

    @Deprecated
    public void updateMailHost(MailHost mailHost) {
        // remove mailhost if default value
        if (mailHost != null && mailHost.equals(new MailHost())) {
            this.mailHost = null;
            return;
        }
        if (this.mailHost == null) {
            this.mailHost = mailHost;
        } else {
            this.mailHost.updateWithNew(mailHost);
        }
    }

    public void updateArtifactRoot(String path) {
        this.artifactConfig.setArtifactsDir(new ArtifactDirectory(path));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ServerConfig that = (ServerConfig) o;

        if (!Objects.equals(artifactConfig, that.artifactConfig)) {
            return false;
        }
        if (getSiteUrl() != null ? !getSiteUrl().equals(that.getSiteUrl()) : that.getSiteUrl() != null) {
            return false;
        }
        if (getSecureSiteUrl() != null ? !getSecureSiteUrl().equals(that.getSecureSiteUrl()) : that.getSecureSiteUrl() != null) {
            return false;
        }
        if (jobTimeout != null ? !jobTimeout.equals(that.jobTimeout) : that.jobTimeout != null) {
            return false;
        }
        if (agentAutoRegisterKey != null ? !agentAutoRegisterKey.equals(that.agentAutoRegisterKey) : that.agentAutoRegisterKey != null) {
            return false;
        }
        if (webhookSecret != null ? !webhookSecret.equals(that.webhookSecret) : that.webhookSecret != null) {
            return false;
        }
        if (commandRepositoryLocation != null ? !commandRepositoryLocation.equals(that.commandRepositoryLocation) : that.commandRepositoryLocation != null) {
            return false;
        }
        if (serverId != null ? !serverId.equals(that.serverId) : that.serverId != null) {
            return false;
        }
        if (securityConfig != null ? !securityConfig.equals(that.securityConfig) : that.securityConfig != null) {
            return false;
        }
        if (mailHost != null ? !mailHost.equals(that.mailHost) : that.mailHost != null) {
            return false;
        }
        if (tokenGenerationKey != null ? !tokenGenerationKey.equals(that.tokenGenerationKey) : that.tokenGenerationKey != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifactConfig, jobTimeout, agentAutoRegisterKey, webhookSecret, commandRepositoryLocation, serverId, siteUrls, securityConfig, mailHost, backupConfig, tokenGenerationKey, errors);
    }

    /**
     * only used for test
     *
     * @deprecated
     */
    public void setArtifactsDir(String artifactsDir) {
        this.artifactConfig.setArtifactsDir(new ArtifactDirectory(artifactsDir));
    }

    public void setMailHost(MailHost mailHost) {
        this.mailHost = mailHost;
    }

    /**
     * only used for test
     *
     * @deprecated
     */
    public void setSiteUrl(String siteUrl) {
        getSiteUrls().setSiteUrl(StringUtils.isBlank(siteUrl) ? new SiteUrl() : new SiteUrl(siteUrl));
    }

    /**
     * only used for test
     *
     * @deprecated
     */
    public void setSecureSiteUrl(String secureSiteUrl) {
        getSiteUrls().setSecureSiteUrl(StringUtils.isBlank(secureSiteUrl) ? new SecureSiteUrl() : new SecureSiteUrl(secureSiteUrl));
    }


    @Override
    public void validate(ValidationContext validationContext) {
        artifactConfig.validate(validationContext);
        try {
            if (Double.parseDouble(jobTimeout) < 0) {
                errors().add(JOB_TIMEOUT, "Timeout cannot be a negative number as it represents number of minutes");
            }
        } catch (NumberFormatException e) {
            errors().add(JOB_TIMEOUT, "Timeout should be a valid number as it represents number of minutes");
        }
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    public SiteUrls getSiteUrls() {
        if (this.siteUrls == null) {
            this.siteUrls = new SiteUrls();
        }

        return siteUrls;
    }

    public SecureSiteUrl getSecureSiteUrl() {
        return getSiteUrls().getSecureSiteUrl();
    }

    public SiteUrl getSiteUrl() {
        return getSiteUrls().getSiteUrl();
    }


    public ServerSiteUrlConfig getSiteUrlPreferablySecured() {
        SiteUrl siteUrl = getSiteUrl();
        SecureSiteUrl secureSiteUrlConfig = getSecureSiteUrl();
        if (secureSiteUrlConfig.hasNonNullUrl()) {
            return secureSiteUrlConfig;
        }
        if (!secureSiteUrlConfig.hasNonNullUrl()) {
            return siteUrl;
        }
        return new SiteUrl();
    }

    public ServerSiteUrlConfig getHttpsUrl() {
        ServerSiteUrlConfig siteUrlPreferSecured = getSiteUrlPreferablySecured();
        return siteUrlPreferSecured.isAHttpsUrl() ? siteUrlPreferSecured : new SecureSiteUrl();
    }

    public boolean hasAnyUrlConfigured() {
        return getSiteUrl().hasNonNullUrl() || getSecureSiteUrl().hasNonNullUrl();
    }

    public Double getPurgeStart() {
        return artifactConfig.getPurgeSettings().getPurgeStart().getPurgeStartDiskSpace();
    }

    public Double getPurgeUpto() {
        return artifactConfig.getPurgeSettings().getPurgeUpto().getPurgeUptoDiskSpace();
    }

    public boolean isArtifactPurgingAllowed() {
        return !(getPurgeStart() == null || getPurgeUpto() == null);
    }

    public void setPurgeLimits(Double purgeStart, Double purgeUpto) {
        this.artifactConfig.getPurgeSettings().setPurgeStart(new PurgeStart(purgeStart));
        this.artifactConfig.getPurgeSettings().setPurgeUpto(new PurgeUpto(purgeUpto));
    }

    public String getJobTimeout() {
        return jobTimeout;
    }

    /**
     * @deprecated Used only in tests
     */
    public void setJobTimeout(String jobTimeout) {
        this.jobTimeout = jobTimeout;
    }

    public String getTimeoutType() {
        return "0".equals(jobTimeout) ? NEVER_TIMEOUT : OVERRIDE_TIMEOUT;
    }

    public String getAgentAutoRegisterKey() {
        return agentAutoRegisterKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public boolean shouldAutoRegisterAgentWith(String agentKey) {
        return (!StringUtils.isBlank(getAgentAutoRegisterKey())) && getAgentAutoRegisterKey().equals(agentKey);
    }

    public String getServerId() {
        return serverId;
    }

    public String getCommandRepositoryLocation() {
        return commandRepositoryLocation;
    }

    public void setCommandRepositoryLocation(String location) {
        this.commandRepositoryLocation = location;
    }

    public String getTokenGenerationKey() {
        return tokenGenerationKey;
    }

    public BackupConfig getBackupConfig() {
        return backupConfig;
    }

    public void setBackupConfig(BackupConfig backupConfig) {
        this.backupConfig = backupConfig;
    }
}
