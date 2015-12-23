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

define(["mithril", "lodash", "pipeline_configs/models/materials", "pipeline_configs/views/test_connection_widget"], function (m, _, Materials, TestConnectionWidget) {
  describe("Test Connection Widget", function () {
    describe('view', function() {
      var $root, root, material;
      beforeEach(function () {
        material = new Materials().createMaterial({
          type: 'git',
          url: "http://git.example.com/git/myProject"
        });

        root = document.createElement("div");
        document.body.appendChild(root);
        $root = $(root);
      });

      afterEach(function () {
        root.parentNode.removeChild(root);
      });

      it("should render test connection button", function () {
        mount(material);

        expect($root.find("a.button")[0].innerText).toBe('Test Connection');
      });

      it("should render with test connection failure message", function () {
        var state = new TestConnectionWidget.Connection.State();
        state.status('Error');
        state.errorMessage('Test Connection Failed');

        mount(material, {connectionState: state});

        expect($root.find(".alert-box")[0].innerText).toBe('Test Connection Failed');
      });

      function mount(material, vm) {
        m.mount(root,
          m.component(TestConnectionWidget, {material: material, pipelineName: 'testPipeLine', vm: vm})
        );
        m.redraw(true);
      };
    });


    describe('Controller testConnection', function(){
      var vm, controller, deferred, pipelineName, material;

      beforeEach(function(){
        material = new Materials().createMaterial({
          type: 'git',
          url: "http://git.example.com/git/myProject"
        });
        pipelineName = m.prop('testPipeLine');
        deferred = $.Deferred();
        vm = new TestConnectionWidget.Connection.State();
        controller = new TestConnectionWidget.controller({material: material, pipelineName: pipelineName, vm: {connectionState: vm}});

        spyOn(TestConnectionWidget.Connection, 'test').and.callFake(function(){
          return deferred.promise();
        })
      });

      it('should mark connection state to in progress', function(){
        controller.testConnection(material, m.prop('testPipeline'));

        expect(vm.status()).toBe('InProgress');
      });

      it('should mark connection state to success if test passes', function() {
        controller.testConnection(material, m.prop('testPipeline'));
        deferred.resolve();

        expect(TestConnectionWidget.Connection.test).toHaveBeenCalledWith(material, pipelineName);
        expect(vm.status()).toBe('Success');
      });

      it('should mark connection state to error if test fails', function() {
        controller.testConnection(material, m.prop('testPipeline'));
        deferred.reject();

        expect(vm.status()).toBe('Error');
      });
    });

    describe('Connection Test', function() {
      var requestArgs, material;

      beforeEach(function(){
        material = new Materials().createMaterial({
          type: 'git',
          url: "http://git.example.com/git/myProject"
        });

        spyOn(m, 'request');
        TestConnectionWidget.Connection.test(material, m.prop('testPipeline'));
        requestArgs = m.request.calls.mostRecent().args[0]
      });

      describe('post', function(){
        it('should post to material_test url', function () {
          expect(requestArgs.method).toBe('POST');
          expect(requestArgs.url).toBe('/go/api/material_test');
        });

        it('should post required headers', function () {
          var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);
          requestArgs.config(xhr);

          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
        });

        it('should post the material for test connection', function(){
          var payload = _.merge(material.toJSON(), {pipeline_name: 'testPipeline'});

          expect(JSON.stringify(requestArgs.data)).toBe(JSON.stringify(payload));
        });

        it('should return test connection failure message', function () {
          var errorMessage = "Failed to find 'hg' on your PATH";

          expect(requestArgs.unwrapError({message: errorMessage})).toBe(errorMessage);
        });

        it('should stringfy the request payload', function() {
          var payload = {'keyOne': 'value'};

          spyOn(JSON, 'stringify').and.callThrough();

          expect(requestArgs.serialize(payload)).toBe(JSON.stringify({ key_one: 'value' }));
          expect(JSON.stringify).toHaveBeenCalled();
        })
      });
    });
  });
});
