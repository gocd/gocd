/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {TestHelper} from "views/pages/spec/test_helper";
import {MaterialSearchResultsWidget} from "views/dashboard/trigger_with_options/material_search_results_widget";
import {timeFormatter} from "helpers/time_formatter";
import {Material} from "models/dashboard/material";
import m from "mithril";

describe("Dashboard Material Search Results Widget", () => {
  const helper                      = new TestHelper();

  let material;

  beforeEach(() => {
    material = new Material({});
    mount();
  });

  afterEach(unmount);

  it("should render results matching count message when search text is absent", () => {
    material.searchText('');
    material.searchResults(json);
    helper.redraw();
    const expectedMessage = 'Last 4 commits listed in chronological order';
    expect(helper.text('.commits .helper')).toContain(expectedMessage);
  });

  it("should render all matched material search revisions and no message", () => {
    material.searchText('some search text');
    material.searchResults(json);
    helper.redraw();
    expect(helper.qa('.commit_info li')).toHaveLength(4);
    expect(helper.q('.commits .helper')).toBeFalsy();
  });

  it("should render commit information", () => {
    material.searchResults(json);
    helper.redraw();

    expect(helper.text('.commit_info .rev')).toContain(json[0].revision);
    expect(helper.text('.commit_info .committer')).toContain(json[0].user);
    expect(helper.text('.commit_info .time')).toContain(timeFormatter.format(json[0].date));
    expect(helper.text('.commit_info .commit_message')).toContain(json[0].comment);
  });

  it("should render no revisions found message", () => {
    material.searchText('foo');
    material.searchResults([]);
    helper.redraw();
    const expectedMessage = `No revisions found matching 'foo'`;
    expect(helper.text('.commits .helper')).toContain(expectedMessage);
  });

  it("should not render view when search is in progress", () => {
    material.searchInProgress(true);
    helper.redraw();
    expect(helper.q('.commits')).toBeFalsy();
  });

  it("should select searched revision onclick", (done) => {
    material.searchText('implemented');
    material.searchResults(json);

    helper.redraw();

    helper.click('.commit_info li:nth-child(2)');

    expect(material.selection()).toBe(json[1].revision);
    expect(material.searchText()).toBe(json[1].revision);

    //timeout is required to wait for the debounced search
    setTimeout(done, 250);
  });

  const json = [
    {
      "revision": "2a4b782a3a7d2eb13868da75149e716b15f52e5d",
      "user":     "GaneshSPatil <ganeshpl@thoughtworks.com>",
      "date":     "2018-02-12T11:02:48Z",
      "comment":  "implemented feature boo"
    },
    {
      "revision": "7f7653464e14682c7c9ce6a8bf85989a9a52eb35",
      "user":     "GaneshSPatil <ganeshpl@thoughtworks.com>",
      "date":     "2018-02-12T11:01:53Z",
      "comment":  "implemented feature boo"
    },
    {
      "revision": "24d682d8b8a99e8862acac8cae092caeca3a51f3",
      "user":     "GaneshSPatil <ganeshpl@thoughtworks.com>",
      "date":     "2018-02-12T11:01:36Z",
      "comment":  "implemented feature baz"
    },
    {
      "revision": "e5b730abdf7954e7ff45a4c15b2333c550559b35",
      "user":     "GaneshSPatil <ganeshpl@thoughtworks.com>",
      "date":     "2018-02-12T11:01:12Z",
      "comment":  "implemented feature bar"
    }
  ];

  function mount() {
    jasmine.Ajax.install();
    helper.mount(() => m(MaterialSearchResultsWidget, {
      material
    }));
  }

  function unmount() {
    helper.unmount();
    jasmine.Ajax.uninstall();
  }
});
