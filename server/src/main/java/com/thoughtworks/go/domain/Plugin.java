/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.json.JsonHelper;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@EqualsAndHashCode(doNotUseGetters = true, callSuper = true)
@ToString(callSuper = true)
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@Table(name = "plugins")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Plugin extends HibernatePersistedObject {
    protected String pluginId;
    @Access(AccessType.PROPERTY)
    private String configuration;
    @Transient
    private Map<String, String> configurationDataMap = Collections.emptyMap();

    public Plugin setConfiguration(String configuration) {
        this.configuration = configuration;
        this.configurationDataMap = JsonHelper.safeFromJson(this.configuration, HashMap.class);
        return this;
    }

    public Set<String> getAllConfigurationKeys() {
        return Collections.unmodifiableSet(getConfigurationDataMap().keySet());
    }

    public String getConfigurationValue(String key) {
        return getConfigurationDataMap().get(key);
    }

    private Map<String, String> getConfigurationDataMap() {
        return configurationDataMap;
    }
}
