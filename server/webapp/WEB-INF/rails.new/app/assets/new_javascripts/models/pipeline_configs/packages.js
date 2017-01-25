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

define(['mithril', 'lodash', 'string-plus', 'models/model_mixins', 'models/validatable_mixin', 'js-routes', 'models/pipeline_configs/plugin_infos', 'models/shared/plugin_configurations', 'models/crud_mixins'],
  function (m, _, s, Mixins, Validatable, Routes, PluginInfos, PluginConfigurations, CrudMixins) {

    var Packages = function (data) {
      Mixins.HasMany.call(this, {
        factory:    Packages.Package.create,
        as:         'Package',
        collection: data,
        uniqueOn:   'id'
      });
    };

    Packages.Package = function (data) {
      this.id            = m.prop(s.defaultToIfBlank(data.id, ''));
      this.name          = m.prop(s.defaultToIfBlank(data.name, ''));
      this.autoUpdate    = m.prop(s.defaultToIfBlank(data.autoUpdate, true));
      this.configuration = m.prop(data.configuration);
      this.packageRepo = m.prop(data.packageRepo);

      this.parent = Mixins.GetterSetter();
      this.etag   = Mixins.GetterSetter();

      Mixins.HasUUID.call(this);

      Validatable.call(this, data);


      this.toJSON = function () {
        /* eslint-disable camelcase */
        return {
          id:            this.id(),
          name:          this.name(),
          auto_update:   this.autoUpdate(),
          package_repo:  this.packageRepo().toJSON(),
          configuration: this.configuration().toJSON()
        };
        /* eslint-enable camelcase */
      };

      CrudMixins.Update.call(this, {
        url:     function (id) {
          /* eslint-disable camelcase */
          return Routes.apiv1AdminPackagePath({package_id: id});
          /* eslint-enable camelcase */
        },
        version: 'v1',
        type:    Packages.Package
      });


      CrudMixins.Create.call(this, {
        url:     function () {
          return Routes.apiv1AdminPackagesPath();
        },
        version: 'v1',
        type:    Packages.Package
      });

      CrudMixins.Refresh.call(this, {
        url:     function (id) {
          /* eslint-disable camelcase */
          return Routes.apiv1AdminPackagePath({package_id: id});
          /* eslint-enable camelcase */
        },
        version: 'v1',
        type:    Packages.Package
      });
    };

    Packages.Package.fromJSON = function (data) {
      return new Packages.Package({
        id:            data.id,
        name:          data.name,
        autoUpdate:    data.auto_update,
        configuration: PluginConfigurations.fromJSON(data.configuration),
        packageRepo:   Packages.Package.PackageRepository.fromJSON(data.package_repo),
        errors:        data.errors
      });
    };

    Mixins.fromJSONCollection({
      parentType: Packages,
      childType:  Packages.Package,
      via:        'addPackage'
    });

    Packages.Package.get = function (id) {
      return new Packages.Package({id: id}).refresh();
    };

    Packages.Package.create = function (data) {
      return new Packages.Package(data);
    };

    Packages.Package.initialize = function (repository, configurations) {
      return new Packages.Package({
        configuration: PluginConfigurations.fromJSON(configProperties(configurations)),
        packageRepo:   new Packages.Package.PackageRepository({
          id:   repository.id(),
          name: repository.name()
        })
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

    Packages.Package.PackageRepository.fromJSON = function (data) {
      data = _.assign({}, data);
      return new Packages.Package.PackageRepository({
        id:   data.id,
        name: data.name
      });
    };

    Packages.getPackage = function (packageId, packageForEdit, packageReference, repository) {
      var existingPackage = repository.packages().findPackage(function (pkg) {
        return pkg.id() === packageId;
      });

      existingPackage.refresh().then(function (packageMaterial) {
        packageForEdit(packageMaterial);
        packageReference(packageId);
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