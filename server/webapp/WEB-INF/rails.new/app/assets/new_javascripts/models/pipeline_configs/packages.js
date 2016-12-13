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

define(['mithril', 'lodash', 'string-plus', 'helpers/mrequest', 'models/errors', 'models/validatable_mixin', 'js-routes', 'models/pipeline_configs/plugin_infos', 'models/shared/plugin_configurations'],
  function (m, _, s, mrequest, Errors, Validatable, Routes, PluginInfos, PluginConfigurations) {

    var Packages             = m.prop([]);
    Packages.packageIdToEtag = {};

    Packages.Package = function (data) {
      Validatable.call(this, data);

      this.init = function (data) {
        this.id            = m.prop(s.defaultToIfBlank(data.id, ''));
        this.name          = m.prop(s.defaultToIfBlank(data.name, ''));
        this.autoUpdate    = m.prop(s.defaultToIfBlank(data.auto_update, true));
        this.configuration = s.collectionToJSON(m.prop(PluginConfigurations.fromJSON(data.configuration || {})));
        this.packageRepo   = m.prop(new Packages.Package.PackageRepository(data.package_repo || {}));
        this.errors        = m.prop(new Errors(data.errors));
      };

      this.init(data);

      this.reInitialize = function (data) {
        this.init(data);
      };

      this.toJSON = function () {
        /* eslint-disable camelcase */
        return {
          id:            this.id(),
          name:          this.name(),
          auto_update:   this.autoUpdate(),
          package_repo:  this.packageRepo.toJSON(),
          configuration: this.configuration
        };
        /* eslint-enable camelcase */
      };

      this.update = function () {
        var self = this;

        var config = function (xhr) {
          xhr.setRequestHeader("Content-Type", "application/json");
          xhr.setRequestHeader("Accept", "application/vnd.go.cd.v1+json");
          xhr.setRequestHeader("If-Match", Packages.packageIdToEtag[self.id()]);
        };

        var extract = function (xhr) {
          if (xhr.status === 200) {
            Packages.packageIdToEtag[self.id()] = xhr.getResponseHeader('ETag');
          }
          return xhr.responseText;
        };

        return m.request({
          method:     'PUT',
          url:        Routes.apiv1AdminPackagePath({package_id: this.id()}), //eslint-disable-line camelcase
          background: false,
          config:     config,
          extract:    extract,
          data:       this,
          type:       Packages.Package
        });
      };

      this.create = function () {
        var extract = function (xhr) {
          if (xhr.status === 200) {
            Packages.packageIdToEtag[JSON.parse(xhr.responseText).id] = xhr.getResponseHeader('ETag');
          }
          return xhr.responseText;
        };

        return m.request({
          method:     'POST',
          url:        Routes.apiv1AdminPackagesPath(),
          background: false,
          config:     mrequest.xhrConfig.v1,
          extract:    extract,
          data:       this,
          type:       Packages.Package
        });
      };
    };

    Packages.Package.initialize = function (repository, configurations) {
      return new Packages.Package({
        /* eslint-disable camelcase */
        configuration: configProperties(configurations),
        package_repo:  {
          id:   repository.id(),
          name: repository.name()
        }
        /* eslint-enable camelcase */
      });
    };

    Packages.Package.setPackageForEdit = function (packageForEdit, pluginId, repository) {
      PluginInfos.PluginInfo.get(pluginId).then(function (pluginInfo) {
        var allConfigurations     = pluginInfo.configurations();
        var packageConfigurations = _.filter(allConfigurations, function (configuration) {
          return configuration.type === 'package';
        });
        var packageMaterial       = Packages.Package.initialize(repository, packageConfigurations);
        packageForEdit(packageMaterial);
      });
    };

    var configProperties = function (configurations) {
      var config = [];
      _.map(configurations, function (configuration) {
        return config.push({key: configuration.key});
      });
      return config;
    };

    Packages.Package.PackageRepository = function (data) {
      this.id   = m.prop(s.defaultToIfBlank(data.id, ''));
      this.name = m.prop(s.defaultToIfBlank(data.name, ''));

      this.toJSON = function () {
        return {
          id:   this.id(),
          name: this.name()
        };
      };
    };

    Packages.findById = function (id) {

      var extract = function (xhr) {
        Packages.packageIdToEtag[id] = xhr.getResponseHeader('ETag');
        return xhr.responseText;
      };

      return m.request({
        method:     'GET',
        url:        Routes.apiv1AdminPackagePath({package_id: id}),  //eslint-disable-line camelcase
        background: false,
        config:     mrequest.xhrConfig.v1,
        extract:    extract,
        type:       Packages.Package
      });
    };

    Packages.getPackage = function (packageId, packageForEdit) {
      Packages.findById(packageId).then(function (packageMaterial) {
        packageForEdit(packageMaterial);
      });
    };

    Packages.vm = function () {
      this.saveState = m.prop('');
      var errors     = [];

      this.startUpdating = function () {
        errors = [];
        this.saveState('in-progress disabled');
      };

      this.saveFailed = function (data) {
        errors.push(data.message);

        if (data.data) {
          if (data.data.configuration) {
            errors = _.concat(errors, _.flattenDeep(_.map(data.data.configuration, function (conf) {
              return _.values(conf.errors);
            })));
          }
        }

        this.saveState('alert');
      };

      this.saveSuccess = function () {
        this.saveState('success');
      };

      this.clearErrors = function () {
        errors = [];
      };

      this.reset = function () {
        errors = [];
        this.saveState('');
      };

      this.errors = function () {
        return errors;
      };

      this.hasErrors = function () {
        return !_.isEmpty(errors);
      };

      this.markClientSideErrors = function () {
        errors.push('There are errors on the page, fix them and save');
      };
    };

    return Packages;
  });