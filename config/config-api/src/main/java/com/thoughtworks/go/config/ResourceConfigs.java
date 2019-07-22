/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.util.CommaSeparatedString;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.comparator.AlphaAsciiCollectionComparator;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.thoughtworks.go.util.CommaSeparatedString.append;

@ConfigTag("resources")
@ConfigCollection(ResourceConfig.class)
public class ResourceConfigs extends BaseCollection<ResourceConfig> implements Comparable<ResourceConfigs>, Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    public ResourceConfigs() {
    }

    public ResourceConfigs(ResourceConfig... resourceConfigs) {
        super(resourceConfigs);
    }

    public ResourceConfigs(String resources) {
        if (StringUtils.isNotEmpty(resources)) {
            String[] resourceArr = resources.split(",");
            for (String resource : resourceArr) {
                try {
                    add(new ResourceConfig(resource));
                } catch (Exception e) {}
            }
        }
    }

    public String getCommaSeparatedResourceNames() {
        return append("", resourceNames());
    }

    public ResourceConfigs(List<ResourceConfig> resourceConfigs) {
        super(resourceConfigs);
    }

    @Override
    public boolean add(ResourceConfig resourceConfig) {
        if (!this.contains(resourceConfig) && !StringUtils.isBlank(resourceConfig.getName())) {
            super.add(resourceConfig);
            return true;
        }
        return false;
    }

    public List<String> resourceNames() {
        Set<String> names = new TreeSet<>();
        for (ResourceConfig resourceConfig : this) {
            names.add(resourceConfig.getName());
        }
        return new ArrayList<>(names);
    }

    @Override
    public String toString() {
        return StringUtil.joinForDisplay(resourceNames());
    }

    @Override
    public int compareTo(ResourceConfigs other) {
        return new AlphaAsciiCollectionComparator<ResourceConfig>().compare(this, other);
    }

    public boolean validateTree(ValidationContext validationContext) {
        boolean isValid = errors().isEmpty();
        for (ResourceConfig resourceConfig : this) {
            isValid = resourceConfig.validateTree(validationContext) && isValid;
        }
        return isValid;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        for (ResourceConfig resourceConfig : this) {
            resourceConfig.validate(validationContext);
        }
    }

    @Override
    public ConfigErrors errors() {
        this.stream().filter(resourceConfig -> !resourceConfig.errors().isEmpty())
                .forEach(resourceConfig -> configErrors.addAll(resourceConfig.errors()));
        return configErrors;
    }

    @Override
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
            add(new ResourceConfig(resourceName.trim()));
        }
    }
}
