/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import filesize from "filesize";
import m from "mithril";
import {ServerInfoWidget} from "views/pages/server_info/server_info_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Server Info Widget", () => {
  const helper = new TestHelper();
  afterEach((done) => helper.unmount(done));

  const metaInfo = {
    go_server_version: "19.4.0 (9155-0f01ab091e85a0d735b8b580eee5f83245fba2e5)",
    jvm_version: "12.0.1",
    pipeline_count: 184,
    usable_space_in_artifacts_repository: 123450698,
    os_information: "Linux 4.14.104-95.84.amzn2.x86_64",
  };

  it("should show Sever Information", () => {
    mount();

    expect(helper.textByTestId("about-page")).toContain("Go Server Version:");
    expect(helper.textByTestId("about-page")).toContain(metaInfo.go_server_version);

    expect(helper.textByTestId("about-page")).toContain("JVM version:");
    expect(helper.textByTestId("about-page")).toContain(metaInfo.jvm_version);

    expect(helper.textByTestId("about-page")).toContain("OS Information:");
    expect(helper.textByTestId("about-page")).toContain(metaInfo.os_information);

    expect(helper.textByTestId("about-page")).toContain("Usable space in artifacts repository:");
    expect(helper.textByTestId("about-page")).toContain(filesize(metaInfo.usable_space_in_artifacts_repository));

    expect(helper.textByTestId("about-page")).toContain("Pipelines Count:");
    expect(helper.textByTestId("about-page")).toContain(`${metaInfo.pipeline_count}`);
  });

  function mount() {
    helper.mount(() => <ServerInfoWidget meta={metaInfo}/>);
  }
});
