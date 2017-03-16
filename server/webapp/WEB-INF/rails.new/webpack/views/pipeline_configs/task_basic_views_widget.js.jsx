/*
 * Copyright 2017 ThoughtWorks, Inc.
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

let m                   = require('mithril');
let f                   = require('helpers/form_helper');
let LookupCommandWidget = require('views/pipeline_configs/lookup_command_widget');

const _             = require('lodash');
const $             = require('jquery');
const angular       = require('angular');
const Routes        = require('gen/js-routes');
const Argument      = require('models/pipeline_configs/argument');
const PluginInfos   = require('models/pipeline_configs/plugin_infos');
const AngularPlugin = require('views/shared/angular_plugin');
const tt            = require('helpers/pipeline_configs/tooltips');

const TaskBasicViews = {
  ant: {
    view (vnode) {
      const task = vnode.attrs.task;
      return (
        <div class="task-basic">
          <f.row>
            <f.inputWithLabel attrName='buildFile'
                              model={task}
                              tooltip={{
                                content:   tt.task.ant.buildFile,
                                direction: 'bottom',
                                size:      'small'
                              }}/>
            <f.inputWithLabel attrName='target'
                              model={task}
                              tooltip={{
                                content:   tt.task.ant.target,
                                direction: 'bottom',
                                size:      'small'
                              }}/>
            <f.inputWithLabel attrName='workingDirectory'
                              model={task}
                              end={true}
                              validate={true}
                              tooltip={{
                                content:   tt.task.ant.workingDirectory,
                                direction: 'bottom',
                                size:      'small'
                              }}/>
          </f.row>
        </div>
      );
    }
  },

  nant: {
    view (vnode) {
      const task = vnode.attrs.task;
      return (
        <div class="task-basic">
          <f.row>
            <f.inputWithLabel attrName='buildFile'
                              model={task}
                              tooltip={{
                                content:   tt.task.nant.buildFile,
                                direction: 'bottom',
                                size:      'medium'
                              }}/>
            <f.inputWithLabel attrName='target'
                              model={task}
                              tooltip={{
                                content:   tt.task.nant.target,
                                direction: 'bottom',
                                size:      'medium'
                              }}/>
            <f.inputWithLabel attrName='workingDirectory'
                              model={task}
                              end={true}
                              validate={true}
                              tooltip={{
                                content:   tt.task.nant.workingDirectory,
                                direction: 'bottom',
                                size:      'small'
                              }}/>

          </f.row>
          <f.row>
            <f.inputWithLabel attrName='nantPath'
                              model={task}
                              end={true}
                              tooltip={{
                                content:   tt.task.nant.path,
                                direction: 'bottom',
                                size:      'medium'
                              }}/>
          </f.row>
        </div>
      );
    }
  },

  exec: {
    view (vnode) {
      const task     = vnode.attrs.task;
      const vm       = Argument.vm(task.args());
      const taskArgs = function () {
        if (task.args().isList()) {
          return (
            <f.column size={8}>
              <f.textareaWithLabel attrName="data"
                                   model={vm}
                                   label="Args"
                                   size={12}/>
            </f.column>);
        }
        return (
          <f.column size={8}>
            <f.inputWithLabel attrName='data'
                              model={vm}
                              label="Args"
                              size={12}
                              largeSize={12}/>
          </f.column>);
      };

      return (
        <div class="task-basic">
          <f.row collapse>
            <f.column size={4}>
              <f.row>
                <f.inputWithLabel attrName='command'
                                  model={task}
                                  validate={true}
                                  isRequired={true}
                                  size={12}
                                  largeSize={12}
                                  tooltip={{
                                    content:   tt.task.custom.command,
                                    direction: 'bottom',
                                    size:      'small'
                                  }}/>
                <f.inputWithLabel attrName='workingDirectory'
                                  model={task}
                                  end={true}
                                  validate={true}
                                  size={12}
                                  largeSize={12}
                                  tooltip={{
                                    content:   tt.task.custom.workingDirectory,
                                    direction: 'bottom',
                                    size:      'medium'
                                  }}/>
              </f.row>
            </f.column>
            {taskArgs()}
          </f.row>
          <LookupCommandWidget model={vnode.attrs.task}/>
        </div>
      );
    }
  },

  rake: {
    view (vnode) {
      const task = vnode.attrs.task;
      return (
        <div class="task-basic">
          <f.row>
            <f.inputWithLabel attrName='buildFile'
                              model={task}
                              tooltip={{
                                content:   tt.task.rake.buildFile,
                                direction: 'bottom',
                                size:      'small'
                              }}/>
            <f.inputWithLabel attrName='target'
                              model={task}
                              tooltip={{
                                content:   tt.task.rake.target,
                                direction: 'bottom',
                                size:      'medium'
                              }}/>
            <f.inputWithLabel attrName='workingDirectory'
                              model={task}
                              end={true}
                              validate={true}
                              tooltip={{
                                content:   tt.task.rake.workingDirectory,
                                direction: 'bottom',
                                size:      'small'
                              }}/>
          </f.row>
        </div>
      );
    }
  },

  fetch: {
    view (vnode) {
      const task = vnode.attrs.task;
      return (
        <div class="task-basic">
          <f.row>
            <f.inputWithLabel attrName='pipeline'
                              model={task}
                              tooltip={{
                                content:   tt.task.fetchArtifacts.pipeline,
                                direction: 'bottom',
                                size:      'large'
                              }}/>
            <f.inputWithLabel attrName='stage'
                              model={task}
                              validate={true}
                              isRequired={true}
                              tooltip={{
                                content:   tt.task.fetchArtifacts.stage,
                                direction: 'bottom',
                                size:      'small'
                              }}/>
            <f.inputWithLabel attrName='job'
                              model={task}
                              validate={true}
                              isRequired={true}
                              end={true}
                              tooltip={{
                                content:   tt.task.fetchArtifacts.job,
                                direction: 'bottom',
                                size:      'small'
                              }}/>
          </f.row>
          <f.row>
            <f.inputWithLabel attrName='source'
                              model={task}
                              validate={true}
                              isRequired={true}
                              tooltip={{
                                content:   tt.task.fetchArtifacts.source,
                                direction: 'bottom',
                                size:      'medium'
                              }}/>
            <f.inputWithLabel attrName='destination'
                              model={task}
                              tooltip={{
                                content:   tt.task.fetchArtifacts.destination,
                                direction: 'bottom',
                                size:      'medium'
                              }}/>
            <f.checkBox attrName='isSourceAFile'
                        model={task}
                        class='check'
                        label='Source is a file'
                        addPadding={true}
                        end={true}/>
          </f.row>
        </div>
      );
    }
  },

  pluggable_task: { //eslint-disable-line camelcase
    oninit (vnode) {
      const self            = this;
      const ctrl            = self;
      self.task             = vnode.attrs.task;
      self.ngControllerName = `controller-${  self.task.uuid()}`;
      self.appName          = `app-${  self.task.uuid()}`;

      PluginInfos.PluginInfo.get(this.task.pluginId()).then((plugin) => {
        self.templateHTML      = AngularPlugin.template(plugin.viewTemplate());
        self.defaultTaskConfig = plugin.configurations();

        self.ngController = angular.module(self.appName, []).controller(self.ngControllerName, ['$scope', '$http', function ($scope, $http) {
          $scope.addError = function (field) {
            $scope.GOINPUTNAME[field.key]                              = {$error: {}};
            $scope.GOINPUTNAME[field.key].$error[_.toLower(field.key)] = field.errors.join();
          };

          $scope.clearErrors = function () {
            $scope.GOINPUTNAME               = {};
            $scope.pluggableTaskGenericError = null;
          };

          $scope.clearErrors();

          const ajaxValidator = _.debounce((configuration, config, newValue) => {
            configuration.setConfiguration(config.key, newValue);
            const req = {
              url:     Routes.apiInternalPluggableTaskValidationPath({
                plugin_id: ctrl.task.pluginId()//eslint-disable-line camelcase
              }),
              method:  'POST',
              headers: {
                'Content-Type': 'application/json',
                'Confirm':      true,
                'X-CSRF-Token': $('meta[name=csrf-token]').attr('content')
              },
              data:    JSON.stringify(configuration)
            };

            $http(req).then(
              $scope.clearErrors.bind($scope),
              (response) => {
                if (response.status === 422) {
                  _.each(response.data, $scope.addError, $scope);
                } else if (response.status === 520) {
                  $scope.pluggableTaskGenericError = response.data.error;
                }
              });
          }, 250);

          _.map(self.defaultTaskConfig, (config) => {
            const configuration = ctrl.task.configuration();
            $scope[config.key]  = configuration.valueFor(config.key);

            $scope.$watch(config.key, (newValue) => {
              configuration.setConfiguration(config.key, newValue);
              ajaxValidator(configuration, config, newValue);
            });
          });
        }]);
      });
    },

    view (vnode) {
      const config = function (_elem, isInitialized) {
        if (!isInitialized) {
          const pluginTaskTemplateElement = $(`#pluggable-task-template-${  vnode.state.task.uuid()}`);
          angular.bootstrap(pluginTaskTemplateElement.get(0), [vnode.state.appName]);
        }
      };

      return (
        <div id={`pluggable-task-template-${  vnode.state.task.uuid()}`} ng-controller={vnode.state.ngControllerName}
             class='task-basic'
             config={config}>
          <div class="callout alert"
               ng-show="pluggableTaskGenericError">{'{{pluggableTaskGenericError}}'}</div>
          <div class='pluggable-task'>
            {m.trust(vnode.state.templateHTML)}
          </div>
        </div>
      );
    }
  }
};

module.exports = TaskBasicViews;
