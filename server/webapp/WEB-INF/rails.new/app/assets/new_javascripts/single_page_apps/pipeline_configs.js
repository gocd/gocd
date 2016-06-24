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

requirejs([
  'jquery', 'mithril',
  'models/pipeline_configs/pluggable_tasks', 'models/pipeline_configs/resources', 'models/pipeline_configs/users', 'models/pipeline_configs/roles',
  'views/pipeline_configs/pipeline_config_widget', 'foundation.util.mediaQuery', 'foundation.dropdownMenu', 'foundation.responsiveToggle',
  'foundation.dropdown'
], function ($, m,
             PluggableTasks, Resources, Users, Roles,
             PipelineConfigWidget) {

  $(function () {
    var pipelineConfigElem            = $('#pipeline-config');
    var url                           = pipelineConfigElem.attr('data-pipeline-api-url');
    var taskPluginTemplateDescriptors = JSON.parse(pipelineConfigElem.attr('data-task-template-plugins'));
    var allResourceNames              = JSON.parse(pipelineConfigElem.attr('data-resource-names'));
    var allUserNames                  = JSON.parse(pipelineConfigElem.attr('data-user-names'));
    var allRoleNames                  = JSON.parse(pipelineConfigElem.attr('data-role-names'));

    PluggableTasks.initializeWith(taskPluginTemplateDescriptors);
    Resources.initializeWith(allResourceNames);
    Users.initializeWith(allUserNames);
    Roles.initializeWith(allRoleNames);

    m.mount(pipelineConfigElem.get(0), PipelineConfigWidget(url));
    $(document).foundation();
  });
});
