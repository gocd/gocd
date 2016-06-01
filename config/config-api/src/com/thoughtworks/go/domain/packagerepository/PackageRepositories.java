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

package com.thoughtworks.go.domain.packagerepository;

import java.util.Collections;
import java.util.HashMap;

import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.util.ListUtil;

import static java.lang.String.format;

@ConfigTag("repositories")
@ConfigCollection(value = PackageRepository.class)
public class PackageRepositories extends BaseCollection<PackageRepository> implements Validatable {

    public PackageRepositories() {
    }

    public PackageRepositories(PackageRepository... packageRepositories) {
        Collections.addAll(this, packageRepositories);
    }

    public PackageRepository find(final String repoId) {
        return ListUtil.find(this, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T repository) {
                return ((PackageRepository) repository).getId().equals(repoId);
            }
        });
    }

    public PackageRepository findPackageRepositoryWithPackageIdOrBomb(String packageId) {
        PackageRepository packageRepository = findPackageRepositoryHaving(packageId);
        if (packageRepository == null){
            throw new RuntimeException(format("Could not find repository for given package id:[%s]", packageId));
        }
        return packageRepository;
    }

    public PackageRepository findPackageRepositoryHaving(String packageId) {
        for (PackageRepository packageRepository : this) {
            for (PackageDefinition packageDefinition : packageRepository.getPackages()) {
                if (packageDefinition.getId().equals(packageId)) {
                    return packageRepository;
                }
            }
        }
        return null;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        validateNameUniqueness();
        validateFingerprintUniqueness();
    }


    @Override
    public ConfigErrors errors() {
        return new ConfigErrors();
    }

    @Override
    public void addError(String fieldName, String message) {

    }

    public void removePackageRepository(String id) {
        PackageRepository packageRepositoryToBeDeleted = this.find(id);
        if (packageRepositoryToBeDeleted == null) {
            throw new RuntimeException(String.format("Could not find repository with id '%s'", id));
        }
        this.remove(packageRepositoryToBeDeleted);
    }

    private void validateNameUniqueness() {
        HashMap<String, PackageRepository> nameMap = new HashMap<>();
        for (PackageRepository repository : this) {
            repository.validateNameUniqueness(nameMap);
        }
    }

    private void validateFingerprintUniqueness() {
        HashMap<String, Packages> map = new HashMap<>();
        for (PackageRepository repository : this) {
            for (PackageDefinition packageDefinition : repository.getPackages()) {
                String fingerprint = packageDefinition.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER);
                if (!map.containsKey(fingerprint)) {
                    map.put(fingerprint, new Packages());
                }
                map.get(fingerprint).add(packageDefinition);
            }
        }

        for (PackageRepository repository : this) {
            for (PackageDefinition packageDefinition : repository.getPackages()) {
                packageDefinition.validateFingerprintUniqueness(map);
            }
        }
    }

    public PackageDefinition findPackageDefinitionWith(String packageId) {
        for (PackageRepository packageRepository : this) {
            for (PackageDefinition packageDefinition : packageRepository.getPackages()) {
                if (packageDefinition.getId().equals(packageId)) {
                    return packageDefinition;
                }
            }
        }
        return null;
    }
}

