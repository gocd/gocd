package com.thoughtworks.go.plugin.access.packagematerial;

import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;

public interface JsonMessageHandler {

    // repository related
    RepositoryConfiguration responseMessageForRepositoryConfiguration(String responseBody);

    String requestMessageForIsRepositoryConfigurationValid(RepositoryConfiguration repositoryConfiguration);

    ValidationResult responseMessageForIsRepositoryConfigurationValid(String responseBody);

    String requestMessageForCheckConnectionToRepository(RepositoryConfiguration repositoryConfiguration);

    Result responseMessageForCheckConnectionToRepository(String responseBody);


    //package related
    com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration responseMessageForPackageConfiguration(String responseBody);

    String requestMessageForIsPackageConfigurationValid(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration);

    ValidationResult responseMessageForIsPackageConfigurationValid(String responseBody);

    String requestMessageForCheckConnectionToPackage(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration);

    Result responseMessageForCheckConnectionToPackage(String responseBody);


    //revision related
    String requestMessageForLatestRevision(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration);

    PackageRevision responseMessageForLatestRevision(String responseBody);

    String requestMessageForLatestRevisionSince(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration, PackageRevision previousRevision);

    PackageRevision responseMessageForLatestRevisionSince(String responseBody);

}
