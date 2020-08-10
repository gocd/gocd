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

import {asSelector} from "helpers/css_proxies";
import m from "mithril";
import {ObjectCache} from "models/base/cache";
import {GitMaterialAttributes, MaterialWithFingerprint, MaterialWithModification} from "models/materials/materials";
import css from "views/pages/config_repos/defined_structs.scss";
import {emptyTree,} from "views/pages/config_repos/spec/test_data";
import {MaterialUsagesVM} from "views/pages/materials/models/material_usages_view_model";
import {MaterialVM} from "views/pages/materials/models/material_view_model";
import {TestHelper} from "views/pages/spec/test_helper";
import {MaterialUsageWidget} from "../material_usage_widget";

interface MockedResultsData {
  content?: MaterialUsagesVM;
  failureReason?: string;
  ready?: boolean;
}

export class DummyCache implements ObjectCache<MaterialUsagesVM> {
  ready: () => boolean;
  contents: () => MaterialUsagesVM;
  failureReason: () => string | undefined;
  prime: (onSuccess: () => void, onError?: () => void) => void = jasmine.createSpy("cache.prime");
  invalidate: () => void                                       = jasmine.createSpy("cache.invalidate");

  constructor(options: MockedResultsData) {
    this.failureReason = () => options.failureReason;
    this.contents      = () => ("content" in options) ? options.content! : emptyTree();
    this.ready         = () => ("ready" in options) ? !!options.ready : true;
  }

  failed(): boolean {
    return !!this.failureReason();
  }
}

describe('MaterialUsageWidgetSpec', () => {
  const sel    = asSelector<typeof css>(css);
  const helper = new TestHelper();
  let materialVM: MaterialVM;

  afterEach((done) => helper.unmount(done));
  beforeEach(() => {
    materialVM
      = new MaterialVM(new MaterialWithModification(new MaterialWithFingerprint("git", "some", new GitMaterialAttributes()), null));
  });

  it('should showcase failed to load pipelines message', () => {
    materialVM.results = new DummyCache({failureReason: "Some failure reason"});
    mount();

    expect(helper.byTestId("flash-message-alert")).toBeInDOM();
    expect(helper.textByTestId("flash-message-alert")).toBe('Failed to load pipelines: Some failure reason');
  });

  it('should render loading message', () => {
    materialVM.results = new DummyCache({ready: false, content: undefined});
    mount();

    expect(helper.q(sel.tree)).not.toBeInDOM();
    expect(helper.q(sel.treeDatum)).not.toBeInDOM();
    expect(helper.q(sel.loading)).toBeInDOM();
    expect(helper.q(sel.spin)).toBeInDOM();
    expect(helper.text(sel.loading)).toBe('Loading pipelines …');
  });

  it('should render tree structure for material usages', () => {
    materialVM.results = new DummyCache({ready: true, content: MaterialUsagesVM.fromJSON(usagesJSON())});
    mount();

    const expectedTextResult = [
      'Pipelines using this material:',
      'group1',
      'pipeline1',
      'pipeline2',
      'group2',
      'pipeline3',
      'pipeline4'
    ];

    expect(helper.q(sel.loading)).not.toBeInDOM();
    expect(helper.q(sel.tree)).toBeInDOM();
    expect(helper.textAll(sel.treeDatum)).toEqual(expectedTextResult);
  });

  it('should assign correct styles', () => {
    materialVM.results = new DummyCache({ready: true, content: MaterialUsagesVM.fromJSON(usagesJSON())});
    mount();

    const groupTreeNode = helper.byTestId('tree-node-group1');
    expect(groupTreeNode).toHaveClass(css.groupDatum);
    expect(groupTreeNode).toHaveClass(css.treeDatum);
    expect(groupTreeNode.parentElement).toHaveClass(css.tree);
    expect(groupTreeNode.parentElement).toHaveClass(css.group);

    const pipelineTreeNode = helper.byTestId('tree-node-pipeline1');
    expect(pipelineTreeNode).toHaveClass(css.pipelineDatum);
    expect(pipelineTreeNode).toHaveClass(css.treeDatum);
    expect(pipelineTreeNode.parentElement).toHaveClass(css.tree);
    expect(pipelineTreeNode.parentElement).toHaveClass(css.pipeline);
  });

  it('should render pipelines and groups as link', () => {
    materialVM.results = new DummyCache({ready: true, content: MaterialUsagesVM.fromJSON(usagesJSON())});
    mount();

    expect(helper.byTestId('group_group_1')).toHaveAttr('href', '/go/admin/pipelines/#!group1');
    expect(helper.byTestId('pipeline_pipeline_1')).toHaveAttr('href', '/go/admin/pipelines/pipeline1/edit#!pipeline1/materials');
  });

  function mount() {
    helper.mount(() => <MaterialUsageWidget materialVM={materialVM}/>);
  }
});

export function usagesJSON() {
  return [{
    group:     "group1",
    pipelines: ["pipeline1", "pipeline2"]
  }, {
    group:     "group2",
    pipelines: ["pipeline3", "pipeline4"]
  }];
}
