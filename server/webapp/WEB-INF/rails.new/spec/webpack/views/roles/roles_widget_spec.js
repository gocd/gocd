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

  const roleJSON = {
    "name":           "blackbird",
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
  };

  const rolesJSON = {
    "_embedded": {
      "roles": [roleJSON]
    }
  };

  const ldapPluginInfoJSON = {
    "id":            "cd.go.authorization.ldap",
    "name":          "Ldap authorization plugin",
    "version":       "1.x.x",
    "type":          "authorization",
    "role_settings": {
      "configurations": [
        {
          "key":      "AttributeName",
          "metadata": {
            "secure":   false,
            "required": false
          }
        },
        {
          "key":      "AttributeValue",
          "metadata": {
            "secure":   false,
            "required": false
          }
        },
        {
          "key":      "GroupMembershipFilter",
          "metadata": {
            "secure":   false,
            "required": false
          }
        },
        {
          "key":      "GroupMembershipSearchBase",
          "metadata": {
            "secure":   false,
            "required": false
          }
        }
      ],
      "view":           {
        "template": "<div></div>"
      }
    }
  };

  const allPluginInfosJSON = [ldapPluginInfoJSON];

  const authConfigJSON = {
    "id":         "ldap",
    "plugin_id":  "cd.go.authorization.ldap",
    "properties": [
      {
        "key":   "Url",
        "value": "ldap://ldap.server.url"
      },
      {
        "key":   "ManagerDN",
        "value": "uid=admin,ou=system"
      }
    ]
  };

  const allAuthConfigJSON = [authConfigJSON];

  beforeEach(() => {
    jasmine.Ajax.install();

    jasmine.Ajax.stubRequest(roleIndexUrl, undefined, 'GET').andReturn({
      responseText: JSON.stringify(rolesJSON),
      status:       200
    });

    m.mount(root, RolesWidget({
      pluginInfos: Stream(PluginInfos.fromJSON(allPluginInfosJSON)),
      authConfigs: Stream(AuthConfigs.fromJSON(allAuthConfigJSON))
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
      m.mount(root, null);
      m.redraw();

      jasmine.Ajax.stubRequest(roleIndexUrl, undefined, 'GET').andReturn({
        responseText: JSON.stringify(rolesJSON),
        status:       200
      });

      m.mount(root, RolesWidget({
        pluginInfos: Stream(PluginInfos.fromJSON([])),
        authConfigs: Stream(AuthConfigs.fromJSON(allAuthConfigJSON))
      }));
      m.redraw(true);
    });

    it("should disable add button if no authorization plugin installed", () => {
      jasmine.Ajax.stubRequest(roleIndexUrl, undefined, 'POST').andReturn({
        responseText: JSON.stringify({data: []}),
        status:       200
      });

      m.redraw();
      expect($root.find('.add-role').get(0)).toBeDisabled();
    });


  });

  describe("list all roles", () => {

    it("should render a list of all roles", () => {
      expect($root.find('.role-name .value').text()).toEqual(roleJSON.name);
      expect($root.find('.auth-config-id .value').text()).toEqual(roleJSON.auth_config_id);
    });

    it("should render error if index call fails", () => {
      jasmine.Ajax.stubRequest(roleIndexUrl).andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      m.mount(root, RolesWidget({
        pluginInfos: Stream(PluginInfos.fromJSON(allPluginInfosJSON)),
        authConfigs: Stream(AuthConfigs.fromJSON(allAuthConfigJSON))
      }));

      m.redraw();

      expect($root.find('.alert.callout')).toContainText('Boom!');
    });
  });

  describe("add a new role", () => {
    afterEach(Modal.destroyAll);

    it("should render new modal to create role", () => {
      expect($root.find('.reveal:visible')).not.toBeInDOM();
      simulateEvent.simulate($root.find('.add-role').get(0), 'click');
      m.redraw();
      expect($('.reveal:visible')).toBeInDOM();
      expect($('.reveal:visible input[data-prop-name]')).not.toBeDisabled();
    });

    it('should show render modal and render role view for first auth config id', () => {
      simulateEvent.simulate($root.find('.add-role').get(0), 'click');
      m.redraw();

      const authConfigId = $('.reveal:visible .modal-body').find('[data-prop-name="authConfigId"]').get(0);

      expect($(authConfigId).val()).toEqual('ldap');
    });

    it("should make request to save role on click of save button", () => {
      jasmine.Ajax.stubRequest(roleIndexUrl, undefined, 'POST').andReturn({
        responseText: JSON.stringify({data: roleJSON}),
        status:       200
      });

      simulateEvent.simulate($root.find('.add-role').get(0), 'click');
      m.redraw();

      const roleName = $('.reveal:visible .modal-body').find('[data-prop-name="name"]').get(0);
      $(roleName).val("ldap");
      simulateEvent.simulate(roleName, 'input');

      const authConfigId = $('.reveal:visible .modal-body').find('[data-prop-name="authConfigId"]').get(0);
      $(authConfigId).val(ldapPluginInfoJSON.id);
      simulateEvent.simulate(authConfigId, 'change');

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

      const authConfigId = $('.reveal:visible .modal-body').find('[data-prop-name="authConfigId"]').get(0);
      $(authConfigId).val(ldapPluginInfoJSON.id);
      simulateEvent.simulate(authConfigId, 'change');

      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .modal-buttons .save').get(0), 'click');
      expect($('.alert')).toContainText('Boom!');
    });
  });

  describe("edit an existing role", () => {
    afterEach(Modal.destroyAll);

    it("should render a new modal to edit existing role", () => {
      jasmine.Ajax.stubRequest(`${roleIndexUrl}/${  roleJSON.name}`, undefined, 'GET').andReturn({
        responseText: JSON.stringify(roleJSON),
        status:       200
      });
      expect($root.find('.reveal:visible')).not.toBeInDOM();

      simulateEvent.simulate($root.find('.edit-role').get(0), 'click');

      expect($('.reveal:visible')).toBeInDOM();
      expect($('.reveal:visible input[data-prop-name=name]')).toBeDisabled();
    });

    it("should display error message if fails to fetch role", () => {
      jasmine.Ajax.stubRequest(`${roleIndexUrl}/${  roleJSON.name}`, undefined, 'GET').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      simulateEvent.simulate($root.find('.edit-role').get(0), 'click');
      m.redraw();

      expect($('.alert')).toContainText('Boom!');
    });


    it("should keep the role expanded while edit modal is open", () => {
      jasmine.Ajax.stubRequest(`${roleIndexUrl}/${  roleJSON.name}`, undefined, 'GET').andReturn({
        responseText: JSON.stringify(roleJSON),
        status:       200
      });

      expect($root.find('.plugin-config-read-only')).not.toHaveClass('show');
      simulateEvent.simulate($root.find('.role-header').get(0), 'click');
      m.redraw();
      expect($root.find('.plugin-config-read-only')).toHaveClass('show');

      simulateEvent.simulate($root.find('.edit-role').get(0), 'click');
      m.redraw();
      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .close-button span').get(0), 'click');
      m.redraw();
      expect($root.find('.plugin-config-read-only')).toHaveClass('show');
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
      jasmine.Ajax.stubRequest(`${roleIndexUrl}/${  roleJSON.name}`, undefined, 'DELETE').andReturn({
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
      jasmine.Ajax.stubRequest(`${roleIndexUrl}/${  roleJSON.name}`, undefined, 'DELETE').andReturn({
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

})
;
