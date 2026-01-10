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
package com.thoughtworks.go.domain.packagerepository;

import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ConfigTag("repositories")
@ConfigCollection(value = PackageRepository.class)
public class PackageRepositories extends BaseCollection<PackageRepository> implements Validatable {

    public PackageRepositories() {
    }

    public PackageRepositories(PackageRepository... packageRepositories) {
        Collections.addAll(this, packageRepositories);
    }

    public @NotNull PackageRepository findByRepoIdOrBomb(final String repoId) {
        return Objects.requireNonNullElseGet(findByRepoId(repoId), () -> {
            throw new RuntimeException("Could not find repository for given id: " + repoId);
        });
    }

    public @Nullable PackageRepository findByRepoId(final String repoId) {
        return stream().filter(repository -> repository.getId().equals(repoId)).findFirst().orElse(null);
    }

    public @NotNull PackageRepository findByPackageIdOrBomb(String packageId) {
        return Objects.requireNonNullElseGet(findByPackageId(packageId), () -> {
            throw new RuntimeException("Could not find repository for given package id: " + packageId);
        });
    }

    public @Nullable PackageRepository findByPackageId(String packageId) {
        return stream()
            .filter(r -> r.findPackage(packageId) != null)
            .findFirst()
            .orElse(null);
    }

    public @Nullable PackageDefinition findDefinitionByPackageId(String packageId) {
        return stream()
            .map(r -> r.findPackage(packageId))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
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
    public void addError(String fieldName, String message) {}

    public void removePackageRepository(String repoId) {
        if (!removeFirstIf(repository -> repository.getId().equals(repoId))) {
            throw new RuntimeException("Could not find repository with id: " + repoId);
        }
    }

    private void validateNameUniqueness() {
        Map<String, PackageRepository> nameMap = new HashMap<>();
        for (PackageRepository repository : this) {
            repository.validateNameUniqueness(nameMap);
        }
    }

    private void validateFingerprintUniqueness() {
        Map<String, Packages> map = new HashMap<>();
        for (PackageRepository repository : this) {
            for (PackageDefinition packageDefinition : repository.getPackages()) {
                String fingerprint = packageDefinition.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER);
                map.computeIfAbsent(fingerprint, k -> new Packages()).add(packageDefinition);
            }
        }

        for (PackageRepository repository : this) {
            for (PackageDefinition packageDefinition : repository.getPackages()) {
                packageDefinition.validateFingerprintUniqueness(map);
            }
        }
    }
}

