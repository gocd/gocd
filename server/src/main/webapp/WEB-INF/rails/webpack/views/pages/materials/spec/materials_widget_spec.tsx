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
import {timeFormatter} from "helpers/time_formatter";
import m from "mithril";
import Stream from "mithril/stream";
import {MaterialModification} from "models/config_repos/types";
import {Filter} from "models/maintenance_mode/material";
import {MaterialWithFingerprint, MaterialWithFingerprintJSON, MaterialWithModification} from "models/materials/materials";
import {Scm, Scms} from "models/materials/pluggable_scm";
import {PackageMaterialAttributes, PluggableScmMaterialAttributes} from "models/materials/types";
import {Package, Packages} from "models/package_repositories/package_repositories";
import {getPackage} from "models/package_repositories/spec/test_data";
import {getPluggableScm} from "views/pages/pluggable_scms/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {MaterialsWidget, MaterialWidget} from "../materials_widget";
import {MaterialVM, MaterialVMs} from "../models/material_view_model";

describe('MaterialWidgetSpec', () => {
  const helper = new TestHelper();
  let material: MaterialWithModification;
  let packages: Packages;
  let scms: Scms;

  afterEach((done) => helper.unmount(done));
  beforeEach(() => {
    material = new MaterialWithModification(MaterialWithFingerprint.fromJSON(git()), null);
    packages = new Packages();
    scms     = new Scms();
  });

  it('should display the header', () => {
    mount();

    expect(helper.byTestId("material-icon")).toBeInDOM();
  });

  it('should render git material attributes in the panel body', () => {
    mount();

    expect(helper.qa('h3')[1].textContent).toBe("Material Attributes");

    const attrsElement = helper.byTestId('material-attributes');

    expect(attrsElement).toBeInDOM();
    expect(helper.qa('li', attrsElement).length).toBe(2);
  });

  it('should render ref with link for plugin', () => {
    const scm = Scm.fromJSON(getPluggableScm());
    scms.push(scm);
    material.config
      = new MaterialWithFingerprint("plugin", "fingerprint", new PluggableScmMaterialAttributes(undefined, true, scm.id(), "", new Filter([])));
    mount();

    expect(helper.qa('h3')[1].textContent).toBe("Material Attributes");

    const attrsElement = helper.byTestId('material-attributes');

    expect(attrsElement).toBeInDOM();
    expect(helper.qa('li', attrsElement).length).toBe(1);
    expect(helper.q('a', helper.byTestId('key-value-value-ref')).textContent).toBe('pluggable.scm.material.name');
    expect(helper.q('a', helper.byTestId('key-value-value-ref'))).toHaveAttr('href', '/go/admin/scms#!pluggable.scm.material.name');
  });

  it('should render ref with link for package', () => {
    const pkg = Package.fromJSON(getPackage());
    packages.push(pkg);
    material.config
      = new MaterialWithFingerprint("package", "fingerprint", new PackageMaterialAttributes(undefined, true, pkg.id()));
    mount();

    expect(helper.qa('h3')[1].textContent).toBe("Material Attributes");

    const attrsElement = helper.byTestId('material-attributes');

    expect(attrsElement).toBeInDOM();
    expect(helper.qa('li', attrsElement).length).toBe(1);
    expect(helper.q('a', helper.byTestId('key-value-value-ref')).textContent).toBe('pkg-name');
    expect(helper.q('a', helper.byTestId('key-value-value-ref'))).toHaveAttr('href', '/go/admin/package_repositories#!pkg-repo-name/packages/pkg-name');
  });

  it('should display info message if no modifications are present', () => {
    mount();

    expect(helper.byTestId('flash-message-info')).toBeInDOM();
    expect(helper.textByTestId('flash-message-info')).toBe("This material was never parsed");
  });

  it('should display latest modifications', () => {
    material.modification
      = new MaterialModification("GoCD Test User <devnull@example.com>", null, "b9b4f4b758e91117d70121a365ba0f8e37f89a9d", "Initial commit", "2019-12-23T10:25:52Z");
    mount();

    expect(helper.q('h3').textContent).toBe("Latest Modification Details");
    expect(helper.byTestId('latest-modification-details')).toBeInDOM();

    const attrs = helper.qa('li', helper.byTestId('latest-modification-details'));

    expect(attrs.length).toBe(5);
    expect(attrs[0].textContent).toBe("UsernameGoCD Test User <devnull@example.com>");
    expect(attrs[1].textContent).toBe("Email(Not specified)");
    expect(attrs[2].textContent).toBe("Revisionb9b4f4b758e91117d70121a365ba0f8e37f89a9d");
    expect(attrs[3].textContent).toBe("CommentInitial commit");
    expect(attrs[4].textContent).toBe("Modified Time" + timeFormatter.format(material.modification.modifiedTime));

    expect(helper.q('span span', attrs[4])).toHaveAttr('title', '23 Dec, 2019 at 10:25:52 +00:00 Server Time');
  });

  function mount() {
    helper.mount(() => <MaterialWidget materialVM={new MaterialVM(material)} packages={Stream(packages)} scms={Stream(scms)}/>);
  }
});

describe('MaterialsWidgetSpec', () => {
  const helper = new TestHelper();
  let materialVMs: MaterialVMs;

  afterEach((done) => helper.unmount(done));
  beforeEach(() => {
    materialVMs = new MaterialVMs();
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
    helper.mount(() => <MaterialsWidget materialVMs={Stream(materialVMs)} packages={Stream(new Packages())} scms={Stream(new Scms())}/>);
  }
});

export function git() {
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
