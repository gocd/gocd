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

define(["jquery", "mithril", "views/pipeline_configs/package_repositories/configuration_widget", "models/pipeline_configs/materials",
    'models/pipeline_configs/repositories', 'models/pipeline_configs/plugin_infos'],
  function ($, m, ConfigurationWidget, Materials, Repositories, PluginInfos) {

    describe("ConfigurationWidget", function () {
      var $root = $('#mithril-mount-point'), root = $root.get(0);

      var config = {
        "key":   "REPO_URL",
        "value": "http://repository"
      };

      var mount = function (configuration) {
        m.mount(root,
          m.component(ConfigurationWidget,
            {
              'configuration': configuration
            })
        );
        m.redraw(true);
      };


      beforeEach(function () {
        var configuration = new Repositories.Repository.Configurations.Configuration(config);
        mount(configuration);
      });

      afterEach(function () {
        m.mount(root, null);
        m.redraw(true);
      });

      it("should have input for a configuration key", function () {
        var configuration = new Repositories.Repository.Configurations.Configuration({'key': 'REPO_URL'});
        mount(configuration);
        var input = $root.find("input[data-prop-name='value']");
        expect(input).toHaveValue('');
        var labels = $($root).find('label');
        expect(labels).toContainText("REPO_URL");
      });

      it('should contain input of type password for PASSWORD key', function () {
        var configuration = new Repositories.Repository.Configurations.Configuration({'key': 'PASSWORD'});
        mount(configuration);
        expect($root).toContainElement("input[type='password']");
        var labels = $($root).find('label');
        expect(labels).toContainText("PASSWORD");
      });

      it("should have prefilled input for a configuration key", function () {
        var input = $root.find("input[data-prop-name='value']");
        expect(input).toHaveValue('http://repository');
      });

      it('should change the configuration model on changing input element', function () {
        var configuration = new Repositories.Repository.Configurations.Configuration(config);
        mount(configuration);
        var input = $root.find("input[data-prop-name='value']");
        $(input).val('http://newrepositoryvalue').trigger('input');
        m.redraw(true);
        expect(configuration.value()).toBe('http://newrepositoryvalue');
      })

    });
  });