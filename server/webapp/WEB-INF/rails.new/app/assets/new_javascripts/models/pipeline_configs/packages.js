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

define(['mithril', 'lodash', 'string-plus', 'models/model_mixins', 'helpers/mrequest', 'models/errors', 'models/pipeline_configs/encrypted_value', 'models/validatable_mixin', 'js-routes', 'models/pipeline_configs/repositories'],
  function (m, _, s, Mixins, mrequest, Errors, EncryptedValue, Validatable, Routes, Repositories) {

    var Packages             = m.prop([]);
    Packages.packageIdToEtag = {};

    function plainOrCipherValue(data) {
      if (data.encrypted_value) {
        return new EncryptedValue({cipherText: s.defaultToIfBlank(data.encrypted_value, '')});
      } else {
        return new EncryptedValue({clearText: s.defaultToIfBlank(data.value, '')});
      }
    }

    Packages.Package = function (data) {
      Validatable.call(this, data);

      this.init = function (data) {
        this.id            = m.prop(s.defaultToIfBlank(data.id, ''));
        this.name          = m.prop(s.defaultToIfBlank(data.name, ''));
        this.autoUpdate    = m.prop(s.defaultToIfBlank(data.auto_update, true));
        this.configuration = s.collectionToJSON(m.prop(Packages.Package.Configurations.fromJSON(data.configuration || {})));
        this.packageRepo   = m.prop(new Packages.Package.PackageRepository(data.package_repo || {}));
      };

      this.init(data);

      this.reInitialize = function (data) {
        this.init(data);
      };

      //this.clone = function () {
      //  return new Packages.Package(JSON.parse(JSON.stringify(this)));
      //};

      this.toJSON = function () {
        /* eslint-disable camelcase */
        return {
          id:              this.id(),
          name:            this.name(),
          auto_update:      this.autoUpdate(),
          package_repo:    this.packageRepo.toJSON(),
          configuration:   this.configuration
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

    Packages.Package.PackageRepository = function (data) {
      this.id   = m.prop(s.defaultToIfBlank(data.id, ''));
      this.name = m.prop(s.defaultToIfBlank(data.name, ''));

      this.toJSON = function () {
        return {
          id: this.id(),
          name: this.name()
        }
      }
    };

    Packages.init = function (repoId) {
      var repository = Repositories.findById(repoId);
      Packages(repository.packages());
    };

    Packages.findById = function (id) {
      var packageMaterial = _.find(Packages(), function (packageMaterial) {
        return _.isEqual(packageMaterial.id(), id);
      });

      if (_.isNil(packageMaterial)) {
        return null;
      }

      var extract = function (xhr) {
        Packages.packageIdToEtag[packageMaterial.id()] = xhr.getResponseHeader('ETag');
        return xhr.responseText;
      };

      return m.request({
        method:     'GET',
        url:        Routes.apiv1AdminPackagePath({package_id: packageMaterial.id()}),  //eslint-disable-line camelcase
        background: false,
        config:     mrequest.xhrConfig.v1,
        extract:    extract,
        type:       Packages.Package
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

    Packages.Package.Configurations = function (data) {
      Mixins.HasMany.call(this, {
        factory:    Packages.Package.Configurations.create,
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

    Packages.Package.Configurations.create = function (data) {
      return new Packages.Package.Configurations.Configuration(data);
    };

    Packages.Package.Configurations.fromJSON = function (data) {
      var configurations = _.map(data, function (d) {
        return new Packages.Package.Configurations.Configuration(d);
      });

      return new Packages.Package.Configurations(configurations);
    };

    Packages.Package.Configurations.Configuration = function (data) {
      this.parent = Mixins.GetterSetter();
      this.key    = m.prop(s.defaultToIfBlank(data.key, ''));
      var _value  = m.prop(plainOrCipherValue(data));

      Mixins.HasEncryptedAttribute.call(this, {attribute: _value, name: 'value'});

      this.toJSON = function () {
        var valueHash = this.isPlainValue() ? {value: this.value()} : {encrypted_value: this.value()};  //eslint-disable-line camelcase

        return _.merge({key: this.key()}, valueHash);
      };
    };

    return Packages;
  });