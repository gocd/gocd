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
package com.thoughtworks.go.config;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.BasicCruiseConfig.AllPipelineConfigs;
import com.thoughtworks.go.config.BasicCruiseConfig.AllTemplatesWithAssociatedPipelines;
import com.thoughtworks.go.config.BasicCruiseConfig.PipelineNameToConfigMap;

// Cloner to handle nullification of specific classes in config objects.
// A specific field can be ignored from being cloned by setting `cloner.setNullTransient(true)` and marking the field as 'transient',
// but if the object being cloned has other fields which have transient fields internally,
// then those do not get cloned as well. This could have unintentinal side effects.
// For instance, if we set cloner.setNullTransient(true) and then invoke deepClone with a BasicCruiseConfig object,
// BasicCruiseConfig.groups does not get cloned properly even though it is not marked as transient.
// Thing is BasicCruiseConfig.groups is a type of ArrayList.
// ArrayList.elementData is a transient field which doesn't get cloned, causing NullPointerExceptions when `groups` is accessed from the cloned object.
// This is one place to mark all the classes to be ignored during clone.
public class GoConfigCloner extends Cloner {
    public GoConfigCloner() {
        nullInsteadOfClone(AllPipelineConfigs.class,
                AllTemplatesWithAssociatedPipelines.class,
                PipelineNameToConfigMap.class,
                CachedPluggableArtifactConfigs.class,
                CachedFetchPluggableArtifactTasks.class);
    }
}
