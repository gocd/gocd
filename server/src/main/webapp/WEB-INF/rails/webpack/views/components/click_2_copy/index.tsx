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
import classnames from "classnames";
import { writeText } from "clipboard-polyfill";
import { RestyleAttrs, RestyleViewComponent } from "jsx/mithril-component";
import m from "mithril";
import { asPromise } from "models/base/accessor";
import { EventAware } from "models/mixins/event_aware";
import * as defaultStyles from "./index.scss";

type Styles = typeof defaultStyles;
type IconChoice = "clip" | "copy";
type Handler = () => void;

interface ClickAttrs extends RestyleAttrs<Styles> {
  icon?: IconChoice;       // choose the display icon
  reader: Promise<string>; // provides the text to copy

  // allowing the copy function to be overridden gives flexibility to transform text before it
  // hits the clipboard and is also good for tests
  copier?: (text: string) => Promise<void>;

  // CONSIDER: Should we manage the event hook lifecycle in a separate view model? That would
  // allow other components or objects to register events on this component more easily.
  onwait?: Handler;
  ondata?: Handler;
  oncopy?: Handler;
  onuichange?: Handler;
  onuiacclaim?: Handler;
  onuidenounce?: Handler;
  onuirestore?: Handler;
  ondatafail?: Handler;
  oncopyfail?: Handler;
  onfail?: Handler;
}

// tslint:disable-next-line no-empty-interface
export interface Click2Copy extends EventAware { }

/** A somewhat fancy click-to-copy button that features UI-feedback and event hooks */
export class Click2Copy extends RestyleViewComponent<Styles, ClickAttrs> {
  css = defaultStyles;

  private readonly timers = new Map<string, number>();
  private override?: string;

  constructor() {
    super();
    Object.assign(Click2Copy.prototype, EventAware.prototype);
    EventAware.call(this);
  }

  oninit(vnode: m.Vnode<ClickAttrs>) {
    const a = vnode.attrs;

    if (a.ondata) {
      this.on("reader:data", a.ondata);
    }

    if (a.oncopy) {
      this.on("copy", a.oncopy);
    }

    if (a.ondatafail) {
      this.on("reader:error", a.ondatafail);
    }

    if (a.oncopyfail) {
      this.on("copy:error", a.oncopyfail);
    }

    if (a.onfail) {
      this.on("reader:error", a.onfail);
      this.on("copy:error", a.onfail);
    }

    if (a.onwait) {
      this.on("ui:wait", a.onwait);
    }

    if (a.onuiacclaim) {
      this.on("ui:acclaim", a.onuiacclaim);
    }

    if (a.onuidenounce) {
      this.on("ui:denounce", a.onuidenounce);
    }

    if (a.onuichange) {
      this.on("ui:acclaim", a.onuichange);
      this.on("ui:denounce", a.onuichange);
    }

    if (a.onuirestore) {
      this.on("ui:restore", a.onuirestore);
    }
  }

  view(vnode: m.Vnode<ClickAttrs>) {
    const { icon } = vnode.attrs;
    const htmlAttrs = {
      class: classnames(this.css.clipButton, this.override || this.icon(icon)),
      onclick: this.onclick(vnode)
    };

    return <button {...htmlAttrs} />;
  }

  onclick(vnode: m.Vnode<ClickAttrs>) {
    const { reader, copier } = vnode.attrs;
    const copy = copier || writeText;

    return (e: MouseEvent) => {
      e.stopPropagation();
      e.preventDefault();

      this.waitIcon();

      reader.then((data) => {
        this.notify("reader:data");
        this.clearTimers();
        copy(data).then(() => {
          this.notify("copy");

          this.override = this.css.iconCheck;
          m.redraw();
          this.notify("ui:acclaim");

          this.revertIcon();
        }).catch(() => this.failWithEvent("copy:error"));
      }).catch(() => this.failWithEvent("reader:error"));
    };
  }

  private failWithEvent(event: string) {
    this.notify(event);

    this.override = this.css.iconFail;
    m.redraw();
    this.notify("ui:denounce");

    this.revertIcon();
  }

  private icon(icon: IconChoice = "clip") {
    return icon === "copy" ? this.css.iconCopy : this.css.iconClip;
  }

  private waitIcon() {
    this.clearTimers();

    const waiting: TimerHandler = () => {
      this.override = this.css.iconEllipsis;
      m.redraw();
      this.notify("ui:wait");
    };

    // only change to wait icon if the operation seems to take a while
    this.timers.set("wait", setTimeout(waiting, 150));
  }

  private revertIcon() {
    this.clearTimers();

    const restore: TimerHandler = () => {
      delete this.override;
      m.redraw();
      this.notify("ui:restore");
    };

    this.timers.set("finish", setTimeout(restore, 1000));
  }

  private clearTimers() {
    this.timers.forEach((t, n) => (clearTimeout(t), this.timers.delete(n)));
  }
}

interface Attrs extends Pick<ClickAttrs, "icon" | "copier" | "oncopy">{
  reader: () => string;
}

export class CopySnippet extends RestyleViewComponent<Styles, Attrs> {
  css = defaultStyles;
  indicate = false;

  oncreate(vnode: m.VnodeDOM<Attrs>) {
    vnode.dom.querySelector(`.${this.css.snippet}`)!.addEventListener("click", (e) => {
      const range = document.createRange();
      range.selectNodeContents(e.currentTarget as Node);

      const selection = (document.getSelection() || window.getSelection())!;
      selection.removeAllRanges();
      selection.addRange(range);
    });
  }

  view(vnode: m.Vnode<Attrs>) {
    const { reader, copier, icon, oncopy } = vnode.attrs;
    return <div class={this.css.snipNClip}>
      <pre class={this.inputClasses()}>{reader()}</pre>
      <Click2Copy css={this.css} reader={asPromise(reader)} copier={copier} icon={icon}
        oncopy={oncopy}
        onuichange={() => { this.indicate = true; }}
        onuirestore={() => { this.indicate = false; }} />
    </div>;
  }

  private inputClasses() {
    return classnames(this.css.snippet, { [this.css.copied]: this.indicate });
  }
}
