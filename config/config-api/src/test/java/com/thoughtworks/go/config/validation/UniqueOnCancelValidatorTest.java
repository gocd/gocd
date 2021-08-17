/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config.validation;

import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.domain.Task;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UniqueOnCancelValidatorTest {
    @Test
    public void shouldNotFailWithExceptionWhenThereAreNoOnCancelTasksForABuiltInTask() throws Exception {
        ConfigElementImplementationRegistry registry = mock(ConfigElementImplementationRegistry.class);
        when(registry.implementersOf(Task.class)).thenReturn(tasks(ExecTask.class));

        String content =
                "<cruise>"
                + "  <pipeline>"
                + "    <stage>"
                + "      <jobs>"
                + "        <job>"
                + "          <tasks>"
                + "            <exec command=\"install_addons.sh\">"
                + "              <runif status=\"passed\" />\n"
                + "            </exec>"
                + "          </tasks>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "  </pipeline>"
                + "</cruise>";

        UniqueOnCancelValidator validator = new UniqueOnCancelValidator();
        validator.validate(elementFor(content), registry);
    }

    @Test
    public void shouldNotFailWithExceptionWhenThereIsOneOnCancelTaskForABuiltInTask() throws Exception {
        ConfigElementImplementationRegistry registry = mock(ConfigElementImplementationRegistry.class);
        when(registry.implementersOf(Task.class)).thenReturn(tasks(ExecTask.class));

        String content =
                "<cruise>"
                + "  <pipeline>"
                + "    <stage>"
                + "      <jobs>"
                + "        <job>"
                + "          <tasks>"
                + "            <exec command=\"install_addons.sh\">"
                + "              <runif status=\"passed\" />"
                + "               <oncancel>\n"
                + "                 <ant buildfile=\"build.xml\" />\n"
                + "               </oncancel>"
                + "            </exec>"
                + "          </tasks>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "  </pipeline>"
                + "</cruise>";

        UniqueOnCancelValidator validator = new UniqueOnCancelValidator();
        validator.validate(elementFor(content), registry);
    }

    @Test
    public void shouldFailWithExceptionWhenThereIsMoreThanOneOnCancelTasksForABuiltInTask() throws Exception {
        ConfigElementImplementationRegistry registry = mock(ConfigElementImplementationRegistry.class);
        when(registry.implementersOf(Task.class)).thenReturn(tasks(ExecTask.class));

        String content =
                "<cruise>"
                + "  <pipeline>"
                + "    <stage>"
                + "      <jobs>"
                + "        <job>"
                + "          <tasks>"
                + "            <exec command=\"install_addons.sh\">"
                + "              <runif status=\"passed\" />"
                + "               <oncancel>\n"
                + "                 <ant buildfile=\"build1.xml\" />\n"
                + "               </oncancel>"
                + "               <oncancel>\n"
                + "                 <ant buildfile=\"build2.xml\" />\n"
                + "               </oncancel>"
                + "            </exec>"
                + "          </tasks>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "  </pipeline>"
                + "</cruise>";

        try {
            UniqueOnCancelValidator validator = new UniqueOnCancelValidator();
            validator.validate(elementFor(content), registry);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Task [exec] should not contain more than 1 oncancel task"));
        }
    }

    @Test
    public void shouldNotFailWithExceptionWhenThereAreNoOnCancelTasksForAPluginInTask() throws Exception {
        ConfigElementImplementationRegistry registry = mock(ConfigElementImplementationRegistry.class);
        when(registry.implementersOf(Task.class)).thenReturn(tasks(ExecTask.class));

        String content =
                "<cruise>"
                + "  <pipeline>"
                + "    <stage>"
                + "      <jobs>"
                + "        <job>"
                + "          <tasks>"
                + "            <task name=\"\">\n"
                + "              <pluginConfiguration id=\"curl.task.plugin\" version=\"1\" />\n"
                + "              <configuration>\n"
                + "                <property>\n"
                + "                  <key>Url</key>\n"
                + "                  <value>URL</value>\n"
                + "                </property>\n"
                + "              </configuration>\n"
                + "              <runif status=\"passed\" />\n"
                + "            </task>"
                + "          </tasks>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "  </pipeline>"
                + "</cruise>";

        UniqueOnCancelValidator validator = new UniqueOnCancelValidator();
        validator.validate(elementFor(content), registry);
    }

    @Test
    public void shouldNotFailWithExceptionWhenThereIsOneOnCancelTasksForAPluginInTask() throws Exception {
        ConfigElementImplementationRegistry registry = mock(ConfigElementImplementationRegistry.class);
        when(registry.implementersOf(Task.class)).thenReturn(tasks(ExecTask.class, PluggableTask.class));

        String content =
                "<cruise>"
                + "  <pipeline>"
                + "    <stage>"
                + "      <jobs>"
                + "        <job>"
                + "          <tasks>"
                + "              <task name=\"\">\n"
                + "                <pluginConfiguration id=\"curl.task.plugin\" version=\"1\" />\n"
                + "                <configuration>\n"
                + "                  <property>\n"
                + "                    <key>Url</key>\n"
                + "                    <value>With_On_Cancel</value>\n"
                + "                  </property>\n"
                + "                </configuration>\n"
                + "                <runif status=\"passed\" />\n"
                + "                <oncancel>\n"
                + "                  <ant buildfile=\"blah\" target=\"blah\" />\n"
                + "                </oncancel>\n"
                + "              </task>"
                + "          </tasks>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "  </pipeline>"
                + "</cruise>";

        UniqueOnCancelValidator validator = new UniqueOnCancelValidator();
        validator.validate(elementFor(content), registry);
    }

    @Test
    public void shouldFailWithExceptionWhenThereIsMoreThanOneOnCancelTasksForAPluginInTask() throws Exception {
        ConfigElementImplementationRegistry registry = mock(ConfigElementImplementationRegistry.class);
        when(registry.implementersOf(Task.class)).thenReturn(tasks(ExecTask.class, PluggableTask.class));

        String content =
                "<cruise>"
                + "  <pipeline>"
                + "    <stage>"
                + "      <jobs>"
                + "        <job>"
                + "          <tasks>"
                + "              <task name=\"\">\n"
                + "                <pluginConfiguration id=\"curl.task.plugin\" version=\"1\" />\n"
                + "                <configuration>\n"
                + "                  <property>\n"
                + "                    <key>Url</key>\n"
                + "                    <value>With_On_Cancel</value>\n"
                + "                  </property>\n"
                + "                </configuration>\n"
                + "                <runif status=\"passed\" />\n"
                + "                <oncancel>\n"
                + "                  <ant buildfile=\"blah\" target=\"blah1\" />\n"
                + "                </oncancel>\n"
                + "                <oncancel>\n"
                + "                  <ant buildfile=\"blah\" target=\"blah2\" />\n"
                + "                </oncancel>\n"
                + "              </task>"
                + "          </tasks>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "  </pipeline>"
                + "</cruise>";

        try {
            UniqueOnCancelValidator validator = new UniqueOnCancelValidator();
            validator.validate(elementFor(content), registry);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Task [task] should not contain more than 1 oncancel task"));
        }
    }

    private List<Class<? extends Task>> tasks(Class<? extends Task>... taskClasses) {
        List<Class<? extends Task>> tasks = new ArrayList<>();
        Collections.addAll(tasks, taskClasses);
        return tasks;
    }

    private Element elementFor(String content) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        return builder.build(new ByteArrayInputStream(content.getBytes())).getRootElement();
    }
}
