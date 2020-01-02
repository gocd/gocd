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
package com.thoughtworks.go.config.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ErrorCollectorTest {

    @Test
    public void shouldCollectGlobalErrors() throws Exception {
        ConfigErrors first = new ConfigErrors();
        first.add("field-one", "error-one");
        ConfigErrors second = new ConfigErrors();
        second.add("field-two", "error-two");
        ArrayList<String> errorBucket = new ArrayList<>();
        ErrorCollector.collectGlobalErrors(errorBucket, Arrays.asList(first, second));
        assertThat(errorBucket.size(),is(2));
        assertThat(errorBucket.contains("error-one"),is(true));
        assertThat(errorBucket.contains("error-two"),is(true));
    }

    @Test
    public void shouldCollectErrorsWithLabel() throws Exception {
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.addError("name", "name is mandatory field");
        packageRepository.getConfiguration().add(new ConfigurationProperty(new ConfigurationKey("name"),new ConfigurationValue("value")));
        packageRepository.getConfiguration().get(0).getConfigurationKey().addError("name", "url is mandatory field");

        HashMap<String, List<String>> errorsMap = new HashMap<>();
        ErrorCollector.collectFieldErrors(errorsMap, "package_repository", packageRepository);

        List<String> nameErrors = new ArrayList<>();
        nameErrors.add("name is mandatory field");
        assertThat(errorsMap.get("package_repository[name]"), is(nameErrors));
        List<String> urlErrors = new ArrayList<>();
        urlErrors.add("url is mandatory field");
        assertThat(errorsMap.get("package_repository[configuration][0][configurationKey][name]"), is(urlErrors));
    }
}
