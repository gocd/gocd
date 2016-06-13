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

define(['mithril', 'lodash', 'string-plus', './model_mixins', './argument', './run_if_conditions'], function (m, _, s, Mixins, Argument, RunIfConditions) {

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

  Tasks.taskByType  = function (type) {
    if (Tasks.isBuiltInTaskType(type)) {
      return new Tasks.Types[type].type({})
    } else {
      return new Tasks.Task.PluginTask({
        type:     type,
        pluginId: type,
        version:  PluggableTasks.Types[type].version
      });
    }
  };

  Tasks.Task = function (type, data) {
    this.constructor.modelType = 'task';
    Mixins.HasUUID.call(this);

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
    }
  };

  Mixins.fromJSONCollection({
    parentType: Tasks,
    childType:  Tasks.Task,
    via:        'addTask'
  });

  Tasks.Task.Ant = function (data) {
    Tasks.Task.call(this, "ant");
    this.target           = m.prop(s.defaultToIfBlank(data.target, ''));
    this.workingDirectory = m.prop(s.defaultToIfBlank(data.workingDirectory, ''));
    this.buildFile        = m.prop(s.defaultToIfBlank(data.buildFile, ''));
    this.runIf            = m.prop(RunIfConditions.create(data.runIf));
    this.onCancelTask     = Tasks.Task.onCancelTask(data.onCancelTask);

    this._attributesToJSON = function () {
      return {
        target:           this.target,
        workingDirectory: this.workingDirectory,
        buildFile:        this.buildFile,
        run_if:           this.runIf().data(),
        on_cancel:        this.onCancelTaskToJSON()
      }
    };

    this.toString = function() {
      return _.join([this.target(), this.buildFile()], ' ');
    };

    this.isEmpty = function() {
      return _.isEmpty(_.compact([this.target(), this.buildFile()]));
    };
  };

  Tasks.Task.Ant.fromJSON = function (data) {
    return new Tasks.Task.Ant({
      target:           data.target,
      workingDirectory: data.working_directory,
      buildFile:        data.build_file,
      runIf:            data.run_if,
      onCancelTask:     data.on_cancel
    });
  };

  Tasks.Task.NAnt = function (data) {
    Tasks.Task.call(this, "nant");
    this.target            = m.prop(s.defaultToIfBlank(data.target, ''));
    this.workingDirectory  = m.prop(s.defaultToIfBlank(data.workingDirectory, ''));
    this.buildFile         = m.prop(s.defaultToIfBlank(data.buildFile, ''));
    this.nantPath          = m.prop(s.defaultToIfBlank(data.nantPath, ''));
    this.runIf             = m.prop(RunIfConditions.create(data.runIf));
    this.onCancelTask      = Tasks.Task.onCancelTask(data.onCancelTask);

    this._attributesToJSON = function () {
      return {
        target:           this.target,
        workingDirectory: this.workingDirectory,
        buildFile:        this.buildFile,
        nantPath:         this.nantPath,
        run_if:           this.runIf().data(),
        on_cancel:        this.onCancelTaskToJSON()
      }
    };

    this.toString = function() {
      return _.join([this.target(), this.buildFile()], ' ');
    };

    this.isEmpty = function() {
      return _.isEmpty(_.compact([this.target(), this.buildFile()]));
    };
  };

  Tasks.Task.NAnt.fromJSON = function (data) {
    return new Tasks.Task.NAnt({
      target:           data.target,
      workingDirectory: data.working_directory,
      buildFile:        data.build_file,
      nantPath:         data.nant_path,
      runIf:            data.run_if,
      onCancelTask:     data.on_cancel
    });
  };

  Tasks.Task.Exec = function (data) {
    Tasks.Task.call(this, "exec");
    this.command          = m.prop(s.defaultToIfBlank(data.command, ''));
    this.args             = m.prop(Argument.create(data.args, data.arguments));
    this.workingDirectory = m.prop(s.defaultToIfBlank(data.workingDirectory, ''));
    this.runIf            = m.prop(RunIfConditions.create(data.runIf));
    this.onCancelTask     = Tasks.Task.onCancelTask(data.onCancelTask);

    this._attributesToJSON = function () {
      return _.assign({
        command:          this.command,
        workingDirectory: this.workingDirectory,
        run_if:           this.runIf().data(),
        on_cancel:        this.onCancelTaskToJSON()
      }, this.args().toJSON());
    };

    this.toString = function () {
      return _.join([this.command(), this.args().toString()], ' ');
    };

    this.isEmpty = function() {
      return _.isEmpty(_.compact([this.command(), this.args().toString()]));
    };
  };

  Tasks.Task.Exec.fromJSON = function (data) {
    return new Tasks.Task.Exec({
      command:          data.command,
      args:             data.args,
      arguments:        data.arguments,
      workingDirectory: data.working_directory,
      runIf:            data.run_if,
      onCancelTask:     data.on_cancel
    });
  };

  Tasks.Task.Rake = function (data) {
    Tasks.Task.call(this, "rake");
    this.target           = m.prop(s.defaultToIfBlank(data.target, ''));
    this.workingDirectory = m.prop(s.defaultToIfBlank(data.workingDirectory, ''));
    this.buildFile        = m.prop(s.defaultToIfBlank(data.buildFile, ''));
    this.runIf            = m.prop(RunIfConditions.create(data.runIf));
    this.onCancelTask     = Tasks.Task.onCancelTask(data.onCancelTask);

    this._attributesToJSON = function () {
      return {
        target:           this.target,
        workingDirectory: this.workingDirectory,
        buildFile:        this.buildFile,
        run_if:           this.runIf().data(),
        on_cancel:        this.onCancelTaskToJSON()
      }
    };

    this.toString = function() {
      return _.join([this.target(), this.buildFile()], ' ');
    };

    this.isEmpty = function() {
      return _.isEmpty(_.compact([this.target(), this.buildFile()]));
    };
  };

  Tasks.Task.Rake.fromJSON = function (data) {
    return new Tasks.Task.Rake({
      target:           data.target,
      workingDirectory: data.working_directory,
      buildFile:        data.build_file,
      runIf:            data.run_if,
      onCancelTask:     data.on_cancel
    });
  };

  Tasks.Task.FetchArtifact = function (data) {
    Tasks.Task.call(this, "fetch");
    this.pipeline      = m.prop(s.defaultToIfBlank(data.pipeline, ''));
    this.stage         = m.prop(s.defaultToIfBlank(data.stage, ''));
    this.job           = m.prop(s.defaultToIfBlank(data.job, ''));
    this.source        = m.prop(s.defaultToIfBlank(data.source, ''));
    this.isSourceAFile = m.prop(s.defaultToIfBlank(data.isSourceAFile, false));
    this.runIf         = m.prop(RunIfConditions.create(data.runIf));
    this.onCancelTask  = Tasks.Task.onCancelTask(data.onCancelTask);

    this._attributesToJSON = function () {
      return {
        pipeline:          this.pipeline,
        stage:             this.stage,
        job:               this.job,
        source:            this.source,
        is_source_a_file:  this.isSourceAFile,
        run_if:            this.runIf().data(),
        on_cancel:         this.onCancelTaskToJSON()
      }
    };

    this.toString = function() {
      return _.join([this.pipeline(), this.stage(), this.job()], ' ');
    };

    this.isEmpty = function() {
      return _.isEmpty(_.compact([this.pipeline(), this.stage(), this.job()]));
    };
  };


  Tasks.Task.FetchArtifact.fromJSON = function (data) {
    return new Tasks.Task.FetchArtifact({
      pipeline:      data.pipeline,
      stage:         data.stage,
      job:           data.job,
      source:        data.source,
      isSourceAFile: data.is_source_a_file,
      runIf:         data.run_if,
      onCancelTask:  data.on_cancel
    });
  };

  Tasks.Task.PluginTask = function (data) {
    Tasks.Task.call(this, "plugin");

    this.pluginId      = m.prop(s.defaultToIfBlank(data.pluginId, ''));
    this.version       = m.prop(s.defaultToIfBlank(data.version, ''));
    this.configuration = s.collectionToJSON(m.prop(s.defaultToIfBlank(data.configuration, new Tasks.Task.PluginTask.Configurations())));
    this.runIf         = m.prop(RunIfConditions.create(data.runIf));
    this.onCancelTask  = Tasks.Task.onCancelTask(data.onCancelTask);

    this._attributesToJSON = function () {
      return {
        pluginId:      this.pluginId,
        version:       this.version,
        configuration: this.configuration,
        run_if:        this.runIf().data(),
        on_cancel:     this.onCancelTaskToJSON()
      }
    }
  };

  Tasks.Task.PluginTask.fromJSON = function (data) {
    return new Tasks.Task.PluginTask({
      pluginId:      data.plugin_id,
      version:       data.version,
      configuration: Tasks.Task.PluginTask.Configurations.fromJSON(data.configuration),
      runIf:         data.run_if,
      onCancelTask:  data.on_cancel
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
    fetch:         {type: Tasks.Task.FetchArtifact, description: "Fetch Artifact"}
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
      return Tasks.Types[data.type].type.fromJSON(data.attributes || {});
    } else {
      return Tasks.Task.PluginTask.fromJSON(data.attributes || {});
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
