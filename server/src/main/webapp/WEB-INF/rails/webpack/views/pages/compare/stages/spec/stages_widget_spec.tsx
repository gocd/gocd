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
import {Jobs, Stage, Stages} from "models/compare/pipeline_instance";
import {TestHelper} from "views/pages/spec/test_helper";
import styles from "../stages.scss";
import {StagesWidget} from "../stages_widget";

describe('StagesWidgetSpec', () => {
  const helper = new TestHelper();

  afterEach((done) => helper.unmount(done));

  function mount(stages: Stages, onClick?: (stage: Stage) => void) {
    helper.mount(() => <StagesWidget stages={stages} onClick={onClick}/>);
  }

  const parameters = [
    {description: "should render stage with status as 'passed' with class passed", input: "passed", output: styles.passed}
    , {description: "should render stage with status as 'building' with class building", input: "building", output: styles.building}
    , {description: "should render stage with status as 'failed' with class failed", input: "failed", output: styles.failed}
    , {description: "should render stage with status as 'failing' with class failing", input: "failing", output: styles.failing}
    , {description: "should render stage with status as 'cancelled' with class cancelled", input: "cancelled", output: styles.cancelled}
    , {description: "should render stage with status as 'unknown' with class unknown", input: "unknown", output: styles.unknown}
    , {description: "should render stage with unknown status with class unknown", input: "unknown", output: styles.unknown}
  ];

  parameters.forEach((parameter) => {
    it(parameter.description, () => {
      const stages = new Stages();
      const jobs   = new Jobs();

      stages.push(new Stage(1, "stage", "1", false, parameter.input, parameter.input, "", "", false, false, jobs));
      mount(stages);

      const stageElement = helper.byTestId("stages");
      const stageCols    = helper.q("td", stageElement);

      expect(stageCols).toHaveClass(parameter.output);
      expect(stageCols).toHaveAttr("title", `stage (${parameter.input})`);
    });
  });

  it('should have class clickable if onclick method is provided', () => {
    const stages = new Stages();
    const jobs   = new Jobs();

    stages.push(new Stage(1, "stage", "1", false, "Passed", "passed", "", "", false, false, jobs));
    const spy = jasmine.createSpy("onClick");

    mount(stages, spy);

    const stageElement = helper.byTestId("stages");
    const stageCols    = helper.q("td", stageElement);

    expect(stageCols).toHaveClass(styles.clickable);
  });

  it('should call the spy on click', () => {
    const stages = new Stages();
    const jobs   = new Jobs();

    const stage = new Stage(1, "stage", "1", false, "Passed", "passed", "", "", false, false, jobs);
    stages.push(stage);
    const spy = jasmine.createSpy("onClick");

    mount(stages, spy);

    const stageElement = helper.byTestId("stages");
    const stageCols    = helper.q("td", stageElement);

    helper.click(stageCols);

    expect(spy).toHaveBeenCalledWith(stage);
  });
});
