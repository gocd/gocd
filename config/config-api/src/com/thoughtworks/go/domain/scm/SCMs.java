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

package com.thoughtworks.go.domain.scm;

import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.util.ListUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.util.ListUtil.join;

@ConfigTag("scms")
@ConfigCollection(value = SCM.class)
public class SCMs extends BaseCollection<SCM> implements Validatable {

    public SCMs() {
    }

    public SCMs(SCM... scms) {
        Collections.addAll(this, scms);
    }

    public SCM find(final String scmId) {
        return ListUtil.find(this, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T scm) {
                return ((SCM) scm).getId().equals(scmId);
            }
        });
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
        SCM scmToBeDeleted = this.find(id);
        if (scmToBeDeleted == null) {
            throw new RuntimeException(String.format("Could not find SCM with id '%s'", id));
        }
        this.remove(scmToBeDeleted);
    }

    private void validateNameUniqueness() {
        HashMap<String, SCMs> map = new HashMap<>();

        for (SCM scm : this) {
            String name = scm.getName().toLowerCase();
            if (!map.containsKey(name)) {
                map.put(name, new SCMs());
            }
            map.get(name).add(scm);
        }

        for (String name : map.keySet()) {
            SCMs scmsWithSameName = map.get(name);
            if (scmsWithSameName.size() > 1) {
                for (SCM scm : scmsWithSameName) {
                    scm.addError(SCM.NAME, String.format("Cannot save SCM, found multiple SCMs called '%s'. SCM names are case-insensitive and must be unique.", scm.getName()));
                }
            }
        }
    }

    private void validateFingerprintUniqueness() {
        HashMap<String, SCMs> map = new HashMap<>();

        for (SCM scm : this) {
            String fingerprint = scm.getFingerprint();
            if (!map.containsKey(fingerprint)) {
                map.put(fingerprint, new SCMs());
            }
            map.get(fingerprint).add(scm);
        }

        for (String fingerprint : map.keySet()) {
            SCMs scmsWithSameFingerprint = map.get(fingerprint);
            if (scmsWithSameFingerprint.size() > 1) {
                List<String> scmNames = new ArrayList<>();
                for (SCM scm : scmsWithSameFingerprint) {
                    scmNames.add(scm.getName());
                }

                for (SCM scm : scmsWithSameFingerprint) {
                    scm.addError(SCM.SCM_ID, String.format("Cannot save SCM, found duplicate SCMs. %s", join(scmNames)));
                }
            }
        }
    }
}
