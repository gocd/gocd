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
describe("ElasticProfilesWidget", () => {

  var $             = require("jquery");
  var m             = require("mithril");
  var Stream        = require("mithril/stream");
  var simulateEvent = require('simulate-event');

  require('jasmine-jquery');
  require('jasmine-ajax');

  var ElasticProfilesWidget = require("views/elastic_profiles/elastic_profiles_widget");
  var PluginInfos           = require('models/shared/plugin_infos');
  var Modal                 = require('views/shared/new_modal');

  var $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  var profileJSON = {
    "id":         "unit-tests",
    "plugin_id":  "cd.go.contrib.elastic-agent.docker",
    "properties": [
      {
        "key":   "Image",
        "value": "gocdcontrib/gocd-dev-build"
      },
      {
        "key":   "Environment",
        "value": "JAVA_HOME=/opt/java\nMAKE_OPTS=-j8"
      }
    ]
  };

  var allProfilesJSON = {
    "_embedded": {
      "profiles": [profileJSON]
    }
  };

  var dockerPluginInfoJSON = {
    "id":                          "cd.go.contrib.elastic-agent.docker",
    "name":                        "Docker Elastic Agent Plugin",
    "version":                     "0.5",
    "type":                        "elastic-agent",
    "profile_settings": {
      "configurations": [
        {
          "key":      "Image",
          "metadata": {
            "secure":   false,
            "required": true
          }
        },
        {
          "key":      "Command",
          "metadata": {
            "secure":   false,
            "required": false
          }
        },
        {
          "key":      "Environment",
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

  var allPluginInfosJSON = [dockerPluginInfoJSON];

  beforeEach(() => {
    jasmine.Ajax.install();
    jasmine.Ajax.stubRequest('/go/api/elastic/profiles', undefined, 'GET').andReturn({
      responseText: JSON.stringify(allProfilesJSON),
      status:       200
    });

    m.mount(root, {
      view: function () {
        var fromJSON = PluginInfos.fromJSON(allPluginInfosJSON);
        return m(ElasticProfilesWidget, {
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

  describe("list all profiles", () => {
    it("should render a list of all profiles", () => {
      expect($root.find('.profile-id')).toContainText(profileJSON.id);
      expect($root.find('.plugin-id')).toContainText(profileJSON.plugin_id);
    });

    it("should render error if index call fails", () => {
      jasmine.Ajax.stubRequest('/go/api/elastic/profiles').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      m.mount(root, {
        view: function () {
          return m(ElasticProfilesWidget);
        }
      });
      m.redraw();

      expect($root.find('.alert.callout')).toContainText('Boom!');
    });
  });

  describe("add a new profile", () => {
    afterEach(Modal.destroyAll);

    it("should popup a new modal to allow adding a profile", () => {
      expect($root.find('.reveal:visible')).not.toBeInDOM();
      simulateEvent.simulate($root.find('.add-profile').get(0), 'click');
      m.redraw();
      expect($('.reveal:visible')).toBeInDOM();
      expect($('.reveal:visible input[data-prop-name]')).not.toBeDisabled();
    });

    it("should allow saving a profile if save is successful", () => {
      simulateEvent.simulate($root.find('.add-profile').get(0), 'click');
      m.redraw();

      expect($('.reveal:visible .modal-buttons').find('.save')).toBeDisabled();

      var profileId = $('.reveal:visible .modal-body').find('[data-prop-name="id"]').get(0);
      $(profileId).val("unit-test");
      simulateEvent.simulate(profileId, 'input');

      var pluginId = $('.reveal:visible .modal-body').find('[data-prop-name="pluginId"]').get(0);
      $(pluginId).val(dockerPluginInfoJSON.id);
      simulateEvent.simulate(pluginId, 'input');

      m.redraw();

      expect($('.reveal:visible .modal-buttons').find('.save')).not.toBeDisabled();

      jasmine.Ajax.stubRequest('/go/api/elastic/profiles', undefined, 'POST').andReturn({
        responseText: JSON.stringify({data: profileJSON}),
        status:       200
      });

      simulateEvent.simulate($('.reveal:visible .modal-buttons').find('.save').get(0), 'click');

      m.redraw();

      var request = jasmine.Ajax.requests.at(jasmine.Ajax.requests.count() - 2);
      expect(request.url).toBe('/go/api/elastic/profiles');
      expect(request.method).toBe('POST');

      expect($('.success')).toContainText('The profile unit-test was created successfully');
    });

    it("should display error message", () => {
      simulateEvent.simulate($root.find('.add-profile').get(0), 'click');
      m.redraw();

      var profileId = $('.reveal:visible .modal-body').find('[data-prop-name="id"]').get(0);
      $(profileId).val("unit-test");
      simulateEvent.simulate(profileId, 'input');

      var pluginId = $('.reveal:visible .modal-body').find('[data-prop-name="pluginId"]').get(0);
      $(pluginId).val(dockerPluginInfoJSON.id);
      simulateEvent.simulate(pluginId, 'input');

      m.redraw();

      jasmine.Ajax.stubRequest('/go/api/elastic/profiles', undefined, 'POST').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .modal-buttons .save').get(0), 'click');
      m.redraw();

      var request = jasmine.Ajax.requests.at(jasmine.Ajax.requests.count() - 1);
      expect(request.url).toBe('/go/api/elastic/profiles');
      expect(request.method).toBe('POST');

      expect($('.alert')).toContainText('Boom!');
    });
  });

  describe("edit an existing profile", () => {
    afterEach(Modal.destroyAll);
    it("should popup a new modal to allow edditing a profile", () => {
      jasmine.Ajax.stubRequest('/go/api/elastic/profiles/' + profileJSON.id, undefined, 'GET').andReturn({
        responseText: JSON.stringify(profileJSON),
        status:       200
      });
      expect($root.find('.reveal:visible')).not.toBeInDOM();

      simulateEvent.simulate($root.find('.edit-profile').get(0), 'click');
      m.redraw();
      expect($('.reveal:visible')).toBeInDOM();
      expect($('.reveal:visible input[data-prop-name]')).toBeDisabled();
    });

    it("should display error message if fetching a profile fails", () => {
      jasmine.Ajax.stubRequest('/go/api/elastic/profiles/' + profileJSON.id, undefined, 'GET').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      simulateEvent.simulate($root.find('.edit-profile').get(0), 'click');
      m.redraw();

      expect($('.alert')).toContainText('Boom!');
    });


    it("should keep the profile expanded while edit modal is open", () => {
      jasmine.Ajax.stubRequest('/go/api/elastic/profiles/' + profileJSON.id, undefined, 'GET').andReturn({
        responseText: JSON.stringify(profileJSON),
        status:       200
      });

      expect($root.find('.plugin-config-read-only')).not.toHaveClass('show');
      simulateEvent.simulate($root.find('.elastic-profile-header').get(0), 'click');
      m.redraw();
      expect($root.find('.plugin-config-read-only')).toHaveClass('show');

      simulateEvent.simulate($root.find('.edit-profile').get(0), 'click');
      m.redraw();
      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .close-button span').get(0), 'click');
      m.redraw();
      expect($root.find('.plugin-config-read-only')).toHaveClass('show');
    });
  });

  describe("delete an existing profile", () => {
    afterEach(Modal.destroyAll);

    it("should show confirm modal when deleting a profile", () => {
      simulateEvent.simulate($root.find('.delete-profile-confirm').get(0), 'click');
      m.redraw();
      expect($('.reveal:visible .modal-title')).toHaveText('Are you sure?');
    });

    it("should show success message when profile is deleted", () => {
      jasmine.Ajax.stubRequest('/go/api/elastic/profiles/' + profileJSON.id, undefined, 'DELETE').andReturn({
        responseText: JSON.stringify({message: 'Success!'}),
        status:       200
      });

      simulateEvent.simulate($root.find('.delete-profile-confirm').get(0), 'click');
      m.redraw();
      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .delete-profile').get(0), 'click');
      m.redraw();

      expect($('.success')).toContainText('Success!');
    });

    it("should show error message when deleting profile fails", () => {
      jasmine.Ajax.stubRequest('/go/api/elastic/profiles/' + profileJSON.id, undefined, 'DELETE').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      simulateEvent.simulate($root.find('.delete-profile-confirm').get(0), 'click');
      m.redraw();
      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .delete-profile').get(0), 'click');
      m.redraw();

      expect($('.alert')).toContainText('Boom!');
    });
  });


});
