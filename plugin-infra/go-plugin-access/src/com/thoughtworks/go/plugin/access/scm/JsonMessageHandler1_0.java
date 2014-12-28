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

package com.thoughtworks.go.plugin.access.scm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.access.scm.revision.ModifiedAction;
import com.thoughtworks.go.plugin.access.scm.revision.ModifiedFile;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import org.apache.commons.lang.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class JsonMessageHandler1_0 implements JsonMessageHandler {
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    @Override
    public SCMPropertyConfiguration responseMessageForSCMConfiguration(String responseBody) {
        try {
            SCMPropertyConfiguration scmConfiguration = new SCMPropertyConfiguration();
            Map<String, Map> configurations;
            try {
                configurations = parseResponseToMap(responseBody);
            } catch (Exception e) {
                throw new RuntimeException("SCM configuration should be returned as a map");
            }
            if (configurations == null || configurations.isEmpty()) {
                throw new RuntimeException("Empty response body");
            }
            for (String key : configurations.keySet()) {
                if (isEmpty(key)) {
                    throw new RuntimeException("SCM configuration key cannot be empty");
                }
                if (!(configurations.get(key) instanceof Map)) {
                    throw new RuntimeException(format("SCM configuration properties for key '%s' should be represented as a Map", key));
                }
                scmConfiguration.add(toSCMProperty(key, configurations.get(key)));
            }
            return scmConfiguration;
        } catch (Exception e) {
            throw new RuntimeException(format("Unable to de-serialize json response. %s", e.getMessage()));
        }
    }

    @Override
    public String requestMessageForIsSCMConfigurationValid(SCMPropertyConfiguration scmConfiguration) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("scm-configuration", propertyToMap(scmConfiguration));
        return toJsonString(configuredValues);
    }

    @Override
    public ValidationResult responseMessageForIsSCMConfigurationValid(String responseBody) {
        return toValidationResult(responseBody);
    }

    @Override
    public String requestMessageForCheckConnectionToSCM(SCMPropertyConfiguration scmConfiguration) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("scm-configuration", propertyToMap(scmConfiguration));
        return toJsonString(configuredValues);
    }

    @Override
    public Result responseMessageForCheckConnectionToSCM(String responseBody) {
        return toResult(responseBody);
    }

    @Override
    public String requestMessageForLatestRevision(SCMPropertyConfiguration scmConfiguration, String flyweightFolder) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("scm-configuration", propertyToMap(scmConfiguration));
        configuredValues.put("flyweight-folder", flyweightFolder);
        return toJsonString(configuredValues);
    }

    @Override
    public SCMRevision responseMessageForLatestRevision(String responseBody) {
        return toSCMRevision(responseBody);
    }

    @Override
    public String requestMessageForLatestRevisionsSince(SCMPropertyConfiguration scmConfiguration, String flyweightFolder, SCMRevision previousRevision) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("scm-configuration", propertyToMap(scmConfiguration));
        configuredValues.put("flyweight-folder", flyweightFolder);
        configuredValues.put("previous-revision", scmRevisionToMap(previousRevision));
        return toJsonString(configuredValues);
    }

    @Override
    public List<SCMRevision> responseMessageForLatestRevisionsSince(String responseBody) {
        if (isEmpty(responseBody)) return null;
        return toSCMRevisions(responseBody);
    }

    @Override
    public String requestMessageForCheckout(SCMPropertyConfiguration scmConfiguration, String destinationFolder, SCMRevision revision) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("scm-configuration", propertyToMap(scmConfiguration));
        configuredValues.put("destination-folder", destinationFolder);
        configuredValues.put("revision", scmRevisionToMap(revision));
        return toJsonString(configuredValues);
    }

    @Override
    public Result responseMessageForCheckout(String responseBody) {
        return toResult(responseBody);
    }

    private List<Map> parseResponseToList(String responseBody) {
        return (List<Map>) new GsonBuilder().create().fromJson(responseBody, Object.class);
    }

    private Map parseResponseToMap(String responseBody) {
        return (Map) new GsonBuilder().create().fromJson(responseBody, Object.class);
    }

    private static String toJsonString(Object object) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(object);
    }

    private SCMProperty toSCMProperty(String key, Map configuration) {
        List<String> errors = new ArrayList<String>();
        String defaultValue = null;
        try {
            defaultValue = (String) configuration.get("default-value");
        } catch (Exception e) {
            errors.add(format("'default-value' property for key '%s' should be of type string", key));
        }

        Boolean partOfIdentity = null;
        try {
            partOfIdentity = (Boolean) configuration.get("part-of-identity");
        } catch (Exception e) {
            errors.add(format("'part-of-identity' property for key '%s' should be of type boolean", key));
        }

        Boolean isSecure = null;
        try {
            isSecure = (Boolean) configuration.get("secure");
        } catch (Exception e) {
            errors.add(format("'secure' property for key '%s' should be of type boolean", key));
        }

        Boolean required = null;
        try {
            required = (Boolean) configuration.get("required");
        } catch (Exception e) {
            errors.add(format("'required' property for key '%s' should be of type boolean", key));
        }

        String displayName = null;
        try {
            displayName = (String) configuration.get("display-name");
        } catch (Exception e) {
            errors.add(format("'display-name' property for key '%s' should be of type string", key));
        }

        Integer displayOrder = null;
        try {
            displayOrder = configuration.get("display-order") == null ? null : Integer.parseInt((String) configuration.get("display-order"));
        } catch (Exception e) {
            errors.add(format("'display-order' property for key '%s' should be of type integer", key));
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException(StringUtils.join(errors, ", "));
        }

        SCMProperty scmProperty = new SCMProperty(key);
        if (!isEmpty(defaultValue)) {
            scmProperty.withDefault(defaultValue);
        }
        if (partOfIdentity != null) {
            scmProperty.with(Property.PART_OF_IDENTITY, partOfIdentity);
        }
        if (isSecure != null) {
            scmProperty.with(Property.SECURE, isSecure);
        }
        if (required != null) {
            scmProperty.with(Property.REQUIRED, required);
        }
        if (!isEmpty(displayName)) {
            scmProperty.with(Property.DISPLAY_NAME, displayName);
        }
        if (displayOrder != null) {
            scmProperty.with(Property.DISPLAY_ORDER, displayOrder);
        }
        return scmProperty;
    }

    ValidationResult toValidationResult(String responseBody) {
        try {
            ValidationResult validationResult = new ValidationResult();

            if (isEmpty(responseBody)) return validationResult;

            List errors;
            try {
                errors = parseResponseToList(responseBody);
            } catch (Exception e) {
                throw new RuntimeException("Validation errors should be returned as list or errors, with each error represented as a map");
            }

            for (Object errorObj : errors) {
                if (!(errorObj instanceof Map)) {
                    throw new RuntimeException("Each validation error should be represented as a map");
                }
                Map errorMap = (Map) errorObj;

                String key;
                try {
                    key = (String) errorMap.get("key");
                } catch (Exception e) {
                    throw new RuntimeException("Validation error key should be of type string");
                }

                String message;
                try {
                    message = (String) errorMap.get("message");
                } catch (Exception e) {
                    throw new RuntimeException("Validation message should be of type string");
                }

                if (isEmpty(key)) {
                    validationResult.addError(new ValidationError(message));
                } else {
                    validationResult.addError(new ValidationError(key, message));
                }
            }

            return validationResult;
        } catch (Exception e) {
            throw new RuntimeException(format("Unable to de-serialize json response. %s", e.getMessage()));
        }
    }

    List<SCMRevision> toSCMRevisions(String responseBody) {
        try {
            List<SCMRevision> scmRevisions = new ArrayList<SCMRevision>();

            Map map;
            try {
                map = parseResponseToMap(responseBody);
            } catch (Exception e) {
                throw new RuntimeException("SCM revisions should be returned as a map");
            }

            if (map == null || map.isEmpty()) {
                throw new RuntimeException("Empty response body");
            }

            if (map.containsKey("revisions") && map.get("revisions") != null) {
                List revisionMaps = null;
                try {
                    revisionMaps = (List) map.get("revisions");
                } catch (Exception e) {
                    throw new RuntimeException("'revisions' should be of type list of map");
                }

                if (!revisionMaps.isEmpty()) {
                    for (Object revision : revisionMaps) {
                        if (!(revision instanceof Map)) {
                            throw new RuntimeException("SCM revision should be of type map");
                        }
                    }

                    for (Object revisionObj : revisionMaps) {
                        Map revisionMap = (Map) revisionObj;

                        SCMRevision scmRevision = getScmRevisionFromMap(revisionMap);

                        scmRevisions.add(scmRevision);
                    }
                }
            }

            return scmRevisions;
        } catch (Exception e) {
            throw new RuntimeException(format("Unable to de-serialize json response. %s", e.getMessage()));
        }
    }

    SCMRevision toSCMRevision(String responseBody) {
        try {
            Map map;
            try {
                map = parseResponseToMap(responseBody);
            } catch (Exception e) {
                throw new RuntimeException("SCM revision should be returned as a map");
            }
            if (map == null || map.isEmpty()) {
                throw new RuntimeException("Empty response body");
            }

            return getScmRevisionFromMap(map);
        } catch (Exception e) {
            throw new RuntimeException(format("Unable to de-serialize json response. %s", e.getMessage()));
        }
    }

    private SCMRevision getScmRevisionFromMap(Map map) {
        String revision;
        try {
            revision = (String) map.get("revision");
        } catch (Exception e) {
            throw new RuntimeException("SCM revision should be of type string");
        }

        if (isEmpty(revision)) {
            throw new RuntimeException("SCM revision's 'revision' is a required field");
        }

        Date timestamp;
        try {
            String timestampString = (String) map.get("timestamp");
            timestamp = new SimpleDateFormat(DATE_PATTERN).parse(timestampString);
        } catch (Exception e) {
            throw new RuntimeException("SCM revision timestamp should be of type string with format yyyy-MM-dd'T'HH:mm:ss.SSS'Z' and cannot be empty");
        }

        String revisionComment;
        try {
            revisionComment = (String) map.get("revisionComment");
        } catch (Exception e) {
            throw new RuntimeException("SCM revision comment should be of type string");
        }

        String user;
        try {
            user = (String) map.get("user");
        } catch (Exception e) {
            throw new RuntimeException("SCM revision user should be of type string");
        }

        Map data = (Map) map.get("data");

        List<ModifiedFile> modifiedFiles = new ArrayList<ModifiedFile>();
        if (map.containsKey("modifiedFiles") && map.get("modifiedFiles") != null) {
            List modifiedFileMaps = null;
            try {
                modifiedFileMaps = (List) map.get("modifiedFiles");
            } catch (Exception e) {
                throw new RuntimeException("SCM revision 'modifiedFiles' should be of type list of map");
            }

            if (!modifiedFileMaps.isEmpty()) {
                for (Object message : modifiedFileMaps) {
                    if (!(message instanceof Map)) {
                        throw new RuntimeException("SCM revision 'modified file' should be of type map");
                    }
                }

                for (Object modifiedFileObj : modifiedFileMaps) {
                    Map modifiedFileMap = (Map) modifiedFileObj;

                    String fileName;
                    try {
                        fileName = (String) modifiedFileMap.get("fileName");
                    } catch (Exception e) {
                        throw new RuntimeException("modified file 'fileName' should be of type string");
                    }

                    if (isEmpty(fileName)) {
                        throw new RuntimeException("modified file 'fileName' is a required field");
                    }

                    String actionStr = null;
                    ModifiedAction action;
                    try {
                        actionStr = (String) modifiedFileMap.get("action");
                    } catch (Exception e) {
                        throw new RuntimeException("modified file 'action' should be of type string");
                    }

                    try {
                        action = ModifiedAction.valueOf(actionStr);
                    } catch (Exception e) {
                        throw new RuntimeException(String.format("modified file 'action' can only be %s, %s, %s", ModifiedAction.added, ModifiedAction.modified, ModifiedAction.deleted));

                    }

                    modifiedFiles.add(new ModifiedFile(fileName, action));
                }
            }
        }

        return new SCMRevision(revision, timestamp, user, revisionComment, data, modifiedFiles);
    }

    private Map propertyToMap(SCMPropertyConfiguration configuration) {
        Map configuredValuesForSCM = new LinkedHashMap();
        for (Property property : configuration.list()) {
            Map map = new LinkedHashMap();
            map.put("value", property.getValue());
            configuredValuesForSCM.put(property.getKey(), map);
        }
        return configuredValuesForSCM;
    }

    private Map scmRevisionToMap(SCMRevision scmRevision) {
        Map map = new LinkedHashMap();
        map.put("revision", scmRevision.getRevision());
        map.put("timestamp", new SimpleDateFormat(DATE_PATTERN).format(scmRevision.getTimestamp()));
        map.put("data", scmRevision.getData());
        return map;
    }

    Result toResult(String responseBody) {
        try {
            Result result = new Result();

            Map map;
            try {
                map = parseResponseToMap(responseBody);
            } catch (Exception e) {
                throw new RuntimeException("Check connection result should be returned as map, with key represented as string and messages represented as list");
            }
            if (map == null || map.isEmpty()) {
                throw new RuntimeException("Empty response body");
            }

            String status;
            try {
                status = (String) map.get("status");
            } catch (Exception e) {
                throw new RuntimeException("Check connection 'status' should be of type string");
            }

            if (isEmpty(status)) {
                throw new RuntimeException("Check connection 'status' is a required field");
            }

            if ("success".equalsIgnoreCase(status)) {
                result.withSuccessMessages(new ArrayList<String>());
            } else {
                result.withErrorMessages(new ArrayList<String>());
            }

            if (map.containsKey("messages") && map.get("messages") != null) {
                List messages = null;
                try {
                    messages = (List) map.get("messages");
                } catch (Exception e) {
                    throw new RuntimeException("Check connection 'messages' should be of type list of string");
                }

                if (!messages.isEmpty()) {
                    for (Object message : messages) {
                        if (!(message instanceof String)) {
                            throw new RuntimeException("Check connection 'message' should be of type string");
                        }
                    }

                    if (result.isSuccessful()) {
                        result.withSuccessMessages(messages);
                    } else {
                        result.withErrorMessages(messages);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException(format("Unable to de-serialize json response. %s", e.getMessage()));
        }
    }
}
