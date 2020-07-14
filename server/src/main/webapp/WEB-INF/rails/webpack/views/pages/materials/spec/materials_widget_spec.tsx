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
import m from "mithril";
import {MaterialWithFingerprint, MaterialWithFingerprintJSON} from "models/materials/materials";
import {TestHelper} from "views/pages/spec/test_helper";
import styles from "../index.scss";
import {MaterialWidget} from "../materials_widget";

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
