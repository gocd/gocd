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

package com.thoughtworks.go.presentation.renderer;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.plugins.presentation.Renderer;
import com.thoughtworks.go.presentation.TaskViewModel;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FreemarkerRendererTest {

    @org.junit.Ignore  
    @Test
    public void shouldRenderIfFor_oneArgPredicate() throws Exception {
        ExecTask task = new ExecTask();
        task.errors().add(ExecTask.COMMAND, "bad command, it is!");
        Map<String, Object> localContext = new HashMap<String, Object>();
        localContext.put("task", task);
        String view = new FreemarkerRenderer().render(new TaskViewModel(task, this.getClass().getResource("one_arg_pred.ftl").toString(), Renderer.FREEMARKER), localContext);
        assertThat(view, is("ERROR: bad command, it is!"));
    }

}
