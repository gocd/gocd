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

package com.thoughtworks.go.plugin.access.packagematerial;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.access.common.handler.JSONResultMessageHandler;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import org.apache.commons.lang.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class JsonMessageHandler1_0 implements JsonMessageHandler {
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private final JSONResultMessageHandler jsonResultMessageHandler;

    public JsonMessageHandler1_0() {
        jsonResultMessageHandler = new JSONResultMessageHandler();
    }

    @Override
    public RepositoryConfiguration responseMessageForRepositoryConfiguration(String responseBody) {
        try {
            RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
            Map<String, Map> configurations;
            try {
                configurations = parseResponseToMap(responseBody);
            } catch (Exception e) {
                throw new RuntimeException("Repository configuration should be returned as a map");
            }
            if (configurations == null || configurations.isEmpty()) {
                throw new RuntimeException("Empty response body");
            }
            for (String key : configurations.keySet()) {
                if (isEmpty(key)) {
                    throw new RuntimeException("Repository configuration key cannot be empty");
                }
                if (!(configurations.get(key) instanceof Map)) {
                    throw new RuntimeException(format("Repository configuration properties for key '%s' should be represented as a Map", key));
                }
                repositoryConfiguration.add(toPackageMaterialProperty(key, configurations.get(key)));
            }
            return repositoryConfiguration;
        } catch (Exception e) {
            throw new RuntimeException(format("Unable to de-serialize json response. %s", e.getMessage()));
        }
    }

    @Override
    public String requestMessageForIsRepositoryConfigurationValid(RepositoryConfiguration repositoryConfiguration) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("repository-configuration", jsonResultMessageHandler.configurationToMap(repositoryConfiguration));
        return toJsonString(configuredValues);
    }

    @Override
    public ValidationResult responseMessageForIsRepositoryConfigurationValid(String responseBody) {
        return jsonResultMessageHandler.toValidationResult(responseBody);
    }

    @Override
    public String requestMessageForCheckConnectionToRepository(RepositoryConfiguration repositoryConfiguration) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("repository-configuration", jsonResultMessageHandler.configurationToMap(repositoryConfiguration));
        return toJsonString(configuredValues);
    }

    @Override
    public Result responseMessageForCheckConnectionToRepository(String responseBody) {
        return jsonResultMessageHandler.toResult(responseBody);
    }

    @Override
    public PackageConfiguration responseMessageForPackageConfiguration(String responseBody) {
        try {
            PackageConfiguration packageConfiguration = new PackageConfiguration();
            Map<String, Map> configurations;
            try {
                configurations = parseResponseToMap(responseBody);
            } catch (Exception e) {
                throw new RuntimeException("Package configuration should be returned as a map");
            }
            if (configurations == null || configurations.isEmpty()) {
                throw new RuntimeException("Empty response body");
            }
            for (String key : configurations.keySet()) {
                if (isEmpty(key)) {
                    throw new RuntimeException("Package configuration key cannot be empty");
                }
                if (!(configurations.get(key) instanceof Map)) {
                    throw new RuntimeException(format("Package configuration properties for key '%s' should be represented as a Map", key));
                }
                packageConfiguration.add(toPackageMaterialProperty(key, configurations.get(key)));
            }
            return packageConfiguration;
        } catch (RuntimeException e) {
            throw new RuntimeException(format("Unable to de-serialize json response. %s", e.getMessage()));
        }
    }

    @Override
    public String requestMessageForIsPackageConfigurationValid(PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("repository-configuration", jsonResultMessageHandler.configurationToMap(repositoryConfiguration));
        configuredValues.put("package-configuration", jsonResultMessageHandler.configurationToMap(packageConfiguration));
        return toJsonString(configuredValues);
    }

    @Override
    public ValidationResult responseMessageForIsPackageConfigurationValid(String responseBody) {
        return jsonResultMessageHandler.toValidationResult(responseBody);
    }

    @Override
    public String requestMessageForCheckConnectionToPackage(PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("repository-configuration", jsonResultMessageHandler.configurationToMap(repositoryConfiguration));
        configuredValues.put("package-configuration", jsonResultMessageHandler.configurationToMap(packageConfiguration));
        return toJsonString(configuredValues);
    }

    @Override
    public Result responseMessageForCheckConnectionToPackage(String responseBody) {
        return jsonResultMessageHandler.toResult(responseBody);
    }

    @Override
    public String requestMessageForLatestRevision(PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("repository-configuration", jsonResultMessageHandler.configurationToMap(repositoryConfiguration));
        configuredValues.put("package-configuration", jsonResultMessageHandler.configurationToMap(packageConfiguration));
        return toJsonString(configuredValues);
    }

    @Override
    public PackageRevision responseMessageForLatestRevision(String responseBody) {
        return toPackageRevision(responseBody);
    }

    @Override
    public String requestMessageForLatestRevisionSince(PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration, PackageRevision previousRevision) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("repository-configuration", jsonResultMessageHandler.configurationToMap(repositoryConfiguration));
        configuredValues.put("package-configuration", jsonResultMessageHandler.configurationToMap(packageConfiguration));
        configuredValues.put("previous-revision", packageRevisionToMap(previousRevision));
        return toJsonString(configuredValues);
    }

    @Override
    public PackageRevision responseMessageForLatestRevisionSince(String responseBody) {
        if (isEmpty(responseBody)) return null;
        return toPackageRevision(responseBody);
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

    private PackageMaterialProperty toPackageMaterialProperty(String key, Map configuration) {
        List<String> errors = new ArrayList<>();
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

        PackageMaterialProperty packageMaterialProperty = new PackageMaterialProperty(key);
        if (!isEmpty(defaultValue)) {
            packageMaterialProperty.withDefault(defaultValue);
        }
        if (partOfIdentity != null) {
            packageMaterialProperty.with(Property.PART_OF_IDENTITY, partOfIdentity);
        }
        if (isSecure != null) {
            packageMaterialProperty.with(Property.SECURE, isSecure);
        }
        if (required != null) {
            packageMaterialProperty.with(Property.REQUIRED, required);
        }
        if (!isEmpty(displayName)) {
            packageMaterialProperty.with(Property.DISPLAY_NAME, displayName);
        }
        if (displayOrder != null) {
            packageMaterialProperty.with(Property.DISPLAY_ORDER, displayOrder);
        }
        return packageMaterialProperty;
    }

    PackageRevision toPackageRevision(String responseBody) {
        try {
            Map map;
            try {
                map = parseResponseToMap(responseBody);
            } catch (Exception e) {
                throw new RuntimeException("Package revision should be returned as a map");
            }
            if (map == null || map.isEmpty()) {
                throw new RuntimeException("Empty response body");
            }

            String revision;
            try {
                revision = (String) map.get("revision");
            } catch (Exception e) {
                throw new RuntimeException("Package revision should be of type string");
            }

            String revisionComment;
            try {
                revisionComment = (String) map.get("revisionComment");
            } catch (Exception e) {
                throw new RuntimeException("Package revision comment should be of type string");
            }


            String user;
            try {
                user = (String) map.get("user");
            } catch (Exception e) {
                throw new RuntimeException("Package revision user should be of type string");
            }

            String trackbackUrl;
            try {
                trackbackUrl = (String) map.get("trackbackUrl");
            } catch (Exception e) {
                throw new RuntimeException("Package revision trackbackUrl should be of type string");
            }


            Date timestamp;
            try {
                String timestampString = (String) map.get("timestamp");
                timestamp = new SimpleDateFormat(DATE_PATTERN).parse(timestampString);
            } catch (Exception e) {
                throw new RuntimeException("Package revision timestamp should be of type string with format yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            }

            Map data = (Map) map.get("data");
            PackageRevision packageRevision = new PackageRevision(revision, timestamp, user, revisionComment, trackbackUrl, data);
            return packageRevision;
        } catch (Exception e) {
            throw new RuntimeException(format("Unable to de-serialize json response. %s", e.getMessage()));
        }
    }

    private Map packageRevisionToMap(PackageRevision packageRevision) {
        Map map = new LinkedHashMap();
        map.put("revision", packageRevision.getRevision());
        map.put("timestamp", new SimpleDateFormat(DATE_PATTERN).format(packageRevision.getTimestamp()));
        map.put("data", packageRevision.getData());
        return map;
    }
}
