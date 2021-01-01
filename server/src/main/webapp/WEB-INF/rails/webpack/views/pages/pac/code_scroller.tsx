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

import {asSelector} from "helpers/css_proxies";
import {NonThrashingScheduler} from "helpers/scheduler";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import headerStyles from "views/components/header_panel/index.scss";
import defaultStyles from "views/pages/pac/styles.scss";

// @ts-ignore
import {throttleRaf} from "helpers/throttle-raf";

const headerSelector = asSelector(headerStyles).pageHeader;
const {builderForm, previewPane} = asSelector(defaultStyles);
const MIN_CODE_PANE_HEIGHT = 500;

interface State {
  onscroll: () => void;
  teardown: () => void;
}

export class CodeScroller extends MithrilComponent<{}, State> {
  oncreate(vnode: m.VnodeDOM<{}, State>) {
    const container = vnode.dom as HTMLElement; // should be the fillable section

    // find dependent elements
    const form = container.querySelector(builderForm) as HTMLElement;
    const pane = container.querySelector(previewPane) as HTMLElement;
    const innerCodePane = pane.querySelector("code") as HTMLElement;

    // measure static distances
    const {paddingTop, paddingBottom} = verticalPadding(pane);
    const headerBottom = document.querySelector(headerSelector)!.getBoundingClientRect().bottom;

    // element used to push/pull content vertically
    const spacer = document.createElement("div");

    const keepInView = throttleRaf(() => {
      const paneRect = pane.getBoundingClientRect();
      const formBottom = (form.lastElementChild as HTMLElement).getBoundingClientRect().bottom;
      const availHeight = formBottom - paneRect.top;

      const distance = headerBottom - paneRect.top;
      const limit = Math.floor(availHeight - paddingTop - MIN_CODE_PANE_HEIGHT);

      if (distance > 0) {
        const offset = Math.floor(Math.min(distance, limit));
        spacer.style.height = offset + "px";
        innerCodePane.style.maxHeight = (availHeight - offset - paddingTop - paddingBottom) + "px";
      } else {
        spacer.style.height = "0";
        // @ts-ignore
        delete innerCodePane.style.maxHeight;
      }
    });

    // scheduler ensures scroll adjustments are not fired too often by the mutation observer
    const scheduler = new NonThrashingScheduler();
    const observer = new MutationObserver(() => scheduler.schedule(keepInView));

    observer.observe(form, {subtree: true, childList: true, attributes: true});
    observer.observe(innerCodePane, {subtree: true, childList: true});

    vnode.state.onscroll = () => { observer.takeRecords(), keepInView(); };
    vnode.state.teardown = () => {
      pane.removeChild(spacer);
      observer.disconnect();
      // @ts-ignore
      delete innerCodePane.style.maxHeight;
    };

    pane.insertBefore(spacer, innerCodePane);
    window.addEventListener("scroll", vnode.state.onscroll);
  }

  view(vnode: m.Vnode<{}, State>) {
    return vnode.children; // pass-through component
  }

  onbeforeremove(vnode: m.VnodeDOM<{}, State>) {
    window.removeEventListener("scroll", vnode.state.onscroll);
    vnode.state.teardown();
  }
}

function verticalPadding(el: HTMLElement) {
  const __s = window.getComputedStyle(el);
  return { paddingTop: parseInt(__s.paddingTop!, 10), paddingBottom: parseInt(__s.paddingBottom!, 10) };
}
