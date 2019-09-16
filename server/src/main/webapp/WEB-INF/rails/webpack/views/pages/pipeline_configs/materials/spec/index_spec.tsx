/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import _ from "lodash";
import m from "mithril";
import {
  DependencyMaterialAttributes,
  GitMaterialAttributes, HgMaterialAttributes,
  Material, Materials,
  P4MaterialAttributes, TfsMaterialAttributes
} from "models/new_pipeline_configs/materials";
import * as collapsiblePanelStyles from "views/components/collapsible_panel/index.scss";
import {MaterialsWidget} from "views/pages/pipeline_configs/materials/index";
import {TestHelper} from "views/pages/spec/test_helper";

describe("MaterialsWidgetSpec", () => {
  const helper = new TestHelper();

  const noop       = _.noop;
  const operations = {
    onDelete: noop,
    onAdd: noop,
    onUpdate: noop
  };

  beforeEach(() => {
    const materials = new Materials(
      new Material("git", new GitMaterialAttributes("http://foo.git", "Test git repo")),
      new Material("hg", new HgMaterialAttributes("http://foo.hg", "Test mercurial repo")),
      new Material("p4", new P4MaterialAttributes("127.0.0.1:3000", "view", "Test p4 repo")),
      new Material("tfs", new TfsMaterialAttributes("http://foo.tfs", "Project/path", "Test tfs repo")),
      new Material("svn", new HgMaterialAttributes("http://foo.svn", "Test svn repo")),
      new Material("dependency", new DependencyMaterialAttributes("pipeline", "stage", "Dependent pipeline"))
    );
    helper.mount(() => <MaterialsWidget materialOperations={operations} materials={materials}/>);
  });

  afterEach(() => {
    helper.unmount();
  });

  it("should render materials collapsible panel", () => {
    expect(helper.byTestId("pipeline-materials-container")).toBeInDOM();
    expect(helper.byTestId("pipeline-materials-container")).toHaveClass(collapsiblePanelStyles.expanded);
  });

  it("material toggle expanded state of materials panel", () => {
    expect(helper.byTestId("pipeline-materials-container")).toHaveClass(collapsiblePanelStyles.expanded);

    helper.clickByTestId("collapse-header", helper.byTestId("pipeline-materials-container"));

    expect(helper.byTestId("pipeline-materials-container")).not.toHaveClass(collapsiblePanelStyles.expanded);
  });

  it("should render 'Add Material' button", () => {
    expect(helper.byTestId("add-material-button")).toBeInDOM();
    expect(helper.textByTestId("add-material-button")).toContain("Add Material");
  });

  it("should render materials table", () => {
    expect(helper.byTestId("materials-index-table")).toBeInDOM();
    expect(helper.byTestId("materials-index-table")).toContainHeaderCells(["Material Name", "Type", "Url", ""]);

    const gitRepo = helper.qa("tr", helper.byTestId("materials-index-table"))[1];
    expect(gitRepo.children[0]).toContainText("Test git repo");
    expect(gitRepo.children[1]).toContainText("Git");
    expect(gitRepo.children[2]).toContainText("http://foo.git");
    expect(helper.byTestId("delete-material-button", gitRepo)).toBeInDOM();
    expect(helper.byTestId("edit-material-button", gitRepo)).toBeInDOM();

    const hgRepo = helper.qa("tr", helper.byTestId("materials-index-table"))[2];
    expect(hgRepo.children[0]).toContainText("Test mercurial repo");
    expect(hgRepo.children[1]).toContainText("Mercurial");
    expect(hgRepo.children[2]).toHaveText("http://foo.hg");
    expect(helper.byTestId("delete-material-button", hgRepo)).toBeInDOM();
    expect(helper.byTestId("edit-material-button", hgRepo)).toBeInDOM();

    const p4Repo = helper.qa("tr", helper.byTestId("materials-index-table"))[3];
    expect(p4Repo.children[0]).toContainText("Test p4 repo");
    expect(p4Repo.children[1]).toContainText("Perforce");
    expect(p4Repo.children[2]).toHaveText("127.0.0.1:3000");
    expect(helper.byTestId("delete-material-button", p4Repo)).toBeInDOM();
    expect(helper.byTestId("edit-material-button", p4Repo)).toBeInDOM();

    const tfsRepo = helper.qa("tr", helper.byTestId("materials-index-table"))[4];
    expect(tfsRepo.children[0]).toContainText("Test tfs repo");
    expect(tfsRepo.children[1]).toContainText("Team Foundation Server");
    expect(tfsRepo.children[2]).toHaveText("http://foo.tfs");
    expect(helper.byTestId("delete-material-button", tfsRepo)).toBeInDOM();
    expect(helper.byTestId("edit-material-button", tfsRepo)).toBeInDOM();

    const svnRepo = helper.qa("tr", helper.byTestId("materials-index-table"))[5];

    expect(svnRepo.children[0]).toContainText("Test svn repo");
    expect(svnRepo.children[1]).toContainText("Subversion");
    expect(svnRepo.children[2]).toHaveText("http://foo.svn");
    expect(helper.byTestId("delete-material-button", svnRepo)).toBeInDOM();
    expect(helper.byTestId("edit-material-button", svnRepo)).toBeInDOM();

    const dependencyMaterial = helper.qa("tr", helper.byTestId("materials-index-table"))[6];
    expect(dependencyMaterial.children[0]).toContainText("Dependent pipeline");
    expect(dependencyMaterial.children[1]).toContainText("Another Pipeline");
    expect(dependencyMaterial.children[2]).toHaveText("pipeline / stage");
    expect(helper.byTestId("delete-material-button", dependencyMaterial)).toBeInDOM();
    expect(helper.byTestId("edit-material-button", dependencyMaterial)).toBeInDOM();
  });

  it("click on edit material should open a modal", () => {
    const gitRepo = helper.qa("tr", helper.byTestId("materials-index-table"))[1];

    expect(helper.byTestId("material-form", document.body)).toBeFalsy();

    helper.clickByTestId("edit-material-button", gitRepo);

    expect(helper.allByTestId("material-form", document.body).length).toBe(1);

    helper.closeModal();
  });

  it("click on add material should open a modal", () => {
    expect(helper.byTestId("material-form", document.body)).toBeFalsy();

    helper.clickByTestId("add-material-button");

    expect(helper.allByTestId("material-form", document.body).length).toBe(1);

    helper.closeModal();
  });
});
