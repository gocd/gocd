/*
 * Copyright 2017 ThoughtWorks, Inc.
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
;(function (c) {
  "use strict";

  function AnsiFormatter() {

    function transform(state, ansiUp) {
      if (state.text.length === 0)
        return state.text;

      if (!state.bright && state.fg === null && state.bg === null)
        return state.text;

      var classes = [], styles = [], node_attrs = {};

      var fg = state.fg, bg = state.bg;

      if (fg === null && state.bright)
        fg = ansiUp.ansi_colors[1][7];

      if (fg) {
        if (fg.class_name !== "truecolor") {
          classes.push(fg.class_name + "-fg");
        }
        else {
          styles.push("color:rgb(" + fg.rgb.join(",") + ")");
        }
      }

      if (bg) {
        if (bg.class_name !== "truecolor") {
          classes.push(bg.class_name + "-bg");
        }
        else {
          styles.push("background-color:rgb(" + bg.rgb.join(",") + ")");
        }
      }

      if (classes.length)
        node_attrs["class"] = classes.join(" ");

      if (styles.length)
        node_attrs.style = styles.join(";");
      return c("span", node_attrs, state.text);
    }

    function compose(segments, ansiUp) {
      if (segments.length === 1) return segments[0];

      return segments;
    }

    this.transform = transform;
    this.compose = compose;
  }

  window.AnsiFormatter = AnsiFormatter;
})(crel);
