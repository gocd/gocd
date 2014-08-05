/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.ModificationVisitor;
import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageEvent;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.domain.Users;
import com.thoughtworks.go.domain.activity.CcTrayStatus;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.testinfo.TestInformation;
import com.thoughtworks.go.domain.testinfo.TestStatus;
import com.thoughtworks.go.domain.testinfo.TestSuite;
import com.thoughtworks.go.server.dao.sparql.ShineDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.EmailNotificationTopic;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@Service
public class StageNotificationService {
    private static final Log LOGGER = LogFactory.getLog(StageNotificationService.class);
    private final PipelineService pipelineService;
    private final UserService userService;
    private EmailNotificationTopic emailNotificationTopic;
    private final SystemEnvironment systemEnvironment;
    private StageService stageService;
    private ServerConfigService serverConfigService;
    private final ShineDao shineDao;
    protected static final String MATERIAL_SECTION_HEADER = "-- CHECK-INS --";
    public static final String FAILED_TEST_SECTION = "-- FAILED TESTS --";

    @Autowired
    public StageNotificationService(PipelineService pipelineService, UserService userService, EmailNotificationTopic emailNotificationTopic,
                                    SystemEnvironment systemEnvironment, StageService stageService, ServerConfigService serverConfigService,
                                    ShineDao shineDao) {
        this.pipelineService = pipelineService;
        this.userService = userService;
        this.emailNotificationTopic = emailNotificationTopic;
        this.systemEnvironment = systemEnvironment;
        this.stageService = stageService;
        this.serverConfigService = serverConfigService;
        this.shineDao = shineDao;
    }

    public void sendNotifications(StageIdentifier stageIdentifier, StageEvent event, Username cancelledBy) {
    	
    	LOGGER.debug("checking notification for stage " + stageIdentifier.getPipelineName() + ":" + stageIdentifier.getStageName());
    	 
        Users users = userService.findValidSubscribers(stageIdentifier.stageConfigIdentifier());
        Stage stage = stageService.findStageWithIdentifier(stageIdentifier);
        Pipeline pipeline = pipelineService.fullPipelineById(stage.getPipelineId());
        MaterialRevisions materialRevisions = pipeline.getMaterialRevisions();
        
        if(event == StageEvent.Breaks){
        	
        	//calculate breakers here
        	Set<String> breakers =  CcTrayStatus.computeBreakersIfStageFailed(stage, materialRevisions);
        	LOGGER.info("Found broken stage!  Sending Notifiaction...");
        	
        	for(String breakerName : breakers){
        		
        		User breaker = userService.findUserByName(breakerName);  //returns an empty NullUser Instance if no user found.
        		
        		if(breaker == null || breaker instanceof NullUser){
	        		String breakerEmailAddr = findEmailAddress(breakerName);
	        		LOGGER.debug("checking email address: " + breakerEmailAddr);
	        		breaker = userService.findUserByEmail(breakerEmailAddr);
	        		
	        		if(breaker instanceof NullUser && breakerEmailAddr != null){
	        			breaker = new User(breakerName);
	        			breaker.setEmail(breakerEmailAddr);
	        		}
        		}
        			
        		LOGGER.debug("found breaker: " + breakerName + " mapped to user " + breaker.toString());
    			breaker.setEmailMe(true);
    			
    			if( ! users.contains(breaker)){
    				users.add(breaker);
    			}
        	}
        }
        
        if (users.isEmpty()) {
        	LOGGER.debug("No users detected for delivery");
            return;
        }
        
        final List<TestSuite> failedTestSuites = shineDao.failedTestsFor(stageIdentifier);
        String emailBody = new EmailBodyGenerator(materialRevisions, cancelledBy, systemEnvironment, stageIdentifier, failedTestSuites).getContent();

        String subject = "Stage [" + stageIdentifier.stageLocator() + "]" + event.describe();
        LOGGER.debug(String.format("Processing notification titled [%s]", subject));
        for (User user : users) {
        	
        	LOGGER.debug("Emailing User " + user.toString());
                StringBuilder emailWithSignature = new StringBuilder(emailBody)
                        .append("\n\n")
                        .append("Sent by Go on behalf of ")
                        .append(user.getName());
                SendEmailMessage sendEmailMessage
                        = new SendEmailMessage(subject, emailWithSignature.toString(), user.getEmail());
                emailNotificationTopic.post(sendEmailMessage);
        }
        LOGGER.debug(String.format("Finished processing notification titled [%s]", subject));
    }

    private String findEmailAddress(String breakerName) {
    	Pattern emailAddrPattern = Pattern.compile(".*?([_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,4})).*?", Pattern.CASE_INSENSITIVE);
    	Matcher matcher = emailAddrPattern.matcher(breakerName);
    	return matcher.group(1);
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
        private List<TestSuite> failedTestSuites;
        protected static final String SECTION_SEPERATOR = "\n\n";
        private static final String SUITE_NAME_PREFIX = "* ";

        public EmailBodyGenerator(MaterialRevisions materialRevisions, Username cancelledBy, SystemEnvironment systemEnvironment, StageIdentifier stageIdentifier, List<TestSuite> failedTestSuites) {
            this.systemEnvironment = systemEnvironment;
            this.stageIdentifier = stageIdentifier;
            this.failedTestSuites = failedTestSuites;
            emailBody = new StringBuilder();

            if (!Username.BLANK.equals(cancelledBy)) {
                emailBody.append("The stage was cancelled by ").append(CaseInsensitiveString.str(cancelledBy.getUsername())).append(".\n");
            }

            addStageLink();
            addFailedTests();
            addMaterialRevisions(materialRevisions);
        }

        private void addFailedTests() {
            if (failedTestSuites.size() == 0) {
                return;
            }
            sectionSeperator();
            emailBody.append(FAILED_TEST_SECTION);
            sectionSeperator();
            emailBody.append(String.format("The following tests failed in pipeline '%s' (instance '%s'):", stageIdentifier.getPipelineName(), stageIdentifier.getPipelineLabel()));
            for (TestSuite failedTestSuite : failedTestSuites) {
                sectionSeperator();
                emailBody.append(SUITE_NAME_PREFIX + failedTestSuite.fullName() + "\n");
                for (TestInformation testInformation : failedTestSuite.tests()) {
                    emailBody.append("   " + testInformation.getName() + "\n");
                    for (String jobName : testInformation.getJobNames()) {
                        emailBody.append("     " + testStatusString(testInformation) + " on '" + jobName + "' (" + jobDetailLink(jobName) + ")\n");
                    }
                }
            }
        }

        private String testStatusString(TestInformation testInformation) {
            return testInformation.getStatus().equals(TestStatus.Error) ? "Errored" : "Failed";
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

        private String jobDetailLink(String jobName) {
            String ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
            int port = systemEnvironment.getServerPort();
            String urlString = String.format("http://%s:%s/go/tab/build/detail/%s/%s", ipAddress, port, stageIdentifier.stageLocator(), jobName);
            return useConfiguredSiteUrl(urlString);
        }


        public void visit(MaterialRevision materialRevision) {
        }

        public void visit(Material material, Revision revision) {
            this.material = material;
        }

        public void visit(Modification modification) {
            sectionSeperator();
            material.emailContent(emailBody, modification);
        }

        private void sectionSeperator() {
            emailBody.append(SECTION_SEPERATOR);
        }

        public void visit(ModifiedFile file) {
            emailBody.append('\n').append(file.getAction()).append(' ').append(file.getFileName());
        }

        public String getContent() {
            return emailBody.toString();
        }
    }
}
