/*
 * Copyright 2017 ThoughtWorks, Inc.
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

describe("RolesWidget", () => {

  const roleIndexUrl = '/go/api/admin/security/roles';

  const $             = require("jquery");
  const m             = require("mithril");
  const Stream        = require("mithril/stream");
  const simulateEvent = require('simulate-event');

  require('jasmine-jquery');
  require('jasmine-ajax');

  const RolesWidget = require("views/roles/roles_widget");
  const PluginInfos = require('models/shared/plugin_infos');
  const AuthConfigs = require('models/auth_configs/auth_configs');
  const Modal       = require('views/shared/new_modal');

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(window.destroyDomElementForTest);

  const pluginRoleJSON = {
    "name":       "blackbird",
    "type":       "plugin",
    "attributes": {
      "auth_config_id": "ldap",
      "properties":     [
        {
          "key":   "AttributeName",
          "value": "memberOf"
        },
        {
          "key":   "AttributeValue",
          "value": "ou=group-name,ou=system,dc=example,dc=com"
        }
      ]
    }
  };

  const pluginRoleJSONButPluginRemoved = {
    "name":       "no-plugin-for-this-role",
    "type":       "plugin",
    "attributes": {
      "properties": []
    }
  };

  const roleJSON = {
    "name":       "admin",
    "type":       "gocd",
    "attributes": {
      "users": ["bob", "alice"]
    }
  };

  const allRolesJSON = {
    "_embedded": {
      "roles": [pluginRoleJSON, roleJSON, pluginRoleJSONButPluginRemoved]
    }
  };

  const ldapPluginInfoJSON = {
    "id":             "cd.go.authorization.ldap",
    "type":           "authorization",
    "status": {
      "state": "active"
    },
    "about":          {"name": "Ldap authorization plugin"},
    "extension_info": {
      "role_settings": {
        "configurations": [],
        "view":           {
          "template": '<div class="plugin-role-view"><label>Name</label><input id="name" type="text"/></div>'
        }
      },
      "capabilities":  {
        "can_authorize": true
      }
    }
  };

  const githubPluginInfoJSON = {
    "id":             "cd.go.authorization.github",
    "type":           "authorization",
    "status": {
      "state": "active"
    },
    "about":          {"name": "GitHub authorization plugin"},
    "extension_info": {
      "role_settings": {
        "configurations": [],
        "view":           {
          "template": '<div class="plugin-role-view"><label>GitHub username:</label><input id="name" type="text"/></div>'
        }
      },
      "capabilities":  {
        "can_authorize": true
      }
    }
  };

  const allPluginInfosJSON = [ldapPluginInfoJSON, githubPluginInfoJSON];

  const firstValidAuthConfigJSON = {
    "id":         "ldap",
    "plugin_id":  "cd.go.authorization.ldap",
    "properties": []
  };

  const secondValidAuthConfigJSON = {
    "id":         "github",
    "plugin_id":  "cd.go.authorization.github",
    "properties": []
  };

  const authConfigJSONWithoutPlugin = {
    "id":         "auth-config-without-plugin",
    "plugin_id":  "cd.go.authorization.foo-plugin",
    "properties": []
  };

  const textElements = (selector) => {
    return $.map($(selector).contents().filter(function () {
      return this.nodeType === 3;
    }), $.text);
  };

  const allAuthConfigJSON = [firstValidAuthConfigJSON, authConfigJSONWithoutPlugin];

  const allPluginInfos               = Stream(PluginInfos.fromJSON([]));
  const allAuthConfigs               = Stream(AuthConfigs.fromJSON({}));
  const authConfigsOfInstalledPlugin = Stream(AuthConfigs.fromJSON({}));

  beforeEach(() => {
    jasmine.Ajax.install();
    jasmine.Ajax.stubRequest(roleIndexUrl, undefined, 'GET').andReturn({
      responseText: JSON.stringify(allRolesJSON),
      status:       200
    });

    allPluginInfos(PluginInfos.fromJSON(allPluginInfosJSON));
    allAuthConfigs(AuthConfigs.fromJSON(allAuthConfigJSON));
    authConfigsOfInstalledPlugin(AuthConfigs.fromJSON([firstValidAuthConfigJSON, secondValidAuthConfigJSON]));

    m.mount(root, RolesWidget({
      pluginInfos: allPluginInfos,
      allAuthConfigs,
      authConfigsOfInstalledPlugin
    }));

    m.redraw(true);
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();

    m.mount(root, null);
    m.redraw();

    expect($('.new-modal-container .reveal')).not.toExist('Did you forget to close the modal before the test?');
  });

  describe("no authorization plugin loaded", () => {
    beforeEach(() => {
      allPluginInfos(PluginInfos.fromJSON([]));
      authConfigsOfInstalledPlugin(AuthConfigs.fromJSON([]));
      m.redraw(true);
    });
    afterEach(Modal.destroyAll);

    it("should disable plugin role radio button", () => {
      simulateEvent.simulate($root.find('.add-role').get(0), 'click');

      m.redraw();
      expect($('.reveal input[name=role-type-selector].plugin-role')).toBeDisabled();
      expect($('.reveal input[name=role-type-selector].core-role')).not.toBeDisabled();
    });

    it("should render info callout in absence of authorization plugin", () => {
      expect($root.find('.roles .callout').text()).toEqual("None of the installed plugin supports role based authorization.");
    });

    it("should list existing roles in absence of authorization plugin", () => {
      const rows = $root.find('.role-description');

      expect(rows.eq(0).find('.role-name .value').text()).toEqual(pluginRoleJSON.name);
      expect(rows.eq(0).find('.auth-config-id .value').text()).toEqual(pluginRoleJSON.attributes.auth_config_id);

      expect(rows.eq(1).find('.role-name .value').text()).toEqual(roleJSON.name);
      expect(rows.eq(1).find('.auth-config-id .value').get(0)).toEqual(undefined);
    });

    it("should list existing plugin roles in absence of authorization plugin with disabled edit and clone button", () => {
      const rows    = $root.find('.role-description');
      const actions = $root.find('.role-actions');

      expect(rows.eq(0).find('.role-name .value').text()).toEqual(pluginRoleJSON.name);
      expect(rows.eq(0).find('.auth-config-id .value').text()).toEqual(pluginRoleJSON.attributes.auth_config_id);
      expect(actions.eq(0).find('.edit-role').hasClass('disabled')).toEqual(true);
      expect(actions.eq(0).find('.clone-role').hasClass('disabled')).toEqual(true);
    });
  });

  describe("no authorization configuration", () => {
    beforeEach(() => {
      authConfigsOfInstalledPlugin(AuthConfigs.fromJSON([]));
      m.redraw(true);
    });

    afterEach(Modal.destroyAll);

    it("should disable only plugin role radio button", () => {
      simulateEvent.simulate($root.find('.add-role').get(0), 'click');

      m.redraw();

      expect($('.reveal input[name=role-type-selector].plugin-role')).toBeDisabled();
      expect($('.reveal input[data-prop-name]')).not.toBeDisabled();
      expect($('.reveal .role-user')).toBeInDOM();
      expect($('.reveal .add-role-user-button')).toBeInDOM();
    });

  });

  describe("list all roles", () => {

    it("should render a list of all roles", () => {
      const rows = $root.find('.role');

      expect(rows.eq(0).find('.role-description').find('.role-name .value').text()).toEqual(pluginRoleJSON.name);
      expect(rows.eq(0).find('.role-description').find('.auth-config-id .value').text()).toEqual(pluginRoleJSON.attributes.auth_config_id);
      expect(rows.eq(0).find('.plugin-role-read-only').find('.key-value-pair dt').eq(0).text()).toEqual("AttributeName");
      expect(rows.eq(0).find('.plugin-role-read-only').find('.key-value-pair dt').eq(1).text()).toEqual("AttributeValue");
      expect(rows.eq(0).find('.plugin-role-read-only').find('.key-value-pair dd pre').eq(0).text()).toEqual("memberOf");
      expect(rows.eq(0).find('.plugin-role-read-only').find('.key-value-pair dd pre').eq(1).text()).toEqual("ou=group-name,ou=system,dc=example,dc=com");

      expect(rows.eq(1).find('.role-name .value').text()).toEqual(roleJSON.name);
      expect(rows.eq(1).find('.auth-config-id .value').get(0)).toEqual(undefined);
      expect(rows.eq(1).find('.role-read-only .tag').eq(0).text()).toEqual("alice");
      expect(rows.eq(1).find('.role-read-only .tag').eq(1).text()).toEqual("bob");

    });

    it("should render error if index call fails", () => {
      jasmine.Ajax.stubRequest(roleIndexUrl).andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      m.mount(root, RolesWidget({
        pluginInfos: allPluginInfos,
        allAuthConfigs,
        authConfigsOfInstalledPlugin
      }));
      m.redraw();

      expect($root.find('.alert.callout')).toContainText('Boom!');
    });
  });

  describe("add a new role", () => {
    afterEach(Modal.destroyAll);

    it("should render new modal to create role", () => {
      expect($('.reveal')).not.toBeInDOM();
      simulateEvent.simulate($root.find('.add-role').get(0), 'click');

      m.redraw();

      expect($('.reveal')).toBeInDOM();
      expect($('.reveal input[data-prop-name]')).not.toBeDisabled();
      expect($('.reveal .role-user')).toBeInDOM();
      expect($('.reveal .add-role-user-button')).toBeInDOM();
    });

    it("should change view on role type selection change", () => {
      expect($('.reveal')).not.toBeInDOM();

      simulateEvent.simulate($root.find('.add-role').get(0), 'click');
      m.redraw();

      expect($('.reveal')).toBeInDOM();

      expect($('.reveal')).toBeInDOM();
      expect($('.reveal input[data-prop-name]')).not.toBeDisabled();
      expect($('.reveal .role-user')).toBeInDOM();
      expect($('.reveal .add-role-user-button')).toBeInDOM();

      simulateEvent.simulate($('.reveal input[name=role-type-selector].plugin-role').get(0), 'click');
      m.redraw();

      expect($('.reveal input[data-prop-name]')).not.toBeDisabled();
      expect($('.reveal [data-prop-name=authConfigId] option:selected').text()).toEqual(`${firstValidAuthConfigJSON.id} (${ldapPluginInfoJSON.about.name})`);
      expect($('.reveal #name')).toBeInDOM();

      simulateEvent.simulate($('.reveal input[name=role-type-selector].core-role').get(0), 'click');
      m.redraw();

      expect($('.reveal')).toBeInDOM();
      expect($('.reveal input[data-prop-name]')).not.toBeDisabled();
      expect($('.reveal .role-user')).toBeInDOM();
      expect($('.reveal .add-role-user-button')).toBeInDOM();
    });

    it("should change plugin role view in modal on change of auth config profile from dropdown", () => {
      simulateEvent.simulate($root.find('.add-role').get(0), 'click');
      m.redraw();
      simulateEvent.simulate($('.reveal input[name=role-type-selector].plugin-role').get(0), 'click');
      m.redraw();

      expect($('.reveal input[data-prop-name]')).not.toBeDisabled();
      expect($('.reveal [data-prop-name=authConfigId] option:selected').text()).toEqual(`${firstValidAuthConfigJSON.id} (${ldapPluginInfoJSON.about.name})`);
      expect($('.reveal .plugin-role-view label').text()).toEqual("Name");

      $('.reveal [data-prop-name=authConfigId]').val("github");
      simulateEvent.simulate($('.reveal [data-prop-name=authConfigId]').get(0), 'change');
      m.redraw();

      expect($('.reveal input[data-prop-name]')).not.toBeDisabled();
      expect($('.reveal [data-prop-name=authConfigId] option:selected').text()).toEqual(`${secondValidAuthConfigJSON.id} (${githubPluginInfoJSON.about.name})`);
      expect($('.reveal .plugin-role-view label').text()).toEqual("GitHub username:");
    });

    it("should make request to save role on click of save button", () => {
      jasmine.Ajax.stubRequest(roleIndexUrl, undefined, 'POST').andReturn({
        responseText: JSON.stringify(pluginRoleJSON),
        status:       200
      });

      simulateEvent.simulate($root.find('.add-role').get(0), 'click');

      const roleName = $('.reveal:visible .modal-body').find('[data-prop-name="name"]').get(0);
      $(roleName).val("ldap");
      simulateEvent.simulate(roleName, 'input');
      m.redraw();

      simulateEvent.simulate($('.reveal:visible .modal-buttons').find('.save').get(0), 'click');
      m.redraw();

      expect($('.success')).toContainText('The role ldap was created successfully');
    });

    it("should display error message", () => {
      jasmine.Ajax.stubRequest(roleIndexUrl, undefined, 'POST').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      simulateEvent.simulate($root.find('.add-role').get(0), 'click');
      m.redraw();

      const roleName = $('.reveal:visible .modal-body').find('[data-prop-name="name"]').get(0);
      $(roleName).val("ldap");
      simulateEvent.simulate(roleName, 'input');

      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .modal-buttons .save').get(0), 'click');
      expect($('.alert')).toContainText('Boom!');
    });

    it("should create user on click of add button in go core role", () => {
      expect($('.reveal')).not.toBeInDOM();

      simulateEvent.simulate($root.find('.add-role').get(0), 'click');
      simulateEvent.simulate($('.reveal input[name=role-type-selector].core-role').get(0), 'click');

      expect($('.reveal .role-user')).toBeInDOM();
      expect($('.reveal .add-role-user-button')).toBeInDOM();

      $('.reveal .role-user').val("bob");
      simulateEvent.simulate($('.reveal .role-user').get(0), 'keyup');
      simulateEvent.simulate($('.reveal .add-role-user-button').get(0), 'click');
      m.redraw();

      expect($('.reveal .tag.current-user-tag')).toBeInDOM();
      expect(textElements('.reveal .tag.current-user-tag')).toEqual(["bob"]);
    });

    it("should remove role user on click of delete icon of user tag", () => {
      expect($('.reveal')).not.toBeInDOM();
      simulateEvent.simulate($root.find('.add-role').get(0), 'click');
      simulateEvent.simulate($('.reveal input[name=role-type-selector].core-role').get(0), 'click');

      $('.reveal .role-user').val("alice");
      simulateEvent.simulate($('.reveal .role-user').get(0), 'keyup');
      simulateEvent.simulate($('.reveal .add-role-user-button').get(0), 'click');

      $('.reveal .role-user').val("bob");
      simulateEvent.simulate($('.reveal .role-user').get(0), 'keyup');
      simulateEvent.simulate($('.reveal .add-role-user-button').get(0), 'click');

      expect(textElements('.reveal .tag')).toEqual(["alice", "bob"]);

      simulateEvent.simulate($('.reveal .tag span').get(0), 'click');

      expect(textElements('.reveal .tag')).toEqual(["bob"]);
    });

  });

  describe("edit an existing role", () => {
    afterEach(Modal.destroyAll);

    it("should render a modal to edit existing core role", () => {
      jasmine.Ajax.stubRequest(`${roleIndexUrl}/${roleJSON.name}`, undefined, 'GET').andReturn({
        responseText:    JSON.stringify(roleJSON),
        status:          200,
        responseHeaders: {
          'ETag':         '"foo"',
          'Content-Type': 'application/json'
        }
      });

      expect($root.find('.reveal:visible')).not.toBeInDOM();
      simulateEvent.simulate($root.find('.edit-role').get(1), 'click');
      m.redraw();

      expect($('.reveal:visible')).toBeInDOM();
      expect($('.reveal:visible input[data-prop-name=name]')).toBeDisabled();

      expect(textElements('.reveal .tag')).toEqual(["alice", "bob"]);
    });

    it("should render a modal to edit existing plugin role", () => {
      jasmine.Ajax.stubRequest(`${roleIndexUrl}/${pluginRoleJSON.name}`, undefined, 'GET').andReturn({
        responseText:    JSON.stringify(pluginRoleJSON),
        status:          200,
        responseHeaders: {
          'ETag':         '"foo"',
          'Content-Type': 'application/json'
        }
      });

      expect($root.find('.reveal:visible')).not.toBeInDOM();
      simulateEvent.simulate($root.find('.edit-role').get(0), 'click');

      expect($('.reveal:visible')).toBeInDOM();
      expect($('.reveal:visible input[data-prop-name=name]')).toBeDisabled();

      expect($('.reveal [data-prop-name=authConfigId] option:selected').text()).toEqual(`${firstValidAuthConfigJSON.id} (${ldapPluginInfoJSON.about.name})`);
    });

    it("should display error message if fails to fetch role", () => {
      jasmine.Ajax.stubRequest(`${roleIndexUrl}/${pluginRoleJSON.name}`, undefined, 'GET').andReturn({
        responseText: JSON.stringify(pluginRoleJSON),
        status:       200
      });

      jasmine.Ajax.stubRequest(`${roleIndexUrl}/${pluginRoleJSON.name}`, undefined, 'PUT').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      simulateEvent.simulate($root.find('.edit-role').get(0), 'click');
      m.redraw();

      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .modal-buttons .save').get(0), 'click');

      expect($('.alert')).toContainText('Boom!');
    });


    it("should keep the role expanded while edit modal is open", () => {
      jasmine.Ajax.stubRequest(`${roleIndexUrl}/${pluginRoleJSON.name}`, undefined, 'GET').andReturn({
        responseText: JSON.stringify(pluginRoleJSON),
        status:       200
      });

      expect($root.find('.plugin-role-read-only')).not.toHaveClass('show');
      simulateEvent.simulate($root.find('.role-header').get(0), 'click');
      m.redraw();
      expect($root.find('.plugin-role-read-only')).toHaveClass('show');

      simulateEvent.simulate($root.find('.edit-role').get(0), 'click');
      m.redraw();
      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .close-button span').get(0), 'click');
      m.redraw();
      expect($root.find('.plugin-role-read-only')).toHaveClass('show');
    });
  });

  describe("delete an existing role", () => {
    afterEach(Modal.destroyAll);

    it("should show confirm modal before deleting a role", () => {
      simulateEvent.simulate($root.find('.delete-role-confirm').get(0), 'click');
      m.redraw();
      expect($('.reveal:visible .modal-title')).toHaveText('Are you sure?');
    });

    it("should show success message when role is deleted", () => {
      jasmine.Ajax.stubRequest(`${roleIndexUrl}/${  pluginRoleJSON.name}`, undefined, 'DELETE').andReturn({
        responseText: JSON.stringify({message: 'Success!'}),
        status:       200
      });

      simulateEvent.simulate($root.find('.delete-role-confirm').get(0), 'click');
      m.redraw();
      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .delete-role').get(0), 'click');
      m.redraw();

      expect($('.success')).toContainText('Success!');
    });

    it("should show error message when deletion of role fails", () => {
      jasmine.Ajax.stubRequest(`${roleIndexUrl}/${  pluginRoleJSON.name}`, undefined, 'DELETE').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      simulateEvent.simulate($root.find('.delete-role-confirm').get(0), 'click');
      m.redraw();
      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .delete-role').get(0), 'click');
      m.redraw();

      expect($('.alert')).toContainText('Boom!');
    });
  });

  describe("Clone an existing profile", () => {
    afterEach(Modal.destroyAll);

    it("should show modal with data from cloning role", () => {
      jasmine.Ajax.stubRequest(`${roleIndexUrl}/${pluginRoleJSON.name}`, undefined, 'GET').andReturn({
        responseText: JSON.stringify(pluginRoleJSON),
        status:       200
      });

      expect($root.find('.reveal:visible')).not.toBeInDOM();

      simulateEvent.simulate($root.find('.clone-role').get(0), 'click');

      m.redraw();
      expect($('.reveal:visible')).toBeInDOM();
      expect($('.reveal:visible input[data-prop-name]')).not.toBeDisabled();
    });

  });
});
