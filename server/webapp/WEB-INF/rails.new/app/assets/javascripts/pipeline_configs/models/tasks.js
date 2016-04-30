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

define(['mithril', 'lodash', 'string-plus', './model_mixins', './argument', './errors'], function (m, _, s, Mixins, Argument, Errors) {

  var Tasks = function (data) {
    Mixins.HasMany.call(this, {factory: Tasks.createByType, as: 'Task', collection: data});
  };

  Tasks.createByType = function (data) {
    if (Tasks.isBuiltInTaskType(data.type)) {
      return new Tasks.Types[data.type].type({});
    } else {
      return new Tasks.Task.PluginTask(data);
    }
  };

  Tasks.Task = function (type, data) {
    this.constructor.modelType = 'task';
    Mixins.HasUUID.call(this);

    this.type = m.prop(type);
    this.parent = Mixins.GetterSetter();
    this.errors = m.prop(s.defaultToIfBlank(data.errors, new Errors()));

    var self = this;

    this.toJSON = function () {
      return {
        type:       self.type(),
        attributes: self._attributesToJSON(),
        errors:     self.errors().errors()

      };
    };

    this._attributesToJSON = function () {
      throw new Error("Subclass responsibility!");
    };
  };

  Mixins.fromJSONCollection({
    parentType: Tasks,
    childType:  Tasks.Task,
    via:        'addTask'
  });

  Tasks.Task.Ant = function (data) {
    Tasks.Task.call(this, "ant", data);
    this.target           = m.prop(s.defaultToIfBlank(data.target, ''));
    this.workingDirectory = m.prop(s.defaultToIfBlank(data.workingDirectory, ''));
    this.buildFile        = m.prop(s.defaultToIfBlank(data.buildFile, ''));

    this._attributesToJSON = function () {
      return {
        target:           this.target,
        workingDirectory: this.workingDirectory,
        buildFile:        this.buildFile
      }
    };

    this.toString = function () {
      return _.join([this.target(), this.buildFile()], ' ');
    };

    this.isEmpty = function () {
      return _.isEmpty(_.compact([this.target(), this.buildFile()]));
    };
  };

  Tasks.Task.Ant.fromJSON = function (data) {
    var attributes = data.attributes;
    return new Tasks.Task.Ant({
      target:           attributes.target,
      workingDirectory: attributes.working_directory,
      buildFile:        attributes.build_file,
      errors:           Errors.fromJson(data)
    });
  };

  Tasks.Task.NAnt = function (data) {
    Tasks.Task.call(this, "nant", data);
    this.target           = m.prop(s.defaultToIfBlank(data.target, ''));
    this.workingDirectory = m.prop(s.defaultToIfBlank(data.workingDirectory, ''));
    this.buildFile        = m.prop(s.defaultToIfBlank(data.buildFile, ''));
    this.nantPath         = m.prop(s.defaultToIfBlank(data.nantPath, ''));

    this._attributesToJSON = function () {
      return {
        target:           this.target,
        workingDirectory: this.workingDirectory,
        buildFile:        this.buildFile,
        nantPath:         this.nantPath
      }
    };

    this.toString = function () {
      return _.join([this.target(), this.buildFile()], ' ');
    };

    this.isEmpty = function () {
      return _.isEmpty(_.compact([this.target(), this.buildFile()]));
    };
  };

  Tasks.Task.NAnt.fromJSON = function (data) {
    var attributes = data.attributes;
    return new Tasks.Task.NAnt({
      target:           attributes.target,
      workingDirectory: attributes.working_directory,
      buildFile:        attributes.build_file,
      nantPath:         attributes.nant_path,
      errors:           Errors.fromJson(data)
    });
  };

  Tasks.Task.Exec = function (data) {
    Tasks.Task.call(this, "exec", data);
    this.command          = m.prop(s.defaultToIfBlank(data.command, ''));
    this.args             = m.prop(Argument.create(data.args, data.arguments));
    this.workingDirectory = m.prop(s.defaultToIfBlank(data.workingDirectory, ''));

    this._attributesToJSON = function () {
      return _.assign({
        command:          this.command,
        workingDirectory: this.workingDirectory,
      }, this.args().toJSON());
    };

    this.toString = function () {
      return _.join([this.command(), this.args().toString()], ' ');
    };

    this.isEmpty = function () {
      return _.isEmpty(_.compact([this.command(), this.args().toString()]));
    };
  };

  Tasks.Task.Exec.fromJSON = function (data) {
    var attributes = data.attributes;
    return new Tasks.Task.Exec({
      command:          attributes.command,
      args:             attributes.args,
      arguments:        attributes.arguments,
      workingDirectory: attributes.working_directory,
      errors:           Errors.fromJson(data)
    });
  };

  Tasks.Task.Rake = function (data) {
    Tasks.Task.call(this, "rake", data);
    this.target           = m.prop(s.defaultToIfBlank(data.target, ''));
    this.workingDirectory = m.prop(s.defaultToIfBlank(data.workingDirectory, ''));
    this.buildFile        = m.prop(s.defaultToIfBlank(data.buildFile, ''));

    this._attributesToJSON = function () {
      return {
        target:           this.target,
        workingDirectory: this.workingDirectory,
        buildFile:        this.buildFile
      }
    };

    this.toString = function () {
      return _.join([this.target(), this.buildFile()], ' ');
    };

    this.isEmpty = function () {
      return _.isEmpty(_.compact([this.target(), this.buildFile()]));
    };
  };

  Tasks.Task.Rake.fromJSON = function (data) {
    var attributes = data.attributes;
    return new Tasks.Task.Rake({
      target:           attributes.target,
      workingDirectory: attributes.working_directory,
      buildFile:        attributes.build_file,
      errors:           Errors.fromJson(data)
    });
  };

  Tasks.Task.FetchArtifact = function (data) {
    Tasks.Task.call(this, "fetchartifact", data);
    this.pipeline = m.prop(s.defaultToIfBlank(data.pipeline, ''));
    this.stage    = m.prop(s.defaultToIfBlank(data.stage, ''));
    this.job      = m.prop(s.defaultToIfBlank(data.job, ''));
    this.source   = m.prop(s.defaultToIfBlank(data.source, new Tasks.Task.FetchArtifact.Source({})));

    this._attributesToJSON = function () {
      return {
        pipeline: this.pipeline,
        stage:    this.stage,
        job:      this.job,
        source:   this.source
      }
    };

    this.toString = function() {
      return _.join([this.pipeline(), this.stage(), this.job()], ' ');
    };

    this.isEmpty = function() {
      return _.isEmpty(_.compact([this.pipeline(), this.stage(), this.job()]));
    };
  };

  Tasks.Task.FetchArtifact.Source = function (data) {
    this.type     = m.prop(data.type);
    this.location = m.prop(data.location);
  };

  Tasks.Task.FetchArtifact.fromJSON = function (data) {
    var attributes = data.attributes;
    var source = new Tasks.Task.FetchArtifact.Source(_.pick(attributes.source, ['type', 'location']));
    return new Tasks.Task.FetchArtifact({
      pipeline: attributes.pipeline,
      stage:    attributes.stage,
      job:      attributes.job,
      source:   source,
      errors:   Errors.fromJson(data)
    });
  };

  Tasks.Task.PluginTask = function (data) {
    Tasks.Task.call(this, "plugin", data);

    this.pluginId      = m.prop(s.defaultToIfBlank(data.pluginId, ''));
    this.version       = m.prop(s.defaultToIfBlank(data.version, ''));
    this.configuration = s.collectionToJSON(m.prop(s.defaultToIfBlank(data.configuration, new Tasks.Task.PluginTask.Configurations())));

    this._attributesToJSON = function () {
      return {
        pluginId:      this.pluginId,
        version:       this.version,
        configuration: this.configuration
      }
    }
  };

  Tasks.Task.PluginTask.fromJSON = function (data) {
    return new Tasks.Task.PluginTask({
      pluginId:      data.plugin_id,
      version:       data.version,
      configuration: Tasks.Task.PluginTask.Configurations.fromJSON(data.configuration)
    });
  };

  Tasks.Task.PluginTask.Configurations = function (data) {
    Mixins.HasMany.call(this, {
      factory:    Tasks.Task.PluginTask.Configurations.create,
      as:         'Configuration',
      collection: data
    });

    function configForKey(key) {
      return this.findConfiguration(function (config) {
        return config.name() === key;
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
        this.createConfiguration({name: key, value: value});
      } else {
        existingConfig.value(value);
      }
    };
  };

  Tasks.Task.PluginTask.Configurations.create = function (data) {
    return new Tasks.Task.PluginTask.Configurations.Configuration(data);
  };

  Tasks.Task.PluginTask.Configurations.Configuration = function (data) {
    this.parent = Mixins.GetterSetter();

    this.name  = m.prop(s.defaultToIfBlank(data.name, ''));
    this.value = m.prop(s.defaultToIfBlank(data.value, ''));
  };

  Tasks.Task.PluginTask.Configurations.Configuration.fromJSON = function (data) {
    return new Tasks.Task.PluginTask.Configurations.Configuration(_.pick(data, ['name', 'value']));
  };

  Mixins.fromJSONCollection({
    parentType: Tasks.Task.PluginTask.Configurations,
    childType:  Tasks.Task.PluginTask.Configurations.Configuration,
    via:        'addConfiguration'
  });

  Tasks.BuiltInTypes = {
    exec:          {type: Tasks.Task.Exec, description: "Custom Command"},
    ant:           {type: Tasks.Task.Ant, description: "Ant"},
    nant:          {type: Tasks.Task.NAnt, description: "NAnt"},
    rake:          {type: Tasks.Task.Rake, description: "Rake"},
    fetchartifact: {type: Tasks.Task.FetchArtifact, description: "Fetch Artifact"}
  };

  Tasks.Types = _.assign({}, Tasks.BuiltInTypes);

  Tasks.findTypeFromDescription = function (description) {
    var matchedKey;
    _.each(Tasks.Types, function (value, key) {
      if (value.description === description) {
        matchedKey = key;
      }
    });
    return matchedKey;
  };

  Tasks.isBuiltInTaskType = function (type) {
    return !!Tasks.BuiltInTypes[type];
  };

  Tasks.Task.fromJSON = function (data) {
    if (Tasks.isBuiltInTaskType(data.type)) {
      return Tasks.Types[data.type].type.fromJSON(data || {});
    } else {
      return Tasks.Task.PluginTask.fromJSON(data.attributes || {});
    }
  };

  return Tasks;
});
