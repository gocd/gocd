/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobPropertiesReader;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.JobStateTransition;
import com.thoughtworks.go.domain.Properties;
import com.thoughtworks.go.domain.Property;
import java.util.LinkedHashMap;
import java.util.Map;
import com.thoughtworks.go.server.controller.actions.PropertyAction;
import com.thoughtworks.go.server.controller.actions.RestfulAction;
import com.thoughtworks.go.server.dao.PropertyDao;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.Csv;
import com.thoughtworks.go.util.CsvRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import static com.thoughtworks.go.server.controller.actions.CsvAction.csvFound;
import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonFound;
import static com.thoughtworks.go.server.controller.actions.PropertyAction.propertyContainsInvalidChars;
import static com.thoughtworks.go.server.controller.actions.PropertyAction.propertyNameToLarge;
import static com.thoughtworks.go.server.controller.actions.PropertyAction.propertyValueToLarge;
import static com.thoughtworks.go.util.GoConstants.CRUISE_AGENT;
import static com.thoughtworks.go.util.GoConstants.CRUISE_JOB_DURATION;
import static com.thoughtworks.go.util.GoConstants.CRUISE_JOB_ID;
import static com.thoughtworks.go.util.GoConstants.CRUISE_PIPELINE_COUNTER;
import static com.thoughtworks.go.util.GoConstants.CRUISE_PIPELINE_LABEL;
import static com.thoughtworks.go.util.GoConstants.CRUISE_RESULT;
import static com.thoughtworks.go.util.GoConstants.CRUISE_STAGE_COUNTER;
import static com.thoughtworks.go.util.GoConstants.CRUISE_TIMESTAMP;
import static com.thoughtworks.go.util.DateUtils.formatISO8601;

@Service
public class PropertiesService implements JobPropertiesReader {
    private static final int MAX_PROPERTY_SIZE = 255;
    private static final String VALID_PROPERTY_REGEX = "[0-9a-zA-Z\\-\\_\\.\\/]";

    private PropertyDao propertyDao;
    private TransactionTemplate transactionTemplate;
    private final JobResolverService jobResolverService;
    private GoConfigService goConfigService;

    @Autowired
    public PropertiesService(PropertyDao propertyDao, GoConfigService goConfigService, TransactionTemplate transactionTemplate, JobResolverService jobResolverService) {
        this.propertyDao = propertyDao;
        this.goConfigService = goConfigService;
        this.transactionTemplate = transactionTemplate;
        this.jobResolverService = jobResolverService;
    }

    public RestfulAction addProperty(Long id, String propertyName, String value) {
        if (propertyName.length() > MAX_PROPERTY_SIZE) {
            return propertyNameToLarge();
        }
        if (value.length() > MAX_PROPERTY_SIZE) {
            return propertyValueToLarge();
        }
        if (invalidPropertyName(propertyName)) {
            return propertyContainsInvalidChars();
        }

        try {
            Property property = new Property(propertyName, value);
            if (propertyDao.save(id, property)) {
                return PropertyAction.created(property);
            }
            return PropertyAction.alreadySet(propertyName);
        } catch (RuntimeException e) {
            return PropertyAction.instanceNotFound(e.getMessage());
        }
    }

    public List<Properties> loadHistory(String pipelineName, String stageName, String jobName,
                                        Long pipelineId, Integer limitCount) {
        if (limitCount <= 0) {
            return new ArrayList<>();
        }
        return propertyDao.loadHistory(pipelineName, stageName, jobName, pipelineId, limitCount);
    }

    public RestfulAction listPropertiesForJob(JobIdentifier jobIdentifier, String type, String propertyKey) {
        Properties properties;
        Long buildId = jobIdentifier.getBuildId();
        if (propertyKey != null) {
            String value = propertyDao.value(buildId, propertyKey);
            if (value == null) {
                return PropertyAction.propertyNotFound(propertyKey);
            }
            properties = new Properties(new Property(propertyKey, value));
        } else {
            properties = propertyDao.list(jobIdentifier.getBuildId());
        }
        return listPropertiesAs(type, properties, jobIdentifier.getBuildName());
    }

    private RestfulAction listPropertiesAs(String type, Properties properties, String jobName) {
        type = type == null ? "csv" : type;
        if ("csv".equalsIgnoreCase(type)) {
            return asCsv(jobName).listProperties(properties);
        } else {
            return asJson().listProperties(properties);
        }
    }

