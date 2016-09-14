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

define(['mithril', 'lodash', 'string-plus', 'models/model_mixins', 'models/pipeline_configs/encrypted_value', 'models/pipeline_configs/scms',
  'models/validatable_mixin', 'js-routes'], function (m, _, s, Mixins, EncryptedValue, SCMs, Validatable, Routes) {

  function plainOrCipherValue(data) {
    if (data.encryptedPassword) {
      return new EncryptedValue({cipherText: s.defaultToIfBlank(data.encryptedPassword, '')});
    } else {
      return new EncryptedValue({clearText: s.defaultToIfBlank(data.password, '')});
    }
  }

  var Materials = function (data) {
    Mixins.HasMany.call(this, {factory: Materials.create, as: 'Material', collection: data, uniqueOn: 'name'});
    Mixins.HasUUID.call(this);
  };

  Materials.create = function (data) {
    return Materials.isBuiltInType(data.type) ? new Materials.Types[data.type].type(data)
                                              : new Materials.Material.PluggableMaterial(data);
  };

  Materials.Filter = function (data) {
    this.constructor.modelType = 'materialFilter';
    Mixins.HasUUID.call(this);
    Validatable.call(this, data);
    this.ignore = s.withNewJSONImpl(m.prop(s.defaultToIfBlank(data.ignore, '')), s.stringToArray);

    this.isBlank = function () {
      return s.isBlank(this.ignore());
    };

    this.toJSON = function () {
      if (this.isBlank()) {
        return {
          filter: null
        };
      } else {
        return {
          filter: {
            ignore: this.ignore.toJSON()
          }
        };
      }
    };
  };

  Materials.Filter.fromJSON = function (data) {
    if (!_.isEmpty(data)) {
      return new Materials.Filter({
        ignore: data.ignore
      });
    }
  };

  Materials.Material = function (type, hasFilter, data) {
    this.constructor.modelType = 'material';
    Mixins.HasUUID.call(this);
    Validatable.call(this, data);
    this.parent           = Mixins.GetterSetter();
    this.type             = m.prop(type);

    if (hasFilter) {
      this.filter = m.prop(s.defaultToIfBlank(data.filter, new Materials.Filter(s.defaultToIfBlank(data.filter, {}))));
    }

    this.validateUniquenessOf('name');

    this.toJSON = function () {
      var attrs = {};

      if (hasFilter) {
        _.merge(attrs, this.filter().toJSON());
      }

      return {
        type:       this.type(),
        attributes: _.merge(attrs, this._attributesToJSON())
      };
    };

    this.testConnection = function (pipelineName) {
      var self = this;

      var xhrConfig = function(xhr) {
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.setRequestHeader("Accept", "application/vnd.go.cd.v1+json");
      };

      var payload = function () {
        return _.merge(self.toJSON(), {pipeline_name: pipelineName()}); //eslint-disable-line camelcase
      };

      var stringfy = function (data) {
        return JSON.stringify(data, s.snakeCaser);
      };

      var onError = function (response) {
        return response.message ? response.message : 'There was an unknown error while checking connection';
      };

      return m.request({
        url:         Routes.apiv1AdminMaterialTestPath(),
        method:      'POST',
        config:      xhrConfig,
        data:        payload(),
        unwrapError: onError,
        serialize:   stringfy
      });
    };

    this._attributesToJSON = function () {
      throw new Error("Subclass responsibility!");
    };

    this._passwordHash = function() {
      if (this.isPlainPasswordValue() || this.isDirtyPasswordValue()) {
        return { password: this.passwordValue() };
      }
      return { encryptedPassword: this.passwordValue() };
    };
  };

  Mixins.fromJSONCollection({
    parentType: Materials,
    childType:  Materials.Material,
    via:        'addMaterial'
  });

  Materials.Material.SVN = function (data) {
    Materials.Material.call(this, "svn", true, data);
    this.name           = m.prop(s.defaultToIfBlank(data.name, ''));
    this.destination    = m.prop(s.defaultToIfBlank(data.destination, ''));
    this.url            = m.prop(s.defaultToIfBlank(data.url, ''));
    this.username       = m.prop(s.defaultToIfBlank(data.username, ''));
    var _password       = m.prop(plainOrCipherValue(data));
    this.checkExternals = m.prop(data.checkExternals);
    this.autoUpdate     = m.prop(s.defaultToIfBlank(data.autoUpdate, true));
    this.invertFilter = m.prop(s.defaultToIfBlank(data.invertFilter, false));
    Mixins.HasEncryptedAttribute.call(this, {attribute: _password, name: 'passwordValue'});

    this.validatePresenceOf('url');

    this._attributesToJSON = function () {
      /* eslint-disable camelcase */
      var attrs = {
        name:           this.name(),
        destination:    this.destination(),
        url:            this.url(),
        username:       this.username(),
        checkExternals: this.checkExternals(),
        auto_update:    this.autoUpdate(),
        invert_filter:  this.invertFilter()
      };
      /* eslint-enable camelcase */

      return _.merge(attrs, this._passwordHash());
    };
  };

  Materials.Material.SVN.fromJSON = function (data) {
    var attr = data.attributes || {};
    return new Materials.Material.SVN({
      url:               attr.url,
      username:          attr.username,
      password:          attr.password,
      encryptedPassword: attr.encrypted_password,
      checkExternals:    attr.check_externals,
      destination:       attr.destination,
      name:              attr.name,
      autoUpdate:        attr.auto_update,
      filter:            Materials.Filter.fromJSON(attr.filter),
      invertFilter:      attr.invert_filter,
      errors:            data.errors
    });
  };

  Materials.Material.Git = function (data) {
    Materials.Material.call(this, "git", true, data);
    this.name         = m.prop(s.defaultToIfBlank(data.name, ''));
    this.destination  = m.prop(s.defaultToIfBlank(data.destination, ''));
    this.url          = m.prop(s.defaultToIfBlank(data.url, ''));
    this.branch       = m.prop(s.defaultToIfBlank(data.branch, 'master'));
    this.shallowClone = m.prop(data.shallowClone);
    this.autoUpdate   = m.prop(s.defaultToIfBlank(data.autoUpdate, true));
    this.invertFilter = m.prop(s.defaultToIfBlank(data.invertFilter, false));

    this.validatePresenceOf('url');

    this._attributesToJSON = function () {
      /* eslint-disable camelcase */
      return {
        name:          this.name(),
        destination:   this.destination(),
        url:           this.url(),
        branch:        this.branch(),
        shallow_clone: this.shallowClone(),
        auto_update:   this.autoUpdate(),
        invert_filter:  this.invertFilter()
      };
      /* eslint-enable camelcase */
    };
  };

  Materials.Material.Git.fromJSON = function (data) {
    var attr = data.attributes || {};
    return new Materials.Material.Git({
      url:          attr.url,
      branch:       attr.branch,
      destination:  attr.destination,
      name:         attr.name,
      autoUpdate:   attr.auto_update,
      filter:       Materials.Filter.fromJSON(attr.filter),
      shallowClone: attr.shallow_clone,
      invertFilter: attr.invert_filter,
      errors:       data.errors
    });
  };

  Materials.Material.Mercurial = function (data) {
    Materials.Material.call(this, "hg", true, data);
    this.name         = m.prop(s.defaultToIfBlank(data.name, ''));
    this.destination  = m.prop(s.defaultToIfBlank(data.destination, ''));
    this.url          = m.prop(s.defaultToIfBlank(data.url, ''));
    this.branch       = m.prop(s.defaultToIfBlank(data.branch, ''));
    this.autoUpdate   = m.prop(s.defaultToIfBlank(data.autoUpdate, true));
    this.invertFilter = m.prop(s.defaultToIfBlank(data.invertFilter, false));

    this.validatePresenceOf('url');

    this._attributesToJSON = function () {
      /* eslint-disable camelcase */
      return {
        name:          this.name(),
        destination:   this.destination(),
        url:           this.url(),
        branch:        this.branch(),
        auto_update:   this.autoUpdate(),
        invert_filter: this.invertFilter()
      };
      /* eslint-enable camelcase */
    };
  };

  Materials.Material.Mercurial.fromJSON = function (data) {
    var attr = data.attributes || {};
    return new Materials.Material.Mercurial({
      url:          attr.url,
      branch:       attr.branch,
      destination:  attr.destination,
      name:         attr.name,
      autoUpdate:   attr.auto_update,
      filter:       Materials.Filter.fromJSON(attr.filter),
      invertFilter: attr.invert_filter,
      errors:       data.errors
    });
  };

  Materials.Material.Perforce = function (data) {
    Materials.Material.call(this, "p4", true, data);
    this.name        = m.prop(s.defaultToIfBlank(data.name, ''));
    this.destination = m.prop(s.defaultToIfBlank(data.destination, ''));
    this.port        = m.prop(s.defaultToIfBlank(data.port, ''));
    this.username    = m.prop(s.defaultToIfBlank(data.username, ''));
    var _password    = m.prop(plainOrCipherValue(data));
    this.view        = m.prop(s.defaultToIfBlank(data.view, ''));
    this.useTickets  = m.prop(data.useTickets);
    this.autoUpdate  = m.prop(s.defaultToIfBlank(data.autoUpdate, true));
    this.invertFilter = m.prop(s.defaultToIfBlank(data.invertFilter, false));
    Mixins.HasEncryptedAttribute.call(this, {attribute: _password, name: 'passwordValue'});

    this.validatePresenceOf('port');
    this.validatePresenceOf('view');

    this._attributesToJSON = function () {
      /* eslint-disable camelcase */
      var attrs = {
        name:          this.name(),
        destination:   this.destination(),
        port:          this.port(),
        username:      this.username(),
        view:          this.view(),
        useTickets:    this.useTickets(),
        auto_update:   this.autoUpdate(),
        invert_filter: this.invertFilter()
      };
      /* eslint-enable camelcase */

      return _.merge(attrs, this._passwordHash());
    };

  };

  Materials.Material.Perforce.fromJSON = function (data) {
    var attr = data.attributes || {};
    return new Materials.Material.Perforce({
      port:              attr.port,
      username:          attr.username,
      password:          attr.password,
      encryptedPassword: attr.encrypted_password,
      useTickets:        attr.use_tickets,
      destination:       attr.destination,
      view:              attr.view,
      autoUpdate:        attr.auto_update,
      name:              attr.name,
      filter:            Materials.Filter.fromJSON(attr.filter),
      invertFilter:      attr.invert_filter,
      errors:            data.errors
    });
  };

  Materials.Material.TFS = function (data) {
    Materials.Material.call(this, "tfs", true, data);
    this.name         = m.prop(s.defaultToIfBlank(data.name, ''));
    this.destination  = m.prop(s.defaultToIfBlank(data.destination, ''));
    this.url          = m.prop(s.defaultToIfBlank(data.url, ''));
    this.domain       = m.prop(s.defaultToIfBlank(data.domain, ''));
    this.username     = m.prop(s.defaultToIfBlank(data.username, ''));
    var _password     = m.prop(plainOrCipherValue(data));
    this.projectPath  = m.prop(s.defaultToIfBlank(data.projectPath, ''));
    this.autoUpdate   = m.prop(s.defaultToIfBlank(data.autoUpdate, true));
    this.invertFilter = m.prop(s.defaultToIfBlank(data.invertFilter, false));
    Mixins.HasEncryptedAttribute.call(this, {attribute: _password, name: 'passwordValue'});

    this.validatePresenceOf('url');
    this.validatePresenceOf('username');
    this.validatePresenceOf('projectPath');

    this._attributesToJSON = function () {
      /* eslint-disable camelcase */
      var attrs = {
        name:          this.name(),
        destination:   this.destination(),
        url:           this.url(),
        domain:        this.domain(),
        username:      this.username(),
        project_path:  this.projectPath(),
        auto_update:   this.autoUpdate(),
        invert_filter: this.invertFilter()
      };
      /* eslint-enable camelcase */

      return _.merge(attrs, this._passwordHash());
    };
  };

  Materials.Material.TFS.fromJSON = function (data) {
    var attr = data.attributes || {};
    return new Materials.Material.TFS({
      url:               attr.url,
      domain:            attr.domain,
      username:          attr.username,
      password:          attr.password,
      encryptedPassword: attr.encrypted_password,
      destination:       attr.destination,
      projectPath:       attr.project_path,
      autoUpdate:        attr.auto_update,
      name:              attr.name,
      filter:            Materials.Filter.fromJSON(attr.filter),
      invertFilter:      attr.invert_filter,
      errors:            data.errors
    });
  };

  Materials.Material.Dependency = function (data) {
    Materials.Material.call(this, "dependency", false, data);
    this.name     = m.prop(s.defaultToIfBlank(data.name, ''));
    this.pipeline = m.prop(s.defaultToIfBlank(data.pipeline, ''));
    this.stage    = m.prop(s.defaultToIfBlank(data.stage, ''));

    this.validatePresenceOf('pipeline');
    this.validatePresenceOf('stage');

    this._attributesToJSON = function () {
      return {
        name:     this.name(),
        pipeline: this.pipeline(),
        stage:    this.stage()
      };
    };
  };

  Materials.Material.Dependency.fromJSON = function (data) {
    var attr = data.attributes || {};
    return new Materials.Material.Dependency({
      pipeline: attr.pipeline,
      stage:    attr.stage,
      name:     attr.name,
      errors:   data.errors
    });
  };

  Materials.Material.PluggableMaterial = function (data) {
    Materials.Material.call(this, "plugin", true, data);
    this.name         = m.prop(''); //TODO: This needs to be removed, added to pass base validation.
    this.pluginInfo   = m.prop(data.pluginInfo);
    this.destination  = m.prop(s.defaultToIfBlank(data.destination, ''));
    this.scm          = m.prop(data.scm);
    this.invertFilter = m.prop(s.defaultToIfBlank(data.invertFilter, false));

    this._attributesToJSON = function () {
      /* eslint-disable camelcase */
      return {
        destination:   this.destination(),
        ref:           this.scm().id(),
        invert_filter: this.invertFilter()
      };
      /* eslint-enable camelcase */
    };
  };

  Materials.Material.PluggableMaterial.fromJSON = function (data) {
    var attr = data.attributes || {};
    return new Materials.Material.PluggableMaterial({
      scm:          SCMs.findById(attr.ref),
      destination:  attr.destination,
      filter:       Materials.Filter.fromJSON(attr.filter),
      invertFilter: attr.invert_filter,
      errors:       data.errors
    });
  };

  Materials.Material.PackageMaterial = function (data) {
    Materials.Material.call(this, "package", true, data);
    this.name = m.prop(''); //TODO: This needs to be removed, added to pass base validation.
    this.ref  = m.prop(data.ref);

    this._attributesToJSON = function () {
      return {
        ref: this.ref()
      };
    };
  };

  Materials.Material.PackageMaterial.fromJSON = function (data) {
    var attr = data.attributes || {};
    return new Materials.Material.PackageMaterial({
      ref:    attr.ref,
      errors: data.errors
    });
  };

  Materials.isBuiltInType = function (type) {
    return _.hasIn(Materials.Types, type);
  };

  Materials.Types = {
    git:        {type: Materials.Material.Git, description: "Git"},
    svn:        {type: Materials.Material.SVN, description: "SVN"},
    hg:         {type: Materials.Material.Mercurial, description: "Mercurial"},
    p4:         {type: Materials.Material.Perforce, description: "Perforce"},
    tfs:        {type: Materials.Material.TFS, description: "Team Foundation Server"},
    dependency: {type: Materials.Material.Dependency, description: "Pipeline Dependency"}
  };


  Materials.Material.fromJSON = function (data) {
    if (Materials.isBuiltInType(data.type)) {
      return Materials.Types[data.type].type.fromJSON(data || {});
    }

    var nonBuiltInTypes = {
      plugin:  Materials.Material.PluggableMaterial,
      package: Materials.Material.PackageMaterial
    };

    return nonBuiltInTypes[data.type].fromJSON(data || {});
  };

  return Materials;
});
