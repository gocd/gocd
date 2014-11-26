package com.thoughtworks.go.plugin.access.packagematerial;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.config.Configuration;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class JsonMessageHandler1_0 implements JsonMessageHandler {

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    @Override
    public RepositoryConfiguration responseMessageForRepositoryConfiguration(String responseBody) {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        Map<String, Map> configurations = parseResponseToMap(responseBody);
        for (String key : configurations.keySet()) {
            repositoryConfiguration.add(toPackageMaterialProperty(key, configurations.get(key)));
        }
        return repositoryConfiguration;
    }

    @Override
    public String requestMessageForIsRepositoryConfigurationValid(RepositoryConfiguration repositoryConfiguration) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("repository-configuration", propertyToMap(repositoryConfiguration));
        return toJsonString(configuredValues);
    }

    @Override
    public ValidationResult responseMessageForIsRepositoryConfigurationValid(String responseBody) {
        return toValidationResult(responseBody);
    }

    @Override
    public String requestMessageForCheckConnectionToRepository(RepositoryConfiguration repositoryConfiguration) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("repository-configuration", propertyToMap(repositoryConfiguration));
        return toJsonString(configuredValues);
    }

    @Override
    public Result responseMessageForCheckConnectionToRepository(String responseBody) {
        return toResult(responseBody);
    }

    @Override
    public PackageConfiguration responseMessageForPackageConfiguration(String responseBody) {
        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration = new com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration();
        Map<String, Map> configurations = parseResponseToMap(responseBody);
        for (String key : configurations.keySet()) {
            packageConfiguration.add(toPackageMaterialProperty(key, configurations.get(key)));
        }
        return packageConfiguration;
    }

    @Override
    public String requestMessageForIsPackageConfigurationValid(PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("repository-configuration", propertyToMap(repositoryConfiguration));
        configuredValues.put("package-configuration", propertyToMap(packageConfiguration));
        return toJsonString(configuredValues);
    }

    @Override
    public ValidationResult responseMessageForIsPackageConfigurationValid(String responseBody) {
        return toValidationResult(responseBody);
    }

    @Override
    public String requestMessageForCheckConnectionToPackage(PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("repository-configuration", propertyToMap(repositoryConfiguration));
        configuredValues.put("package-configuration", propertyToMap(packageConfiguration));
        return toJsonString(configuredValues);
    }

    @Override
    public Result responseMessageForCheckConnectionToPackage(String responseBody) {
        return toResult(responseBody);
    }

    @Override
    public String requestMessageForLatestRevision(PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("repository-configuration", propertyToMap(repositoryConfiguration));
        configuredValues.put("package-configuration", propertyToMap(packageConfiguration));
        return toJsonString(configuredValues);
    }

    @Override
    public PackageRevision responseMessageForLatestRevision(String responseBody) {
        return toPackageRevision(responseBody);
    }

    @Override
    public String requestMessageForLatestRevisionSince(PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration, PackageRevision previousRevision) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("repository-configuration", propertyToMap(repositoryConfiguration));
        configuredValues.put("package-configuration", propertyToMap(packageConfiguration));
        configuredValues.put("previous-revision", packageRevisionToMap(previousRevision));
        return toJsonString(configuredValues);
    }

    @Override
    public PackageRevision responseMessageForLatestRevisionSince(String responseBody) {
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
        String defaultValue = (String) configuration.get("default-value");
        Boolean partOfIdentity = (Boolean) configuration.get("part-of-identity");
        Boolean isSecure = (Boolean) configuration.get("secure");
        Boolean required = (Boolean) configuration.get("required");
        String displayName = (String) configuration.get("display-name");
        Integer displayOrder = configuration.get("display-order") == null ? null : Integer.parseInt((String) configuration.get("display-order"));

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

    private ValidationResult toValidationResult(String responseBody) {
        List<Map> errors = parseResponseToList(responseBody);
        ValidationResult validationResult = new ValidationResult();
        for (Map error : errors) {
            String key = (String) error.get("key");
            String message = (String) error.get("message");
            if (isEmpty(key)) {
                validationResult.addError(new ValidationError(message));
            } else {
                validationResult.addError(new ValidationError(key, message));
            }
        }
        return validationResult;
    }

    private PackageRevision toPackageRevision(String responseBody) {
        try {
            Map map = parseResponseToMap(responseBody);
            String revision = (String) map.get("revision");
            String revisionComment = (String) map.get("revisionComment");
            String user = (String) map.get("user");
            String timestampString = (String) map.get("timestamp");
            String trackbackUrl = (String) map.get("trackbackUrl");
            Map data = (Map) map.get("data");
            Date timestamp = new SimpleDateFormat(DATE_PATTERN).parse(timestampString);
            PackageRevision packageRevision = new PackageRevision(revision, timestamp, user, revisionComment, trackbackUrl, data);
            return packageRevision;
        } catch (Exception e) {
            throw new RuntimeException("could not parse response to package revision", e);
        }
    }

    private Map propertyToMap(Configuration configuration) {
        Map configuredValuesForRepo = new LinkedHashMap();
        for (Property property : configuration.list()) {
            Map map = new TreeMap();
            map.put("value", property.getValue());
            configuredValuesForRepo.put(property.getKey(), map);
        }
        return configuredValuesForRepo;
    }

    private Map packageRevisionToMap(PackageRevision packageRevision) {
        Map map = new LinkedHashMap();
        map.put("revision", packageRevision.getRevision());
        map.put("timestamp", new SimpleDateFormat(DATE_PATTERN).format(packageRevision.getTimestamp()));
        map.put("data", packageRevision.getData());
        return map;
    }

    private Result toResult(String responseBody) {
        Map map = parseResponseToMap(responseBody);
        String status = (String) map.get("status");
        List<String> messages = (List<String>) map.get("messages");
        Result result = new Result();
        if ("success".equals(status)) {
            result.withSuccessMessages(messages);
        } else {
            result.withErrorMessages(messages);
        }
        return result;
    }
}
