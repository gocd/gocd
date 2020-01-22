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

import {ProfileUsage} from "models/elastic_profiles/types";
import {UsageElasticProfileModal} from "views/pages/elastic_agent_configurations/elastic_agent_profiles_modals";
import {TestHelper} from "views/pages/spec/test_helper";

describe("UsageElasticProfileModalSpec", () => {
  const helper = new TestHelper();

  afterEach((done) => helper.unmount(done));

  function mount(usages: ProfileUsage[] = []) {
    const modal = new UsageElasticProfileModal("profile-id", usages);

    helper.mount(modal.body.bind(modal));
  }

  it("should render message is usages is empty", () => {
    mount();

    expect(helper.q("span").innerText).toBe("No usages for profile 'profile-id' found.");
  });

  it("should render usages", () => {
    const usages = [new ProfileUsage("pipeline-name", "stage-name", "job-name")];
    mount(usages);

    const table = helper.byTestId("table");
    expect(table).toBeInDOM();
    expect(helper.qa("th", table).length).toBe(4);
    expect(helper.qa("th", table)[0].innerText.toLowerCase()).toBe("pipeline");
    expect(helper.qa("th", table)[1].innerText.toLowerCase()).toBe("stage");
    expect(helper.qa("th", table)[2].innerText.toLowerCase()).toBe("job");
    expect(helper.qa("th", table)[3].innerText).toBe("");

    expect(helper.qa("td", table)[0].innerText).toBe("pipeline-name");
    expect(helper.qa("td", table)[1].innerText).toBe("stage-name");
    expect(helper.qa("td", table)[2].innerText).toBe("job-name");
    expect(helper.qa("td", table)[3].innerText).toBe("Job Settings");
    expect(helper.q("a", helper.qa("td", table)[3]))
      .toHaveAttr("href", "/go/admin/pipelines/pipeline-name/stages/stage-name/job/job-name/settings");
  });

  it("should redirect to template settings if template name is present", () => {
    const usages = [new ProfileUsage("pipeline-name", "stage-name", "job-name", "template-name")];
    mount(usages);

    const table = helper.byTestId("table");
    expect(helper.q("a", helper.qa("td", table)[3]))
      .toHaveAttr("href", "/go/admin/templates/template-name/stages/stage-name/job/job-name/settings");
  });
});
