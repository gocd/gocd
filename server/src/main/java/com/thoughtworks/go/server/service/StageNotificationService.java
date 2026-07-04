/*
 * Copyright Thoughtworks, Inc.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Optional;

@Service
public class StageNotificationService {
    static final String MATERIAL_SECTION_HEADER = "-- CHANGES --";
    private static final Logger LOGGER = LoggerFactory.getLogger(StageNotificationService.class);
    private final PipelineService pipelineService;
    private final UserService userService;
    private final EmailNotificationTopic emailNotificationTopic;
    private final StageService stageService;
    private final ServerConfigService serverConfigService;

    @Autowired
    public StageNotificationService(PipelineService pipelineService, UserService userService, EmailNotificationTopic emailNotificationTopic,
                                    StageService stageService, ServerConfigService serverConfigService) {
        this.pipelineService = pipelineService;
        this.userService = userService;
        this.emailNotificationTopic = emailNotificationTopic;
        this.stageService = stageService;
        this.serverConfigService = serverConfigService;
    }

    public void sendNotifications(StageIdentifier stageIdentifier, StageEvent event, Username cancelledBy) {
        Users users = userService.findValidNotificationSubscribers(event, stageIdentifier.stageConfigIdentifier());
        if (users.isEmpty()) {
            return;
        }
        Stage stage = stageService.findStageWithIdentifier(stageIdentifier);
        Pipeline pipeline = pipelineService.fullPipelineById(stage.getPipelineId());
        MaterialRevisions materialRevisions = pipeline.getMaterialRevisions();

        String emailBody = new EmailBodyGenerator(materialRevisions, stageIdentifier, event, cancelledBy).getContent();

        String subject = "Stage [" + stageIdentifier.stageLocator() + "]" + event.describe();
        LOGGER.debug("Processing notification titled [{}]", subject);
        users.filter(user -> user.hasSubscribedFor(stageIdentifier.stageConfigIdentifier(), event, materialRevisions))
             .forEach(user -> emailNotificationTopic.post(
                 new SendEmailMessage(subject,
                     emailBody + "\n\nSent by Go on behalf of " + user.getName(),
                     user.getEmail())));
        LOGGER.debug("Finished processing notification titled [{}]", subject);
    }

    private class EmailBodyGenerator implements ModificationVisitor {
        private static final Logger LOGGER = LoggerFactory.getLogger(EmailBodyGenerator.class);
        @SuppressWarnings("TextBlockMigration")
        private static final String SECTION_SEPARATOR = "\n\n";

        private final StringBuilder emailBody;
        private Material material;
        private final StageIdentifier stageIdentifier;

        public EmailBodyGenerator(MaterialRevisions materialRevisions, StageIdentifier stageIdentifier, StageEvent event, Username cancelledBy) {
            this.stageIdentifier = stageIdentifier;
            emailBody = new StringBuilder();

            if (StageEvent.Cancelled.equals(event) && !Username.BLANK.equals(cancelledBy)) {
                emailBody.append("The stage was cancelled by ").append(CaseInsensitiveString.str(cancelledBy.getUsername())).append(".\n");
            }

            addStageLink();
            addMaterialRevisions(materialRevisions);
        }

        private void addMaterialRevisions(MaterialRevisions materialRevisions) {
            sectionSeparator();
            emailBody.append(MATERIAL_SECTION_HEADER);
            materialRevisions.accept(this);
        }

        private void addStageLink() {
            stageDetailLink().ifPresent(url -> emailBody.append(String.format("See details: %s", url)));
        }

        private Optional<URL> stageDetailLink() {
            try {
                return Optional.of(StageNotificationService.this.serverConfigService.siteUrlWithPath(stageIdentifier.webPathAfterContext()));
            } catch (Exception e) {
                LOGGER.warn("Could not generate email stage detail link for stage {} due to {}", stageIdentifier, e.toString());
                return Optional.empty();
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
            sectionSeparator();
            material.emailContent(emailBody, modification);
        }

        private void sectionSeparator() {
            emailBody.append(SECTION_SEPARATOR);
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
