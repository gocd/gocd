/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.EmailNotificationTopic;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@Service
public class StageNotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StageNotificationService.class);
    private final PipelineService pipelineService;
    private final UserService userService;
    private EmailNotificationTopic emailNotificationTopic;
    private final SystemEnvironment systemEnvironment;
    private StageService stageService;
    private ServerConfigService serverConfigService;
    protected static final String MATERIAL_SECTION_HEADER = "-- CHECK-INS --";

    @Autowired
    public StageNotificationService(PipelineService pipelineService, UserService userService, EmailNotificationTopic emailNotificationTopic,
                                    SystemEnvironment systemEnvironment, StageService stageService, ServerConfigService serverConfigService) {
        this.pipelineService = pipelineService;
        this.userService = userService;
        this.emailNotificationTopic = emailNotificationTopic;
        this.systemEnvironment = systemEnvironment;
        this.stageService = stageService;
        this.serverConfigService = serverConfigService;
    }

    public void sendNotifications(StageIdentifier stageIdentifier, StageEvent event, Username cancelledBy) {
        Users users = userService.findValidSubscribers(stageIdentifier.stageConfigIdentifier());
        if (users.isEmpty()) {
            return;
        }
        Stage stage = stageService.findStageWithIdentifier(stageIdentifier);
        Pipeline pipeline = pipelineService.fullPipelineById(stage.getPipelineId());
        MaterialRevisions materialRevisions = pipeline.getMaterialRevisions();

        String emailBody = new EmailBodyGenerator(materialRevisions, cancelledBy, systemEnvironment, stageIdentifier).getContent();

        String subject = "Stage [" + stageIdentifier.stageLocator() + "]" + event.describe();
        LOGGER.debug("Processing notification titled [{}]", subject);
        for (User user : users) {
            if (user.matchNotification(stageIdentifier.stageConfigIdentifier(), event, materialRevisions)) {
                StringBuilder emailWithSignature = new StringBuilder(emailBody)
                        .append("\n\n")
                        .append("Sent by Go on behalf of ")
                        .append(user.getName());
                SendEmailMessage sendEmailMessage
                        = new SendEmailMessage(subject, emailWithSignature.toString(), user.getEmail());
                emailNotificationTopic.post(sendEmailMessage);
            }
        }
        LOGGER.debug("Finished processing notification titled [{}]", subject);
    }

    //only for test
    void setEmailNotificationTopic(EmailNotificationTopic emailNotificationTopic) {
        this.emailNotificationTopic = emailNotificationTopic;
    }

    private class EmailBodyGenerator implements ModificationVisitor {
        private final StringBuilder emailBody;
        private Material material;
        private final SystemEnvironment systemEnvironment;
        private final StageIdentifier stageIdentifier;
        protected static final String SECTION_SEPERATOR = "\n\n";

        public EmailBodyGenerator(MaterialRevisions materialRevisions, Username cancelledBy, SystemEnvironment systemEnvironment, StageIdentifier stageIdentifier) {
            this.systemEnvironment = systemEnvironment;
            this.stageIdentifier = stageIdentifier;
            emailBody = new StringBuilder();

            if (!Username.BLANK.equals(cancelledBy)) {
                emailBody.append("The stage was cancelled by ").append(CaseInsensitiveString.str(cancelledBy.getUsername())).append(".\n");
            }

            addStageLink();
            addMaterialRevisions(materialRevisions);
        }

        private void addMaterialRevisions(MaterialRevisions materialRevisions) {
            sectionSeperator();
            emailBody.append(MATERIAL_SECTION_HEADER);
            materialRevisions.accept(this);
        }

        private void addStageLink() {
            emailBody.append(String.format("See details: %s", stageDetailLink()));
        }

        private String stageDetailLink() {
            String ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
            int port = systemEnvironment.getServerPort();
            String urlString = String.format("http://%s:%s/go/pipelines/%s", ipAddress, port, stageIdentifier.stageLocator());
            return useConfiguredSiteUrl(urlString);
        }

        private String useConfiguredSiteUrl(String urlString) {
            try {
                return StageNotificationService.this.serverConfigService.siteUrlFor(urlString, false);
            } catch (URISyntaxException e) {
                throw bomb("Could not construct URL.", e);
            }
        }


        @Override
        public void visit(MaterialRevision materialRevision) {
        }

        @Override
        public void visit(Material material, Revision revision) {
            this.material = material;
        }

        @Override
        public void visit(Modification modification) {
            sectionSeperator();
            material.emailContent(emailBody, modification);
        }

        private void sectionSeperator() {
            emailBody.append(SECTION_SEPERATOR);
        }

        @Override
        public void visit(ModifiedFile file) {
            emailBody.append('\n').append(file.getAction()).append(' ').append(file.getFileName());
        }

        public String getContent() {
            return emailBody.toString();
        }
    }
}
