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
import m from "mithril";

export abstract class MithrilComponent<Attrs = {}, State = {}> implements m.Component<Attrs, State> {
  // Required for type checking JSX attributes
  // @ts-ignore: unused
  // tslint:disable-next-line
  private __tsx_attrs: Attrs & m.Lifecycle<Attrs, State> & { key?: string | number };

  // Copy of m.Component<A>.view required by TS
  abstract view(vnode: m.Vnode<Attrs, State>): m.Children | null | void;
}

export abstract class MithrilViewComponent<Attrs = {}> implements m.ClassComponent<Attrs> {
  // Required for type checking JSX attributes
  // @ts-ignore: unused
  // tslint:disable-next-line
  private __tsx_attrs: Attrs & m.Lifecycle<Attrs, this> & { key?: string | number };

  // Copy of m.ClassComponent<A>.view required by TS
  abstract view(vnode: m.Vnode<Attrs, this>): m.Children | null | void;
}

export interface RestyleAttrs<T> {
  [key: string]: any;

  css?: T;
}

export abstract class RestyleViewComponent<S, Attrs extends RestyleAttrs<S> = RestyleAttrs<S>> extends MithrilViewComponent<Attrs> {
  abstract css: S;

  oninit(vnode: m.Vnode<Attrs, {}>) {
    adoptStylesheet(this, vnode);
  }
}

export abstract class RestyleComponent<S, Attrs extends RestyleAttrs<S> = RestyleAttrs<S>, State = {}> extends MithrilComponent<Attrs, State> {
  abstract css: S;

  oninit(vnode: m.Vnode<Attrs, State>) {
    adoptStylesheet(this, vnode);
  }
}

interface Restylable<T> {
  css: T;
}

function adoptStylesheet<T, U extends RestyleAttrs<T>>(receiver: Restylable<T>, vnode: m.Vnode<U>) {
  if (vnode.attrs.css) {
    receiver.css = vnode.attrs.css;
  }
}

// Set up type checks
declare global {
  namespace JSX {
    // Where to look for component type information
    interface ElementAttributesProperty {
      __tsx_attrs: any;

      [name: string]: any;
    }
  }
}
