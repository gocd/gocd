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
import {ConceptDiagram} from "views/components/concept_diagram";
import {TestHelper} from "views/pages/spec/test_helper";

describe("AddPipeline: ConceptDiagram", () => {
  const helper = new TestHelper();
  const image = require("../../../../../app/assets/images/go_logo.svg");

  beforeEach(() => {
    helper.mount(() => <ConceptDiagram image={image}>A <strong>simple</strong> explanation</ConceptDiagram>);
  });

  afterEach(helper.unmount.bind(helper));

  it("Renders the concept diagram SVG", () => {
    const img = helper.q(`object[type="image/svg+xml"]`);
    expect(img).toBeTruthy();
    expect(img.getAttribute("data")).toBe(image);
  });

  it("Renders the concept diagram caption", () => {
    expect(helper.q("figure figcaption").textContent).toBe("A simple explanation");
    // preserves formatting
    expect(helper.q("figure figcaption strong").textContent).toBe("simple");
  });
});
