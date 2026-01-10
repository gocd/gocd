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
package com.thoughtworks.go.domain.scm;

import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

@ConfigTag("scms")
@ConfigCollection(value = SCM.class)
public class SCMs extends BaseCollection<SCM> implements Validatable {

    public SCMs() {
    }

    public SCMs(SCM... scms) {
        Collections.addAll(this, scms);
    }

    public @Nullable SCM find(final String scmId) {
        return stream().filter(scm -> scm.getId().equals(scmId)).findFirst().orElse(null);
    }

    public @Nullable SCM findDuplicate(final SCM other) {
        return stream().filter(current -> current.equalsByIdOrFingerprint(other)).findFirst().orElse(null);
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
        throw new RuntimeException("Not Implemented");
    }

    public void removeSCM(String id) {
        if (!removeFirstIf(scm ->  scm.getId().equals(id))) {
            throw new RuntimeException(String.format("Could not find SCM with id '%s'", id));
        }
    }

    private void validateNameUniqueness() {
        this.stream().collect(groupingBy(scm -> scm.getName().toLowerCase())).values().stream()
            .filter(scmList -> scmList.size() > 1)
            .flatMap(Collection::stream)
            .forEach(scm -> scm.addError(SCM.NAME, String.format("Cannot save SCM, found multiple SCMs called '%s'. SCM names are case-insensitive and must be unique.", scm.getName())));
    }

    private void validateFingerprintUniqueness() {
        this.stream().collect(groupingBy(SCM::getFingerprint)).values().stream()
            .filter(scmList -> scmList.size() > 1)
            .forEach(scmsWithSameFingerprint -> {
                String errorMessage = scmsWithSameFingerprint.stream().map(SCM::getName).collect(joining(", ", "Cannot save SCM, found duplicate SCMs. ", ""));

                for (SCM scm : scmsWithSameFingerprint) {
                    scm.addError(SCM.SCM_ID, errorMessage);
                }
            });
    }
}
