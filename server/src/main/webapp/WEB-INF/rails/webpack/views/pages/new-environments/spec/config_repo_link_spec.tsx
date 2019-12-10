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

import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril" ;
import {ConfigRepoLink} from "views/pages/new-environments/config_repo_link";
import {TestHelper} from "views/pages/spec/test_helper";

describe('ConfigRepoLink', () => {
  const helper = new TestHelper();
  it("should render config repo link", () => {
    const configRepoId = "config-repo-1";
    helper.mount(() => <ConfigRepoLink dataTestId={"config-repo-link"} configRepoId={configRepoId}/>);

    expect(helper.byTestId("config-repo-link")).toBeInDOM();
    expect(helper.byTestId("config-repo-link")).toHaveText(`(Config Repository:${configRepoId})`);
    expect(helper.q("a", helper.byTestId("config-repo-link"))).toHaveText(configRepoId);
    expect(helper.q("a", helper.byTestId("config-repo-link"))).toHaveAttr("href", SparkRoutes.ConfigRepoViewPath(configRepoId));

    helper.unmount();
  });
});
