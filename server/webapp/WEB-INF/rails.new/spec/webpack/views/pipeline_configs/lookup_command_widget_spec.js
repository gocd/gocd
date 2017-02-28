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
describe("Lookup Command Widget", function () {
  var $ = require("jquery");
  var m = require("mithril");

  require('jasmine-jquery');

  var LookupCommandWidget = require("views/pipeline_configs/lookup_command_widget");
  var Tasks               = require("models/pipeline_configs/tasks");

  var $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  function mount(model, snippet) {
    m.mount(root, {
      view: function () {
        return m(LookupCommandWidget, {model: model, snippet: snippet});
      }
    });
    m.redraw();
  }

  var unmount = function () {
    m.mount(root, null);
    m.redraw();
  };

  describe("view", function () {
    var model, snippet, enableTextComplete;

    beforeEach(function () {
      model   = new Tasks.Task.Exec({command: 'ls'});
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
      spyOn(LookupCommandWidget.Command, 'textComplete').and.callFake(enableTextComplete);

      mount(model, snippet);
    });

    afterEach(function () {
      unmount();
    });

    it("should render a text box with textcomplete enabled", function () {
      expect($root.find("input[name=lookup]").size()).toBe(1);
      expect(enableTextComplete).toHaveBeenCalled();
    });

    it("should render the command snippet title", function () {
      expect($root.find(".snippet>header>h5.snippet-title")).toHaveText(snippet.name());
    });

    it("should render snippet description with more info", function () {
      expect($root.find(".snippet>p")).toHaveText(snippet.description() + "more info");
      expect($root.find(".snippet>p>a").attr('href')).toBe(snippet.moreInfo());
    });

    it("should render the snippet author info", function () {
      expect($root.find(".snippet>header>div.author>a")).toHaveText(snippet.author());
      expect($root.find(".snippet>header>div.author>a").attr('href')).toBe(snippet.authorInfo());
    });
  });

  describe('controller', function () {
    describe('selectSnippet', function () {
      var snippet;
      beforeEach(function () {
        snippet = new LookupCommandWidget.Command.Snippet({
          name:      'build',
          command:   'maven',
          arguments: ['clean', 'build']
        });
        spyOn(m, 'redraw');
      });

      it('should update task command and arguments', function () {
        var task       = new Tasks.Task.Exec({command: 'ls', arguments: ['-al']});
        var controller = new LookupCommandWidget.oninit({
          attrs: {
            model:    task,
            snippets: new LookupCommandWidget.Command.Snippets([snippet])
          }
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
        var vnode   = {};
        vnode.attrs = {};
        new LookupCommandWidget.oninit(vnode).searchSnippets('scp');

        expect(LookupCommandWidget.Command.lookup).toHaveBeenCalledWith('scp');
      });

      it('should call the textcomplete callback with snippet names on success of lookup', function () {
        var build    = new LookupCommandWidget.Command.Snippet({name: 'build'});
        var rake     = new LookupCommandWidget.Command.Snippet({name: 'rake'});
        var callback = jasmine.createSpy('callback');

        var vnode   = {};
        vnode.attrs = {};
        new LookupCommandWidget.oninit(vnode).searchSnippets('scp', callback);
        deferred.resolve([build, rake]);

        expect(callback).toHaveBeenCalledWith(['build', 'rake']);
      });

      it('should call the textcomplete callback with empty array on failure of lookup', function () {
        var callback = jasmine.createSpy('callback');

        var vnode = {
          attrs:    {},
          state:    {},
          children: []
        };
        new LookupCommandWidget.oninit(vnode).searchSnippets('scp', callback);
        deferred.reject();

        expect(callback).toHaveBeenCalledWith([]);
      });
    });
  });

  describe('Command.Snippets', function () {
    var snippets;
    beforeEach(function () {
      var build   = new LookupCommandWidget.Command.Snippet({name: 'build'});
      var rake    = new LookupCommandWidget.Command.Snippet({name: 'rake'});
      var ansible = new LookupCommandWidget.Command.Snippet({name: 'ansible'});
      var curl    = new LookupCommandWidget.Command.Snippet({name: 'curl'});
      snippets    = new LookupCommandWidget.Command.Snippets([build, rake, ansible, curl]);
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

    it('should get all the command snippets', function () {
      jasmine.Ajax.withMock(function () {
        jasmine.Ajax.stubRequest('/go/api/admin/internal/command_snippets?prefix=rake', undefined, 'GET').andReturn({
          responseText: JSON.stringify({
            _embedded: {
              "command_snippets": [
                {
                  name: 'foo'
                }
              ]
            }
          }),
          status:       200,
          headers:      {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          },
          contentType:  'appication/json'
        });

        var successCallback = jasmine.createSpy().and.callFake(function (snippets) {
          expect(snippets[0].name()).toBe('foo');
        });
        LookupCommandWidget.Command.lookup('rake').then(successCallback);
        expect(successCallback).toHaveBeenCalled();
      });

    });
  });
});
