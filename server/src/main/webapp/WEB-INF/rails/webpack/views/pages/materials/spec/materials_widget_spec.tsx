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
import {docsUrl} from "gen/gocd_version";
import m from "mithril";
import Stream from "mithril/stream";
import {MaterialWithFingerprint, MaterialWithFingerprintJSON, MaterialWithFingerprints} from "models/materials/materials";
import {TestHelper} from "views/pages/spec/test_helper";
import styles from "../index.scss";
import {MaterialsWidget, MaterialWidget} from "../materials_widget";

describe('MaterialWidgetSpec', () => {
  const helper = new TestHelper();
  let material: MaterialWithFingerprint;

  afterEach((done) => helper.unmount(done));
  beforeEach(() => {
    material = MaterialWithFingerprint.fromJSON(git());
  });

  it('should display the name of the material with type', () => {
    mount();

    expect(helper.byTestId("material-type")).toBeInDOM();
    expect(helper.textByTestId("material-type")).toBe('Git');
    expect(helper.textByTestId("material-display-name")).toBe('some-name');
  });

  [
    {type: "git", classname: styles.git},
    {type: "hg", classname: styles.mercurial},
    {type: "p4", classname: styles.perforce},
    {type: "svn", classname: styles.subversion},
    {type: "tfs", classname: styles.tfs},
    {type: "dependency", classname: styles.unknown},
    {type: "package", classname: styles.package},
    {type: "plugin", classname: styles.unknown}
  ].forEach((parameter) => {
    it(`should display icon for ${parameter.type} `, () => {
      material.type(parameter.type);
      mount();

      expect(helper.byTestId("material-icon")).toHaveClass(parameter.classname);
    });
  });

  function mount() {
    helper.mount(() => <MaterialWidget material={material}/>);
  }

  function git() {
    return {
      type:        "git",
      fingerprint: "some-fingerprint",
      attributes:  {
        name:          "some-name",
        auto_update:   true,
        url:           "git@github.com:sample_repo/example.git",
        branch:        "master",
        shallow_clone: false
      }
    } as MaterialWithFingerprintJSON;
  }
});

describe('MaterialsWidgetSpec', () => {
  const helper = new TestHelper();
  let materials: MaterialWithFingerprints;

  afterEach((done) => helper.unmount(done));
  beforeEach(() => {
    materials = new MaterialWithFingerprints();
  });

  it('should display help text when no materials have been defined', () => {
    mount();

    const flashElement = helper.byTestId("flash-message-info");
    expect(flashElement).toBeInDOM();
    expect(flashElement.textContent).toContain('Either no pipelines have been set up or you are not authorized to view the same.');
    expect(helper.q('a', flashElement)).toHaveAttr('href', docsUrl('configuration/dev_authorization.html#specifying-permissions-for-pipeline-groups'));

    const helpElement = helper.byTestId("materials-help");
    expect(helpElement).toBeInDOM();
    expect(helpElement.textContent).toBe('A material is a cause for a pipeline to run. The GoCD Server continuously polls configured materials and when a new change or commit is found, the corresponding pipelines are run or "triggered". Learn More');
    expect(helper.q('a', helpElement)).toHaveAttr('href', docsUrl('introduction/concepts_in_go.html#materials'));
  });

  function mount() {
    helper.mount(() => <MaterialsWidget materials={Stream(materials)}/>);
  }
});
