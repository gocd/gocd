/* eslint-disable */
export default new (function Shellwords() {
  this.scan = function scan(string, pattern, callback) {
    var match, result;
    result = "";
    while (string.length > 0) {
      match = string.match(pattern);
      if (match) {
        result += string.slice(0, match.index);
        result += callback(match);
        string = string.slice(match.index + match[0].length);
      } else {
        result += string;
        string = "";
      }
    }
    return result;
  };

  this.split = function split(line, callback) {
    if (line == null) {
      line = "";
    }
    let words = [];
    let field = "";
    let rawParsed = "";
    this.scan(line, /\s*(?:([^\s\\\'\"]+)|'((?:[^\'\\]|\\.)*)'|"((?:[^\"\\]|\\.)*)"|(\\.?)|(\S))(\s|$)?/, function(match) {
      var dq, esc, garbage, raw, seperator, sq, word;
      raw = match[0], word = match[1], sq = match[2], dq = match[3], esc = match[4], garbage = match[5], seperator = match[6];
      if (garbage != null) {
        throw new Error("Unmatched quote");
      }
      rawParsed += match[0];
      field += word || (sq || dq || esc).replace(/\\(?=.)/, "");
      if (seperator != null) {
        words.push(field);
        if ("function" === typeof callback) {
          callback(rawParsed);
        }
        rawParsed = "";
        return field = "";
      }
    });
    if (field) {
      words.push(field);
    }
    return words;
  };

  this.escape = function escape(str) {
    if (str == null) {
      str = "";
    }
    if (str == null) {
      return "''";
    }
    return str.replace(/([^A-Za-z0-9_\-.,:\/@\n])/g, "\\$1").replace(/\n/g, "'\n'");
  };
})();
