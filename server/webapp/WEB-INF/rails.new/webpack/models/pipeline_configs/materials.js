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

var Stream         = require('mithril/stream');
var _              = require('lodash');
var s              = require('string-plus');
var $              = require('jquery');
var Mixins         = require('models/mixins/model_mixins');
var EncryptedValue = require('models/pipeline_configs/encrypted_value');
var SCMs           = require('models/pipeline_configs/scms');
var Validatable    = require('models/mixins/validatable_mixin');
var Routes         = require('gen/js-routes');
var mrequest       = require('helpers/mrequest');

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

Materials.create = data => Materials.isBuiltInType(data.type) ? new Materials.Types[data.type].type(data)
  : new Materials.Material.PluggableMaterial(data);

Materials.Filter = function (data) {
  this.constructor.modelType = 'materialFilter';
  Mixins.HasUUID.call(this);
  Validatable.call(this, data);
  this.ignore = s.withNewJSONImpl(Stream(s.defaultToIfBlank(data.ignore, '')), s.stringToArray);

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

Materials.Filter.fromJSON = data => {
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
  this.parent = Mixins.GetterSetter();
  this.type   = Stream(type);

  if (hasFilter) {
    this.filter = Stream(s.defaultToIfBlank(data.filter, new Materials.Filter({})));
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

    var payload = () => //eslint-disable-line camelcase
    JSON.stringify(_.merge(self.toJSON(), {pipeline_name: pipelineName()}));

    return $.Deferred(function () {
      var deferred = this;

      var jqXHR = $.ajax({
        method:      'POST',
        url:         Routes.apiv1AdminInternalMaterialTestPath(),
        beforeSend:  mrequest.xhrConfig.forVersion('v1'),
        data:        payload(),
        contentType: 'application/json'
      });

      var didFulfill = data => {
        deferred.resolve(data);
      };

      var didReject = jqXHR => {
        deferred.reject(mrequest.unwrapErrorExtractMessage(jqXHR.responseJSON, jqXHR, 'There was an unknown error while checking connection'));
      };

      jqXHR.then(didFulfill, didReject);
    }).promise();

  };

  this._attributesToJSON = () => {
    throw new Error("Subclass responsibility!");
  };

  this._passwordHash = function () {
    if (this.isPlainPasswordValue() || this.isDirtyPasswordValue()) {
      return {password: this.passwordValue()};
    }
    return {encryptedPassword: this.passwordValue()};
  };
};

Mixins.fromJSONCollection({
  parentType: Materials,
  childType:  Materials.Material,
  via:        'addMaterial'
});

Materials.Material.SVN = function (data) {
  Materials.Material.call(this, "svn", true, data);
  this.name           = Stream(s.defaultToIfBlank(data.name, ''));
  this.destination    = Stream(s.defaultToIfBlank(data.destination, ''));
  this.url            = Stream(s.defaultToIfBlank(data.url, ''));
  this.username       = Stream(s.defaultToIfBlank(data.username, ''));
  var _password       = Stream(plainOrCipherValue(data));
  this.checkExternals = Stream(data.checkExternals);
  this.autoUpdate     = Stream(s.defaultToIfBlank(data.autoUpdate, true));
  this.invertFilter   = Stream(s.defaultToIfBlank(data.invertFilter, false));
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

Materials.Material.SVN.fromJSON = data => {
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
  this.name         = Stream(s.defaultToIfBlank(data.name, ''));
  this.destination  = Stream(s.defaultToIfBlank(data.destination, ''));
  this.url          = Stream(s.defaultToIfBlank(data.url, ''));
  this.branch       = Stream(s.defaultToIfBlank(data.branch, 'master'));
  this.shallowClone = Stream(data.shallowClone);
  this.autoUpdate   = Stream(s.defaultToIfBlank(data.autoUpdate, true));
  this.invertFilter = Stream(s.defaultToIfBlank(data.invertFilter, false));

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
      invert_filter: this.invertFilter()
    };
    /* eslint-enable camelcase */
  };
};

