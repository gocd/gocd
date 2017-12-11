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

const $                     = require("jquery");
const _                     = require("lodash");
const m                     = require("mithril");
const simulateEvent         = require('simulate-event');
const TemplatesConfigWidget = require("views/template_configs/templates_config_widget");

describe("TemplatesConfigWidget", () => {
  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  const templateJSON = {
    "name":      "scratch",
    "_links":    {
      "self": {
        "href": "https://ci.example.com/api/admin/templates/scratch"
      }
    },
    "_embedded": {
      "pipelines": [
        {
          "name":   "up42",
          "_links": {
            "self": {
              "href": "https://ci.example.com/api/admin/pipelines/up42"
            }
          }
        }
      ]
    }
  };

  const unusedTemplateJSON = {
    "name":      "scratch2",
    "_links":    {
      "self": {
        "href": "https://ci.example.com/api/admin/templates/scratch2"
      }
    },
    "_embedded": {
      "pipelines": []
    }
  };

  const allTemplatesJSON = {
    "_embedded": {
      "templates": [templateJSON, unusedTemplateJSON]
    }
  };

  const removeModal = function () {
    $('.new-modal-container').each((_i, elem) => {
      _.each($(elem).data('modals'), (modal) => {
        modal.destroy();
      });
    });
  };

  beforeEach(() => {
    jasmine.Ajax.install();
    jasmine.Ajax.stubRequest('/go/api/admin/templates', undefined, 'GET').andReturn({
      responseText: JSON.stringify(allTemplatesJSON),
      status:       200
    });

    m.mount(root, {
      view () {
        return m(TemplatesConfigWidget);
      }
    });
    m.redraw();
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();

    m.mount(root, null);
    m.redraw();
  });

  describe("list all templates", () => {
    it("should render a list of all templates", () => {
      expect($root.find('#template-link-header')).toContainText(templateJSON.name);
    });

    it("should render error if index call fails", () => {
      jasmine.Ajax.stubRequest('/go/api/admin/templates').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       401
      });

      m.mount(root, {
        view () {
          return m(TemplatesConfigWidget);
        }
      });
      m.redraw();

      expect($root.find('.alert.callout')).toContainText('Boom!');
    });
  });

  describe("Add new Template", () => {
    it('should redirect to add template page', () => {
      spyOn(m.route, 'set');

      $('.add-template').click();

      expect(m.route.set).toHaveBeenCalledWith('/create/new');
    });

    it('should allow admins only to add new template', () => {
      m.mount(root, {
        view () {
          return m(TemplatesConfigWidget, {isUserAdmin: true});
        }
      });
      m.redraw();

      expect($('.add-template')).not.toBeDisabled();
    });

    it('should not allow admins only to add new template', () => {
      m.mount(root, {
        view () {
          return m(TemplatesConfigWidget, {isUserAdmin: false});
        }
      });
      m.redraw();

      expect($('.add-template')).toBeDisabled();
    });

  });

  describe("Edit Template", () => {
    it('should redirect to edit template page', () => {
      spyOn(m.route, 'set');

      $('.edit-template').click();

      expect(m.route.set).toHaveBeenCalledWith('/scratch');
    });
  });

  describe("Delete Template", () => {
    afterEach(removeModal);

    it("should show confirm modal when deleting a profile", () => {
      simulateEvent.simulate($('.delete-template-confirm').get(0), 'click');
      m.redraw();
      expect($('.reveal:visible .modal-title')).toHaveText('Are you sure?');
    });

    it('should allow admins to delete a template', () => {
      m.mount(root, {
        view () {
          return m(TemplatesConfigWidget, {isUserAdmin: true});
        }
      });
      m.redraw();
      expect($('.delete-template-confirm')[1]).not.toHaveClass('disabled');
    });

    it('should not allow non admins users to delete a template', () => {
      m.mount(root, {
        view () {
          return m(TemplatesConfigWidget, {isUserAdmin: false});
        }
      });
      m.redraw();
      expect($('.delete-template-confirm')[1]).toHaveClass('disabled');
    });

    it('should not allow admins to delete a template if its used in a pipeline', () => {
      m.mount(root, {
        view () {
          return m(TemplatesConfigWidget, {isUserAdmin: true});
        }
      });
      m.redraw();
      expect($('.delete-template-confirm')[0]).toHaveClass('disabled');
    });

    it("should show success message when template is deleted", () => {
      jasmine.Ajax.stubRequest(`/go/api/admin/templates/${  templateJSON.name}`, undefined, 'DELETE').andReturn({
        responseText: JSON.stringify({message: 'Success!'}),
        status:       200
      });

      $('.delete-template-confirm').click();
      m.redraw();
      $('.reveal:visible .delete-template').click();
      m.redraw();

      expect($('.success')).toContainText('Success!');
    });

    it("should show error message when deleting template fails", () => {
      jasmine.Ajax.stubRequest(`/go/api/admin/templates/${  templateJSON.name}`, undefined, 'DELETE').andReturn({
        responseText: JSON.stringify({message: 'Boom!'}),
        status:       422
      });

      simulateEvent.simulate($('.delete-template-confirm').get(0), 'click');
      m.redraw();
      simulateEvent.simulate($('.delete-template').get(0), 'click');
      m.redraw();

      expect($('.alert')).toContainText('Boom!');
    });
  });

  describe("Pipeline Information", () => {
    it('should show the information of pipelines which uses current template', () => {
      expect($('.exp-col-body')[0]).toHaveClass('hide');
      $('#template-link-header').click();
      m.redraw();
      expect($('.exp-col-body')[0]).toHaveClass('show');

      expect($('.exp-col-body')[0]).toContainText('up42');
    });

    it('should show information message for no associated pipelines', () => {
      expect($('.exp-col-body')[1]).toHaveClass('hide');
      $('#template-link-header').click();
      m.redraw();

      const message = "No pipelines are associated with this template";
      expect($('.exp-col-body')[1]).toContainText(message);
    });

    it('should toggle pipeline information view on clicks', () => {
      expect($('.exp-col-body')).toHaveClass('hide');
      $('#template-link-header').click();
      m.redraw();
      expect($('.exp-col-body')).toHaveClass('show');
      $('#template-link-header').click();
      m.redraw();
      expect($('.exp-col-body')).toHaveClass('hide');
    });
  });
});
