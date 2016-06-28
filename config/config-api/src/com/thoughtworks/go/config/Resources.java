/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.comparator.AlphaAsciiCollectionComparator;
import org.apache.commons.lang.StringUtils;

@ConfigTag("resources")
@ConfigCollection(Resource.class)
public class Resources extends BaseCollection<Resource> implements Comparable<Resources>, Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    public Resources() {
    }

    public Resources(Resource... resources) {
        super(resources);
    }

    public Resources(String resources) {
        String[] resourceArray = resources.split(",");
        for (String resource : resourceArray) {
            try {
                add(new Resource(resource));
            } catch (Exception e) {
                continue;
            }
        }
    }

    public Resources(List<Resource> resources) {
        super(resources);
    }

    public boolean add(Resource resource) {
        if (!this.contains(resource) && !StringUtils.isBlank(resource.getName())) {
            super.add(resource);
            return true;
        }
        return false;
    }

    public List<String> resourceNames() {
        Set<String> names = new TreeSet<>();
        for (Resource resource : this) {
            names.add(resource.getName());
        }
        return new ArrayList<>(names);
    }

    public String toString() {
        return StringUtil.joinForDisplay(resourceNames());
    }

    public int compareTo(Resources other) {
        return new AlphaAsciiCollectionComparator<Resource>().compare(this, other);
    }

    public boolean validateTree(ValidationContext validationContext) {
        boolean isValid = errors().isEmpty();
        for (Resource resource : this) {
            isValid = resource.validateTree(validationContext) && isValid;
        }
        return isValid;
    }
    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public String exportToCsv() {
        return join(resourceNames(), ", ");
    }

    private static String join(List<String> c, String join) {
        StringBuffer sb = new StringBuffer();
        for (String s : c) {
            sb.append(s);
            sb.append(join);
        }
        return sb.toString();
    }

    public void importFromCsv(String csv) {
        clear();
        String[] resourceNames = csv.split(",");
        for (String resourceName : resourceNames) {
            add(new Resource(resourceName.trim()));
        }
    }

}
