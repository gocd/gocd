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

import {el} from "helpers/dom";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import {Stream} from "mithril/stream";
import stream from "mithril/stream";
import {Errors} from "models/mixins/errors";
import {Primary} from "views/components/buttons";
import css from "views/pages/pac/styles.scss";
import {PipelineConfigVMAware as Attrs} from "views/pages/pipelines/pipeline_config_view_model";
import {ServerErrors} from "views/pages/pipelines/server_errors";

export class DownloadAction extends MithrilComponent<Attrs> {
  private globalError: Stream<string> = stream();
  private globalErrorDetail: Stream<Errors> = stream();

  view(vnode: m.Vnode<Attrs>) {
    return <div class={css.downloadAction}>
      <ServerErrors message={this.globalError} details={this.globalErrorDetail}/>

      <Primary onclick={this.handleDownload(vnode)}>Download</Primary>
    </div>;
  }

  handleDownload(vnode: m.Vnode<Attrs>) {
    const vm = vnode.attrs.vm;
    return (e: MouseEvent) => {
      e.stopPropagation();
      e.preventDefault();

      this.clearErrors();

      if (vm.pipeline.isValid()) {
        vm.preview(vm.pluginId(), true).then((result) => {
          result.do((res) => {
            const name = result.header("content-disposition")!.replace(/^attachment; filename=/, "").replace(/^(")(.+)(\1)/, "$2");
            const data = new Blob([res.body], { type: result.header("content-type") }); // simpler than setting responseType to `blob` as that has many side effects.

            const a = el("a", { href: URL.createObjectURL(data), download: name, style: "display:none" }, []);

            document.body.appendChild(a); // Firefox requires this to be added to the DOM before click()
            a.click();
            document.body.removeChild(a);
          }, (err) => {
            this.globalError(err.message);
            if (err.body) {
              this.globalErrorDetail(vm.pipeline.consumeErrorsResponse(JSON.parse(err.body).data));
            }
          });
        }).catch((reason) => {
          this.globalError(reason);
        }).finally(m.redraw);
      } else {
        this.globalError("Please fix the validation errors above");
      }
    };
  }

  clearErrors() {
    this.globalError = stream();
    this.globalErrorDetail = stream();
  }
}
