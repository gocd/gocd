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

define(['mithril', 'lodash', 'string-plus', 'models/model_mixins', 'models/pipeline_configs/argument', 'models/pipeline_configs/run_if_conditions',
    'models/validatable_mixin'], function (m, _, s, Mixins, Argument, RunIfConditions, Validatable) {

  var Tasks = function (data) {
    Mixins.HasMany.call(this, {factory: Tasks.createByType, as: 'Task', collection: data});
  };

  Tasks.createByType = function (data) {
    if (Tasks.isBuiltInTaskType(data.type)) {
      return new Tasks.Types[data.type].type({});
    } else {
      return new Tasks.Task.PluginTask.fromPluginInfo(data.pluginInfo);
    }
  };

  Tasks.Task = function (type, data) {
    this.constructor.modelType = 'task';
    Mixins.HasUUID.call(this);
    Validatable.call(this, data);

    this.type   = m.prop(type);
    this.parent = Mixins.GetterSetter();

    var self = this;

    this.toJSON = function () {
      return {
        type:       self.type(),
        attributes: self._attributesToJSON()
      };
    };

    this._attributesToJSON = function () {
      throw new Error("Subclass responsibility!");
    };

    this.hasOnCancelTask = function () {
      return ! _.isNil(this.onCancelTask);
    };

    this.onCancelTaskToJSON = function () {
      return this.onCancelTask ? this.onCancelTask.toJSON() : null;
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
    this.runIf            = m.prop(RunIfConditions.create(data.runIf));
    this.onCancelTask     = Tasks.Task.onCancelTask(data.onCancelTask);

    this._attributesToJSON = function () {
      /* eslint-disable camelcase */
      return {
        target:           this.target,
        workingDirectory: this.workingDirectory,
        buildFile:        this.buildFile,
        run_if:           this.runIf().data(),
        on_cancel:        this.onCancelTaskToJSON()
      };
      /* eslint-enable camelcase */
    };

    this.toString = function() {
      return _.join([this.target(), this.buildFile()], ' ');
    };

    this.summary = function () {
      return {
        buildFile:        this.buildFile(),
        target:           this.target(),
        workingDirectory: this.workingDirectory()
      };
    };

    this.isEmpty = function() {
      return _.isEmpty(_.compact([this.target(), this.buildFile()]));
    };
  };

  Tasks.Task.Ant.fromJSON = function (data) {
    var attr = data.attributes || {};
    return new Tasks.Task.Ant({
      target:           attr.target,
      workingDirectory: attr.working_directory,
      buildFile:        attr.build_file,
      runIf:            attr.run_if,
      onCancelTask:     attr.on_cancel,
      errors:           data.errors
    });
  };

  Tasks.Task.NAnt = function (data) {
    Tasks.Task.call(this, "nant", data);
    this.target            = m.prop(s.defaultToIfBlank(data.target, ''));
    this.workingDirectory  = m.prop(s.defaultToIfBlank(data.workingDirectory, ''));
    this.buildFile         = m.prop(s.defaultToIfBlank(data.buildFile, ''));
    this.nantPath          = m.prop(s.defaultToIfBlank(data.nantPath, ''));
    this.runIf             = m.prop(RunIfConditions.create(data.runIf));
    this.onCancelTask      = Tasks.Task.onCancelTask(data.onCancelTask);

    this._attributesToJSON = function () {
      /* eslint-disable camelcase */
      return {
        target:           this.target,
        workingDirectory: this.workingDirectory,
        buildFile:        this.buildFile,
        nantPath:         this.nantPath,
        run_if:           this.runIf().data(),
        on_cancel:        this.onCancelTaskToJSON()
      };
      /* eslint-enable camelcase */
    };

    this.toString = function() {
      return _.join([this.target(), this.buildFile()], ' ');
    };

    this.summary = function () {
      return {
        buildFile:        this.buildFile(),
        target:           this.target(),
        workingDirectory: this.workingDirectory(),
        nantPath:         this.nantPath()
      };
    };

    this.isEmpty = function() {
      return _.isEmpty(_.compact([this.target(), this.buildFile()]));
    };
  };

  Tasks.Task.NAnt.fromJSON = function (data) {
    var attr = data.attributes || {};
    return new Tasks.Task.NAnt({
      target:           attr.target,
      workingDirectory: attr.working_directory,
      buildFile:        attr.build_file,
      nantPath:         attr.nant_path,
      runIf:            attr.run_if,
      onCancelTask:     attr.on_cancel,
      errors:           data.errors
    });
  };

  Tasks.Task.Exec = function (data) {
    Tasks.Task.call(this, "exec", data);
    this.command          = m.prop(s.defaultToIfBlank(data.command, ''));
    this.args             = m.prop(Argument.create(data.args, data.arguments));
    this.workingDirectory = m.prop(s.defaultToIfBlank(data.workingDirectory, ''));
    this.runIf            = m.prop(RunIfConditions.create(data.runIf));
    this.onCancelTask     = Tasks.Task.onCancelTask(data.onCancelTask);

    this.validatePresenceOf('command');

    this._attributesToJSON = function () {
      /* eslint-disable camelcase */
      return _.assign({
        command:          this.command,
        workingDirectory: this.workingDirectory,
        run_if:           this.runIf().data(),
        on_cancel:        this.onCancelTaskToJSON()
      }, this.args().toJSON());
      /* eslint-enable camelcase */
    };

    this.toString = function () {
      return _.join([this.command(), this.args().toString()], ' ');
    };

    this.summary = function () {
      return {
        command: this.command(),
        args   : this.args().toString()
      };
    };

    this.isEmpty = function() {
      return _.isEmpty(_.compact([this.command(), this.args().toString()]));
    };
  };

  Tasks.Task.Exec.fromJSON = function (data) {
    var attr = data.attributes || {};
    return new Tasks.Task.Exec({
      command:          attr.command,
      args:             attr.args,
      arguments:        attr.arguments,
      workingDirectory: attr.working_directory,
      runIf:            attr.run_if,
      onCancelTask:     attr.on_cancel,
      errors:           data.errors
    });
  };

  Tasks.Task.Rake = function (data) {
    Tasks.Task.call(this, "rake", data);
    this.target           = m.prop(s.defaultToIfBlank(data.target, ''));
    this.workingDirectory = m.prop(s.defaultToIfBlank(data.workingDirectory, ''));
    this.buildFile        = m.prop(s.defaultToIfBlank(data.buildFile, ''));
    this.runIf            = m.prop(RunIfConditions.create(data.runIf));
    this.onCancelTask     = Tasks.Task.onCancelTask(data.onCancelTask);

    this._attributesToJSON = function () {
      /* eslint-disable camelcase */
      return {
        target:           this.target,
        workingDirectory: this.workingDirectory,
        buildFile:        this.buildFile,
        run_if:           this.runIf().data(),
        on_cancel:        this.onCancelTaskToJSON()
      };
      /* eslint-enable camelcase */
    };

    this.toString = function() {
      return _.join([this.target(), this.buildFile()], ' ');
    };

    this.summary = function () {
      return {
        buildFile:        this.buildFile(),
        target:           this.target(),
        workingDirectory: this.workingDirectory()
      };
    };

    this.isEmpty = function() {
      return _.isEmpty(_.compact([this.target(), this.buildFile()]));
    };
  };

  Tasks.Task.Rake.fromJSON = function (data) {
    var attr = data.attributes || {};
    return new Tasks.Task.Rake({
      target:           attr.target,
      workingDirectory: attr.working_directory,
      buildFile:        attr.build_file,
      runIf:            attr.run_if,
      onCancelTask:     attr.on_cancel,
      errors:           data.errors
    });
  };

  Tasks.Task.FetchArtifact = function (data) {
    Tasks.Task.call(this, "fetch", data);
    this.pipeline      = m.prop(s.defaultToIfBlank(data.pipeline, ''));
    this.stage         = m.prop(s.defaultToIfBlank(data.stage, ''));
    this.job           = m.prop(s.defaultToIfBlank(data.job, ''));
    this.source        = m.prop(s.defaultToIfBlank(data.source, ''));
    this.isSourceAFile = m.prop(s.defaultToIfBlank(data.isSourceAFile, false));
    this.destination   = m.prop(s.defaultToIfBlank(data.destination, ''));
    this.runIf         = m.prop(RunIfConditions.create(data.runIf));
    this.onCancelTask  = Tasks.Task.onCancelTask(data.onCancelTask);

    this.validatePresenceOf('stage');
    this.validatePresenceOf('job');
    this.validatePresenceOf('source');

    this._attributesToJSON = function () {
      /* eslint-disable camelcase */
      return {
        pipeline:          this.pipeline,
        stage:             this.stage,
        job:               this.job,
        source:            this.source,
        is_source_a_file:  this.isSourceAFile,
        destination:       this.destination,
        run_if:            this.runIf().data(),
        on_cancel:         this.onCancelTaskToJSON()
      };
      /* eslint-enable camelcase */
    };

    this.toString = function() {
      return _.join([this.pipeline(), this.stage(), this.job()], ' ');
    };

    this.summary = function () {
      return {
        pipeline:    this.pipeline(),
        stage:       this.stage(),
        job:         this.job(),
        source:      this.source(),
        destination: this.destination()
      };
    };

    this.isEmpty = function() {
      return _.isEmpty(_.compact([this.pipeline(), this.stage(), this.job()]));
    };
  };


  Tasks.Task.FetchArtifact.fromJSON = function (data) {
    var attr = data.attributes || {};
    return new Tasks.Task.FetchArtifact({
      pipeline:      attr.pipeline,
      stage:         attr.stage,
      job:           attr.job,
      source:        attr.source,
      isSourceAFile: attr.is_source_a_file,
      destination:   attr.destination,
      runIf:         attr.run_if,
      onCancelTask:  attr.on_cancel,
      errors:        data.errors
    });
  };

  Tasks.Task.PluginTask = function (data) {
    Tasks.Task.call(this, 'pluggable_task', data);

    this.pluginId      = m.prop(s.defaultToIfBlank(data.pluginId, ''));
    this.version       = m.prop(s.defaultToIfBlank(data.version, ''));
    this.configuration = s.collectionToJSON(m.prop(s.defaultToIfBlank(data.configuration, new Tasks.Task.PluginTask.Configurations())));
    this.runIf         = m.prop(RunIfConditions.create(data.runIf));
    this.onCancelTask  = Tasks.Task.onCancelTask(data.onCancelTask);

    this.isEmpty = function() {
      return this.configuration().isEmptyConfiguration();
    };

    this.toString = function() {
      return _.join(this.configuration().mapConfigurations(function(configuration) {
        return _.join([configuration.key(), ':', ' ', configuration.value()], '');
      }), ' ');
    };

    this.summary = function () {
      var data = {};

      this.configuration().mapConfigurations(function (conf) {
        data[_.capitalize(conf.key())] = conf.value();
      });

      return data;
    };

    this._attributesToJSON = function () {
      /* eslint-disable camelcase */
      return {
        plugin_configuration: {
          id:      this.pluginId,
          version: this.version
        },
        configuration: this.configuration,
        run_if:        this.runIf().data(),
        on_cancel:     this.onCancelTaskToJSON()
      };
      /* eslint-enable camelcase */
    };
  };

  Tasks.Task.PluginTask.fromJSON = function (data) {
    var attr = data.attributes || {};
    return new Tasks.Task.PluginTask({
      pluginId:      attr.plugin_configuration.id,
      version:       attr.plugin_configuration.version,
      configuration: Tasks.Task.PluginTask.Configurations.fromJSON(attr.configuration),
      runIf:         attr.run_if,
      onCancelTask:  attr.on_cancel,
      errors:        data.errors
    });
  };

  Tasks.Task.PluginTask.fromPluginInfo = function (pluginInfo) {
    return new Tasks.Task.PluginTask({
      pluginId:      pluginInfo.id(),
      version:       pluginInfo.version(),
      configuration: Tasks.Task.PluginTask.Configurations.fromJSON(pluginInfo.configurations())
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
        return config.key() === key;
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
        this.createConfiguration({key: key, value: value});
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

    this.key  = m.prop(s.defaultToIfBlank(data.key, ''));
    this.value = m.prop(s.defaultToIfBlank(data.value, ''));
  };

  Tasks.Task.PluginTask.Configurations.Configuration.fromJSON = function (data) {
    return new Tasks.Task.PluginTask.Configurations.Configuration(data);
  };

  Mixins.fromJSONCollection({
    parentType: Tasks.Task.PluginTask.Configurations,
    childType:  Tasks.Task.PluginTask.Configurations.Configuration,
    via:        'addConfiguration'
  });

  Tasks.BuiltInTypes = {
    exec:  {type: Tasks.Task.Exec, description: "Custom Command"},
    ant:   {type: Tasks.Task.Ant, description: "Ant"},
    nant:  {type: Tasks.Task.NAnt, description: "NAnt"},
    rake:  {type: Tasks.Task.Rake, description: "Rake"},
    fetch: {type: Tasks.Task.FetchArtifact, description: "Fetch Artifact"}
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
      return Tasks.Task.PluginTask.fromJSON(data || {});
    }
  };

  Tasks.Task.onCancelTask = function(data) {
    if(data) {
      return Tasks.Task.fromJSON(data);
    }
    return null;
  };

  return Tasks;
});
