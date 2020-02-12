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

import {ApiResult} from "helpers/api_request_builder";
import {asSelector} from "helpers/css_proxies";
import m from "mithril";
import buttonStyles from "views/components/buttons/index.scss";
import {TestHelper} from "views/pages/spec/test_helper";
import {PipelineConfigVM} from "../../pipelines/pipeline_config_view_model";
import {DownloadAction} from "../download_action";
import errorStyles from "../styles.scss";

describe("AddPaC: DownloadAction", () => {
  const helper = new TestHelper();
  const sel = asSelector<typeof buttonStyles>(buttonStyles);
  const selErr = asSelector<typeof errorStyles>(errorStyles);

  afterEach(() => helper.unmount());

  it("clicking download causes a full validation and preview API call", (done) => {
    const vm = new PipelineConfigVM();

    spyOn(vm, "preview").and.callFake((p, v) => {
      return new Promise<ApiResult<string>>((resolve, _) => {
        resolve(ApiResult.error(JSON.stringify({data: { errors: { foo: ["boom"] } } }), "that didn't go so well", 422, new Map()));
      }).then((result) => {
        setTimeout(() => {
          m.redraw.sync();
          expect(helper.text(selErr.errorResponse)).toBe("that didn't go so well: pipelineConfig.foo: boom.");
          done();
        }, 10);
        return result;
      });
    });

    spyOn(vm.pipeline, "isValid").and.returnValue(true);

    helper.mount(() => <DownloadAction pluginId={() => "foo"} vm={vm}/>);

    expect(helper.text(sel.btnPrimary)).toBe("Download Config");

    helper.click(sel.btnPrimary);

    expect(vm.pipeline.isValid).toHaveBeenCalled();
    expect(vm.preview).toHaveBeenCalledWith("foo", true);
  });
});
