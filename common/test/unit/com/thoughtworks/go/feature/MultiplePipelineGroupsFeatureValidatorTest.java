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

package com.thoughtworks.go.feature;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

public class MultiplePipelineGroupsFeatureValidatorTest {
    private MultiplePipelineGroupsFeatureValidator feature;

    @Before
    public void setUp() {
        feature = new MultiplePipelineGroupsFeatureValidator();
    }

    @Test
    public void shouldOnlySupportMultiplePipelineGroupsFeature() {
        assertThat(feature.support(EnterpriseFeature.MULTIPLE_PIPELINE_GROUP), is(true));
        assertThat(feature.support(EnterpriseFeature.OPERATE_PERMISSION), is(false));
    }
}
