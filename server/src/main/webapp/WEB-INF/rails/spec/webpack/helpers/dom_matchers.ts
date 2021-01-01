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

import _ from "lodash";

beforeEach(() => {
  jasmine.addMatchers({
    toBeChecked() {
      return {
        compare(actual: HTMLElement) {
          return { pass: !!(actual && (selfOrFirst(actual) as HTMLInputElement).checked) };
        }
      };
    },

    toBeDisabled() {
      return {
        compare(actual: HTMLElement) {
          actual = selfOrFirst(actual);
          return { pass: !!actual && actual.hasAttribute("disabled") };
        }
      };
    },

    toBeEmpty() {
      return {
        compare(actual: HTMLElement) {
          return { pass: !actual.childNodes.length };
        }
      };
    },

    toBeHidden() {
      return {
        compare(actual: HTMLElement | NodeCollection) {
          return { pass: allSatisfyPredicate(actual, (el) => !el.getClientRects().length) };
        }
      };
    },

    toBeInDOM() {
      return {
        compare(actual: HTMLElement | NodeCollection) {
          const el = selfOrFirst(actual);
          return { pass: !!el && document.documentElement.contains(el) };
        }
      };
    },

    toBeVisible() {
      return {
        compare(actual: HTMLElement | NodeCollection) {
          return { pass: allSatisfyPredicate(actual, (el) => !!el.getClientRects().length) };
        }
      };
    },

    toContainElement() {
      return {
        compare(actual: HTMLElement | NodeCollection, expected: string) {
          return { pass: satisfiesPredicate(actual, (el) => !!el.querySelector(expected)) };
        }
      };
    },

    toContainHtml() {
      return {
        compare(actual: HTMLElement, expected: string) {
          actual = selfOrFirst(actual);
          return { pass: actual.innerHTML.includes(normalizeHTML(expected)) };
        }
      };
    },

    toContainText() {
      return {
        compare(actual: HTMLElement, expected: string) {
          if (!actual) {
            return { pass: false };
          }

          const allText = isNodeCollection(actual) ? _.map(actual, (el) => el.textContent).join("") : actual.textContent || "";
          return { pass: allText.includes(expected) };
        }
      };
    },

    toExist() {
      return {
        compare(actual: HTMLElement | NodeCollection) {
          return { pass: satisfiesPredicate(actual, (el) => !!el) };
        }
      };
    },

    toHaveAttr() {
      return {
        compare(actual: HTMLElement, attr: string, expected?: any) {
          actual = selfOrFirst(actual);

          if (!actual) {
            return { pass: false };
          }

          if (arguments.length > 2) {
            return { pass: actual.hasAttribute(attr) && expected === actual.getAttribute(attr) };
          }

          return { pass: actual.hasAttribute(attr) };
        }
      };
    },

    toHaveHtml() {
      return {
        compare(actual: HTMLElement, expected: string) {
          actual = selfOrFirst(actual);
          return { pass: actual.innerHTML === normalizeHTML(expected) };
        }
      };
    },

    toHaveLength() {
      return {
        compare(actual: {length: number}, expected: number) {
          return { pass: expected === actual.length };
        }
      };
    },

    toHaveProp() {
      return {
        compare(actual: HTMLElement, prop: string, expected?: any) {
          actual = selfOrFirst(actual);

          if (!actual) {
            return { pass: false };
          }

          if (arguments.length > 2) {
            return { pass: (prop in actual) && expected === (actual as any)[prop] };
          }
          return { pass: (prop in actual) };
        }
      };
    },

    toHaveText() {
      return {
        compare(actual: HTMLElement | NodeCollection, expected: string) {
          if (!actual) {
            return { pass: false };
          }

          const allText = isNodeCollection(actual) ? _.map(actual, (el) => el.textContent).join("") : actual.textContent || "";
          return { pass: expected === allText };
        }
      };
    },

    toHaveValue() {
      return {
        compare(actual: HTMLElement, expected: string) {
          return { pass: !!actual && expected === (actual as HTMLInputElement).value }; // does not handle select-multiple elements as jQuery does
        }
      };
    },

  });
});

function selfOrFirst(el: HTMLElement | NodeCollection): HTMLElement {
  return ((el && isNodeCollection(el)) ? el.item(0) : el) as HTMLElement;
}

function normalizeHTML(html: string) {
  const buf = document.createElement("div");
  buf.innerHTML = html;
  return buf.innerHTML;
}

function satisfiesPredicate(actual: HTMLElement | NodeCollection, pred: Predicate<HTMLElement>) {
  if (isNodeCollection(actual)) {
    return !!actual.length && _.some(actual, pred);
  }
  return pred(actual);
}

function allSatisfyPredicate(actual: HTMLElement | NodeCollection, pred: Predicate<HTMLElement>) {
  if (isNodeCollection(actual)) {
    return !!actual.length && _.every(actual, pred);
  }
  return pred(actual);
}

type Predicate<T> = (item: T) => boolean;
type NodeCollection = NodeListOf<HTMLElement> | HTMLCollectionBase;

function isNodeCollection(thing: any): thing is NodeCollection {
  // testing for `length` alone can give false positives for <select/>
  return !!thing && "function" === typeof thing.entries;
}
