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
package com.thoughtworks.go.apiv9.shared.representers.helpers;


import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import com.thoughtworks.go.plugin.api.task.TaskView;

public class TaskMother {
  public class StubTask implements Task {

      private TaskConfig taskConfig;

      public StubTask() {
          this.taskConfig = new TaskConfig();
      }
      @Override
      public TaskConfig config() {
          return taskConfig;
      }

      @Override
      public TaskExecutor executor() {
          return null;
      }

      @Override
      public TaskView view() {
          return null;
      }

      @Override
      public ValidationResult validate(TaskConfig configuration) {
          return null;
      }
  }
}
