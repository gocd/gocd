/*
 * Copyright 2015 ThoughtWorks, Inc.
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

define(['mithril', 'lodash', 'string-plus', './model_mixins'], function (m, _, s, Mixins) {

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
  };

  Mixins.fromJSONCollection({
    parentType: Tasks,
    childType:  Tasks.Task,
    via:        'addTask'
  });

  Tasks.Task.Ant = function (data) {
    Tasks.Task.call(this, "ant");
    this.target     = m.prop(s.defaultToIfBlank(data.target, ''));
    this.workingDir = m.prop(s.defaultToIfBlank(data.workingDir, ''));
    this.buildFile  = m.prop(s.defaultToIfBlank(data.buildFile, ''));

    this._attributesToJSON = function () {
      return {
        target:     this.target,
        workingDir: this.workingDir,
        buildFile:  this.buildFile
      }
    }
  };

  Tasks.Task.Ant.fromJSON = function (data) {
    return new Tasks.Task.Ant({
      target:     data.target,
      workingDir: data.working_dir,
      buildFile:  data.build_file
    });
  };

  Tasks.Task.NAnt = function (data) {
    Tasks.Task.call(this, "nant");
    this.target            = m.prop(s.defaultToIfBlank(data.target, ''));
    this.workingDir        = m.prop(s.defaultToIfBlank(data.workingDir, ''));
    this.buildFile         = m.prop(s.defaultToIfBlank(data.buildFile, ''));
    this.nantHome          = m.prop(s.defaultToIfBlank(data.nantHome, ''));
    this._attributesToJSON = function () {
      return {
        target:     this.target,
        workingDir: this.workingDir,
        buildFile:  this.buildFile,
        nantHome:   this.nantHome
      }
    }

  };

  Tasks.Task.NAnt.fromJSON = function (data) {
    return new Tasks.Task.NAnt({
      target:     data.target,
      workingDir: data.working_dir,
      buildFile:  data.build_file,
      nantHome:   data.nant_home
    });
  };

  Tasks.Task.Exec = function (data) {
    Tasks.Task.call(this, "exec");
    this.command    = m.prop(s.defaultToIfBlank(data.command, ''));
    this.args       = m.prop(s.defaultToIfBlank(data.args, ''));
    this.workingDir = m.prop(s.defaultToIfBlank(data.workingDir, ''));

    this._attributesToJSON = function () {
      return {
        command:    this.command,
        args:       this.args,
        workingDir: this.workingDir
      }
    }
  };

  Tasks.Task.Exec.fromJSON = function (data) {
    return new Tasks.Task.Exec({
      command:    data.command,
      args:       data.args,
      workingDir: data.working_dir
    });
  };

  Tasks.Task.Rake = function (data) {
    Tasks.Task.call(this, "rake");
    this.target     = m.prop(s.defaultToIfBlank(data.target, ''));
    this.workingDir = m.prop(s.defaultToIfBlank(data.workingDir, ''));
    this.buildFile  = m.prop(s.defaultToIfBlank(data.buildFile, ''));

    this._attributesToJSON = function () {
      return {
        target:     this.target,
        workingDir: this.workingDir,
        buildFile:  this.buildFile
      }
    }
  };

  Tasks.Task.Rake.fromJSON = function (data) {
    return new Tasks.Task.Rake({
      target:     data.target,
      workingDir: data.working_dir,
      buildFile:  data.build_file
    });
  };

  Tasks.Task.FetchArtifact = function (data) {
    Tasks.Task.call(this, "fetchartifact");
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
    }
  };

  Tasks.Task.FetchArtifact.Source = function (data) {
    this.type     = m.prop(data.type);
    this.location = m.prop(data.location);
  };

  Tasks.Task.FetchArtifact.fromJSON = function (data) {
    var source = new Tasks.Task.FetchArtifact.Source(_.pick(data.source, ['type', 'location']));

    return new Tasks.Task.FetchArtifact({
      pipeline: data.pipeline,
      stage:    data.stage,
      job:      data.job,
      source:   source
    });
  };

  Tasks.Task.PluginTask = function (data) {
    Tasks.Task.call(this, "plugin");

    this.pluginId      = m.prop(s.defaultToIfBlank(data.pluginId, ''));
    this.version       = m.prop(s.defaultToIfBlank(data.version, ''));
    this.configuration = s.overrideToJSON(m.prop(s.defaultToIfBlank(data.configuration, new Tasks.Task.PluginTask.Configurations())));

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
      return Tasks.Types[data.type].type.fromJSON(data.attributes || {});
    } else {
      return Tasks.Task.PluginTask.fromJSON(data.attributes || {});
    }
  };

  return Tasks;
});
