/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.update.CreateOrUpdateConfigServerSiteUrlsCommand;
import com.thoughtworks.go.config.update.CreateOrUpdateDefaultJobTimeoutCommand;
import com.thoughtworks.go.config.update.UpdateArtifactConfigCommand;
import com.thoughtworks.go.domain.ServerSiteUrlConfig;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.web.BaseUrlProvider;
import com.thoughtworks.go.validators.HostNameValidator;
import com.thoughtworks.go.validators.PortValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import static com.thoughtworks.go.server.newsecurity.utils.SessionUtils.currentUsername;
import static com.thoughtworks.go.util.GoConstants.TEST_EMAIL_SUBJECT;

@Service
public class ServerConfigService implements BaseUrlProvider {
    private GoConfigService goConfigService;

    private GoMailSenderProvider provider = GoMailSenderProvider.DEFAULT_PROVIDER;


    @Autowired
    public ServerConfigService(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    private void validate(MailHost mailHost, LocalizedOperationResult operationResult) {
        HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) validateHostName(mailHost.getHostName());
        if (!result.isSuccessful()) {
            operationResult.notAcceptable(result.message());
        }
        result = (HttpLocalizedOperationResult) validatePort(mailHost.getPort());
        if (!result.isSuccessful()) {
            operationResult.notAcceptable(result.message());
        }
        result = (HttpLocalizedOperationResult) validateEmail(mailHost.getFrom());
        if (!result.isSuccessful()) {
            operationResult.notAcceptable("From address is not a valid email address.");
        }
        result = (HttpLocalizedOperationResult) validateEmail(mailHost.getAdminMail());
        if (!result.isSuccessful()) {
            operationResult.notAcceptable("Admin address is not a valid email address.");
        }
    }

    public LocalizedOperationResult validateHostName(String hostName) {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        new HostNameValidator().validate(hostName, result);
        return result;
    }

    public LocalizedOperationResult validatePort(Integer port) {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        new PortValidator().validate(port, result);
        return result;
    }

    public LocalizedOperationResult validateEmail(String email) {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        try {
            new InternetAddress(email, true);
        } catch (AddressException e) {
            result.notAcceptable("Not a valid email address.");
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
            result.notAcceptable("Email: " + validationBean.getError());
        }
    }

    @Override
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

    public String getAutoregisterKey() {
        return serverConfig().getAgentAutoRegisterKey();
    }

    public String getWebhookSecret() {
        return serverConfig().getWebhookSecret();
    }

    @Override
    public boolean hasAnyUrlConfigured() {
        return serverConfig().hasAnyUrlConfigured();
    }

    public SiteUrls getServerSiteUrls() {
        return serverConfig().getSiteUrls();
    }

    public void createOrUpdateServerSiteUrls(SiteUrls siteUrls) {
        goConfigService.updateConfig(new CreateOrUpdateConfigServerSiteUrlsCommand(siteUrls), currentUsername());
    }

    public String getDefaultJobTimeout() {
        return serverConfig().getJobTimeout();
    }

    public void createOrUpdateDefaultJobTimeout(String defaultJobTimeout) {
        goConfigService.updateConfig(new CreateOrUpdateDefaultJobTimeoutCommand(defaultJobTimeout), currentUsername());
    }

    public ArtifactConfig getArtifactsConfig() {
        return serverConfig().getArtifactConfig();
    }

    public void updateArtifactConfig(ArtifactConfig modifiedArtifactConfig) {
        goConfigService.updateConfig(new UpdateArtifactConfigCommand(modifiedArtifactConfig), currentUsername());
    }
}
