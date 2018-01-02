/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.elastic;

import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigSubtag;
import com.thoughtworks.go.config.ConfigTag;

@ConfigTag("elastic")
public class ElasticConfig {
    @ConfigAttribute(value = "jobStarvationTimeout", optional = true, allowNull = false)
    private int jobStarvationTimeout = 2;

    @ConfigSubtag
    private ElasticProfiles profiles = new ElasticProfiles();

    public Long getJobStarvationTimeout() {
        return jobStarvationTimeout * 60 * 1000L;
    }

    public ElasticProfiles getProfiles() {
        return profiles;
    }

    public void setProfiles(ElasticProfiles profiles) {
        this.profiles = profiles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ElasticConfig that = (ElasticConfig) o;

        if (jobStarvationTimeout != that.jobStarvationTimeout) return false;
        return profiles != null ? profiles.equals(that.profiles) : that.profiles == null;

    }

    @Override
    public int hashCode() {
        int result = jobStarvationTimeout;
        result = 31 * result + (profiles != null ? profiles.hashCode() : 0);
        return result;
    }
}
