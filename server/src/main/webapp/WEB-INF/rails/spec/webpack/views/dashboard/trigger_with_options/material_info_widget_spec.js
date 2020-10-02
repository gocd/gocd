/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import {TestHelper} from "../../../../../webpack/views/pages/spec/test_helper";
import {timeFormatter} from "helpers/time_formatter";
import {MaterialInfoWidget} from "views/dashboard/trigger_with_options/material_info_widget";
import {TriggerWithOptionsInfo} from "models/dashboard/trigger_with_options_info";
import m from "mithril";

describe("Dashboard Trigger With Options Material Info Widget", () => {


  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  let triggerWithOptionsInfo;

  beforeEach(() => {
    triggerWithOptionsInfo = TriggerWithOptionsInfo.fromJSON(json);
  });

  function mount(material) {
    material.performSearch         = jasmine.createSpy('performSearch');
    material.searchText            = jasmine.createSpy('searchText');
    material.searchInProgress      = jasmine.createSpy('searchInProgress');
    material.materialSearchResults = jasmine.createSpy('materialSearchResult');
    material.isRevisionSelected    = jasmine.createSpy('isRevisionSelected');
    material.materialSearchResults.and.returnValue(searchResults);
    helper.mount(() => m(MaterialInfoWidget, {
      material
    }));
  }

  it("it should render material info when revision is present", () => {
    mount(triggerWithOptionsInfo.materials[0]);
    const material = json.materials[0];

    expect(helper.q('.name-value .meta')).toContainText(material.type);

    expect(helper.q('.name-value .meta')).toContainText(material.name);

    expect(helper.q('.name-value .destination')).toContainText(material.folder);

    expect(helper.q('.name-value .date')).toContainText(timeFormatter.format(material.revision.date));

    expect(helper.q('.name-value .user')).toContainText(material.revision.user);

    expect(helper.q('.name-value .comment')).toContainText(material.revision.comment);

    expect(helper.q('.name-value .last-run-revision')).toContainText(material.revision.last_run_revision);
  });

  it("it should render material info when revision is not present", () => {
    mount(triggerWithOptionsInfo.materials[1]);
    const material = json.materials[1];

    expect(helper.q('.name-value .meta')).toContainText(material.type);
    expect(helper.q('.name-value .meta')).toContainText(material.name);

    expect(helper.q('.name-value .destination')).toContainText('not specified');

    expect(helper.q('.name-value .date')).toContainText('never ran');

    expect(helper.q('.name-value .user')).toContainText('never ran');

    expect(helper.q('.name-value .comment')).toContainText('never ran');

    expect(helper.q('.name-value .last-run-revision')).toContainText('never ran');
  });

  it('should render searched material revision spinner', () => {
    mount(triggerWithOptionsInfo.materials[0]);
    expect(helper.q('.commits')).toBeInDOM();
  });

  const json = {
    "variables": [],
    "materials": [
      {
        "type":        "Git",
        "name":        "material1",
        "folder":      "gocd",
        "fingerprint": "3dcc10e7943de637211a4742342fe456ffbe832577bb377173007499434fd819",
        "revision":    {
          "date":              "2018-02-08T04:32:11Z",
          "user":              "Ganesh S Patil <ganeshpl@thoughtworks.com>",
          "comment":           "Refactor Pipeline Widget (#4311)\n\n* Extract out PipelineHeaderWidget and PipelineOperationsWidget into seperate msx files",
          "last_run_revision": "a2d23c5505ac571d9512bdf08d6287e47dcb52d5"
        }
      },

      {
        "type":        "Git",
        "name":        "material2",
        "fingerprint": "3dcc10e7943de637211a4742342fe456ffbe832577bb377173007499434fd819",
        "revision":    {}
      },
      {
        "type":        "Git",
        "name":        "material2",
        "fingerprint": "3dcc10e7943de637211a4742342fe456ffbe832577bb377173007499434fd819",
        "revision":    {
          'some-junk': '123'
        }
      }
    ]
  };

  const searchResults = [
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
    },
    {
      "revision": "c30118c0a6e7e6042a50e2db1e191db081e915f0",
      "user":     "GaneshSPatil <ganeshpl@thoughtworks.com>",
      "date":     "2018-02-12T11:01:02Z",
      "comment":  "implemented feature foo"
    }
  ];

});
