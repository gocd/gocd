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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.ServerSiteUrlConfig;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.controller.beans.GoMailSenderProvider;
import com.thoughtworks.go.server.security.LdapContextSourceConfigurator;
import com.thoughtworks.go.server.security.LdapUserSearch;
import com.thoughtworks.go.server.service.result.DefaultLocalizedResult;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedResult;
import com.thoughtworks.go.server.web.BaseUrlProvider;
import com.thoughtworks.go.validators.HostNameValidator;
import com.thoughtworks.go.validators.PortValidator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.GoConstants.TEST_EMAIL_SUBJECT;

@Service
public class ServerConfigService implements BaseUrlProvider {
    private GoConfigService goConfigService;
    private UserService userService;

    private GoMailSenderProvider provider = GoMailSenderProvider.DEFAULT_PROVIDER;
    static final String ANY_USER = "GO_TEST_USER";


    @Autowired
    public ServerConfigService(GoConfigService goConfigService, UserService userService) {
        this.goConfigService = goConfigService;
        this.userService = userService;
    }

    public void updateServerConfig(MailHost mailHost, LdapConfig ldapConfig, PasswordFileConfig passwordFileConfig, String artifactsDir,
                                   Double purgeStart, Double purgeUpto, String jobTimeout, boolean shouldAllowAutoLogin, String siteUrl, String secureSiteUrl,
                                   String taskRepositoryLocation, final HttpLocalizedOperationResult result, final String md5) {
        if (!mailHost.equals(new MailHost(new GoCipher()))) {
            validate(mailHost, result);
        }

        if (!shouldAllowAutoLogin && !userService.canUserTurnOffAutoLogin()) {
            result.notAcceptable(LocalizedMessage.string("CANNOT_TURN_OFF_AUTO_LOGIN"));
            return;
        }

        if (result.isSuccessful()) {
            try {
                ConfigSaveState configSaveState = goConfigService.updateServerConfig(mailHost, ldapConfig, passwordFileConfig, shouldAllowAutoLogin, md5, artifactsDir, purgeStart,
                        purgeUpto, jobTimeout, siteUrl,
                        secureSiteUrl, taskRepositoryLocation);
                if (ConfigSaveState.MERGED.equals(configSaveState)) {
                    result.setMessage(LocalizedMessage.composite(LocalizedMessage.string("SAVED_CONFIGURATION_SUCCESSFULLY"), LocalizedMessage.string("CONFIG_MERGED")));
                } else if (ConfigSaveState.UPDATED.equals(configSaveState)) {
                    result.setMessage(LocalizedMessage.string("SAVED_CONFIGURATION_SUCCESSFULLY"));
                }
            } catch (RuntimeException exception) {
                updateFailed(exception.getMessage(), result);
            }
        }
    }

    private void updateFailed(String description, HttpLocalizedOperationResult result) {
        result.badRequest(LocalizedMessage.string("FAILED_TO_SAVE_THE_SERVER_CONFIGURATION", description));
    }

    private void validate(MailHost mailHost, LocalizedOperationResult operationResult) {
        DefaultLocalizedResult result = (DefaultLocalizedResult) validateHostName(mailHost.getHostName());
        if (!result.isSuccessful()) {
            operationResult.notAcceptable(result.localizable());
        }
        result = (DefaultLocalizedResult) validatePort(mailHost.getPort());
        if (!result.isSuccessful()) {
            operationResult.notAcceptable(result.localizable());
        }
        result = (DefaultLocalizedResult) validateEmail(mailHost.getFrom());
        if (!result.isSuccessful()) {
            operationResult.notAcceptable(LocalizedMessage.string("INVALID_FROM_ADDRESS", mailHost.getFrom()));
        }
        result = (DefaultLocalizedResult) validateEmail(mailHost.getAdminMail());
        if (!result.isSuccessful()) {
            operationResult.notAcceptable(LocalizedMessage.string("INVALID_ADMIN_ADDRESS", mailHost.getAdminMail()));
        }
    }

    public LocalizedResult validateHostName(String hostName) {
        DefaultLocalizedResult result = new DefaultLocalizedResult();
        new HostNameValidator().validate(hostName, result);
        return result;
    }

    public LocalizedResult validatePort(int port) {
        DefaultLocalizedResult result = new DefaultLocalizedResult();
        new PortValidator().validate(port, result);
        return result;
    }

    public LocalizedResult validateEmail(String email) {
        DefaultLocalizedResult result = new DefaultLocalizedResult();
        if (!EmailValidator.getInstance().isValid(email)) {
            result.invalid("INVALID_EMAIL", email);
        }
        return result;
    }

    public void sendTestMail(MailHost mailHost, LocalizedOperationResult result) {
        validate(mailHost, result);
        if (!result.isSuccessful()) {
            return;
        }
        GoMailSender sender = provider.createSender(mailHost);
        ValidationBean validationBean = sender.send(TEST_EMAIL_SUBJECT, GoSmtpMailSender.emailBody(), mailHost.getAdminMail());
        if (!validationBean.isValid()) {
            result.notAcceptable(LocalizedMessage.string("FAILED_TO_SEND_TEST_MAIL", validationBean.getError()));
        }
    }

    public void validateLdapSettings(LdapConfig ldapConfig, HttpLocalizedOperationResult result) {
        try {
            new LdapUserSearch(goConfigService, ldapContextSource(ldapConfig)).search(ANY_USER, ldapConfig);
        } catch (LdapUserSearch.NotAllResultsShownException ex) {
            // Connected to ldap sucessfully. Dont care about results.
        } catch (RuntimeException e) {
            result.connectionError(LocalizedMessage.string("CANNOT_CONNECT_TO_LDAP", e.getMessage()));
        }
    }

    DefaultSpringSecurityContextSource ldapContextSource(LdapConfig ldapConfig) {
        DefaultSpringSecurityContextSource source = new DefaultSpringSecurityContextSource(ldapConfig.uri());

        //so user can define the variable java.naming.referral=follow in the server.sh
        source.setBaseEnvironmentProperties(System.getProperties());
        new LdapContextSourceConfigurator(ldapConfig).configure(source);
        try {
            source.afterPropertiesSet();
        } catch (Exception e) {
            bomb("Cannot create ldap context", e);
        }
        return source;
    }

    public String siteUrlFor(String url, boolean forceSsl) throws URISyntaxException {
        String scheme = new URI(url).getScheme();
        ServerSiteUrlConfig siteUrl = forceSsl || (scheme != null && scheme.equals("https")) ? getSecureSiteUrl() : serverConfig().getSiteUrl();
        return siteUrl.siteUrlFor(url, false);
    }

    private ServerSiteUrlConfig getSecureSiteUrl() {
        return serverConfig().getHttpsUrl();
    }

    private ServerConfig serverConfig() {
        return goConfigService.getCurrentConfig().server();
    }

    public String getAutoregisterKey(){
        return serverConfig().getAgentAutoRegisterKey();
    }

    public boolean hasAutoregisterKey() {
        return StringUtils.isNotBlank(getAutoregisterKey());
    }

    public boolean hasAnyUrlConfigured() {
        return serverConfig().hasAnyUrlConfigured();
    }

    public Long elasticJobStarvationThreshold() {
        return serverConfig().getElasticConfig().getJobStarvationTimeout();
    }
}
