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
describe("AuthConfigsWidget", () => {

  const authConfigIndexUrl = '/go/api/admin/security/auth_configs';

  const $             = require("jquery");
  const m             = require("mithril");
  const Stream        = require("mithril/stream");
  const simulateEvent = require('simulate-event');

  require('jasmine-jquery');
  require('jasmine-ajax');

  const AuthConfigsWidget = require("views/auth_configs/auth_configs_widget");
  const PluginInfos       = require('models/shared/plugin_infos');
  const Modal             = require('views/shared/new_modal');

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(window.destroyDomElementForTest);

  const authConfigJSON = {
    "id":         "ldap",
    "plugin_id":  "cd.go.authorization.ldap",
    "properties": [
      {
        "key":   "Url",
        "value": "ldap://ldap.server"
      },
      {
        "key":   "ManagerDN",
        "value": "uid=admin,ou=system"
      }
    ]
  };

  const authConfigsJSON = {
    "_embedded": {
      "auth_configs": [authConfigJSON]
    }
  };

  const ldapPluginInfoJSON = {
    "id":                   "cd.go.authorization.ldap",
    "type":                 "authorization",
    "status": {
      "state": "active"
    },
    "about":                {
      "name":    "Ldap authorization plugin",
      "version": "1.x.x",
    },
    "capabilities":       {
      "can_verify_connection": true
    },
    "extension_info": {
      "auth_config_settings": {
        "configurations": [
          {
            "key":      "LdapUrl",
            "metadata": {
              "secure":   false,
              "required": true
            }
          }],
        "view":           {
          "template": '<div class="form_item_block">' +
                      '<label ng-class="{ \'is-invalid-label\': GOINPUTNAME[LdapUrl].$error.server}">LDAP URI</label>' +
                      '<input ng-class="{\'is-invalid-input\': GOINPUTNAME[LdapUrl].$error.server}" type="text" ng-model="LdapUrl"/> ' +
                      '<span class="form_error form-error" ng-class="{\'is-visible\': GOINPUTNAME[LdapUrl].$error.server}" ng-show="GOINPUTNAME[LdapUrl].$error.server">{{GOINPUTNAME[LdapUrl].$error.server}}</span> ' +
                      '</div>'
        }
      }
    }
  };

  const githubPluginInfoJSON = {
    "id":                   "cd.go.authorization.github",
    "type":                 "authorization",
    "status": {
      "state": "active"
    },
    "about":                {
      "name":    "GitHub authorization plugin",
      "version": "1.x.x",
    },
    "capabilities":       {
      "can_verify_connection": false
    },
    "extension_info": {
      "auth_config_settings": {
        "configurations": [],
        "view":           {
          "template": '<div class="form_item_block">' +
                      '<label ng-class="{ \'is-invalid-label\': GOINPUTNAME[GithubUrl].$error.server}">GITHUB URL</label>' +
                      '<input ng-class="{\'is-invalid-input\': GOINPUTNAME[GithubUrl].$error.server}" type="text" ng-model="GithubUrl"/> ' +
                      '<span class="form_error form-error" ng-class="{\'is-visible\': GOINPUTNAME[GithubUrl].$error.server}" ng-show="GOINPUTNAME[GithubUrl].$error.server">{{GOINPUTNAME[GithubUrl].$error.server}}</span> ' +
                      '</div>'
        }
      }
    }
  };

  const validationFailedJSON = () => ({
    "status":      "validation-failed",
    "message":     "Validation failed for the given Auth Config",
    "auth_config": {
      "id":         "test",
      "plugin_id":  "cd.go.authorization.ldap",
      "properties": [
        {
          "errors": {
            "LdapUrl": [
              "LdapUrl must not be blank."
            ]
          },
          "key":    "LdapUrl"
        }
      ]
    }
  });

  const responseJSON = () => ({
    "status":      "success",
    "message":     "Connection ok",
    "auth_config": authConfigJSON
  });

  const allPluginInfosJSON = [ldapPluginInfoJSON, githubPluginInfoJSON];

  beforeEach(() => {
    jasmine.Ajax.install();

    jasmine.Ajax.stubRequest(authConfigIndexUrl, undefined, 'GET').andReturn({
      responseText: JSON.stringify(authConfigsJSON),
      status:       200
    });

    m.mount(root, {
      view () {
        const fromJSON = PluginInfos.fromJSON(allPluginInfosJSON);
        return m(AuthConfigsWidget, {
          pluginInfos: Stream(fromJSON)
        });
      }
    });
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

      jasmine.Ajax.stubRequest(authConfigIndexUrl, undefined, 'GET').andReturn({
        responseText: JSON.stringify(authConfigsJSON),
        status:       200
      });

      m.mount(root, {
        view () {
          const fromJSON = PluginInfos.fromJSON([]);
          return m(AuthConfigsWidget, {
            pluginInfos: Stream(fromJSON)
          });
        }
      });
      m.redraw(true);
    });

    it("should disable add button if no authorization plugin installed", () => {
      jasmine.Ajax.stubRequest(authConfigIndexUrl, undefined, 'POST').andReturn({
        responseText: JSON.stringify({data: []}),
        status:       200
      });

      m.redraw();
      expect($root.find('.add-auth-config').get(0)).toBeDisabled();
    });

    it("should list existing auth-configs in absence of authorization-plugin with disabled edit and clone button", () => {
      const rows    = $root.find('.plugin-description');
      const actions = $root.find('.plugin-actions');

      expect(rows.eq(0).find('.auth-config-id .value').text()).toEqual(authConfigJSON.id);
      expect(rows.eq(0).find('.plugin-id .value').text()).toEqual(authConfigJSON.plugin_id);
      expect(actions.eq(0).find('.edit-auth-config').hasClass('disabled')).toEqual(true);
      expect(actions.eq(0).find('.clone-auth-config').hasClass('disabled')).toEqual(true);
    });
  });

  describe("list all auth configs", () => {

    it("should render a list of all auth configs", () => {
      expect($root.find('.auth-config-id')).toContainText(authConfigJSON.id);
      expect($root.find('.plugin-id')).toContainText(authConfigJSON.plugin_id);
    });

    it("should render error if index call fails", () => {
      jasmine.Ajax.stubRequest(authConfigIndexUrl).andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      m.mount(root, {
        view () {
          return m(AuthConfigsWidget);
        }
      });
      m.redraw();

      expect($root.find('.alert.callout')).toContainText('Boom!');
    });
  });

  describe("add a new auth config", () => {
    afterEach(Modal.destroyAll);

    it("should render new modal to create auth config", () => {
      expect($root.find('.reveal:visible')).not.toBeInDOM();
      simulateEvent.simulate($root.find('.add-auth-config').get(0), 'click');
      m.redraw();
      expect($('.reveal:visible')).toBeInDOM();
      expect($('.reveal:visible input[data-prop-name]')).not.toBeDisabled();
    });

    it('should show render modal and render auth config view for first plugin id', () => {
      simulateEvent.simulate($root.find('.add-auth-config').get(0), 'click');
      m.redraw();

      const pluginId = $('.reveal:visible .modal-body').find('[data-prop-name="pluginId"]').get(0);

      expect($(pluginId).val()).toEqual('cd.go.authorization.ldap');
    });

    it("should make request to save auth config on click of save button", () => {
      jasmine.Ajax.stubRequest(authConfigIndexUrl, undefined, 'POST').andReturn({
        responseText: JSON.stringify({data: authConfigJSON}),
        status:       200
      });

      simulateEvent.simulate($root.find('.add-auth-config').get(0), 'click');
      m.redraw();

      const authConfigId = $('.reveal:visible .modal-body').find('[data-prop-name="id"]').get(0);
      $(authConfigId).val("ldap");
      simulateEvent.simulate(authConfigId, 'input');

      const pluginId = $('.reveal:visible .modal-body').find('[data-prop-name="pluginId"]').get(0);
      $(pluginId).val(ldapPluginInfoJSON.id);
      simulateEvent.simulate(pluginId, 'change');

      m.redraw();
      simulateEvent.simulate($('.reveal:visible .modal-buttons').find('.save').get(0), 'click');

      m.redraw();
      expect($('.success')).toContainText('The auth config ldap was created successfully');
    });

    it("should display error message", () => {
      jasmine.Ajax.stubRequest(authConfigIndexUrl, undefined, 'POST').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      simulateEvent.simulate($root.find('.add-auth-config').get(0), 'click');
      m.redraw();
      const authConfigId = $('.reveal:visible .modal-body').find('[data-prop-name="id"]').get(0);
      $(authConfigId).val("ldap");
      simulateEvent.simulate(authConfigId, 'input');

      const pluginId = $('.reveal:visible .modal-body').find('[data-prop-name="pluginId"]').get(0);
      $(pluginId).val(ldapPluginInfoJSON.id);
      simulateEvent.simulate(pluginId, 'change');

      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .modal-buttons .save').get(0), 'click');
      expect($('.alert')).toContainText('Boom!');
    });

    it("should change auth config view in modal on change of plugin from dropdown", () => {
      simulateEvent.simulate($root.find('.add-auth-config').get(0), 'click');
      m.redraw();

      expect($('.reveal .modal-body input[data-prop-name]')).not.toBeDisabled();
      expect($('.reveal .modal-body [data-prop-name=pluginId] option:selected').text()).toEqual(ldapPluginInfoJSON.about.name);
      expect($('.reveal .modal-body .form_item_block label').text()).toEqual("LDAP URI");

      $('.reveal [data-prop-name=pluginId]').val("cd.go.authorization.github");
      simulateEvent.simulate($('.reveal [data-prop-name=pluginId]').get(0), 'change');
      m.redraw();

      expect($('.reveal input[data-prop-name]')).not.toBeDisabled();
      expect($('.reveal .modal-body [data-prop-name=pluginId] option:selected').text()).toEqual(githubPluginInfoJSON.about.name);
      expect($('.reveal .modal-body .form_item_block label').text()).toEqual("GITHUB URL");
    });

  });

  describe("edit an existing auth config", () => {
    afterEach(Modal.destroyAll);

    it("should render a new modal to edit existing auth config", () => {
      jasmine.Ajax.stubRequest(`/go/api/admin/security/auth_configs/${  authConfigJSON.id}`, undefined, 'GET').andReturn({
        responseText:    JSON.stringify(authConfigJSON),
        status:          200,
        responseHeaders: {
          'ETag': '"foo"'
        }
      });
      expect($root.find('.reveal:visible')).not.toBeInDOM();

      simulateEvent.simulate($root.find('.edit-auth-config').get(0), 'click');

      expect($('.reveal:visible')).toBeInDOM();
      expect($('.reveal:visible input[data-prop-name=id]')).toBeDisabled();
    });

    it("should display error message if fails to fetch auth config", () => {
      jasmine.Ajax.stubRequest(`/go/api/admin/security/auth_configs/${  authConfigJSON.id}`, undefined, 'GET').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      simulateEvent.simulate($root.find('.edit-auth-config').get(0), 'click');

      expect($('.alert')).toContainText('Boom!');
    });


    it("should keep the auth config expanded while edit modal is open", () => {
      jasmine.Ajax.stubRequest(`/go/api/admin/security/auth_configs/${  authConfigJSON.id}`, undefined, 'GET').andReturn({
        responseText: JSON.stringify(authConfigJSON),
        status:       200
      });

      expect($root.find('.plugin-config-read-only')).not.toHaveClass('show');
      simulateEvent.simulate($root.find('.auth-config-header').get(0), 'click');
      m.redraw();
      expect($root.find('.plugin-config-read-only')).toHaveClass('show');

      simulateEvent.simulate($root.find('.edit-auth-config').get(0), 'click');
      m.redraw();
      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .close-button span').get(0), 'click');
      m.redraw();
      expect($root.find('.plugin-config-read-only')).toHaveClass('show');
    });
  });

  describe("delete an existing auth config", () => {
    afterEach(Modal.destroyAll);

    it("should show confirm modal before deleting a auth config", () => {
      simulateEvent.simulate($root.find('.delete-auth-config-confirm').get(0), 'click');
      m.redraw();
      expect($('.reveal:visible .modal-title')).toHaveText('Are you sure?');
    });

    it("should show success message when auth config is deleted", () => {
      jasmine.Ajax.stubRequest(`/go/api/admin/security/auth_configs/${  authConfigJSON.id}`, undefined, 'DELETE').andReturn({
        responseText: JSON.stringify({message: 'Success!'}),
        status:       200
      });

      simulateEvent.simulate($root.find('.delete-auth-config-confirm').get(0), 'click');
      m.redraw();
      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .delete-auth-config').get(0), 'click');
      m.redraw();

      expect($('.success')).toContainText('Success!');
    });

    it("should show error message when deletion of auth config fails", () => {
      jasmine.Ajax.stubRequest(`/go/api/admin/security/auth_configs/${  authConfigJSON.id}`, undefined, 'DELETE').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      simulateEvent.simulate($root.find('.delete-auth-config-confirm').get(0), 'click');
      m.redraw();
      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .delete-auth-config').get(0), 'click');
      m.redraw();

      expect($('.alert')).toContainText('Boom!');
    });
  });

  describe("verify connection", () => {
    afterEach(Modal.destroyAll);

    it("should show Connection Ok message on successful verification.", () => {
      jasmine.Ajax.stubRequest('/go/api/admin/internal/security/auth_configs/verify_connection', undefined, 'POST').andReturn({
        responseText: JSON.stringify(responseJSON()),
        status:       200
      });

      simulateEvent.simulate($root.find('.add-auth-config').get(0), 'click');

      const authConfigId = $('.reveal:visible .modal-body').find('[data-prop-name="id"]').get(0);
      $(authConfigId).val("ldap");
      simulateEvent.simulate(authConfigId, 'input');
      simulateEvent.simulate($('.reveal:visible .modal-buttons .verify-connection').get(0), 'click');
      m.redraw();
      expect($('.reveal:visible .modal-body .callout.success')).toContainText('Connection OK.');

    });

    it("should show error message on unsuccessful verification.", () => {

      jasmine.Ajax.stubRequest('/go/api/admin/internal/security/auth_configs/verify_connection', undefined, 'POST').andReturn({
        responseText: JSON.stringify({data: authConfigJSON, message: "Unable to connect ldap server."}),
        status:       412
      });

      simulateEvent.simulate($root.find('.add-auth-config').get(0), 'click');

      const authConfigId = $('.reveal:visible .modal-body').find('[data-prop-name="id"]').get(0);
      $(authConfigId).val("ldap");
      simulateEvent.simulate(authConfigId, 'input');

      simulateEvent.simulate($('.reveal:visible .modal-buttons .verify-connection').get(0), 'click');
      m.redraw();
      expect($('.reveal:visible .modal-body .callout.alert').text()).toEqual('Unable to connect ldap server.');
    });

    it("should show field specific error message on validation failed.", () => {
      jasmine.Ajax.stubRequest('/go/api/admin/internal/security/auth_configs/verify_connection', undefined, 'POST').andReturn({
        responseText: JSON.stringify(validationFailedJSON()),
        status:       422
      });

      simulateEvent.simulate($root.find('.add-auth-config').get(0), 'click');

      const authConfigId = $('.reveal:visible .modal-body').find('[data-prop-name="id"]').get(0);
      $(authConfigId).val("ldap");
      simulateEvent.simulate(authConfigId, 'input');

      simulateEvent.simulate($('.reveal:visible .modal-buttons .verify-connection').get(0), 'click');
      m.redraw();

      expect($('.reveal:visible .modal-body .callout.alert').text()).toEqual('Validation failed for the given Auth Config');
      expect($('.reveal:visible .modal-body .form_item_block span').text()).toEqual('LdapUrl must not be blank.');
    });

    it("should show and remove error message from field on verify connection based on response.", () => {
      jasmine.Ajax.stubRequest('/go/api/admin/internal/security/auth_configs/verify_connection', undefined, 'POST').andReturn({
        responseText: JSON.stringify(validationFailedJSON()),
        status:       422
      });

      simulateEvent.simulate($root.find('.add-auth-config').get(0), 'click');
      simulateEvent.simulate($('.reveal:visible .modal-buttons .verify-connection').get(0), 'click');
      m.redraw();

      expect($('.reveal:visible .modal-body .callout.alert').text()).toEqual('Validation failed for the given Auth Config');
      expect($('.reveal:visible .modal-body .form_item_block span').text()).toEqual('LdapUrl must not be blank.');

      jasmine.Ajax.stubRequest('/go/api/admin/internal/security/auth_configs/verify_connection', undefined, 'POST').andReturn({
        responseText: JSON.stringify(responseJSON()),
        status:       200
      });

      simulateEvent.simulate($('.reveal:visible .modal-buttons .verify-connection').get(0), 'click');
      expect($('.reveal:visible .modal-body .form_item_block span').text()).toEqual('');

    });
  });

  describe("Clone an existing auth config", () => {
    afterEach(Modal.destroyAll);

    it("should show modal with data from cloning auth config", () => {
      jasmine.Ajax.stubRequest(`/go/api/admin/security/auth_configs/${  authConfigJSON.id}`, undefined, 'GET').andReturn({
        responseText: JSON.stringify(authConfigJSON),
        status:       200
      });

      expect($root.find('.reveal:visible')).not.toBeInDOM();

      simulateEvent.simulate($root.find('.clone-auth-config').get(0), 'click');

      expect($('.reveal:visible')).toBeInDOM();
      expect($('.reveal:visible input[data-prop-name]')).not.toBeDisabled();
    });

  });
});
