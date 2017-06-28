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
describe("ElasticProfilesWidget", () => {

  const $             = require("jquery");
  const m             = require("mithril");
  const Stream        = require("mithril/stream");
  const simulateEvent = require('simulate-event');

  require('jasmine-jquery');
  require('jasmine-ajax');

  const ElasticProfilesWidget = require("views/elastic_profiles/elastic_profiles_widget");
  const PluginInfos           = require('models/shared/plugin_infos');
  const Modal                 = require('views/shared/new_modal');

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  const dockerElasticProfileJSON = {
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

  const ecsElasticProfileJSON = {
    "id":         "ecs-profile",
    "plugin_id":  "cd.go.contrib.elastic-agent.ecs",
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

  const allProfilesJSON = {
    "_embedded": {
      "profiles": [dockerElasticProfileJSON, ecsElasticProfileJSON]
    }
  };

  const dockerPluginInfoJSON = {
    "id":             "cd.go.contrib.elastic-agent.docker",
    "about":          {
      "name":    "Docker Elastic Agent Plugin",
      "version": "0.5"
    },
    "type":             "elastic-agent",
    "status": {
      "state": "active"
    },
    "extension_info": {
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
          "template": '<div><label class="docker-image">Docker image</label></div>'
        }
      }
    },
    "_links":         {
      "image": {
        "href": "http://docker-plugin-image-url"
      }
    }
  };

  const ecsPluginInfoJSON = {
    "id":             "cd.go.contrib.elastic-agent.ecs",
    "about":          {
      "name":    "ECS Elastic Agent Plugin",
      "version": "0.5"
    },
    "type":             "elastic-agent",
    "status": {
      "state": "active"
    },
    "extension_info": {
      "profile_settings": {
        "configurations": [],
        "view":           {
          "template": '<div><label class="ecs-ami">AMI</label></div>'
        }
      },
      "capabilities":     {
        "supports_status_report": true
      }
    },
    "_links":         {
      "image": {
        "href": "http://ecs-plugin-image-url"
      }
    }
  };

  const allPluginInfosJSON = [dockerPluginInfoJSON, ecsPluginInfoJSON];
  const allPluginInfos     = Stream(PluginInfos.fromJSON([]));

  beforeEach(() => {
    jasmine.Ajax.install();
    jasmine.Ajax.stubRequest('/go/api/elastic/profiles', undefined, 'GET').andReturn({
      responseText: JSON.stringify(allProfilesJSON),
      status:       200
    });

    allPluginInfos(PluginInfos.fromJSON(allPluginInfosJSON));

    m.mount(root, {
      view() {
        return m(ElasticProfilesWidget, {
          pluginInfos: allPluginInfos
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

  describe("no elastic agent plugin loaded", () => {
    beforeEach(() => {
      allPluginInfos(PluginInfos.fromJSON([]));
      m.redraw(true);
    });

    it("should disable add button if no elastic plugin installed", () => {
      expect($root.find('.add-profile').get(0)).toBeDisabled();
    });

    it("should list existing profiles in absence of elastic plugin", () => {
      expect($root.find('.elastic-profiles .callout').text()).toEqual("No elastic agent plugin installed.");
      expect($root.find('.profile-id .value').eq(0)).toContainText(dockerElasticProfileJSON.id);
      expect($root.find('.profile-id .value').eq(1)).toContainText(ecsElasticProfileJSON.id);
    });

  });

  describe("list all profiles", () => {
    it("should render all profiles and group it by plugin id", () => {
      const allProfiles = $root.find('.elastic-profile-plugin-group');

      expect(allProfiles.length).toEqual(2);

      expect(allProfiles.eq(0).find('.plugin-icon img').attr("src")).toEqual(dockerPluginInfoJSON._links.image.href);
      expect(allProfiles.eq(0).find('.elastic-profile-plugin-group-header .plugin-name')).toContainText(dockerPluginInfoJSON.about.name);
      expect(allProfiles.eq(0).find('.elastic-profile-plugin-group-header .plugin-id')).toContainText(dockerElasticProfileJSON.plugin_id);
      expect(allProfiles.eq(0).find('.elastic-profile-plugin-group-header').find('.plugin-status')).not.toBeInDOM();

      expect(allProfiles.eq(0).find('.elastic-profile-plugin-group-content .elastic-profile .profile-id')).toContainText(dockerElasticProfileJSON.id);
      expect(allProfiles.eq(0).find('.elastic-profile-plugin-group-content .elastic-profile .plugin-actions .edit-profile')).toBeInDOM();
      expect(allProfiles.eq(0).find('.elastic-profile-plugin-group-content .elastic-profile .plugin-actions .clone-profile')).toBeInDOM();
      expect(allProfiles.eq(0).find('.elastic-profile-plugin-group-content .elastic-profile .plugin-actions .delete-profile-confirm')).toBeInDOM();

      expect(allProfiles.eq(1).find('.plugin-icon img').attr("src")).toEqual(ecsPluginInfoJSON._links.image.href);
      expect(allProfiles.eq(1).find('.elastic-profile-plugin-group-header .plugin-name')).toContainText(ecsPluginInfoJSON.about.name);
      expect(allProfiles.eq(1).find('.elastic-profile-plugin-group-header .plugin-id')).toContainText(ecsElasticProfileJSON.plugin_id);
      expect(allProfiles.eq(1).find('.elastic-profile-plugin-group-header').find('.plugin-status').attr('href')).toEqual(`status_reports/${ecsElasticProfileJSON.plugin_id}`);
      expect(allProfiles.eq(1).find('.elastic-profile-plugin-group-header').find('.plugin-status')).toContainText('Status Report');

      expect(allProfiles.eq(1).find('.elastic-profile-plugin-group-content .elastic-profile .profile-id')).toContainText(ecsElasticProfileJSON.id);
      expect(allProfiles.eq(1).find('.elastic-profile-plugin-group-content .elastic-profile .plugin-actions .edit-profile')).toBeInDOM();
      expect(allProfiles.eq(1).find('.elastic-profile-plugin-group-content .elastic-profile .plugin-actions .clone-profile')).toBeInDOM();
      expect(allProfiles.eq(1).find('.elastic-profile-plugin-group-content .elastic-profile .plugin-actions .delete-profile-confirm')).toBeInDOM();
    });

    it("should render error if index call fails", () => {
      jasmine.Ajax.stubRequest('/go/api/elastic/profiles').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      m.mount(root, {
        view() {
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

    it('should show modal and render profile view of first plugin id', () => {
      simulateEvent.simulate($root.find('.add-profile').get(0), 'click');
      m.redraw();

      const pluginId = $('.reveal:visible .modal-body').find('[data-prop-name="pluginId"]').get(0);

      expect($(pluginId).val()).toEqual(dockerElasticProfileJSON.plugin_id);
    });

    it("should allow saving a profile if save is successful", () => {
      simulateEvent.simulate($root.find('.add-profile').get(0), 'click');
      m.redraw();

      const profileId = $('.reveal:visible .modal-body').find('[data-prop-name="id"]').get(0);
      $(profileId).val("unit-test");
      simulateEvent.simulate(profileId, 'input');

      const pluginId = $('.reveal:visible .modal-body').find('[data-prop-name="pluginId"]').get(0);
      $(pluginId).val(dockerPluginInfoJSON.id);
      simulateEvent.simulate(pluginId, 'input');

      m.redraw();

      jasmine.Ajax.stubRequest('/go/api/elastic/profiles', undefined, 'POST').andReturn({
        responseText: JSON.stringify({data: dockerElasticProfileJSON}),
        status:       200
      });

      simulateEvent.simulate($('.reveal:visible .modal-buttons').find('.save').get(0), 'click');

      m.redraw();

      const request = jasmine.Ajax.requests.at(jasmine.Ajax.requests.count() - 2);
      expect(request.url).toBe('/go/api/elastic/profiles');
      expect(request.method).toBe('POST');

      expect($('.success')).toContainText('The profile unit-test was created successfully');
    });

    it("should display error message", () => {
      simulateEvent.simulate($root.find('.add-profile').get(0), 'click');
      m.redraw();

      const profileId = $('.reveal:visible .modal-body').find('[data-prop-name="id"]').get(0);
      $(profileId).val("unit-test");
      simulateEvent.simulate(profileId, 'input');

      const pluginId = $('.reveal:visible .modal-body').find('[data-prop-name="pluginId"]').get(0);
      $(pluginId).val(dockerPluginInfoJSON.id);
      simulateEvent.simulate(pluginId, 'input');

      m.redraw();

      jasmine.Ajax.stubRequest('/go/api/elastic/profiles', undefined, 'POST').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .modal-buttons .save').get(0), 'click');
      m.redraw();

      const request = jasmine.Ajax.requests.at(jasmine.Ajax.requests.count() - 1);
      expect(request.url).toBe('/go/api/elastic/profiles');
      expect(request.method).toBe('POST');

      expect($('.alert')).toContainText('Boom!');
    });

    it("should change elastic profile view in modal on change of plugin from dropdown", () => {
      simulateEvent.simulate($root.find('.add-profile').get(0), 'click');
      m.redraw();

      expect($('.reveal .modal-body input[data-prop-name]')).not.toBeDisabled();
      expect($('.reveal .modal-body [data-prop-name=pluginId] option:selected').text()).toEqual(dockerPluginInfoJSON.about.name);
      expect($('.reveal .modal-body label.docker-image').text()).toEqual("Docker image");

      $('.reveal [data-prop-name=pluginId]').val("cd.go.contrib.elastic-agent.ecs");
      simulateEvent.simulate($('.reveal [data-prop-name=pluginId]').get(0), 'change');
      m.redraw();


      expect($('.reveal input[data-prop-name]')).not.toBeDisabled();
      expect($('.reveal .modal-body [data-prop-name=pluginId] option:selected').text()).toEqual(ecsPluginInfoJSON.about.name);
      expect($('.reveal .modal-body label.ecs-ami').text()).toEqual("AMI");
    });

  });

  describe("edit an existing profile", () => {
    afterEach(Modal.destroyAll);
    it("should popup a new modal to allow edditing a profile", () => {
      jasmine.Ajax.stubRequest(`/go/api/elastic/profiles/${dockerElasticProfileJSON.id}`, undefined, 'GET').andReturn({
        responseText:    JSON.stringify(dockerElasticProfileJSON),
        responseHeaders: {
          'ETag': '"foo"'
        },
        status:          200
      });
      expect($root.find('.reveal:visible')).not.toBeInDOM();

      simulateEvent.simulate($root.find('.edit-profile').get(0), 'click');
      m.redraw();
      expect($('.reveal:visible')).toBeInDOM();
      expect($('.reveal:visible input[data-prop-name]')).toBeDisabled();
    });

    it("should display error message if fetching a profile fails", () => {
      jasmine.Ajax.stubRequest(`/go/api/elastic/profiles/${dockerElasticProfileJSON.id}`, undefined, 'GET').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      simulateEvent.simulate($root.find('.edit-profile').get(0), 'click');
      m.redraw();

      expect($('.alert')).toContainText('Boom!');
    });


    it("should keep the profile expanded while edit modal is open", () => {
      jasmine.Ajax.stubRequest(`/go/api/elastic/profiles/${dockerElasticProfileJSON.id}`, undefined, 'GET').andReturn({
        responseText: JSON.stringify(dockerElasticProfileJSON),
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
      jasmine.Ajax.stubRequest(`/go/api/elastic/profiles/${dockerElasticProfileJSON.id}`, undefined, 'DELETE').andReturn({
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
      jasmine.Ajax.stubRequest(`/go/api/elastic/profiles/${dockerElasticProfileJSON.id}`, undefined, 'DELETE').andReturn({
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

  describe("Clone an existing profile", () => {
    afterEach(Modal.destroyAll);

    it("should show modal with profile daa", () => {
      jasmine.Ajax.stubRequest(`/go/api/elastic/profiles/${dockerElasticProfileJSON.id}`, undefined, 'GET').andReturn({
        responseText:    JSON.stringify(dockerElasticProfileJSON),
        status:          200,
        responseHeaders: {
          'ETag': '"foo"'
        }
      });
      expect($root.find('.reveal:visible')).not.toBeInDOM();

      simulateEvent.simulate($root.find('.clone-profile').get(0), 'click');

      m.redraw();
      expect($('.reveal:visible')).toBeInDOM();
      expect($('.reveal:visible input[data-prop-name]')).not.toBeDisabled();
    });

    it("should display error message if fetching a profile fails", () => {
      jasmine.Ajax.stubRequest(`/go/api/elastic/profiles/${dockerElasticProfileJSON.id}`, undefined, 'GET').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      simulateEvent.simulate($root.find('.clone-profile').get(0), 'click');
      m.redraw();

      expect($('.alert')).toContainText('Boom!');
    });

    it("should allow cloning a profile if save is successful", () => {
      jasmine.Ajax.stubRequest(`/go/api/elastic/profiles/${dockerElasticProfileJSON.id}`, undefined, 'GET').andReturn({
        responseText:    JSON.stringify(dockerElasticProfileJSON),
        status:          200,
        responseHeaders: {
          'ETag':         '"foo"',
          'Content-Type': 'application/json'
        }
      });

      simulateEvent.simulate($root.find('.clone-profile').get(0), 'click');
      m.redraw();

      const profileId = $('.reveal:visible .modal-body').find('[data-prop-name="id"]').get(0);
      $(profileId).val("foo-clone");
      simulateEvent.simulate(profileId, 'input');

      jasmine.Ajax.stubRequest('/go/api/elastic/profiles', undefined, 'POST').andReturn({
        responseText: JSON.stringify({data: dockerElasticProfileJSON}),
        status:       200
      });

      simulateEvent.simulate($('.reveal:visible .modal-buttons').find('.save').get(0), 'click');

      const request = jasmine.Ajax.requests.at(jasmine.Ajax.requests.count() - 2);
      expect(request.url).toBe('/go/api/elastic/profiles');
      expect(request.method).toBe('POST');

      expect($('.success')).toContainText('The profile foo-clone was cloned successfully');
    });

  });

});
