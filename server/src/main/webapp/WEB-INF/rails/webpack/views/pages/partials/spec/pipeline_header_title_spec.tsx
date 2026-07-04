/*
 * Copyright Thoughtworks, Inc.
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
import {v4 as uuid4} from "uuid";
import {PipelineHeaderTitle} from "views/pages/partials/pipeline_header_title";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Pipeline Header Title", () => {
  const helper        = new TestHelper();
  const PIPELINE_NAME = `pipeline_name_${uuid4()}`;

  afterEach(() => helper.unmount());

  it("should render pipeline label and name", () => {
    helper.mount(() => <PipelineHeaderTitle pipelineName={PIPELINE_NAME}/>);

    expect(helper.byTestId("page-header-pipeline-label")).toBeInDOM();
    expect(helper.byTestId("page-header-pipeline-label")).toHaveText("Pipeline");

    expect(helper.byTestId("page-header-pipeline-name")).toBeInDOM();
    expect(helper.byTestId("page-header-pipeline-name")).toHaveText(PIPELINE_NAME);
  });

  it("should set the full pipeline name as the title attribute so it is readable when truncated", () => {
    helper.mount(() => <PipelineHeaderTitle pipelineName={PIPELINE_NAME}/>);

    expect(helper.byTestId("page-header-pipeline-name")).toHaveAttr("title", PIPELINE_NAME);
  });

  it("should render the pipeline name as a plain text (no link) when no link is given", () => {
    helper.mount(() => <PipelineHeaderTitle pipelineName={PIPELINE_NAME}/>);

    expect(helper.q("a", helper.byTestId("page-header-pipeline-name"))).toBeFalsy();
  });

  it("should render the pipeline name as a link when a link is given", () => {
    const link = "/go/pipelines/foo/history";
    helper.mount(() => <PipelineHeaderTitle pipelineName={PIPELINE_NAME} link={link} linkTitle="Pipeline Activities"/>);

    const anchor = helper.q("a", helper.byTestId("page-header-pipeline-name"));
    expect(anchor).toBeInDOM();
    expect(anchor.getAttribute("href")).toBe(link);
    expect(helper.text(anchor)).toBe(PIPELINE_NAME);
  });
});
