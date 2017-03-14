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
;(function (_, c) {
  "use strict";

  function HelloMrfANSIpants() {
    this._sgr_regex = /^([!\x3c-\x3f]?)([\d;]*)([\x20-\x2f]?[\x40-\x7e])([\s\S]*)/m;
    this.ansi_colors = [
        [
            { rgb: [0, 0, 0], class_name: "ansi-black" },
            { rgb: [187, 0, 0], class_name: "ansi-red" },
            { rgb: [0, 187, 0], class_name: "ansi-green" },
            { rgb: [187, 187, 0], class_name: "ansi-yellow" },
            { rgb: [0, 0, 187], class_name: "ansi-blue" },
            { rgb: [187, 0, 187], class_name: "ansi-magenta" },
            { rgb: [0, 187, 187], class_name: "ansi-cyan" },
            { rgb: [255, 255, 255], class_name: "ansi-white" }
        ],
        [
            { rgb: [85, 85, 85], class_name: "ansi-bright-black" },
            { rgb: [255, 85, 85], class_name: "ansi-bright-red" },
            { rgb: [0, 255, 0], class_name: "ansi-bright-green" },
            { rgb: [255, 255, 85], class_name: "ansi-bright-yellow" },
            { rgb: [85, 85, 255], class_name: "ansi-bright-blue" },
            { rgb: [255, 85, 255], class_name: "ansi-bright-magenta" },
            { rgb: [85, 255, 255], class_name: "ansi-bright-cyan" },
            { rgb: [255, 255, 255], class_name: "ansi-bright-white" }
        ]
    ];
    this.setup_256_palette();
    this.bright = false;
    this.fg = this.bg = null;
    this._buffer = '';
    this._sgr_regex = /^([!\x3c-\x3f]?)([\d;]*)([\x20-\x2f]?[\x40-\x7e])([\s\S]*)/m;
  }

  HelloMrfANSIpants.prototype.setup_256_palette = function () {
      var _this = this;
      this.palette_256 = [];
      this.ansi_colors.forEach(function (palette) {
          palette.forEach(function (rec) {
              _this.palette_256.push(rec);
          });
      });
      var levels = [0, 95, 135, 175, 215, 255];
      for (var r = 0; r < 6; ++r) {
          for (var g = 0; g < 6; ++g) {
              for (var b = 0; b < 6; ++b) {
                  var c = { rgb: [levels[r], levels[g], levels[b]], class_name: 'truecolor' };
                  this.palette_256.push(c);
              }
          }
      }
      var grey_level = 8;
      for (var i = 0, col; i < 24; ++i, grey_level += 10) {
          col = { rgb: [grey_level, grey_level, grey_level], class_name: 'truecolor' };
          this.palette_256.push(col);
      }
  };

  HelloMrfANSIpants.prototype.detect_incomplete_ansi = function (txt) {
    return !(/.*?[\x40-\x7e]/.test(txt));
  };

  HelloMrfANSIpants.prototype.process = function (txt) {
    var _this = this;
    var pkt = this._buffer + txt;
    this._buffer = '';
    var raw_text_pkts = pkt.split(/\x1B\[/);
    if (raw_text_pkts.length == 1)
      raw_text_pkts.push('');
    var last_pkt = raw_text_pkts[raw_text_pkts.length - 1];
    if ((last_pkt.length > 0) && this.detect_incomplete_ansi(last_pkt)) {
      this._buffer = "\x1B[" + last_pkt;
      raw_text_pkts.pop();
      raw_text_pkts.push('');
    } else {
      if (last_pkt.slice(-1) == "\x1B") {
        this._buffer = "\x1B";
        console.log("raw", raw_text_pkts);
        raw_text_pkts.pop();
        raw_text_pkts.push(last_pkt.substr(0, last_pkt.length - 1));
        console.log(raw_text_pkts);
        console.log(last_pkt);
      }
      if ((raw_text_pkts.length === 2) && (raw_text_pkts[1] === "") && (raw_text_pkts[0].slice(-1) === "\x1B")) {
        this._buffer = "\x1B";
        last_pkt = raw_text_pkts.shift();
        raw_text_pkts.unshift(last_pkt.substr(0, last_pkt.length - 1));
      }
    }
    var first_txt = this.maybe_wrap_text_in_node(raw_text_pkts.shift());
    var blocks = _.map(raw_text_pkts, function (block) { return _this.maybe_wrap_text_in_node(_this.process_ansi(block)); });
    if (first_txt instanceof Node || first_txt.length > 0) blocks.unshift(first_txt);
    if (blocks.length === 1) return blocks[0];
    return blocks;
  };

  HelloMrfANSIpants.prototype.maybe_wrap_text_in_node = function (txt) {
    if (txt.length === 0)
      return txt;
    if (!this.bright && this.fg === null && this.bg === null)
      return txt;
    var styles = [];
    var classes = [];
    var fg = this.fg;
    var bg = this.bg;
    if (fg === null && this.bright)
      fg = this.ansi_colors[1][7];

    if (fg) {
      if (fg.class_name != 'truecolor') {
        classes.push(fg.class_name + "-fg");
      }
      else {
        styles.push("color:rgb(" + fg.rgb.join(',') + ")");
      }
    }
    if (bg) {
      if (bg.class_name != 'truecolor') {
        classes.push(bg.class_name + "-bg");
      }
      else {
        styles.push("background-color:rgb(" + bg.rgb.join(',') + ")");
      }
    }

    var node_attr = {};

    if (classes.length)
      node_attr["class"] = classes.join(' ');
    if (styles.length)
      node_attr.style = styles.join(';');
    return c("span", node_attr, txt);
  };

  HelloMrfANSIpants.prototype.process_ansi = function (block) {
    var matches = block.match(this._sgr_regex);
    if (!matches)
      return block;
    var orig_txt = matches[4];
    if (matches[1] !== '' || matches[3] !== 'm')
      return orig_txt;
    var sgr_cmds = matches[2].split(';');
    while (sgr_cmds.length > 0) {
      var sgr_cmd_str = sgr_cmds.shift();
      var num = parseInt(sgr_cmd_str, 10);
      if (isNaN(num) || num === 0) {
        this.fg = this.bg = null;
        this.bright = false;
      }
      else if (num === 1) {
        this.bright = true;
      }
      else if (num == 39) {
        this.fg = null;
      }
      else if (num == 49) {
        this.bg = null;
      }
      else if ((num >= 30) && (num < 38)) {
        var bidx = this.bright ? 1 : 0;
        this.fg = this.ansi_colors[bidx][(num - 30)];
      }
      else if ((num >= 90) && (num < 98)) {
        this.fg = this.ansi_colors[1][(num - 90)];
      }
      else if ((num >= 40) && (num < 48)) {
        this.bg = this.ansi_colors[0][(num - 40)];
      }
      else if ((num >= 100) && (num < 108)) {
        this.bg = this.ansi_colors[1][(num - 100)];
      }
      else if (num === 38 || num === 48) {
        if (sgr_cmds.length > 0) {
          var is_foreground = (num === 38);
          var mode_cmd = sgr_cmds.shift();
          if (mode_cmd === '5' && sgr_cmds.length > 0) {
            var palette_index = parseInt(sgr_cmds.shift(), 10);
            if (palette_index >= 0 && palette_index <= 255) {
              if (is_foreground)
                this.fg = this.palette_256[palette_index];
              else
                this.bg = this.palette_256[palette_index];
            }
          }
          if (mode_cmd === '2' && sgr_cmds.length > 2) {
            var r = parseInt(sgr_cmds.shift(), 10);
            var g = parseInt(sgr_cmds.shift(), 10);
            var b = parseInt(sgr_cmds.shift(), 10);
            if ((r >= 0 && r <= 255) && (g >= 0 && g <= 255) && (b >= 0 && b <= 255)) {
              var c = { rgb: [r, g, b], class_name: 'truecolor' };
              if (is_foreground)
                this.fg = c;
              else
                this.bg = c;
            }
          }
        }
      }
    }
    return orig_txt;
  };

  window.HelloMrfANSIpants = HelloMrfANSIpants;
})(_, crel);

