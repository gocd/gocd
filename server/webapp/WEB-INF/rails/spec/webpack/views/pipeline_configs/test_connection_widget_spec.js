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
describe("Test Connection Widget", () => {

  const $             = require('jquery');
  const m             = require('mithril');
  const Stream        = require('mithril/stream');
  const simulateEvent = require('simulate-event');

  const Materials            = require("models/pipeline_configs/materials");
  const TestConnectionWidget = require("views/pipeline_configs/test_connection_widget");

  describe('view', () => {
    let $root, root;
    beforeEach(() => {
      [$root, root] = window.createDomElementForTest();
    });
    afterEach(window.destroyDomElementForTest);
    let material;
    beforeEach(() => {
      material = new Materials().createMaterial({
        type: 'git',
        url:  "http://git.example.com/git/myProject"
      });
    });

    afterEach(() => {
      m.mount(root, null);
      m.redraw();
    });


    it("should render test connection button", () => {
      mount(material);
      expect($($root.find(".test-connection")[0])).toHaveText('Test Connection');
    });

    it("should render with test connection failure message", () => {
      const state = new TestConnectionWidget.Connection.State();
      state.status('Error');
      state.errorMessage('Test Connection Failed');

      mount(material, {connectionState: state});

      expect($($root.find(".callout")[0])).toHaveText('Test Connection Failed');
    });

    it('should show the check connection result', () => {
      mount(material);

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/admin/internal/material_test', undefined, 'POST').andReturn({
          responseText: JSON.stringify({status: 'failure'}),
          status:       500,
          headers:      {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        simulateEvent.simulate($root.find('.test-connection').get(0), 'click');
        m.redraw();

        expect($('.alert').text()).toContain('There was an unknown error while checking connection');
      });
    });

    function mount(material, vm) {
      m.mount(root, {
        view() {
          return m(TestConnectionWidget, {material, pipelineName: Stream('testPipeLine'), vm});
        }
      });
      m.redraw();
    }
  });


  describe('Controller testConnection', () => {
    let vm, controller, deferred, pipelineName, material, vnode;

    beforeEach(() => {
      deferred     = $.Deferred();
      material     = new Materials().createMaterial({
        type: 'git',
        url:  "http://git.example.com/git/myProject"
      });
      pipelineName = Stream('testPipeLine');
      vm           = new TestConnectionWidget.Connection.State();

      vnode       = {};
      vnode.attrs = {
        material,
        pipelineName,
        vm: {connectionState: vm}
      };
      vnode.state = {};

      TestConnectionWidget.oninit(vnode);
      controller = vnode.state;

      spyOn(material, 'testConnection').and.callFake(() => deferred.promise());
    });

    afterEach(() => {
      vm = new TestConnectionWidget.Connection.State();
      vm.status('');
      vm.errorMessage('');
    });

    it('should mark connection state to in progress', () => {
      controller.testConnection();

      expect(vm.status()).toBe('InProgress');
    });

    it('should mark connection state to success if test passes', () => {
      controller.testConnection();
      deferred.resolve();

      expect(material.testConnection).toHaveBeenCalledWith(pipelineName);
      expect(vm.status()).toBe('Success');
    });

    it('should mark connection state to error if test fails', () => {
      controller.testConnection();
      deferred.reject();

      expect(vm.status()).toBe('Error');
    });
  });
});
