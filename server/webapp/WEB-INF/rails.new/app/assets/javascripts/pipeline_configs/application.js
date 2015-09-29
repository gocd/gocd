/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

requirejs(['jquery', 'mithril', 'pipeline_configs/models/pluggable_tasks', 'pipeline_configs/views/pipeline_config_widget', 'foundation.topbar'], function ($, m, PluggableTasks, PipelineConfigWidget) {

  $(function () {
    $(document).foundation();

    var pipelineConfigElem            = $('#pipeline-config');
    var url                           = pipelineConfigElem.attr('data-pipeline-api-url');
    var taskPluginTemplateDescriptors = JSON.parse(pipelineConfigElem.attr('data-task-template-plugins'));
    PluggableTasks.initializeWith(taskPluginTemplateDescriptors);

    m.mount(pipelineConfigElem.get(0), PipelineConfigWidget(url));
  });
});