    public void saveCruiseProperties(final JobInstance instance) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                savePipelineLabel(instance);
                savePipelineCounter(instance);
                saveStageCounter(instance);
                saveBuildAgent(instance);
                saveBuildResult(instance);
                saveBuildDuration(instance);
                saveBuildTransition(instance);
                saveBuildBuildId(instance);
            }
        });
    }

    private void saveStageCounter(JobInstance job) {
        propertyDao.save(job.getId(), new Property(CRUISE_STAGE_COUNTER, job.getStageCounter()));
    }

    private void savePipelineLabel(JobInstance job) {
        propertyDao.save(job.getId(), new Property(CRUISE_PIPELINE_LABEL, job.getIdentifier().getPipelineLabel()));
    }

    private void savePipelineCounter(JobInstance job) {
        propertyDao.save(job.getId(),
                new Property(CRUISE_PIPELINE_COUNTER, String.valueOf(job.getIdentifier().getPipelineCounter())));
    }

    private void saveBuildBuildId(JobInstance instance) {
        propertyDao.save(instance.getId(), new Property(CRUISE_JOB_ID, String.valueOf(instance.getId())));
    }

    private void saveBuildTransition(JobInstance instance) {
        for (JobStateTransition transition : instance.getTransitions()) {
            propertyDao.save(instance.getId(), new Property(getTransitionKey(transition.getCurrentState()),
                    formatISO8601(transition.getStateChangeTime())));
        }
    }

    static String getTransitionKey(JobState state) {
        String index = state.ordinal() < 10 ? "0" + state.ordinal() : String.valueOf(state.ordinal());
        return CRUISE_TIMESTAMP + index + "_" + state.toLowerCase();
    }

    private void saveBuildDuration(JobInstance instance) {
        propertyDao.save(instance.getId(), new Property(CRUISE_JOB_DURATION, instance.getCurrentBuildDuration()));
    }

    private void saveBuildResult(JobInstance instance) {
        propertyDao.save(instance.getId(), new Property(CRUISE_RESULT, instance.getResult().toString()));
    }

    private void saveBuildAgent(JobInstance instance) {
        propertyDao.save(instance.getId(), new Property(CRUISE_AGENT, goConfigService.agentByUuid(instance.getAgentUuid()).getHostname()));
    }

    public Properties getPropertiesForJob(long id) {
        return propertyDao.list(id);
    }

    public static Csv fromAllPropertiesHistory(List<Properties> jobPropertiesHistory) {
        Csv csv = new Csv();
        for (Properties properties : jobPropertiesHistory) {
            CsvRow row = csv.newRow();
            for (Property property : properties) {
                row.put(property.getKey(), property.getValue());
            }
        }
        return csv;
    }

    public static Csv fromProperties(Properties properties) {
        Csv csv = new Csv();
        CsvRow row = csv.newRow();
        for (Property property : properties) {
            row.put(property.getKey(), property.getValue());
        }
        return csv;
    }

    public Properties getPropertiesForOriginalJob(JobIdentifier oldId) {
        JobIdentifier jobIdentifier = jobResolverService.actualJobIdentifier(oldId);
        return getPropertiesForJob(jobIdentifier.getBuildId());
    }

    public interface PropertyLister {
        RestfulAction listPropertiesHistory(List<Properties> jobPropertiesHistory);

        RestfulAction listProperties(Properties properties);

    }

    public static PropertyLister asJson() {
        return new JsonPropertyLister();
    }

    public static PropertyLister asCsv(String jobName) {
        return new CsvPropertyLister(jobName);
    }

    private static class JsonPropertyLister implements PropertyLister {
        public RestfulAction listPropertiesHistory(List<Properties> jobPropertiesHistory) {
            return propHistoryAsJson(jobPropertiesHistory);
        }

        public RestfulAction listProperties(Properties properties) {
            return jsonFound(properties);
        }

        private RestfulAction propHistoryAsJson(List<Properties> jobPropertiesHistory) {
            List jsonList = new ArrayList();
            for (Properties properties : jobPropertiesHistory) {
                Map<String, Object> jsonMap = new LinkedHashMap<>();
                jsonMap.put("properties", properties.toJson());
                jsonList.add(jsonMap);
            }
            return jsonFound(jsonList);
        }
    }

    private static class CsvPropertyLister implements PropertyLister {
        private final String jobName;

        public CsvPropertyLister(String jobName) {
            this.jobName = jobName;
        }

        public RestfulAction listPropertiesHistory(List<Properties> jobProperties) {
            return csvFound(fromAllPropertiesHistory(jobProperties), jobName);
        }

        public RestfulAction listProperties(Properties properties) {
            return csvFound(fromProperties(properties), jobName);
        }

    }

    public boolean invalidPropertyName(String name) {
        return name.replaceAll(VALID_PROPERTY_REGEX, "").length() != 0;
    }
}
