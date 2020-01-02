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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;

public class EnvironmentConfigMother {
    public static final String OMNIPRESENT_AGENT = "omnipresent-agent";

    public static EnvironmentsConfig environments(String... names) {
        EnvironmentsConfig environmentsConfig = new EnvironmentsConfig();
        for (String name : names) {
            environmentsConfig.add(environment(name));
        }
        return environmentsConfig;
    }

    private static BasicEnvironmentConfig env(String name) {
        return new BasicEnvironmentConfig(new CaseInsensitiveString(name));
    }

    public static BasicEnvironmentConfig remote(String name) {
        BasicEnvironmentConfig env = environment(name);
        env.setOrigins(new RepoConfigOrigin(new ConfigRepoConfig(MaterialConfigsMother.git("foo.git"), "json-plugon", "repo1"), "revision1"));
        return env;
    }

    public static BasicEnvironmentConfig environment(String name) {
        BasicEnvironmentConfig uat = new BasicEnvironmentConfig(new CaseInsensitiveString(name));
        uat.addPipeline(new CaseInsensitiveString(name + "-pipeline"));
        return uat;
    }

    public static BasicEnvironmentConfig environment(String name, String... pipelines) {
        BasicEnvironmentConfig config = env(name);
        for (String pipeline : pipelines) {
            config.addPipeline(new CaseInsensitiveString(pipeline));
        }
        return config;
    }
}
