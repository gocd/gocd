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

import {ErrorMessages} from "models/mixins/error_messages";
import {PipelineParameter} from "models/pipeline_configs/parameter";

describe("Parameters model", () => {
  it("should have a correctly formatted name", () => {
    let param = new PipelineParameter("valid_name", "");
    expect(param.isValid()).toBe(true);
    expect(param.errors().count()).toBe(0);

    param = new PipelineParameter("", "");
    expect(param.isValid()).toBe(false);
    expect(param.errors().count()).toBe(1);
    expect(param.errors().errorsForDisplay("name")).toBe("Name must be present.");

    param = new PipelineParameter("invalid name", "");
    expect(param.isValid()).toBe(false);
    expect(param.errors().count()).toBe(1);
    expect(param.errors().errorsForDisplay("name")).toBe(ErrorMessages.idFormat("name"));
  });

  it("should serialize correctly", () => {
    const param = new PipelineParameter("my-param", "lalala");
    expect(param.toApiPayload()).toEqual({ name: "my-param", value: "lalala"});
  });

  it("should know if it's empty", () => {
    let param = new PipelineParameter("", "");
    expect(param.isEmpty()).toBeTruthy();

    param = new PipelineParameter("my-param", "lalala");
    expect(param.isEmpty()).toBeFalsy();
  });
});
