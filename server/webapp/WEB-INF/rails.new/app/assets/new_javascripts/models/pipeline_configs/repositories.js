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

define([
  'mithril', 'lodash', 'string-plus', 'models/model_mixins', 'models/validatable_mixin', 'js-routes',
  'models/shared/plugin_configurations', 'models/pipeline_configs/packages', 'models/crud_mixins'
], function (m, _, s, Mixins, Validatable, Routes, PluginConfigurations, Packages, CrudMixins) {

  var Repositories = function (data) {
    Mixins.HasMany.call(this, {
      factory:    Repositories.Repository.create,
      as:         'Repository',
      plural:     'Repositories',
      collection: data,
      uniqueOn:   'id'
    });

    this.findRepositoryByPackageId = function (packageId) {
      return this.findRepository(function (repository) {
        return repository.packages().findPackage(function (pkg) {
          return pkg.id() === packageId;
        });
      });
    };
  };

  CrudMixins.Index({
    type:     Repositories,
    indexUrl: Routes.apiv1AdminRepositoriesPath(),
    version:  'v1',
    dataPath: '_embedded.package_repositories'
  });

  Repositories.findByPackageId = function (packageId) {
    var getRelevantPackage = function (repository) {
      return repository.packages().findPackage(function (packageMaterial) {
        return packageMaterial.id() === packageId;
      });
    };
    Repositories.all().then(function (repositories) {
      repositories.findRepository(function (repository) {
        return getRelevantPackage(repository).id() === packageId;
      });
    });
  };

  Repositories.Repository = function (data) {
    this.id             = m.prop(s.defaultToIfBlank(data.id, ''));
    this.name           = m.prop(s.defaultToIfBlank(data.name, ''));
    this.pluginMetadata = m.prop(data.pluginMetadata);
    this.configuration  = m.prop(data.configuration);
    this.packages       = m.prop(data.packages);

    this.parent = Mixins.GetterSetter();
    this.etag   = Mixins.GetterSetter();

    Mixins.HasUUID.call(this);

    Validatable.call(this, data);


    this.toJSON = function () {
      /* eslint-disable camelcase */
      return {
        repo_id:         this.id(),
        name:            this.name(),
        plugin_metadata: this.pluginMetadata().toJSON(),
        configuration:   this.configuration().toJSON()
      };
      /* eslint-enable camelcase */
    };

    CrudMixins.AllOperations.call(this, ['update', 'create', 'refresh'], {
      type:        Repositories.Repository,
      indexUrl:    Routes.apiv1AdminRepositoriesPath(),
      resourceUrl: function (id) {
        /* eslint-disable camelcase */
        return Routes.apiv1AdminRepositoryPath({repo_id: id});
        /* eslint-enable camelcase */
      },
      version:     'v1',
      dataPath:    '_embedded.package_repositories'
    });
  };

  Repositories.Repository.get = function (id) {
    return new Repositories.Repository({id: id}).refresh();
  };

  Repositories.Repository.create = function (data) {
    return new Repositories.Repository(data);
  };

  Repositories.Repository.PluginMetadata = function (data) {
    this.id      = m.prop(s.defaultToIfBlank(data.id, ''));
    this.version = m.prop(s.defaultToIfBlank(data.version, ''));

    this.toJSON = function () {
      return {
        id:      this.id(),
        version: this.version()
      };
    };
  };

  Repositories.Repository.PluginMetadata.fromJSON = function (data) {
    data = _.assign({}, data);
    return new Repositories.Repository.PluginMetadata({
      id:      data.id,
      version: data.version
    });
  };

  Repositories.Repository.fromJSON = function (data) {
    return new Repositories.Repository({
      id:             data.repo_id,
      name:           data.name,
      pluginMetadata: Repositories.Repository.PluginMetadata.fromJSON(data.plugin_metadata),
      configuration:  PluginConfigurations.fromJSON(data.configuration),
      packages:       Packages.fromJSON(_.get(data, '_embedded.packages', [])),
      errors:         data.errors
    });
  };

  Mixins.fromJSONCollection({
    parentType: Repositories,
    childType:  Repositories.Repository,
    via:        'addRepository'
  });

  Repositories.Repository.initialize = function (pluginInfo, configurations) {
    return new Repositories.Repository({
      pluginMetadata: new Repositories.Repository.PluginMetadata({
        id:      pluginInfo.id(),
        version: pluginInfo.version()
      }),
      configuration:  PluginConfigurations.fromJSON(configProperties(configurations))
    });
  };

  var configProperties = function (configurations) {
    var config = [];
    _.map(configurations, function (configuration) {
      return config.push({key: configuration.key});
    });
    return config;
  };

  Repositories.vm = function () {
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

  return Repositories;
});