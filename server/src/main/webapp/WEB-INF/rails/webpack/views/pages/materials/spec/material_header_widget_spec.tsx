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

import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril";
import {MaterialModification} from "models/config_repos/types";
import {MaterialWithFingerprint, MaterialWithModification, P4MaterialAttributes, PackageMaterialAttributes, PluggableScmMaterialAttributes} from "models/materials/materials";
import headerStyles from "views/pages/config_repos/index.scss";
import {TestHelper} from "views/pages/spec/test_helper";
import styles from "../index.scss";
import {MaterialHeaderWidget} from "../material_header_widget";
import {git} from "./materials_widget_spec";

describe('MaterialHeaderWidgetSpec', () => {
  const helper = new TestHelper();
  let material: MaterialWithModification;

  afterEach((done) => helper.unmount(done));
  beforeEach(() => {
    material = new MaterialWithModification(MaterialWithFingerprint.fromJSON(git()));
  });

  it('should display the name of the material with attributes', () => {
    mount();

    expect(helper.byTestId("material-type")).toBeInDOM();
    expect(helper.textByTestId("material-type")).toBe('some-name');
    expect(helper.textByTestId("material-display-name")).toBe('git@github.com:sample_repo/example.git [ master ]');
  });

  it('should display the name as attributes if name is not provided', () => {
    material.config.attributes().name(undefined);
    mount();

    expect(helper.textByTestId("material-type")).toBe('git@github.com:sample_repo/example.git');
    expect(helper.textByTestId("material-display-name")).toBe('git@github.com:sample_repo/example.git [ master ]');
  });

  [
    {type: "git", classname: styles.git},
    {type: "hg", classname: styles.mercurial},
    {type: "svn", classname: styles.subversion},
    {type: "tfs", classname: styles.tfs},
    {type: "dependency", classname: styles.unknown},
  ].forEach((parameter) => {
    it(`should display icon for ${parameter.type} `, () => {
      material.config.type(parameter.type);
      mount();

      expect(helper.byTestId("material-icon")).toHaveClass(parameter.classname);
    });
  });

  [
    {type: "p4", classname: styles.perforce, attrs: new P4MaterialAttributes()},
    {type: "package", classname: styles.package, attrs: new PackageMaterialAttributes()},
    {type: "plugin", classname: styles.plugin, attrs: new PluggableScmMaterialAttributes(undefined, true, "", "")}
  ].forEach((parameter) => {
    it(`should display icon for ${parameter.type} `, () => {
      material.config.attributes(parameter.attrs);
      material.config.type(parameter.type);
      mount();

      expect(helper.byTestId("material-icon")).toHaveClass(parameter.classname);
    });
  });

  it('should show never parsed if no modifications are present', () => {
    mount();

    expect(helper.byTestId("latest-mod-in-header")).toBeInDOM();
    expect(helper.textByTestId("latest-mod-in-header")).toBe("This material was never parsed");
  });

  it('should show latest modification details', () => {
    material.modification
      = new MaterialModification("A_Long_username_with_a_long_long_long_long_long_text", null, "b07d423864ec120362b3584635cb07d423864ec120362b3584635c", "A very long comment to be shown on the header which should be trimmed and rest part should not be shown", "");
    mount();

    expect(helper.byTestId("latest-mod-in-header")).toBeInDOM();
    expect(helper.q(`.${headerStyles.comment}`).textContent).toBe("A very long comment to be shown on the header which should be trimmed and rest part sho...");
    expect(helper.q(`.${headerStyles.committerInfo}`).textContent).toBe("A_Long_username_with_a_long_long... | b07d423864ec120362b3584635cb07d423864...  | VSM");
  });

  it('should not show username or separator if empty', () => {
    material.modification = new MaterialModification("", null, "b07d423864ec120362b3584635c", "A very long comment to be shown on the header", "");
    mount();

    expect(helper.byTestId("latest-mod-in-header")).toBeInDOM();
    expect(helper.q(`.${headerStyles.committerInfo}`).textContent).toBe("b07d423864ec120362b3584635c | VSM");
  });

  it('should render link to VSM', () => {
    material.modification
      = new MaterialModification("username", null, "b07d423864ec120362b3584635cb07", "dummy comment", "");
    mount();

    expect(helper.q(`.${headerStyles.committerInfo}`).textContent).toBe("username | b07d423864ec120362b3584635cb07  | VSM");
    expect(helper.byTestId("vsm-link")).toBeInDOM();
    expect(helper.textByTestId("vsm-link")).toBe('VSM');
    expect(helper.byTestId("vsm-link")).toHaveAttr('href', SparkRoutes.materialsVsmLink(material.config.fingerprint(), material.modification.revision));
    expect(helper.byTestId("vsm-link")).toHaveAttr('title', 'Value Stream Map');
  });

  it('should render the first line of the modification comment', () => {
    material.modification
      = new MaterialModification("A_Long_username_with_a_long_long_long_long_long_text", null, "b07d423864ec120362b3584635cb07d423864ec120362b3584635c", "A very long comment to be shown on the header.\nWhich should be trimmed and rest part should not be shown", "");
    mount();

    expect(helper.byTestId("latest-mod-in-header")).toBeInDOM();
    expect(helper.q(`.${headerStyles.comment}`).textContent).toBe("A very long comment to be shown on the header....");
  });

  it('should render the first line as truncated if longer than max chars of the modification comment', () => {
    material.modification
      = new MaterialModification("A_Long_username_with_a_long_long_long_long_long_text", null, "b07d423864ec120362b3584635cb07d423864ec120362b3584635c", "A very long comment to be shown on the header which should be trimmed and rest part should not be shown.\n Also this is the complete message", "");
    mount();

    expect(helper.byTestId("latest-mod-in-header")).toBeInDOM();
    expect(helper.q(`.${headerStyles.comment}`).textContent).toBe("A very long comment to be shown on the header which should be trimmed and rest part sho...");
  });

  function mount() {
    helper.mount(() => <MaterialHeaderWidget material={material}/>);
  }
});
