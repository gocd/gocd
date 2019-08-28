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
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Frame} from "models/shared/analytics_frame";
import {Spinner} from "views/components/spinner";
import Styles from "./index.scss";

interface Attrs {
  model: Frame;
  init: (win: Window, data: object) => void;
}

export class AnalyticsiFrameWidget extends MithrilViewComponent<Attrs> {
  private currentUrl: string | null       = null;
  private loadingContent: Stream<boolean> = Stream(false) as Stream<boolean>;

  loadFrameContent(vnode: m.VnodeDOM<Attrs>) {
    const iframe = vnode.dom.querySelector("iframe") as HTMLIFrameElement;

    iframe.onload = () => {
      vnode.attrs.init(iframe.contentWindow!, {
        uid: vnode.attrs.model.uid,
        pluginId: vnode.attrs.model.pluginId(),
        initialData: vnode.attrs.model.data()
      });
    };

    this.currentUrl = vnode.attrs.model.url();
    vnode.attrs.model.load(this.beforeLoad.bind(this), this.afterLoad.bind(this));
  }

  beforeLoad() {
    this.loadingContent(true);
    m.redraw();
  }

  afterLoad() {
    this.loadingContent(false);
    m.redraw.sync();
  }

  oncreate(vnode: m.VnodeDOM<Attrs>) {
    this.loadFrameContent.apply(this, [vnode]);
  }

  onupdate(vnode: m.VnodeDOM<Attrs>) {
    if (vnode.attrs && vnode.attrs.model && this.currentUrl !== vnode.attrs.model.url()) {
      this.loadFrameContent.apply(this, [vnode]);
    }
  }

  view(vnode: m.Vnode<Attrs>) {
    const model    = vnode.attrs.model;
    const attrs    = {src: model.view(), scrolling: "no"};
    const errorXHR = model.errors();

    if (this.hasErrors(errorXHR)) {
      return (<div class={`frame-container ${Styles.frameWrapper}`}>
        <iframe class={Styles.iframe} src={`data:text/html;charset=utf-8,${errorXHR!.responseText}`}/>
      </div>);
    }

    if (this.loadingContent()) {
      return <div class={`frame-container ${Styles.frameWrapper}`}>
        <Spinner/>
      </div>;
    }

    const errorAttrs = errorXHR ? {"data-error-text": errorXHR.responseText} : {};
    return <div class={`frame-container ${Styles.frameWrapper}`} {...errorAttrs}>
      <iframe class={Styles.iframe} sandbox="allow-scripts" {...attrs}/>
    </div>;
  }

  private hasErrors(errorXHR: JQuery.jqXHR | null) {
    return errorXHR && errorXHR.getResponseHeader("content-type")!.indexOf("text/html") !== -1;
  }
}
