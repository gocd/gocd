/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.elastic.ElasticConfig;
import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.ServerSiteUrlConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.StringUtil;

import javax.annotation.PostConstruct;
import java.util.UUID;

@ConfigTag("server")
public class ServerConfig implements Validatable {
    public static final String SERVER_BACKUPS = "serverBackups";
    @ConfigAttribute(value = "artifactsdir", alwaysWrite = true) private String artifactsDir = "artifacts";
    @ConfigAttribute(value = "siteUrl", optional = true) private ServerSiteUrlConfig siteUrl = new ServerSiteUrlConfig();
    @ConfigAttribute(value = "secureSiteUrl", optional = true) private ServerSiteUrlConfig secureSiteUrl = new ServerSiteUrlConfig();
    @ConfigAttribute(value = "purgeStart", optional = true, allowNull = true) private Double purgeStart;
    @ConfigAttribute(value = "purgeUpto", optional = true, allowNull = true) private Double purgeUpto;
    @ConfigAttribute(value = "jobTimeout", optional = true) private String jobTimeout = "0";
    @ConfigAttribute(value="agentAutoRegisterKey", optional = true, allowNull = true) private String agentAutoRegisterKey;
    @ConfigAttribute(value="commandRepositoryLocation", alwaysWrite = true) private String commandRepositoryLocation = "default";

    @ConfigSubtag private ElasticConfig elasticConfig = new ElasticConfig();

    @SkipParameterResolution
    @ConfigAttribute(value = "serverId", optional = true, allowNull = true)
    private String serverId;

    @ConfigSubtag private SecurityConfig securityConfig = new SecurityConfig();
    @ConfigSubtag private MailHost mailHost = new MailHost(new GoCipher());

    private ConfigErrors errors = new ConfigErrors();

    public static final String JOB_TIMEOUT = "JOB_TIMEOUT";

    public static final String NEVER_TIMEOUT = "neverTimeout";
    public static final String OVERRIDE_TIMEOUT = "overrideTimeout";
    public static final String PURGE_START = "purgeStart";


    public ServerConfig() {
    }

    public ServerConfig(SecurityConfig securityConfig, MailHost mailHost, ServerSiteUrlConfig serverSiteUrl, ServerSiteUrlConfig secureSiteUrl) {
        this.securityConfig = securityConfig;
        this.mailHost = mailHost;
        this.siteUrl = serverSiteUrl;
        this.secureSiteUrl = secureSiteUrl;
    }

    @PostConstruct
    public void ensureServerIdExists() {
        if (serverId == null) {
            serverId = UUID.randomUUID().toString();
        }
    }

    @PostConstruct
    public void ensureAgentAutoregisterKeyExists() {
        if (agentAutoRegisterKey == null) {
            agentAutoRegisterKey = UUID.randomUUID().toString();
        }
    }

    public ServerConfig(SecurityConfig securityConfig, MailHost mailHost) {
        this(securityConfig, mailHost, new ServerSiteUrlConfig(), new ServerSiteUrlConfig());
    }

    public ServerConfig(String artifactsDir, SecurityConfig securityConfig) {
        this.artifactsDir = artifactsDir;
        this.securityConfig = securityConfig;
    }

    public ServerConfig(String artifactsDir, SecurityConfig securityConfig, Double purgeStart, Double purgeUpto) {
        this(artifactsDir, securityConfig);
        this.purgeStart = purgeStart;
        this.purgeUpto = purgeUpto;
    }

    public ServerConfig(String artifacts, SecurityConfig securityConfig, double purgeStart, double purgeUpto, String jobTimeout) {
        this(artifacts, securityConfig, purgeStart, purgeUpto);
        this.jobTimeout = jobTimeout;
    }

    public ServerConfig(String artifacts, SecurityConfig securityConfig, int purgeStart, int purgeUpto, String jobTimeout, String agentAutoRegisterKey) {
        this(artifacts, securityConfig, purgeStart, purgeUpto, jobTimeout);
        this.agentAutoRegisterKey = agentAutoRegisterKey;
    }

    public ServerConfig(ElasticConfig elasticConfig) {
        this.elasticConfig = elasticConfig;
    }

    public String artifactsDir() {
        return artifactsDir;
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

    public boolean anonymousAccess() {
        return securityConfig.anonymousAccess();
    }

    public MailHost mailHost() {
        return mailHost;
    }

    public void updateMailHost(MailHost mailHost) {
        this.mailHost.updateWithNew(mailHost);
    }

    public void updateArtifactRoot(String path) {
        this.artifactsDir = path;
    }

    public ElasticConfig getElasticConfig() {
        return elasticConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServerConfig)) {
            return false;
        }

        ServerConfig that = (ServerConfig) o;

