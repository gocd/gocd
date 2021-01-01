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

import {OnloadScheduler, Scheduler} from "helpers/scheduler";
import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import headerStyles from "views/components/header_panel/index.scss";

const headerSelector = `.${headerStyles.pageHeader}`;

interface Attrs {
  id: string;
  sm: ScrollManager;
  onnavigate?: () => void;
}

interface State {
  dom: Element;
}

/** Represents a the stateful object to control the autoscroll criteria and scroll capability. */
export interface ScrollManager {
  getTarget(): string;
  setTarget(target: string): void;
  shouldScroll(criteria: string): boolean;
  scrollToEl(dom: Element): void;
  hasTarget(): boolean;
}

/**
 * Roughly simulates the behavior of a named anchor, but does not actually
 * generate any DOM elements. Rather, this component wraps and passes through
 * existing components with new behavior to allow components to be programmatically
 * (automatically?) scrolled into view through a `ScrollManager`.
 */
export class Anchor extends MithrilComponent<Attrs, State> {
  private inited = false;

  oncreate(vnode: m.VnodeDOM<Attrs, State>) {
    vnode.state.dom = vnode.dom;
    this.inited     = true;
    this.scrollToEl(vnode);
  }

  view(vnode: m.Vnode<Attrs, State>) {
    this.scrollToEl(vnode);
    return vnode.children;
  }

  scrollToEl(vnode: m.Vnode<Attrs, State>) {
    const {id, sm} = vnode.attrs;
    if (this.inited && sm.shouldScroll(id)) {
      sm.scrollToEl(vnode.state.dom);

      if ("function" === typeof vnode.attrs.onnavigate) {
        vnode.attrs.onnavigate();
      }
    }
  }
}

/**
 * The main `ScrollManager` implementation that keeps track of a scroll target (a string
 * identifier), used in `<Anchor/>`. It has facilities to automatically scroll to an
 * arbitrary element, which when used in conjunction with `<Anchor/>`, will automatically
 * scroll the page to the `<Anchor/>` represented by the scroll target identifier.
 *
 * This will typically be a singleton per SPA, but there is no restriction to do otherwise,
 * depending on the needs of the application.
 */
export class AnchorVM implements ScrollManager {
  private target: string = "";
  private hasMoved: boolean = false;
  private readonly scheduler: Scheduler = new OnloadScheduler();

  getTarget() {
    return this.target;
  }

  setTarget(target: string) {
    if (target !== this.target) {
      this.target = target;
      this.hasMoved = false;
    }
  }

  hasTarget(): boolean {
    return !_.isEmpty(this.target);
  }

  /**
   * Tests whether or not a specified identifier matches the scroll targer identifier. This is
   * designed to be called during `view()`, thereby succeeding at most once per target change.
   * This detail is important because `view()` may be run multiple times. By succeeding at most
   * once per target change, this ensures that a scroll is only ever scheduled once upon updating
   * the scroll target, even if the page redraws a few times in the meanwhile.
   */
  shouldScroll(criteria: string) {
    const result = this.target === criteria && !this.hasMoved;
    if (result) {
      this.hasMoved = true;
    }

    return result;
  }

  /**
   * Brings the top of `el` as close as possible to the bottom of the fixed-position page header. Visually,
   * this is the "top" of the page.
   */
  scrollToEl(el: Element) {
    this.scheduler.schedule(() => {
      const headerBottom = document.querySelector(headerSelector)!.getBoundingClientRect().bottom;
      const distance = el.getBoundingClientRect().top - (headerBottom + 10); // give a little extra space
      window.scrollBy(0, distance);
    });
  }
}
