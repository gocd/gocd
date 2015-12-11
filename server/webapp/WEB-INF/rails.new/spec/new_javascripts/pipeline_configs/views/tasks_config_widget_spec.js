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
        buildFile:        'build-moduleA.xml',
        target:           "clean",
        workingDirectory: "moduleA"
      });

      nantTask = new Tasks.Task.NAnt({
        buildFile:        'build-moduleA.xml',
        target:           "clean",
        workingDirectory: "moduleA",
        nantPath:         'C:\\NAnt'
      });

      execTask = new Tasks.Task.Exec({
        command:          'bash',
        args:             ['-c', 'ls -al /'],
        workingDirectory: "moduleA"
      });

      rakeTask = new Tasks.Task.Rake({
        buildFile:        'foo.rake',
        target:           "clean",
        workingDirectory: "moduleA"
      });

      fetchArtifactTask = new Tasks.Task.FetchArtifact({
        pipeline: 'Build',
        stage:    "Dist",
        job:      "RPM",
        source:   new Tasks.Task.FetchArtifact.Source({type: 'dir', location: 'pkg'})
      });

      tasks = m.prop(new Tasks());

      _.each([
        antTask,
        nantTask,
        execTask,
        rakeTask,
        fetchArtifactTask
      ], function (task) {
        tasks().addTask(task);
      });

      root = document.createElement("div");
      document.body.appendChild(root);
      $root = $(root);

      m.mount(root,
        m.component(TasksConfigWidget, {tasks: tasks})
      );
      m.redraw(true);
    });

    afterEach(function () {
      root.parentNode.removeChild(root);
    });

    it("should add a new task", function () {
      expect(tasks().countTask()).toBe(5);
      expect($root.find('.task-definition').length).toBe(5);

      var addTaskButton = $root.find('.add-task a').get(0);
      var evObj         = document.createEvent('MouseEvents');
      evObj.initEvent('click', true, false);
      addTaskButton.onclick(evObj);
      m.redraw(true);

      expect(tasks().countTask()).toBe(6);
      expect($root.find('.task-definition').length).toBe(6);
    });
  });
});
