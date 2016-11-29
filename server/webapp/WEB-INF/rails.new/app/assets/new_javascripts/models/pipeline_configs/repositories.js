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

define(['mithril', 'lodash', 'string-plus', 'models/model_mixins', 'helpers/mrequest', 'models/errors', 'models/pipeline_configs/encrypted_value', 'models/validatable_mixin', 'js-routes'],
  function (m, _, s, Mixins, mrequest, Errors, EncryptedValue, Validatable, Routes) {

    var Repositories          = m.prop([]);
    Repositories.repoIdToEtag = {};

    function plainOrCipherValue(data) {
      if (data.encrypted_value) {
        return new EncryptedValue({cipherText: s.defaultToIfBlank(data.encrypted_value, '')});
      } else {
        return new EncryptedValue({clearText: s.defaultToIfBlank(data.value, '')});
      }
    }

    Repositories.Repository = function (data) {
      Validatable.call(this, data);

      var embeddedPackages = function (data) {
        var getPackages = function(embedded) {
          return embedded.packages ? embedded.packages : '';
        };
        return data._embedded ? getPackages(data._embedded) : '';
      };


      this.init = function (data) {
        this.id             = m.prop(s.defaultToIfBlank(data.repo_id));
        this.name           = m.prop(s.defaultToIfBlank(data.name, ''));
        this.pluginMetadata = m.prop(new Repositories.Repository.PluginMetadata(data.plugin_metadata || {}));
        this.configuration  = s.collectionToJSON(m.prop(Repositories.Repository.Configurations.fromJSON(data.configuration || {})));
        this.packages       = m.prop(Repositories.Repository.Packages.fromJSON(embeddedPackages(data)));
        this.errors         = m.prop(new Errors(data.errors));
      };

      this.init(data);

      //this.reInitialize = function (data) {
      //  this.init(data);
      //};

      //this.clone = function () {
      //  return new Repositories.Repository(JSON.parse(JSON.stringify(this)));
      //};

      this.toJSON = function () {
        /* eslint-disable camelcase */
        return {
          repo_id:         this.id(),
          name:            this.name(),
          plugin_metadata: this.pluginMetadata().toJSON(),
          configuration:   this.configuration
        };
        /* eslint-enable camelcase */
      };

      this.update = function () {
        var self = this;

        var config = function (xhr) {
          xhr.setRequestHeader("Content-Type", "application/json");
          xhr.setRequestHeader("Accept", "application/vnd.go.cd.v1+json");
          xhr.setRequestHeader("If-Match", Repositories.repoIdToEtag[self.id()]);
        };

        var extract = function (xhr) {
          if (xhr.status === 200) {
            Repositories.repoIdToEtag[self.id()] = xhr.getResponseHeader('ETag');
          }
          return xhr.responseText;
        };

        return m.request({
          method:     'PUT',
          url:        Routes.apiv1AdminRepositoryPath({repo_id: this.id()}), //eslint-disable-line camelcase
          background: false,
          config:     config,
          extract:    extract,
          data:       this,
          type:       Repositories.Repository
        });
      };

      this.create = function () {
        var extract = function (xhr) {
          if (xhr.status === 200) {
            Repositories.repoIdToEtag[JSON.parse(xhr.responseText).repo_id] = xhr.getResponseHeader('ETag');
          }
          return xhr.responseText;
        };

        return m.request({
          method:     'POST',
          url:        Routes.apiv1AdminRepositoriesPath(),
          background: false,
          config:     mrequest.xhrConfig.v1,
          extract:    extract,
          data:       this,
          type:       Repositories.Repository
        });
      };
    };

    Repositories.Repository.initialize = function(pluginInfo, configurations) {
      return new Repositories.Repository({
        plugin_metadata: {
          id:      pluginInfo.id(),
          version: pluginInfo.version()
        },
        configuration: configProperties(configurations)
      });
    };

    var configProperties = function(configurations) {
      var config = [];
      _.map(configurations, function(configuration) {
        return config.push({key: configuration.key});
      });
      return config;
    };

    Repositories.Repository.Packages = function (data) {
      Mixins.HasMany.call(this, {
        factory:    Repositories.Repository.Packages,
        as:         'Package',
        collection: data
      });
    };

    Repositories.Repository.Packages.fromJSON = function (data) {
      var packages = _.map(data, function (d) {
        return new Repositories.Repository.Packages.Package(d);
      });

      return new Repositories.Repository.Packages(packages);
    };


    Repositories.Repository.Packages.Package = function(data) {
      this.id = m.prop(s.defaultToIfBlank(data.id, ''));
      this.name = m.prop(s.defaultToIfBlank(data.name, ''));
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

    Repositories.Repository.Configurations = function (data) {
      Mixins.HasMany.call(this, {
        factory:    Repositories.Repository.Configurations.create,
        as:         'Configuration',
        collection: data
      });

      function configForKey(key) {
        return this.findConfiguration(function (config) {
          return _.isEqual(config.key(), key);
        });
      }

      this.valueFor = function (key) {
        var config = configForKey.call(this, key);

        if (config) {
          return config.value();
        }
      };

      this.setConfiguration = function (key, value) {
        var existingConfig = configForKey.call(this, key);

        if (!existingConfig) {
          return this.createConfiguration({key: key, value: value});
        }

        if (existingConfig.isSecureValue()) {
          if (!_.isEqual(value, existingConfig.value())) {
            existingConfig.becomeUnSecureValue();
            existingConfig.value(value);
          }
        } else {
          existingConfig.value(value);
        }
      };
    };

    Repositories.Repository.Configurations.create = function (data) {
      return new Repositories.Repository.Configurations.Configuration(data);
    };

    Repositories.Repository.Configurations.fromJSON = function (data) {
      var configurations = _.map(data, function (d) {
        return new Repositories.Repository.Configurations.Configuration(d);
      });

      return new Repositories.Repository.Configurations(configurations);
    };

    Repositories.Repository.Configurations.Configuration = function (data) {
      this.parent = Mixins.GetterSetter();
      this.key    = m.prop(s.defaultToIfBlank(data.key, ''));
      var _value  = m.prop(plainOrCipherValue(data));

      Mixins.HasEncryptedAttribute.call(this, {attribute: _value, name: 'value'});

      this.toJSON = function () {
        var valueHash = this.isPlainValue() ? {value: this.value()} : {encrypted_value: this.value()};  //eslint-disable-line camelcase

        return _.merge({key: this.key()}, valueHash);
      };
    };

    Repositories.init = function () {
      return m.request({
        method:        'GET',
        url:           Routes.apiv1AdminRepositoriesPath(),
        config:        mrequest.xhrConfig.v1,
        type:          Repositories.Repository,
        unwrapSuccess: function (response) {
          return response._embedded.package_repositories;
        }
      }).then(Repositories);
    };

    Repositories.filterByPluginId = function (pluginId) {
      return _.filter(Repositories(), function (repository) {
        return _.isEqual(repository.pluginMetadata().id(), pluginId);
      });
    };

    Repositories.findById = function (id) {
      var repository = _.find(Repositories(), function (repository) {
        return _.isEqual(repository.id(), id);
      });

      if (_.isNil(repository)) {
        return null;
      }

      var config = function (xhr) {
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.setRequestHeader("Accept", "application/vnd.go.cd.v1+json");
        xhr.setRequestHeader( 'Cache-Control', "no-cache");
      };

      var extract = function (xhr) {
        Repositories.repoIdToEtag[repository.id()] = xhr.getResponseHeader('ETag');
        return xhr.responseText;
      };

      return m.request({
        method:     'GET',
        url:        Routes.apiv1AdminRepositoryPath({repo_id: repository.id()}),  //eslint-disable-line camelcase
        config:     config,
        extract:    extract,
        type:       Repositories.Repository
      });
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