        if (artifactsDir != null ? !artifactsDir.equals(that.artifactsDir) : that.artifactsDir != null) {
            return false;
        }
        if (jobTimeout != null ? !jobTimeout.equals(that.jobTimeout) : that.jobTimeout != null) {
            return false;
        }
        if (serverId != null ? !serverId.equals(that.serverId) : that.serverId != null) {
            return false;
        }
        if (agentAutoRegisterKey != null ? !agentAutoRegisterKey.equals(that.agentAutoRegisterKey) : that.agentAutoRegisterKey != null) {
            return false;
        }
        if (mailHost != null ? !mailHost.equals(that.mailHost) : that.mailHost != null) {
            return false;
        }
        if (purgeStart != null ? !purgeStart.equals(that.purgeStart) : that.purgeStart != null) {
            return false;
        }
        if (purgeUpto != null ? !purgeUpto.equals(that.purgeUpto) : that.purgeUpto != null) {
            return false;
        }
        if (secureSiteUrl != null ? !secureSiteUrl.equals(that.secureSiteUrl) : that.secureSiteUrl != null) {
            return false;
        }
        if (securityConfig != null ? !securityConfig.equals(that.securityConfig) : that.securityConfig != null) {
            return false;
        }
        if (siteUrl != null ? !siteUrl.equals(that.siteUrl) : that.siteUrl != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = artifactsDir != null ? artifactsDir.hashCode() : 0;
        result = 31 * result + (agentAutoRegisterKey != null ? agentAutoRegisterKey.hashCode() : 0);
        result = 31 * result + (siteUrl != null ? siteUrl.hashCode() : 0);
        result = 31 * result + (secureSiteUrl != null ? secureSiteUrl.hashCode() : 0);
        result = 31 * result + (purgeStart != null ? purgeStart.hashCode() : 0);
        result = 31 * result + (purgeUpto != null ? purgeUpto.hashCode() : 0);
        result = 31 * result + (jobTimeout != null ? jobTimeout.hashCode() : 0);
        result = 31 * result + (securityConfig != null ? securityConfig.hashCode() : 0);
        result = 31 * result + (mailHost != null ? mailHost.hashCode() : 0);
        return result;
    }

    /**
     * only used for test
     *
     * @deprecated
     */
    public void setArtifactsDir(String artifactsDir) {
        this.artifactsDir = artifactsDir;
    }

    /**
     * only used for test
     *
     * @deprecated
     */
    public void setMailHost(MailHost mailHost) {
        this.mailHost = mailHost;
    }

    /**
     * only used for test
     *
     * @deprecated
     */
    public void setSiteUrl(String siteUrl) {
        this.siteUrl = StringUtil.isBlank(siteUrl) ? new ServerSiteUrlConfig() : new ServerSiteUrlConfig(siteUrl);
    }

    /**
     * only used for test
     *
     * @deprecated
     */
    public void setSecureSiteUrl(String secureSiteUrl) {
         this.secureSiteUrl = StringUtil.isBlank(secureSiteUrl) ? new ServerSiteUrlConfig() : new ServerSiteUrlConfig(secureSiteUrl);
    }


    public void validate(ValidationContext validationContext) {
        if (!(purgeStart == null && purgeUpto == null)) {
            if (purgeUpto != null && (purgeStart == null || purgeStart == 0)) {
                errors().add(PURGE_START, "Error in artifact cleanup values. The trigger value is has to be specified when a goal is set");
            } else if (purgeStart > purgeUpto) {
                errors().add(PURGE_START, String.format("Error in artifact cleanup values. The trigger value (%sGB) should be less than the goal (%sGB)", purgeStart, purgeUpto));
            }
        }
        try {
            if (Double.parseDouble(jobTimeout) < 0) {
                errors().add(JOB_TIMEOUT, "Timeout cannot be a negative number as it represents number of minutes");
            }
        } catch (NumberFormatException e) {
            errors().add(JOB_TIMEOUT, "Timeout should be a valid number as it represents number of minutes");
        }
    }

    public ConfigErrors errors() {
        return errors;
    }

    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    public ServerSiteUrlConfig getSecureSiteUrl() {
        return secureSiteUrl;
    }

    public ServerSiteUrlConfig getSiteUrl() {
        return siteUrl;
    }

    public ServerSiteUrlConfig getSiteUrlPreferablySecured() {
        ServerSiteUrlConfig siteUrl = getSiteUrl();
        ServerSiteUrlConfig secureSiteUrlConfig = getSecureSiteUrl();
        if (secureSiteUrlConfig.hasNonNullUrl()) {
            return secureSiteUrlConfig;
        }
        if (!secureSiteUrlConfig.hasNonNullUrl()) {
            return siteUrl;
        }
        return new ServerSiteUrlConfig();
    }

    public ServerSiteUrlConfig getHttpsUrl() {
        ServerSiteUrlConfig siteUrlPreferSecured = getSiteUrlPreferablySecured();
        return siteUrlPreferSecured.isAHttpsUrl() ? siteUrlPreferSecured : new ServerSiteUrlConfig();
    }

    public boolean hasAnyUrlConfigured() {
        return siteUrl.hasNonNullUrl() || secureSiteUrl.hasNonNullUrl();
    }

    public Double getPurgeStart() {
        return purgeStart;
    }

    public Double getPurgeUpto() {
        return purgeUpto;
    }

    public boolean isArtifactPurgingAllowed() {
        return !(purgeStart == null || purgeUpto == null);
    }

    public void setPurgeLimits(Double purgeStart, Double purgeUpto) {
        this.purgeStart = purgeStart;
        this.purgeUpto = purgeUpto;
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

    public boolean shouldAutoRegisterAgentWith(String agentKey) {
        return (!StringUtil.isBlank(getAgentAutoRegisterKey())) && getAgentAutoRegisterKey().equals(agentKey);
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
}
