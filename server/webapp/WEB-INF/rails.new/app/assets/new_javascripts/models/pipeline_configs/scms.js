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


define(['mithril', 'lodash', 'string-plus', 'models/model_mixins', 'helpers/mrequest', 'models/errors', 'models/pipeline_configs/encrypted_value',
  'models/validatable_mixin', 'js-routes'],
  function (m, _, s, Mixins, mrequest, Errors, EncryptedValue, Validatable, Routes) {
    var SCMs = m.prop([]);
    SCMs.scmIdToEtag = {};

    function plainOrCipherValue(data) {
      if (data.encrypted_value) {
        return new EncryptedValue({cipherText: s.defaultToIfBlank(data.encrypted_value, '')});
      } else {
        return new EncryptedValue({clearText: s.defaultToIfBlank(data.value, '')});
      }
    }

    SCMs.SCM = function (data) {
      Validatable.call(this, data);

      this.init = function (data) {
        this.id             = m.prop(s.defaultToIfBlank(data.id));
        this.name           = m.prop(s.defaultToIfBlank(data.name, ''));
        this.autoUpdate     = m.prop(s.defaultToIfBlank(data.auto_update));
        this.pluginMetadata = m.prop(new SCMs.SCM.PluginMetadata(data.plugin_metadata || {}));
        this.configuration  = s.collectionToJSON(m.prop(SCMs.SCM.Configurations.fromJSON(data.configuration || {})));
        this.errors         = m.prop(new Errors(data.errors));
      };

      this.init(data);

      this.reInitialize = function (data) {
        this.init(data);
      };

      this.clone = function () {
        return new SCMs.SCM(JSON.parse(JSON.stringify(this)));
      };

      this.toJSON = function() {
        /* eslint-disable camelcase */
        return {
          id:              this.id(),
          name:            this.name(),
          auto_update:     this.autoUpdate(),
          plugin_metadata: this.pluginMetadata().toJSON(),
          configuration:   this.configuration
        };
        /* eslint-enable camelcase */
      };

      this.update = function() {
        var self = this;

        var config =  function (xhr) {
          xhr.setRequestHeader("Content-Type", "application/json");
          xhr.setRequestHeader("Accept", "application/vnd.go.cd.v1+json");
          xhr.setRequestHeader("If-Match", SCMs.scmIdToEtag[self.id()]);
        };

        var extract = function(xhr) {
          if(xhr.status === 200) {
            SCMs.scmIdToEtag[self.id()] = xhr.getResponseHeader('ETag');
          }
          return xhr.responseText;
        };

        return m.request({
          method:     'PATCH',
          url:        Routes.apiv1AdminScmPath({material_name: this.name()}), //eslint-disable-line camelcase
          background: false,
          config:     config,
          extract:    extract,
          data:       this,
          type:       SCMs.SCM
        });
      };

      this.create = function() {
        var extract = function(xhr) {
          if(xhr.status === 200) {
            SCMs.scmIdToEtag[JSON.parse(xhr.responseText).id] = xhr.getResponseHeader('ETag');
          }
          return xhr.responseText;
        };

        return m.request({
          method:     'POST',
          url:        Routes.apiv1AdminScmsPath(),
          background: false,
          config:     mrequest.xhrConfig.v1,
          extract:    extract,
          data:       this,
          type:       SCMs.SCM
        });
      };
    };

    SCMs.SCM.PluginMetadata = function (data) {
      this.id      = m.prop(s.defaultToIfBlank(data.id, ''));
      this.version = m.prop(s.defaultToIfBlank(data.version, ''));

      this.toJSON = function () {
        return {
          id:      this.id(),
          version: this.version()
        };
      };
    };

    SCMs.SCM.Configurations = function (data) {
      Mixins.HasMany.call(this, {
        factory:    SCMs.SCM.Configurations.create,
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

    SCMs.SCM.Configurations.create = function (data) {
      return new SCMs.SCM.Configurations.Configuration(data);
    };

    SCMs.SCM.Configurations.fromJSON = function (data) {
      var configurations = _.map(data, function (d) {
        return new SCMs.SCM.Configurations.Configuration(d);
      });

      return new SCMs.SCM.Configurations(configurations);
    };

    SCMs.SCM.Configurations.Configuration = function (data) {
      this.parent = Mixins.GetterSetter();
      this.key    = m.prop(s.defaultToIfBlank(data.key, ''));
      var _value  = m.prop(plainOrCipherValue(data));

      Mixins.HasEncryptedAttribute.call(this, {attribute: _value, name: 'value'});

      this.toJSON = function () {
        var valueHash = this.isPlainValue() ? {value: this.value()} : {encrypted_value: this.value()};  //eslint-disable-line camelcase

        return _.merge({key: this.key()}, valueHash);
      };
    };

    SCMs.init = function () {
      return m.request({
        method:        'GET',
        url:           Routes.apiv1AdminScmsPath(),
        config:        mrequest.xhrConfig.v1,
        type:          SCMs.SCM,
        unwrapSuccess: function (response) {
          return response._embedded.scms;
        }
      }).then(SCMs);
    };

    SCMs.filterByPluginId = function (pluginId) {
      return _.filter(SCMs(), function (scm) {
        return _.isEqual(scm.pluginMetadata().id(), pluginId);
      });
    };

    SCMs.findById = function (id) {
      var scm = _.find(SCMs(), function (scm) {
        return _.isEqual(scm.id(), id);
      });

      if(_.isNil(scm)) {
        return null;
      }

      var extract = function(xhr) {
        SCMs.scmIdToEtag[scm.id()] = xhr.getResponseHeader('ETag');
        return xhr.responseText;
      };

      return m.request({
        method:     'GET',
        url:        Routes.apiv1AdminScmPath({material_name: scm.name()}),  //eslint-disable-line camelcase
        background: false,
        config:     mrequest.xhrConfig.v1,
        extract:    extract,
        type:       SCMs.SCM
      });
    };

    SCMs.vm = function () {
      this.saveState = m.prop('');
      var errors    = [];

      this.startUpdating = function () {
        errors = [];
        this.saveState('in-progress disabled');
      };

      this.saveFailed = function (data) {
        errors.push(data.message);

        if(data.data) {
          if(data.data.configuration) {
            errors = _.concat(errors, _.flattenDeep(_.map(data.data.configuration, function(conf) {return _.values(conf.errors);})));
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

    return SCMs;
  });