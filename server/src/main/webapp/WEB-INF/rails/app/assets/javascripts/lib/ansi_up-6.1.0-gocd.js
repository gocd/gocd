/*  ansi_up.js
 *  author : http://github.com/drudru/ansi_up
 *  license : MIT
 *  http://github.com/drudru/ansi_up
 */
(function (root, factory) {
  if (typeof define === 'function' && define.amd) {
    // AMD. Register as an anonymous module.
    define(['exports'], factory);
  } else if (typeof exports === 'object' && typeof exports.nodeName !== 'string') {
    // CommonJS
    factory(exports);
  } else {
    // Browser globals
    var exp = {};
    factory(exp);
    root.AnsiUp = exp.default;
  }
}(this, function (exports) {
"use strict";
var PacketKind;
(function (PacketKind) {
  PacketKind[PacketKind["EOS"] = 0] = "EOS";
  PacketKind[PacketKind["Text"] = 1] = "Text";
  PacketKind[PacketKind["Incomplete"] = 2] = "Incomplete";
  PacketKind[PacketKind["ESC"] = 3] = "ESC";
  PacketKind[PacketKind["Unknown"] = 4] = "Unknown";
  PacketKind[PacketKind["SGR"] = 5] = "SGR";
  PacketKind[PacketKind["OSCURL"] = 6] = "OSCURL";
  PacketKind[PacketKind["OSCURLEND"] = 7] = "OSCURLEND";
})(PacketKind || (PacketKind = {}));
class AnsiUp {
  constructor() {
    this.VERSION = "6.1.0-gocd";
    this.setup_palettes();
    this._use_classes = false;
    this.bold = false;
    this.faint = false;
    this.italic = false;
    this.underline = false;
    this.fg = this.bg = null;
    this._buffer = '';
    this._url_allowlist = { 'http': 1, 'https': 1 };
    this._escape_html = true;
    this.boldStyle = 'font-weight:bold';
    this.faintStyle = 'opacity:0.7';
    this.italicStyle = 'font-style:italic';
    this.underlineStyle = 'text-decoration:underline';
  }
  set use_classes(arg) {
    this._use_classes = arg;
  }
  get use_classes() {
    return this._use_classes;
  }
  set url_allowlist(arg) {
    this._url_allowlist = arg;
  }
  get url_allowlist() {
    return this._url_allowlist;
  }
  set escape_html(arg) {
    this._escape_html = arg;
  }
  get escape_html() {
    return this._escape_html;
  }
  set boldStyle(arg) { this._boldStyle = arg; }
  get boldStyle() { return this._boldStyle; }
  set faintStyle(arg) { this._faintStyle = arg; }
  get faintStyle() { return this._faintStyle; }
  set italicStyle(arg) { this._italicStyle = arg; }
  get italicStyle() { return this._italicStyle; }
  set underlineStyle(arg) { this._underlineStyle = arg; }
  get underlineStyle() { return this._underlineStyle; }
  setup_palettes() {
    this.ansi_colors =
      [
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
    this.palette_256 = [];
    this.ansi_colors.forEach(palette => {
      palette.forEach(rec => {
        this.palette_256.push(rec);
      });
    });
    let levels = [0, 95, 135, 175, 215, 255];
    for (let r = 0; r < 6; ++r) {
      for (let g = 0; g < 6; ++g) {
        for (let b = 0; b < 6; ++b) {
          let col = { rgb: [levels[r], levels[g], levels[b]], class_name: 'truecolor' };
          this.palette_256.push(col);
        }
      }
    }
    let grey_level = 8;
    for (let i = 0; i < 24; ++i, grey_level += 10) {
      let gry = { rgb: [grey_level, grey_level, grey_level], class_name: 'truecolor' };
      this.palette_256.push(gry);
    }
  }
  escape_txt_for_html(txt) {
    if (!this._escape_html)
      return txt;
    return txt.replace(/[&<>"']/gm, (str) => {
      if (str === "&")
        return "&amp;";
      if (str === "<")
        return "&lt;";
      if (str === ">")
        return "&gt;";
      if (str === "\"")
        return "&quot;";
      if (str === "'")
        return "&#x27;";
    });
  }
  append_buffer(txt) {
    var str = this._buffer + txt;
    this._buffer = str;
  }
  get_next_packet() {
    var pkt = {
      kind: PacketKind.EOS,
      text: ''
    };
    var len = this._buffer.length;
    if (len == 0)
      return pkt;
    var pos = this._buffer.indexOf("\x1B");
    if (pos == -1) {
      pkt.kind = PacketKind.Text;
      pkt.text = this._buffer;
      this._buffer = '';
      return pkt;
    }
    if (pos > 0) {
      pkt.kind = PacketKind.Text;
      pkt.text = this._buffer.slice(0, pos);
      this._buffer = this._buffer.slice(pos);
      return pkt;
    }
    if (pos == 0) {
      if (len < 3) {
        pkt.kind = PacketKind.Incomplete;
        return pkt;
      }
      var next_char = this._buffer.charAt(1);
      if ((next_char != '[') && (next_char != ']') && (next_char != '(')) {
        pkt.kind = PacketKind.ESC;
        pkt.text = this._buffer.slice(0, 1);
        this._buffer = this._buffer.slice(1);
        return pkt;
      }
      if (next_char == '[') {
        if (!this._csi_regex) {
          this._csi_regex = rgx `
                        ^                           # beginning of line
                                                    #
                                                    # First attempt
                        (?:                         # legal sequence
                          \x1b\[                      # CSI
                          ([\x3c-\x3f]?)              # private-mode char
                          ([\d;:]*)                    # any digits or semicolons or colons
                          ([\x20-\x2f]?               # an intermediate modifier
                          [\x40-\x7e])                # the command
                        )
                        |                           # alternate (second attempt)
                        (?:                         # illegal sequence
                          \x1b\[                      # CSI
                          [\x20-\x7e]*                # anything legal
                          ([\x00-\x1f:])              # anything illegal
                        )
                    `;
        }
        let match = this._buffer.match(this._csi_regex);
        if (match === null) {
          pkt.kind = PacketKind.Incomplete;
          return pkt;
        }
        if (match[4]) {
          pkt.kind = PacketKind.ESC;
          pkt.text = this._buffer.slice(0, 1);
          this._buffer = this._buffer.slice(1);
          return pkt;
        }
        if ((match[1] != '') || (match[3] != 'm'))
          pkt.kind = PacketKind.Unknown;
        else
          pkt.kind = PacketKind.SGR;
        pkt.text = match[2];
        var rpos = match[0].length;
        this._buffer = this._buffer.slice(rpos);
        return pkt;
      }
      else if (next_char == ']') {
        if (len < 4) {
          pkt.kind = PacketKind.Incomplete;
          return pkt;
        }
        if ((this._buffer.charAt(2) != '8')
          || (this._buffer.charAt(3) != ';')) {
          pkt.kind = PacketKind.ESC;
          pkt.text = this._buffer.slice(0, 1);
          this._buffer = this._buffer.slice(1);
          return pkt;
        }
        if (!this._osc_st) {
          this._osc_st = rgxG `
                        (?:                         # legal sequence
                          (\x1b\\)                    # ESC \
                          |                           # alternate
                          (\x07)                      # BEL (what xterm did)
                        )
                        |                           # alternate (second attempt)
                        (                           # illegal sequence
                          [\x00-\x06]                 # anything illegal
                          |                           # alternate
                          [\x08-\x1a]                 # anything illegal
                          |                           # alternate
                          [\x1c-\x1f]                 # anything illegal
                        )
                    `;
        }
        this._osc_st.lastIndex = 0;
        {
          let match = this._osc_st.exec(this._buffer);
          if (match === null) {
            pkt.kind = PacketKind.Incomplete;
            return pkt;
          }
          if (match[3]) {
            pkt.kind = PacketKind.ESC;
            pkt.text = this._buffer.slice(0, 1);
            this._buffer = this._buffer.slice(1);
            return pkt;
          }
        }
        if (!this._osc_regex) {
          this._osc_regex = rgx `
                        ^                           # beginning of line
                                                    #
                        \x1b\]8;                    # OSC Hyperlink
                        [\x20-\x3a\x3c-\x7e]*       # params (excluding ;)
                        ;                           # end of params
                        ([\x21-\x7e]{0,512})        # URL capture
                        (?:                         # ST
                          (?:\x1b\\)                  # ESC \
                          |                           # alternate
                          (?:\x07)                    # BEL (what xterm did)
                        )
                    `;
        }
        let match = this._buffer.match(this._osc_regex);
        if (match === null) {
          pkt.kind = PacketKind.ESC;
          pkt.text = this._buffer.slice(0, 1);
          this._buffer = this._buffer.slice(1);
          return pkt;
        }
        pkt.kind = match[1] ? PacketKind.OSCURL : PacketKind.OSCURLEND;
        pkt.text = match[1];
        var rpos = match[0].length;
        this._buffer = this._buffer.slice(rpos);
        return pkt;
      }
      else if (next_char == '(') {
        pkt.kind = PacketKind.Unknown;
        this._buffer = this._buffer.slice(3);
        return pkt;
      }
    }
  }
  ansi_to_html(txt) {
    const nodes = this.ansi_to_structured(txt);
    return this.render_nodes_to_html(nodes);
  }
  ansi_to_structured(txt) {
    this.append_buffer(txt);
    const rootNodes = [];
    const renderCtx = [];
    let pendingText = '';
    while (true) {
      const packet = this.get_next_packet();
      if (packet.kind === PacketKind.EOS || packet.kind === PacketKind.Incomplete) {
        break;
      }
      if (packet.kind === PacketKind.ESC || packet.kind === PacketKind.Unknown)
        continue;
      if (packet.kind === PacketKind.Text) {
        pendingText += packet.text;
      }
      else if (packet.kind === PacketKind.SGR) {
        this.flush_text(pendingText, renderCtx, rootNodes);
        pendingText = '';
        this.process_ansi(packet);
        this.update_style_stack(renderCtx);
      }
      else if (packet.kind === PacketKind.OSCURL) {
        this.flush_text(pendingText, renderCtx, rootNodes);
        pendingText = '';
        let parts = packet.text.split(':');
        if (parts.length >= 1 && this._url_allowlist[parts[0]]) {
          renderCtx.push({ type: 'url', url: packet.text, children: [] });
        }
      }
      else if (packet.kind === PacketKind.OSCURLEND) {
        this.flush_text(pendingText, renderCtx, rootNodes);
        pendingText = '';
        this.close_url_frame(renderCtx, rootNodes);
      }
    }
    this.flush_text(pendingText, renderCtx, rootNodes);
    this.close_url_frame(renderCtx, rootNodes);
    return rootNodes;
  }
  flush_text(pendingText, render_ctx_stack, rootNodes) {
    if (pendingText.length === 0)
      return;
    let node = { type: 'text', text: pendingText };
    const styleFrame = render_ctx_stack.find(f => f.type === 'style');
    if (styleFrame) {
      node = {
        type: 'styled',
        attrs: styleFrame.attrs,
        children: [node]
      };
    }
    const urlFrame = render_ctx_stack.find(f => f.type === 'url');
    if (urlFrame) {
      urlFrame.children.push(node);
    }
    else {
      rootNodes.push(node);
    }
  }
  update_style_stack(render_ctx_stack) {
    const filtered = render_ctx_stack.filter(f => f.type !== 'style');
    render_ctx_stack.splice(0, render_ctx_stack.length, ...filtered);
    if (this.bold || this.faint || this.italic || this.underline || this.fg || this.bg) {
      render_ctx_stack.push({
        type: 'style',
        attrs: { bold: this.bold, faint: this.faint, italic: this.italic, underline: this.underline, fg: this.fg, bg: this.bg, text: '' }
      });
    }
  }
  close_url_frame(render_ctx_stack, rootNodes) {
    const urlIndex = render_ctx_stack.findIndex(f => f.type === 'url');
    if (urlIndex !== -1) {
      const urlFrame = render_ctx_stack[urlIndex];
      const linkNode = {
        type: 'link',
        url: urlFrame.url,
        children: urlFrame.children.length === 0 ? [{ type: 'text', text: '' }] : urlFrame.children
      };
      rootNodes.push(linkNode);
      render_ctx_stack.splice(urlIndex, 1);
    }
  }
  process_ansi(pkt) {
    let sgr_cmds = pkt.text.split(';');
    while (sgr_cmds.length > 0) {
      let sgr_cmd_str = sgr_cmds.shift();
      let num = parseInt(sgr_cmd_str, 10);
      if (isNaN(num) || num === 0) {
        this.fg = null;
        this.bg = null;
        this.bold = false;
        this.faint = false;
        this.italic = false;
        this.underline = false;
      }
      else if (num === 1) {
        this.bold = true;
      }
      else if (num === 2) {
        this.faint = true;
      }
      else if (num === 3) {
        this.italic = true;
      }
      else if (num === 4) {
        this.underline = true;
      }
      else if (num === 21) {
        this.bold = false;
      }
      else if (num === 22) {
        this.faint = false;
        this.bold = false;
      }
      else if (num === 23) {
        this.italic = false;
      }
      else if (num === 24) {
        this.underline = false;
      }
      else if (num === 39) {
        this.fg = null;
      }
      else if (num === 49) {
        this.bg = null;
      }
      else if ((num >= 30) && (num < 38)) {
        this.fg = this.ansi_colors[0][(num - 30)];
      }
      else if ((num >= 40) && (num < 48)) {
        this.bg = this.ansi_colors[0][(num - 40)];
      }
      else if ((num >= 90) && (num < 98)) {
        this.fg = this.ansi_colors[1][(num - 90)];
      }
      else if ((num >= 100) && (num < 108)) {
        this.bg = this.ansi_colors[1][(num - 100)];
      }
      else if (num === 38 || num === 48) {
        let is_itu416 = sgr_cmd_str.charAt(2) === ':';
        let params = is_itu416 ? sgr_cmd_str.split(':').slice(1) : sgr_cmds;
        if (num === 38) {
          this.fg = this.get_rgb_color(params, is_itu416);
        }
        else {
          this.bg = this.get_rgb_color(params, is_itu416);
        }
      }
    }
  }
  get_rgb_color(sgr_params, is_itu416) {
    if (sgr_params.length === 0) {
      return;
    }
    let color_mode = sgr_params.shift();
    if (color_mode === '5') {
      let palette_index = parseInt(sgr_params.shift(), 10);
      if (palette_index >= 0 && palette_index <= 255) {
        return this.palette_256[palette_index];
      }
    }
    if (color_mode === '2' && sgr_params.length > 2) {
      if (is_itu416 && sgr_params.length === 4)
        sgr_params.shift();
      let r = parseInt(sgr_params.shift(), 10);
      let g = parseInt(sgr_params.shift(), 10);
      let b = parseInt(sgr_params.shift(), 10);
      if ((r >= 0 && r <= 255) && (g >= 0 && g <= 255) && (b >= 0 && b <= 255)) {
        return { rgb: [r, g, b], class_name: 'truecolor' };
      }
    }
    return null;
  }
  has_styling(val) {
    return val.bold || val.italic || val.faint || val.underline || val.fg !== null || val.bg !== null;
  }
  styled_node_to_html(node) {
    if (!this.has_styling(node.attrs)) {
      return this.render_nodes_to_html(node.children);
    }
    let { styles, classes } = this.attrs_to_styles_classes(node.attrs);
    const class_string = !classes.length ? '' : ` class="${classes.join(' ')}"`;
    const style_string = !styles.length ? '' : ` style="${styles.join(';')}"`;
    return `<span${style_string}${class_string}>${(this.render_nodes_to_html(node.children))}</span>`;
  }
  attrs_to_styles_classes(fragment) {
    let styles = [];
    let classes = [];
    let fg = fragment.fg;
    let bg = fragment.bg;
    if (fragment.bold)
      styles.push(this._boldStyle);
    if (fragment.faint)
      styles.push(this._faintStyle);
    if (fragment.italic)
      styles.push(this._italicStyle);
    if (fragment.underline)
      styles.push(this._underlineStyle);
    if (!this._use_classes) {
      if (fg)
        styles.push(`color:rgb(${fg.rgb.join(',')})`);
      if (bg)
        styles.push(`background-color:rgb(${bg.rgb.join(',')})`);
    }
    else {
      if (fg) {
        if (fg.class_name !== 'truecolor') {
          classes.push(`${fg.class_name}-fg`);
        }
        else {
          styles.push(`color:rgb(${fg.rgb.join(',')})`);
        }
      }
      if (bg) {
        if (bg.class_name !== 'truecolor') {
          classes.push(`${bg.class_name}-bg`);
        }
        else {
          styles.push(`background-color:rgb(${bg.rgb.join(',')})`);
        }
      }
    }
    return { styles, classes };
  }
  render_nodes_to_html(nodes) {
    return nodes.map(node => this.render_node_to_html(node)).join('');
  }
  render_node_to_html(node) {
    if (node.type === 'text') {
      return this.escape_txt_for_html(node.text);
    }
    else if (node.type === 'styled') {
      return this.styled_node_to_html(node);
    }
    else if (node.type === 'link') {
      return this.hyperlink_to_html(node);
    }
    return '';
  }
  hyperlink_to_html(node) {
    return `<a href="${this.escape_txt_for_html(node.url)}">${this.render_nodes_to_html(node.children)}</a>`;
  }
}
function rgx(tmplObj, ...subst) {
  let regexText = tmplObj.raw[0];
  let wsrgx = /^\s+|\s+\n|\s*#[\s\S]*?\n|\n/gm;
  let txt2 = regexText.replace(wsrgx, '');
  return new RegExp(txt2);
}
function rgxG(tmplObj, ...subst) {
  let regexText = tmplObj.raw[0];
  let wsrgx = /^\s+|\s+\n|\s*#[\s\S]*?\n|\n/gm;
  let txt2 = regexText.replace(wsrgx, '');
  return new RegExp(txt2, 'g');
}

  Object.defineProperty(exports, "__esModule", {value: true});
  exports.default = AnsiUp;
}));
