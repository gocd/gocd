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

define(["jquery", "mithril", "lodash", "views/pipeline_configs/lookup_command_widget", "models/pipeline_configs/tasks"], function ($, m, _, LookupCommandWidget, Tasks) {
  describe("Lookup Command Widget", function () {
    var $root = $('#mithril-mount-point'), root = $root.get(0);

    function mount(model, snippet) {
      m.mount(root,
        m.component(LookupCommandWidget, {model: model, snippet: snippet})
      );
      m.redraw(true);
    }

    describe("view", function() {
      var model, snippet, enableTextComplete;

      beforeAll(function(){
        model = new Tasks.Task.Exec({command: 'ls'});
        snippet = new LookupCommandWidget.Command.Snippet({
          /* eslint-disable camelcase */
          name:          'scp',
          description:   'scp desc',
          author:        'go team',
          author_info:   'tw',
          more_info:     'more',
          command:       'scp',
          arguments:     ['src', 'dest'],
          relative_path: '/build/scp.xml'
          /* eslint-enable camelcase */
        });

        enableTextComplete = jasmine.createSpy('enableTextComplete');

        spyOn(LookupCommandWidget.Command, 'textComplete').and.callFake(function () {
          return enableTextComplete;
        });

        mount(model, snippet);
      });

      it("should render a text box with textcomplete enabled", function () {
        expect($root.find("input[name=lookup]").size()).toBe(1);
        expect(enableTextComplete).toHaveBeenCalled();
      });

      it("should render the command snippet title", function () {
        expect($root.find(".snippet>header>h5.snippet-title").text()).toBe(snippet.name());
      });

      it("should render snippet description with more info", function () {
        expect($root.find(".snippet>p").text()).toBe(snippet.description() + "more info");
        expect($root.find(".snippet>p>a").attr('href')).toBe(snippet.moreInfo());
      });

      it("should render the snippet author info", function() {
        expect($root.find(".snippet>header>div.author>a").text()).toBe(snippet.author());
        expect($root.find(".snippet>header>div.author>a").attr('href')).toBe(snippet.authorInfo());
      });
    });

    describe('controller', function () {
      describe('selectSnippet', function () {
        var snippet;
        beforeEach(function(){
          snippet = new LookupCommandWidget.Command.Snippet({
            name:      'build',
            command:   'maven',
            arguments: ['clean', 'build']
          });
          spyOn(m, 'redraw');
        });

        it('should update task command and arguments', function () {
          var task = new Tasks.Task.Exec({command: 'ls', arguments: ['-al']});
          var controller = new LookupCommandWidget.controller({
            model:    task,
            snippets: new LookupCommandWidget.Command.Snippets([snippet])
          });

          controller.selectSnippet(null, 'build');

          expect(task.command()).toEqual(snippet.command());
          expect(task.args().data()).toEqual(snippet.arguments());
          expect(m.redraw).toHaveBeenCalled();
        });
      });

      describe('searchSnippet', function () {
        var deferred;

        beforeEach(function () {
          deferred = $.Deferred();

          spyOn(LookupCommandWidget.Command, 'lookup').and.callFake(function () {
            return deferred.promise();
          });
        });

        it('should lookup for command snippets for a search term', function () {
          new LookupCommandWidget.controller({}).searchSnippets('scp');

          expect(LookupCommandWidget.Command.lookup).toHaveBeenCalledWith('scp');
        });

        it('should call the textcomplete callback with snippet names on success of lookup', function () {
          var build = new LookupCommandWidget.Command.Snippet({name: 'build'});
          var rake = new LookupCommandWidget.Command.Snippet({name: 'rake'});
          var callback = jasmine.createSpy('callback');

          new LookupCommandWidget.controller({}).searchSnippets('scp', callback);
          deferred.resolve([build, rake]);

          expect(callback).toHaveBeenCalledWith(['build', 'rake']);
        });

        it('should call the textcomplete callback with empty array on failure of lookup', function () {
          var callback = jasmine.createSpy('callback');

          new LookupCommandWidget.controller({}).searchSnippets('scp', callback);
          deferred.reject();

          expect(callback).toHaveBeenCalledWith([]);
        });
      });
    });

    describe('Command.Snippets', function () {
      var snippets;
      beforeEach(function(){
        var build   = new LookupCommandWidget.Command.Snippet({name: 'build'});
        var rake    = new LookupCommandWidget.Command.Snippet({name: 'rake'});
        var ansible = new LookupCommandWidget.Command.Snippet({name: 'ansible'});
        var curl    = new LookupCommandWidget.Command.Snippet({name: 'curl'});
        snippets = new LookupCommandWidget.Command.Snippets([build, rake, ansible, curl]);
      });

      describe('findByName', function () {
        it('should return a snippet matching the name', function () {
          var snippet = snippets.findByName('ansible');

          expect(snippet.name()).toBe('ansible');
        });

        describe('allNames', function () {
          it('should have names of all the snippets', function () {
            var names = snippets.allNames();

            expect(names).toEqual(['build', 'rake', 'ansible', 'curl']);
          });
        });
      });
    });

    describe('Command.lookup', function () {
      var requestArgs;

      beforeEach(function () {
        spyOn(m, 'request');
        LookupCommandWidget.Command.lookup('rake');

        requestArgs = m.request.calls.mostRecent().args[0];
      });

      it('should get snippets from admin_command_snippets url', function () {
        expect(requestArgs.method).toBe('GET');
        expect(requestArgs.url).toBe('/go/api/admin/internal/command_snippets?prefix=rake');
      });

      it('should post required headers', function () {
        var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);
        requestArgs.config(xhr);

        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
      });

      it('should unwrap the response data to return list of snippets', function () {
        var snippets = {_embedded: {command_snippets: ['snippet']}}; // eslint-disable-line camelcase

        expect(requestArgs.unwrapSuccess(snippets)).toEqual(['snippet']);
      });
    });
  });
});
