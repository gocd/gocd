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

  var Materials = function (data) {
    Mixins.HasMany.call(this, {factory: Materials.create, as: 'Material', collection: data, uniqueOn: 'name'});
  };

  Materials.create = function (data) {
    return new Materials.Types[data.type].type(data);
  };

  Materials.Filter = function (data) {
    this.constructor.modelType = 'materialFilter';
    Mixins.HasUUID.call(this);

    this.ignore = m.prop(s.defaultToIfBlank(data.ignore, []));

    this.toJSON = function () {
      return {
        filter: {
          ignore: this.ignore()
        }
      };
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

    this.parent = Mixins.GetterSetter();

    this.type       = m.prop(type);
    this.name       = m.prop(s.defaultToIfBlank(data.name, ''));
    this.autoUpdate = m.prop(data.autoUpdate);

    if (hasFilter) {
      this.filter = m.prop(s.defaultToIfBlank(data.filter, new Materials.Filter(s.defaultToIfBlank(data.filter, {}))));
    }

    this._validate = function (errors) {
      if (!s.isBlank(this.name())) {
        this.parent().validateUniqueMaterialName(this, errors);
      }
    };


    this.toJSON = function () {
      var attrs = {
        name:        this.name(),
        auto_update: this.autoUpdate()
      };

      if (hasFilter) {
        _.merge(attrs, this.filter().toJSON());
      }

      return {
        type:       this.type(),
        attributes: _.merge(attrs, this._attributesToJSON())
      };
    };

    this._attributesToJSON = function () {
      throw new Error("Subclass responsibility!");
    };

  };

  Mixins.fromJSONCollection({
    parentType: Materials,
    childType:  Materials.Material,
    via:        'addMaterial'
  });

  Materials.Material.SVN = function (data) {
    Materials.Material.call(this, "svn", true, data);
    this.destination    = m.prop(s.defaultToIfBlank(data.destination, ''));
    this.url            = m.prop(s.defaultToIfBlank(data.url, ''));
    this.username       = m.prop(s.defaultToIfBlank(data.username, ''));
    this.password       = m.prop(s.defaultToIfBlank(data.password, ''));
    this.checkExternals = m.prop(data.checkExternals);

    this.validate = function () {
      var errors = new Mixins.Errors();

      this._validate(errors);

      if (s.isBlank(this.url())) {
        errors.add('url', Mixins.ErrorMessages.mustBePresent("url"));
      }

      return errors;
    };

    this._attributesToJSON = function () {
      return {
        destination:    this.destination(),
        url:            this.url(),
        username:       this.username(),
        password:       this.password(),
        checkExternals: this.checkExternals()
      };
    };
  };

  Materials.Material.SVN.fromJSON = function (data) {
    return new Materials.Material.SVN({
      url:            data.url,
      username:       data.username,
      password:       data.password,
      checkExternals: data.check_externals,
      destination:    data.destination,
      name:           data.name,
      autoUpdate:     data.auto_update,
      filter:         Materials.Filter.fromJSON(data.filter)
    });
  };

  Materials.Material.Git = function (data) {
    Materials.Material.call(this, "git", true, data);
    this.destination = m.prop(s.defaultToIfBlank(data.destination, ''));
    this.url         = m.prop(s.defaultToIfBlank(data.url, ''));
    this.branch      = m.prop(s.defaultToIfBlank(data.branch, ''));

    this.validate = function () {
      var errors = new Mixins.Errors();

      this._validate(errors);

      if (s.isBlank(this.url())) {
        errors.add('url', Mixins.ErrorMessages.mustBePresent("url"));
      }

      return errors;
    };

    this._attributesToJSON = function () {
      return {
        destination: this.destination(),
        url:         this.url(),
        branch:      this.branch()
      };
    };
  };

  Materials.Material.Git.fromJSON = function (data) {
    return new Materials.Material.Git({
      url:         data.url,
      branch:      data.branch,
      destination: data.destination,
      name:        data.name,
      autoUpdate:  data.auto_update,
      filter:      Materials.Filter.fromJSON(data.filter)
    });
  };

  Materials.Material.Mercurial = function (data) {
    Materials.Material.call(this, "hg", true, data);
    this.destination = m.prop(s.defaultToIfBlank(data.destination, ''));
    this.url         = m.prop(s.defaultToIfBlank(data.url, ''));
    this.branch      = m.prop(s.defaultToIfBlank(data.branch, ''));

    this.validate = function () {
      var errors = new Mixins.Errors();

      this._validate(errors);

      if (s.isBlank(this.url())) {
        errors.add('url', Mixins.ErrorMessages.mustBePresent("url"));
      }

      return errors;
    };

    this._attributesToJSON = function () {
      return {
        destination: this.destination(),
        url:         this.url(),
        branch:      this.branch()
      };
    };
  };

  Materials.Material.Mercurial.fromJSON = function (data) {
    return new Materials.Material.Mercurial({
      url:         data.url,
      branch:      data.branch,
      destination: data.destination,
      name:        data.name,
      autoUpdate:  data.auto_update,
      filter:      Materials.Filter.fromJSON(data.filter)
    });
  };

  Materials.Material.Perforce = function (data) {
    Materials.Material.call(this, "p4", true, data);
    this.destination = m.prop(s.defaultToIfBlank(data.destination, ''));
    this.port        = m.prop(s.defaultToIfBlank(data.port, ''));
    this.username    = m.prop(s.defaultToIfBlank(data.username, ''));
    this.password    = m.prop(s.defaultToIfBlank(data.password, ''));
    this.view        = m.prop(s.defaultToIfBlank(data.view, ''));
    this.useTickets  = m.prop(data.useTickets);


    this.validate = function () {
      var errors = new Mixins.Errors();

      this._validate(errors);

      if (s.isBlank(this.port())) {
        errors.add('port', Mixins.ErrorMessages.mustBePresent("port"));
      }

      if (s.isBlank(this.view())) {
        errors.add('view', Mixins.ErrorMessages.mustBePresent("view"));
      }

      return errors;
    };

    this._attributesToJSON = function () {
      return {
        destination: this.destination(),
        port:        this.port(),
        username:    this.username(),
        password:    this.password(),
        view:        this.view(),
        useTickets:  this.useTickets()
      };
    };

  };

  Materials.Material.Perforce.fromJSON = function (data) {
    return new Materials.Material.Perforce({
      port:        data.port,
      username:    data.username,
      password:    data.password,
      useTickets:  data.use_tickets,
      destination: data.destination,
      view:        data.view,
      autoUpdate:  data.auto_update,
      name:        data.name,
      filter:      Materials.Filter.fromJSON(data.filter)
    });
  };

  Materials.Material.TFS = function (data) {
    Materials.Material.call(this, "tfs", true, data);
    this.destination = m.prop(s.defaultToIfBlank(data.destination, ''));
    this.url         = m.prop(s.defaultToIfBlank(data.url, ''));
    this.domain      = m.prop(s.defaultToIfBlank(data.domain, ''));
    this.username    = m.prop(s.defaultToIfBlank(data.username, ''));
    this.password    = m.prop(s.defaultToIfBlank(data.password, ''));
    this.projectPath = m.prop(s.defaultToIfBlank(data.projectPath, ''));

    this.validate = function () {
      var errors = new Mixins.Errors();

      this._validate(errors);

      if (s.isBlank(this.url())) {
        errors.add('url', Mixins.ErrorMessages.mustBePresent("url"));
      }

      if (s.isBlank(this.username())) {
        errors.add('username', Mixins.ErrorMessages.mustBePresent("username"));
      }

      if (s.isBlank(this.projectPath())) {
        errors.add('projectPath', Mixins.ErrorMessages.mustBePresent("projectPath"));
      }

      return errors;
    };

    this._attributesToJSON = function () {
      return {
        destination:  this.destination(),
        url:          this.url(),
        domain:       this.domain(),
        username:     this.username(),
        password:     this.password(),
        project_path: this.projectPath()
      };
    };

  };

  Materials.Material.TFS.fromJSON = function (data) {
    return new Materials.Material.TFS({
      url:         data.url,
      domain:      data.domain,
      username:    data.username,
      password:    data.password,
      destination: data.destination,
      projectPath: data.project_path,
      autoUpdate:  data.auto_update,
      name:        data.name,
      filter:      Materials.Filter.fromJSON(data.filter)
    });
  };

  Materials.Types = {
    git: {type: Materials.Material.Git, description: "Git"},
    svn: {type: Materials.Material.SVN, description: "SVN"},
    hg:  {type: Materials.Material.Mercurial, description: "Mercurial"},
    p4:  {type: Materials.Material.Perforce, description: "Perforce"},
    tfs: {type: Materials.Material.TFS, description: "Team Foundation Server"}
  };

  Materials.Material.fromJSON = function (data) {
    return Materials.Types[data.type].type.fromJSON(data.attributes || {});
  };

  return Materials;
});
