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
describe("Test Connection Widget", function () {

  var $      = require('jquery');
  var m      = require('mithril');
  var Stream = require('mithril/stream');

  var Materials            = require("models/pipeline_configs/materials");
  var TestConnectionWidget = require("views/pipeline_configs/test_connection_widget");

  describe('view', function () {
    var $root, root;
    beforeEach(() => {
      [$root, root] = window.createDomElementForTest();
    });
    afterEach(window.destroyDomElementForTest);
    var material;
    beforeEach(function () {
      material = new Materials().createMaterial({
        type: 'git',
        url:  "http://git.example.com/git/myProject"
      });
    });

    afterEach(function () {
      m.mount(root, null);
      m.redraw();
    });


    it("should render test connection button", function () {
      mount(material);
      expect($($root.find(".test-connection")[0])).toHaveText('Test Connection');
    });

    it("should render with test connection failure message", function () {
      var state = new TestConnectionWidget.Connection.State();
      state.status('Error');
      state.errorMessage('Test Connection Failed');

      mount(material, {connectionState: state});

      expect($($root.find(".callout")[0])).toHaveText('Test Connection Failed');
    });

    function mount(material, vm) {
      m.mount(root, {
        view: function () {
          return m(TestConnectionWidget, {material: material, pipelineName: 'testPipeLine', vm: vm});
        }
      });
      m.redraw();
    }
  });


  describe('Controller testConnection', function () {
    var vm, controller, deferred, pipelineName, material, vnode;

    beforeEach(function () {
      deferred     = $.Deferred();
      material     = new Materials().createMaterial({
        type: 'git',
        url:  "http://git.example.com/git/myProject"
      });
      pipelineName = Stream('testPipeLine');
      vm           = new TestConnectionWidget.Connection.State();

      vnode       = {};
      vnode.attrs = {
        material:     material,
        pipelineName: pipelineName,
        vm:           {connectionState: vm}
      };
      vnode.state = {};

      TestConnectionWidget.oninit(vnode);
      controller = vnode.state;

      spyOn(material, 'testConnection').and.callFake(function () {
        return deferred.promise();
      });
    });

    afterEach(function () {
      vm = new TestConnectionWidget.Connection.State();
      vm.status('');
      vm.errorMessage('');
    });

    it('should mark connection state to in progress', function () {
      controller.testConnection();

      expect(vm.status()).toBe('InProgress');
    });

    it('should mark connection state to success if test passes', function () {
      controller.testConnection();
      deferred.resolve();

      expect(material.testConnection).toHaveBeenCalledWith(pipelineName);
      expect(vm.status()).toBe('Success');
    });

    it('should mark connection state to error if test fails', function () {
      controller.testConnection();
      deferred.reject();

      expect(vm.status()).toBe('Error');
    });
  });
});
