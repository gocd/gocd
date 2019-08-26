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
import simulateEvent from "simulate-event";
import * as collapsiblePanelStyles from "views/components/collapsible_panel/index.scss";
import {MaterialsWidget} from "views/pages/pipeline_configs/materials/index";
import {TestHelper} from "views/pages/spec/test_helper";

describe("MaterialsWidgetSpec", () => {
  const helper = new TestHelper();

  const noop       = _.noop;
  const operations = {
    onDelete: noop,
    onAdd: noop
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
    expect(helper.findByDataTestId("pipeline-materials-container")).toBeInDOM();
    expect(helper.findByDataTestId("pipeline-materials-container")).toHaveClass(collapsiblePanelStyles.expanded);
  });

  it("material toggle expanded state of materials panel", () => {
    expect(helper.findByDataTestId("pipeline-materials-container")).toHaveClass(collapsiblePanelStyles.expanded);

    const materialsPanelHeader = helper.findIn(helper.findByDataTestId("pipeline-materials-container"),
                                               "collapse-header")[0];

    simulateEvent.simulate(materialsPanelHeader, "click");
    m.redraw.sync();

    expect(helper.findByDataTestId("pipeline-materials-container")).not.toHaveClass(collapsiblePanelStyles.expanded);
  });

  it("should render 'Add Material' button", () => {
    expect(helper.findByDataTestId("add-material-button")).toBeInDOM();
    expect(helper.findByDataTestId("add-material-button")).toContainText("Add Material");
  });

  it("should render materials table", () => {
    expect(helper.findByDataTestId("materials-index-table")).toBeInDOM();
    expect(helper.findByDataTestId("materials-index-table")).toContainHeaderCells(["Material Name", "Type", "Url", ""]);

    const gitRepo = helper.findByDataTestId("materials-index-table").find("tr")[1];
    expect(gitRepo.children[0]).toContainText("Test git repo");
    expect(gitRepo.children[1]).toContainText("Git");
    expect(gitRepo.children[2]).toContainText("http://foo.git");
    expect(helper.findIn(gitRepo, "delete-material-button")).toBeInDOM();

    const hgRepo = helper.findByDataTestId("materials-index-table").find("tr")[2];
    expect(hgRepo.children[0]).toContainText("Test mercurial repo");
    expect(hgRepo.children[1]).toContainText("Mercurial");
    expect(hgRepo.children[2]).toHaveText("http://foo.hg");
    expect(helper.findIn(hgRepo, "delete-material-button")).toBeInDOM();

    const p4Repo = helper.findByDataTestId("materials-index-table").find("tr")[3];
    expect(p4Repo.children[0]).toContainText("Test p4 repo");
    expect(p4Repo.children[1]).toContainText("Perforce");
    expect(p4Repo.children[2]).toHaveText("127.0.0.1:3000");
    expect(helper.findIn(p4Repo, "delete-material-button")).toBeInDOM();

    const tfsRepo = helper.findByDataTestId("materials-index-table").find("tr")[4];
    expect(tfsRepo.children[0]).toContainText("Test tfs repo");
    expect(tfsRepo.children[1]).toContainText("Team Foundation Server");
    expect(tfsRepo.children[2]).toHaveText("http://foo.tfs");
    expect(helper.findIn(tfsRepo, "delete-material-button")).toBeInDOM();

    const svnRepo = helper.findByDataTestId("materials-index-table").find("tr")[5];
    expect(svnRepo.children[0]).toContainText("Test svn repo");
    expect(svnRepo.children[1]).toContainText("Subversion");
    expect(svnRepo.children[2]).toHaveText("http://foo.svn");
    expect(helper.findIn(svnRepo, "delete-material-button")).toBeInDOM();

    const dependencyMaterial = helper.findByDataTestId("materials-index-table").find("tr")[6];
    expect(dependencyMaterial.children[0]).toContainText("Dependent pipeline");
    expect(dependencyMaterial.children[1]).toContainText("Another Pipeline");
    expect(dependencyMaterial.children[2]).toHaveText("pipeline / stage");
    expect(helper.findIn(dependencyMaterial, "delete-material-button")).toBeInDOM();
  });
});
