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

describe("Lookup Command Widget", () => {
  const $ = require("jquery");
  const m = require("mithril");

  require('jasmine-jquery');

  const LookupCommandWidget = require("views/pipeline_configs/lookup_command_widget");
  const Tasks               = require("models/pipeline_configs/tasks");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  function mount(model, snippet) {
    m.mount(root, {
      view() {
        return m(LookupCommandWidget, {model, snippet});
      }
    });
    m.redraw();
  }

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };

  describe("view", () => {
    let model, snippet, enableTextComplete;

    beforeEach(() => {
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

    afterEach(() => {
      unmount();
    });

    it("should render a text box with textcomplete enabled", () => {
      expect($root.find("input[name=lookup]").size()).toBe(1);
      expect(enableTextComplete).toHaveBeenCalled();
    });

    it("should render the command snippet title", () => {
      expect($root.find(".snippet>header>h5.snippet-title")).toHaveText(snippet.name());
    });

    it("should render snippet description with more info", () => {
      expect($root.find(".snippet>p")).toHaveText(`${snippet.description()}more info`);
      expect($root.find(".snippet>p>a").attr('href')).toBe(snippet.moreInfo());
    });

    it("should render the snippet author info", () => {
      expect($root.find(".snippet>header>div.author>a")).toHaveText(snippet.author());
      expect($root.find(".snippet>header>div.author>a").attr('href')).toBe(snippet.authorInfo());
    });
  });

  describe('controller', () => {
    describe('selectSnippet', () => {
      let snippet;
      beforeEach(() => {
        snippet = new LookupCommandWidget.Command.Snippet({
          name:      'build',
          command:   'maven',
          arguments: ['clean', 'build']
        });
        spyOn(m, 'redraw');
      });

      it('should update task command and arguments', () => {
        const task       = new Tasks.Task.Exec({command: 'ls', arguments: ['-al']});
        const controller = new LookupCommandWidget.oninit({
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

    describe('searchSnippet', () => {
      let deferred;

      beforeEach(() => {
        deferred = $.Deferred();

        spyOn(LookupCommandWidget.Command, 'lookup').and.callFake(() => deferred.promise());
      });

      it('should lookup for command snippets for a search term', () => {
        const vnode   = {};
        vnode.attrs = {};
        new LookupCommandWidget.oninit(vnode).searchSnippets('scp');

        expect(LookupCommandWidget.Command.lookup).toHaveBeenCalledWith('scp');
      });

      it('should call the textcomplete callback with snippet names on success of lookup', () => {
        const build    = new LookupCommandWidget.Command.Snippet({name: 'build'});
        const rake     = new LookupCommandWidget.Command.Snippet({name: 'rake'});
        const callback = jasmine.createSpy('callback');

        const vnode   = {};
        vnode.attrs = {};
        new LookupCommandWidget.oninit(vnode).searchSnippets('scp', callback);
        deferred.resolve([build, rake]);

        expect(callback).toHaveBeenCalledWith(['build', 'rake']);
      });

      it('should call the textcomplete callback with empty array on failure of lookup', () => {
        const callback = jasmine.createSpy('callback');

        const vnode = {
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

  describe('Command.Snippets', () => {
    let snippets;
    beforeEach(() => {
      const build   = new LookupCommandWidget.Command.Snippet({name: 'build'});
      const rake    = new LookupCommandWidget.Command.Snippet({name: 'rake'});
      const ansible = new LookupCommandWidget.Command.Snippet({name: 'ansible'});
      const curl    = new LookupCommandWidget.Command.Snippet({name: 'curl'});
      snippets    = new LookupCommandWidget.Command.Snippets([build, rake, ansible, curl]);
    });

    describe('findByName', () => {
      it('should return a snippet matching the name', () => {
        const snippet = snippets.findByName('ansible');

        expect(snippet.name()).toBe('ansible');
      });

      describe('allNames', () => {
        it('should have names of all the snippets', () => {
          const names = snippets.allNames();

          expect(names).toEqual(['build', 'rake', 'ansible', 'curl']);
        });
      });
    });
  });

  describe('Command.lookup', () => {

    it('should get all the command snippets', () => {
      jasmine.Ajax.withMock(() => {
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

        const successCallback = jasmine.createSpy().and.callFake((snippets) => {
          expect(snippets[0].name()).toBe('foo');
        });
        LookupCommandWidget.Command.lookup('rake').then(successCallback);
        expect(successCallback).toHaveBeenCalled();
      });

    });
  });
});
