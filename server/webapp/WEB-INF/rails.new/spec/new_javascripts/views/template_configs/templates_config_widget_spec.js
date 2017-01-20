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

define(["jquery", "mithril", "views/template_configs/templates_config_widget"], function ($, m, TemplatesConfigWidget) {

  describe("TemplatesConfigWidget", function () {
    var $root = $('#mithril-mount-point'), root = $root.get(0);

    var templateJSON = {
      "name":         "scratch",
      "_links": {
        "self": {
          "href": "https://ci.example.com/api/admin/templates/scratch"
        }
      },
      "_embedded": {
        "pipelines": [
          {
            "name" : "up42",
            "_links": {
              "self": {
                "href": "https://ci.example.com/api/admin/pipelines/up42"
              }
            }
          }
        ]
      }
    };

    var unusedTemplateJSON = {
      "name":         "scratch2",
      "_links": {
        "self": {
          "href": "https://ci.example.com/api/admin/templates/scratch2"
        }
      },
      "_embedded": {
        "pipelines": [
        ]
      }
    };

    var allTemplatesJSON = {
      "_embedded": {
        "templates": [templateJSON, unusedTemplateJSON]
      }
    };

    var removeModal = function () {
      $('.modal-parent').each(function (_i, elem) {
        $(elem).data('modal').destroy();
      });
    };

    beforeEach(function () {
      jasmine.Ajax.install();
      jasmine.Ajax.stubRequest('/go/api/admin/templates', undefined, 'GET').andReturn({
        responseText: JSON.stringify(allTemplatesJSON),
        status:       200
      });

      m.mount(root, m.component(TemplatesConfigWidget));
      m.redraw(true);
    });

    afterEach(function () {
      jasmine.Ajax.uninstall();

      m.mount(root, null);
      m.redraw(true);
    });

    describe("list all templates", function () {
      it("should render a list of all templates", function () {
        expect($root.find('#template-link-header')).toContainText(templateJSON.name);
      });

      it("should render error if index call fails", function () {
        jasmine.Ajax.stubRequest('/go/api/admin/templates').andReturn({
          responseText: JSON.stringify({message: 'Boom!'}),
          status:       401
        });

        m.mount(root, m.component(TemplatesConfigWidget));
        m.redraw(true);

        expect($root.find('.alert.callout')).toContainText('Boom!');
      });
    });

    describe("Add new Template", function () {
      it('should redirect to add template page', function () {
        spyOn(m, 'route');

        $('.add-template').click();

        expect(m.route).toHaveBeenCalledWith('/:new');
      });

      it('should allow admins only to add new template', function () {
        m.mount(root, m.component(TemplatesConfigWidget, {isUserAdmin: true}));
        m.redraw(true);
        expect($('.add-template')).not.toBeDisabled();
      });

      it('should not allow admins only to add new template', function () {
        m.mount(root, m.component(TemplatesConfigWidget, {isUserAdmin: false}));
        m.redraw(true);
        expect($('.add-template')).toBeDisabled();
      });

    });

    describe("Edit Template", function () {
      it('should redirect to edit template page', function () {
        spyOn(m, 'route');

        $('.edit-template').click();

        expect(m.route).toHaveBeenCalledWith('/scratch');
      });
    });

    describe("Delete Template", function () {
      afterEach(removeModal);

      it("should show confirm modal when deleting a profile", function () {
        $('.delete-template-confirm')[0].click();
        m.redraw(true);
        expect($('.reveal:visible .modal-title')).toHaveText('Are you sure?');
      });

      it('should allow admins to delete a template', function () {
        m.mount(root, m.component(TemplatesConfigWidget, {isUserAdmin: true}));
        m.redraw(true);
        expect($('.delete-template-confirm')[1]).not.toHaveClass('disabled');
      });

      it('should not allow non admins users to delete a template', function () {
        m.mount(root, m.component(TemplatesConfigWidget, {isUserAdmin: false}));
        m.redraw(true);
        expect($('.delete-template-confirm')[1]).toHaveClass('disabled');
      });

      it('should not allow admins to delete a template if its used in a pipeline', function () {
        m.mount(root, m.component(TemplatesConfigWidget, {isUserAdmin: true}));
        m.redraw(true);
        expect($('.delete-template-confirm')[0]).toHaveClass('disabled');
      });

      it("should show success message when template is deleted", function () {
        jasmine.Ajax.stubRequest('/go/api/admin/templates/' + templateJSON.name, undefined, 'DELETE').andReturn({
          responseText: JSON.stringify({message: 'Success!'}),
          status:       200
        });

        $('.delete-template-confirm').click();
        m.redraw(true);
        $('.reveal:visible .delete-template').click();
        m.redraw(true);

        expect($('.success')).toContainText('Success!');
      });

      it("should show error message when deleting template fails", function () {
        jasmine.Ajax.stubRequest('/go/api/admin/templates/' + templateJSON.name, undefined, 'DELETE').andReturn({
          responseText: JSON.stringify({message: 'Boom!'}),
          status:       401
        });

        $('.delete-template-confirm').click();
        m.redraw(true);
        $('.reveal:visible .delete-template').click();
        m.redraw(true);

        expect($('.alert')).toContainText('Boom!');
      });
    });

    describe("Pipeline Information", function () {
      it('should show the information of pipelines which uses current template', function () {
        expect($('.exp-col-body')[0]).toHaveClass('hide');
        $('#template-link-header').click();
        m.redraw(true);
        expect($('.exp-col-body')[0]).toHaveClass('show');

        expect($('.exp-col-body')[0]).toContainText('up42');
      });
	
	    it('should show information message for no associated pipelines', function () {
		    expect($('.exp-col-body')[1]).toHaveClass('hide');
		    $('#template-link-header').click();
		    m.redraw(true);
		    
		    var message = "No pipelines are associated with this template";
		    expect($('.exp-col-body')[1]).toContainText(message);
	    });

      it('should toggle pipeline information view on clicks', function () {
        expect($('.exp-col-body')).toHaveClass('hide');
        $('#template-link-header').click();
        m.redraw(true);
        expect($('.exp-col-body')).toHaveClass('show');
        $('#template-link-header').click();
        m.redraw(true);
        expect($('.exp-col-body')).toHaveClass('hide');
      });
    });

  });
});