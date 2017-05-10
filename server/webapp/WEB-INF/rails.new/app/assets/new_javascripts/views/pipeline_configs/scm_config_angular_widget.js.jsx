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

define(['mithril', 'lodash', 'jquery', 'string-plus', 'models/pipeline_configs/plugin_infos', 'views/shared/angular_plugin'],
  function (m, _, $, s, PluginInfos, AngularPlugin) {
    var SCMConfigAngularWidget = {
      controller: function (args) {
        var self              = this;
        self.scm              = args.scm;
        self.plugin           = args.plugin;
        self.pluginId         = args.scm.pluginMetadata().id();
        self.uuid             = s.uuid();
        self.ngControllerName = 'controller-' + self.uuid;
        self.appName          = 'app-' + self.uuid;

        PluginInfos.PluginInfo.get(self.pluginId).then(function (plugin) {
          self.templateHTML = AngularPlugin.template(plugin.viewTemplate());
          self.defaultConfig = plugin.configurations();

          self.ngController = angular.module(self.appName, []).controller(self.ngControllerName, ['$scope', '$http', function ($scope) {
            $scope.addError = function (field) {
              $scope.GOINPUTNAME[field.key] = {$error: {}};
              $scope.GOINPUTNAME[field.key].$error[_.toLower(field.key)] = field.errors.join();
            };

            $scope.clearErrors = function () {
              $scope.GOINPUTNAME = {};
              $scope.pluggableTaskGenericError = null;
            };

            $scope.clearErrors();

            _.map(self.defaultConfig, function (config) {
              $scope[config.key] = self.scm.configuration().valueFor(config.key);

              $scope.$watch(config.key, function (newValue) {
                self.scm.configuration().setConfiguration(config.key, newValue);
              });
            });

            args.parentView.onClose(function () {
              _.map(self.defaultConfig, function (config) {
                $scope[config.key] = self.scm.configuration().valueFor(config.key);
              });

              $scope.$apply();
            });
          }]);
        });
      },

      view: function (ctrl) {
        var config = function (_elem, isInitialized) {
          if (!isInitialized) {
            var pluginTaskTemplateElement = $('#pluggable-scm-template-' + ctrl.uuid);
            angular.bootstrap(pluginTaskTemplateElement.get(0), [ctrl.appName]);
          }
        };

        return (
          <div class='plugin-view' id={'pluggable-scm-template-' + ctrl.uuid} ng-controller={ctrl.ngControllerName} config={config}>
            <div class="callout alert"
                 ng-show="pluggableTaskGenericError">{'{{pluggableTaskGenericError}}'}</div>
            {m.trust(ctrl.templateHTML)}
          </div>
        );
      }
    };

    return SCMConfigAngularWidget;
  }
);