Materials.Material.Git.fromJSON = data => {
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
  this.name         = Stream(s.defaultToIfBlank(data.name, ''));
  this.destination  = Stream(s.defaultToIfBlank(data.destination, ''));
  this.url          = Stream(s.defaultToIfBlank(data.url, ''));
  this.branch       = Stream(s.defaultToIfBlank(data.branch, ''));
  this.autoUpdate   = Stream(s.defaultToIfBlank(data.autoUpdate, true));
  this.invertFilter = Stream(s.defaultToIfBlank(data.invertFilter, false));

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

Materials.Material.Mercurial.fromJSON = data => {
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
  this.name         = Stream(s.defaultToIfBlank(data.name, ''));
  this.destination  = Stream(s.defaultToIfBlank(data.destination, ''));
  this.port         = Stream(s.defaultToIfBlank(data.port, ''));
  this.username     = Stream(s.defaultToIfBlank(data.username, ''));
  var _password     = Stream(plainOrCipherValue(data));
  this.view         = Stream(s.defaultToIfBlank(data.view, ''));
  this.useTickets   = Stream(data.useTickets);
  this.autoUpdate   = Stream(s.defaultToIfBlank(data.autoUpdate, true));
  this.invertFilter = Stream(s.defaultToIfBlank(data.invertFilter, false));
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

Materials.Material.Perforce.fromJSON = data => {
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
  this.name         = Stream(s.defaultToIfBlank(data.name, ''));
  this.destination  = Stream(s.defaultToIfBlank(data.destination, ''));
  this.url          = Stream(s.defaultToIfBlank(data.url, ''));
  this.domain       = Stream(s.defaultToIfBlank(data.domain, ''));
  this.username     = Stream(s.defaultToIfBlank(data.username, ''));
  var _password     = Stream(plainOrCipherValue(data));
  this.projectPath  = Stream(s.defaultToIfBlank(data.projectPath, ''));
  this.autoUpdate   = Stream(s.defaultToIfBlank(data.autoUpdate, true));
  this.invertFilter = Stream(s.defaultToIfBlank(data.invertFilter, false));
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

Materials.Material.TFS.fromJSON = data => {
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
  this.name     = Stream(s.defaultToIfBlank(data.name, ''));
  this.pipeline = Stream(s.defaultToIfBlank(data.pipeline, ''));
  this.stage    = Stream(s.defaultToIfBlank(data.stage, ''));

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

Materials.Material.Dependency.fromJSON = data => {
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
  this.name         = Stream(''); //TODO: This needs to be removed, added to pass base validation.
  this.pluginInfo   = Stream(data.pluginInfo);
  this.destination  = Stream(s.defaultToIfBlank(data.destination, ''));
  this.scm          = Stream(data.scm);
  this.invertFilter = Stream(s.defaultToIfBlank(data.invertFilter, false));

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

Materials.Material.PluggableMaterial.fromJSON = data => {
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
  this.name = Stream(''); //TODO: This needs to be removed, added to pass base validation.
  this.ref  = Stream(data.ref);

  this._attributesToJSON = function () {
    return {
      ref: this.ref()
    };
  };
};

Materials.Material.PackageMaterial.fromJSON = data => {
  var attr = data.attributes || {};
  return new Materials.Material.PackageMaterial({
    ref:    attr.ref,
    errors: data.errors
  });
};

Materials.isBuiltInType = type => _.hasIn(Materials.Types, type);

Materials.Types = {
  git:        {type: Materials.Material.Git, description: "Git"},
  svn:        {type: Materials.Material.SVN, description: "SVN"},
  hg:         {type: Materials.Material.Mercurial, description: "Mercurial"},
  p4:         {type: Materials.Material.Perforce, description: "Perforce"},
  tfs:        {type: Materials.Material.TFS, description: "Team Foundation Server"},
  dependency: {type: Materials.Material.Dependency, description: "Pipeline Dependency"}
};


Materials.Material.fromJSON = data => {
  if (Materials.isBuiltInType(data.type)) {
    return Materials.Types[data.type].type.fromJSON(data || {});
  }

  var nonBuiltInTypes = {
    plugin:  Materials.Material.PluggableMaterial,
    package: Materials.Material.PackageMaterial
  };

  return nonBuiltInTypes[data.type].fromJSON(data || {});
};

module.exports = Materials;
