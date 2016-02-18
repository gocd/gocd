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

package com.thoughtworks.go.config.serialization;

import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.MagicalGoConfigXmlLoader;
import com.thoughtworks.go.config.RakeTask;
import com.thoughtworks.go.domain.KillAllChildProcessTask;
import com.thoughtworks.go.domain.NullTask;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RakeTaskTest {
    @Test
    public void shouldDefaultToUsingAKillAllChildrenCancelTask() throws Exception {
        RakeTask rakeTask = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()).fromXmlPartial("<rake/>",RakeTask.class);
        assertThat(rakeTask.cancelTask(), is(instanceOf(KillAllChildProcessTask.class)));
    }

    @Test
    public void shouldNotKillAllChildrenWhenEmptyOnCancel() throws Exception {
        RakeTask rakeTask = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()).fromXmlPartial(
                "<rake>"
                + "  <oncancel />"
                + "</rake>", RakeTask.class
        );
        assertThat(rakeTask.cancelTask(), is(instanceOf(NullTask.class)));
    }
}
