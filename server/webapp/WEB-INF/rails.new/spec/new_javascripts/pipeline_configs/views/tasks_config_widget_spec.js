/*
 * Copyright 2015 ThoughtWorks, Inc.
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

define(["jquery", "mithril", "pipeline_configs/models/tasks", "pipeline_configs/views/tasks_config_widget"], function ($, m, Tasks, TasksConfigWidget) {
  describe("Tasks Widget", function () {
    var root, $root;
    var tasks;

    var antTask, nantTask, execTask, rakeTask, fetchArtifactTask;
    beforeEach(function () {
      antTask = new Tasks.Task.Ant({
        buildFile:  'build-moduleA.xml',
        target:     "clean",
        workingDir: "moduleA"
      });

      nantTask = new Tasks.Task.NAnt({
        buildFile:  'build-moduleA.xml',
        target:     "clean",
        workingDir: "moduleA",
        nantHome:   'C:\\NAnt'
      });

      execTask = new Tasks.Task.Exec({
        command:    'bash',
        args:       ['-c', 'ls -al /'],
        workingDir: "moduleA"
      });

      rakeTask = new Tasks.Task.Rake({
        buildFile:  'foo.rake',
        target:     "clean",
        workingDir: "moduleA"
      });

      fetchArtifactTask = new Tasks.Task.FetchArtifact({
        pipeline: 'Build',
        stage:    "Dist",
        job:      "RPM",
        source:   new Tasks.Task.FetchArtifact.Source({type: 'dir', location: 'pkg'})
      });

      tasks = new Tasks();

      _.invoke([
          antTask,
          nantTask,
          execTask,
          rakeTask,
          fetchArtifactTask
        ],
        'parent', tasks);

      root  = document.createElement("div");
      document.body.appendChild(root);
      $root = $(root);

      m.render(root,
        m.component(TasksConfigWidget, {tasks: tasks})
      );
    });

    afterEach(function () {
      root.parentNode.removeChild(root);
    });

    describe("SVN", function () {
      it("should foo", function () {
        expect(true).toBe(true);
      });

    });

  });
});
