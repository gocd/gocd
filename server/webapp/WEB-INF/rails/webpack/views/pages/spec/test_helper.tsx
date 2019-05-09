/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import * as $ from "jquery";
import * as m from "mithril";
import * as simulateEvent from "simulate-event";
import {Page} from "views/pages/page";

let mounted = false;
type ElementWithInput = HTMLInputElement | HTMLTextAreaElement;
type ElementWithValue = ElementWithInput | HTMLSelectElement;

export class TestHelper {
  root?: HTMLElement;

  mount(callback: () => m.Children) {
    this.initialize();

    m.mount(this.root!, {
      view() {
        return callback();
      }
    });
    this.redraw();
  }

  mountPage(callback: () => Page<any, any>) {
    this.initialize();

    m.mount(this.root!, callback());
    this.redraw();
  }

  route(defaultRoute: string, callback: () => m.RouteDefs) {
    this.initialize();

    m.route(this.root!, defaultRoute, callback());
    this.redraw();
  }

  unmount(done?: () => void) {
    if (!mounted) {
      throw new Error("Did you forget to mount?");
    }

    mounted = false;

    m.mount(this.root!, null);
    this.redraw();

    this.root!.remove();
    this.root = undefined;

    if (done) {
      done();
    }
  }

  // native implementations - prefer these
  byTestId(id: string, context?: Element): Element {
    return this.q(`[data-test-id='${id}']`, context);
  }

  q(selector: string, context?: Element): Element {
    return (context || this.root!).querySelector(selector)!;
  }

  qa(selector: string, context?: Element): NodeListOf<Element> {
    return (context || this.root!).querySelectorAll(selector);
  }

  text(selector: string, context?: Element): string {
    return (this.q(selector, context).textContent || "").trim();
  }

  textAll(selector: string, context?: Element): string[] {
    const result: string[] = [];
    const elements = Array.from(this.qa(selector, context));

    for (const el of elements) {
      result.push((el.textContent || "").trim());
    }

    return result;
  }

  onchange(selector: string, value: string) {
    const input: ElementWithValue = this.q(selector) as ElementWithValue;
    input.value = value;
    simulateEvent.simulate(input, "change");
    this.redraw();
  }

  oninput(selector: string, value: string) {
    const input: ElementWithInput = this.q(selector) as ElementWithInput;
    input.value = value;
    simulateEvent.simulate(input, "input");
    this.redraw();
  }

  click(selector: string) {
    const element = this.q(selector);
    if (!element) {
      throw new Error(`Unable to find element with selector ${selector}`);
    }
    simulateEvent.simulate(element, "click");
    this.redraw();
  }

  clickButtonOnActiveModal(buttonSelector: string) {
    const element = document.querySelector(`.new-modal-container ${buttonSelector}`);
    if (!element) {
      throw new Error(`Unable to find button with selector ${buttonSelector}`);
    }
    simulateEvent.simulate(element, "click");
    this.redraw();
  }

  redraw() {
    m.redraw();
  }

  dump() {
    // tslint:disable-next-line:no-console
    console.log(this.root!.innerHTML);
  }

  clickByDataTestId(id: string) {
    this.click(this.dataTestIdSelector(id));
  }

  findByClass(className: string) {
    return this.root!.getElementsByClassName(className);
  }

  // jQuery implementations
  findByDataTestId(id: string) {
    return $(this.root!).find(`[data-test-id='${id}']`);
  }

  find(selector: string) {
    return $(this.root!).find(selector);
  }

  findIn(elem: any, id: string) {
    return $(elem).find(this.dataTestIdSelector(id));
  }

  private initialize() {
    if (mounted) {
      throw new Error("Did you forget to unmount from a previous test?");
    }
    mounted   = true;
    this.root = document.createElement("root");
    document.body.appendChild(this.root);
  }

  private dataTestIdSelector(id: string) {
    return `[data-test-id='${id}']`;
  }
}
