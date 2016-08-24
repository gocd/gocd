//  lodash-inflection.js
//  (c) 2014 Jeremy Ruppel
//  lodash-inflection is freely distributable under the MIT license.
//  Portions of lodash-inflection are inspired or borrowed from ActiveSupport
//  Version 1.0.0

(function(root, factory) {
  if (typeof define === 'function' && define.amd) {
    // AMD. Register as an anonymous module.
    define(['lodash'], factory);
  } else if (typeof require === 'function' && typeof exports === 'object') {
    // Node. Does not work with strict CommonJS, but
    // only CommonJS-like environments that support module.exports,
    // like Node.
    module.exports = factory(require('lodash').runInContext());
  } else {
    // Browser globals (root is window)
    factory(root._);
  }
})(this, function(_, undefined) {
  var plurals      = [];
  var singulars    = [];
  var uncountables = [];

  function includes(haystack, needle) {
    var f = _.include || _.includes;
    return f(haystack, needle);
  }

  /**
   * Inflector
   */
  var inflector = {

    /**
     * `gsub` is a method that is just slightly different than our
     * standard `String#replace`. The main differences are that it
     * matches globally every time, and if no substitution is made
     * it returns `null`. It accepts a string for `word` and
     * `replacement`, and `rule` can be either a string or a regex.
     */
    gsub: function(word, rule, replacement) {
      var pattern = new RegExp(rule.source || rule, 'gi');

      return pattern.test(word) ? word.replace(pattern, replacement) : null;
    },

    /**
     * `plural` creates a new pluralization rule for the inflector.
     * `rule` can be either a string or a regex.
     */
    plural: function(rule, replacement) {
      plurals.unshift([rule, replacement]);
    },

    /**
     * Pluralizes the string passed to it. It also can accept a
     * number as the second parameter. If a number is provided,
     * it will pluralize the word to match the number. Optionally,
     * you can pass `true` as a third parameter. If found, this
     * will include the count with the output.
     */
    pluralize: function(word, count, includeNumber) {
      var result;

      if (count !== undefined) {
        count = parseFloat(count);
        result = (count === 1) ? this.singularize(word) : this.pluralize(word);
        result = (includeNumber) ? [count, result].join(' ') : result;
      } else {
        if (includes(uncountables, word)) {
          return word;
        }

        result = word;

        _(plurals).find(function(rule) {
          var gsub = this.gsub(word, rule[0], rule[1]);

          return gsub ? (result = gsub) : false;
        }.bind(this));
      }

      return result;
    },

    /**
     * `singular` creates a new singularization rule for the
     * inflector. `rule` can be either a string or a regex.
     */
    singular: function(rule, replacement) {
      singulars.unshift([rule, replacement]);
    },

    /**
     * `singularize` returns the singular version of the plural
     * passed to it.
     */
    singularize: function(word) {
      if (includes(uncountables, word)) {
        return word;
      }

      var result = word;

      _(singulars).find(function(rule) {
        var gsub = this.gsub(word, rule[0], rule[1]);

        return gsub ? (result = gsub) : false;
      }.bind(this));

      return result;
    },

    /**
     * `irregular` is a shortcut method to create both a
     * pluralization and singularization rule for the word at
     * the same time. You must supply both the singular form
     * and the plural form as explicit strings.
     */
    irregular: function(singular, plural) {
      this.plural('\\b' + singular + '\\b', plural);
      this.singular('\\b' + plural + '\\b', singular);
    },

    /**
     * `uncountable` creates a new uncountable rule for `word`.
     * Uncountable words do not get pluralized or singularized.
     */
    uncountable: function(word) {
      uncountables.unshift(word);
    },

    /**
     * `ordinalize` adds an ordinal suffix to `number`.
     */
    ordinalize: function(number) {
      if (isNaN(number)) {
        return number;
      }

      number = number.toString();
      var lastDigit = number.slice(-1);
      var lastTwoDigits = number.slice(-2);

      if (lastTwoDigits === '11' || lastTwoDigits === '12' || lastTwoDigits === '13') {
        return number + 'th';
      }

      switch (lastDigit) {
        case '1':
          return number + 'st';
        case '2':
          return number + 'nd';
        case '3':
          return number + 'rd';
        default:
          return number + 'th';
      }
    },

    /**
     * `titleize` capitalizes the first letter of each word in
     * the string `words`. It preserves the existing whitespace.
     */
    titleize: function(words) {
      if (typeof words !== 'string') {
        return words;
      }

      return words.replace(/\S+/g, function(word) {
        return word.charAt(0).toUpperCase() + word.slice(1);
      });
    },

    /**
     * Resets the inflector's rules to their initial state,
     * clearing out any custom rules that have been added.
     */
    resetInflections: function() {
      plurals      = [];
      singulars    = [];
      uncountables = [];

      this.plural(/$/,                         's');
      this.plural(/s$/,                        's');
      this.plural(/(ax|test)is$/,              '$1es');
      this.plural(/(octop|vir)us$/,            '$1i');
      this.plural(/(octop|vir)i$/,             '$1i');
      this.plural(/(alias|status)$/,           '$1es');
      this.plural(/(bu)s$/,                    '$1ses');
      this.plural(/(buffal|tomat)o$/,          '$1oes');
      this.plural(/([ti])um$/,                 '$1a');
      this.plural(/([ti])a$/,                  '$1a');
      this.plural(/sis$/,                      'ses');
      this.plural(/(?:([^f])fe|([lr])?f)$/,     '$1$2ves');
      this.plural(/(hive)$/,                   '$1s');
      this.plural(/([^aeiouy]|qu)y$/,          '$1ies');
      this.plural(/(x|ch|ss|sh)$/,             '$1es');
      this.plural(/(matr|vert|ind)(?:ix|ex)$/, '$1ices');
      this.plural(/([m|l])ouse$/,              '$1ice');
      this.plural(/([m|l])ice$/,               '$1ice');
      this.plural(/^(ox)$/,                    '$1en');
      this.plural(/^(oxen)$/,                  '$1');
      this.plural(/(quiz)$/,                   '$1zes');

      this.singular(/s$/,                                                            '');
      this.singular(/(n)ews$/,                                                       '$1ews');
      this.singular(/([ti])a$/,                                                      '$1um');
      this.singular(/((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)ses$/, '$1$2sis');
      this.singular(/(^analy)ses$/,                                                  '$1sis');
      this.singular(/([^f])ves$/,                                                    '$1fe');
      this.singular(/(hive)s$/,                                                      '$1');
      this.singular(/(tive)s$/,                                                      '$1');
      this.singular(/([lr])ves$/,                                                    '$1f');
      this.singular(/([^aeiouy]|qu)ies$/,                                            '$1y');
      this.singular(/(s)eries$/,                                                     '$1eries');
      this.singular(/(m)ovies$/,                                                     '$1ovie');
      this.singular(/(ss)$/,                                                         '$1');
      this.singular(/(x|ch|ss|sh)es$/,                                               '$1');
      this.singular(/([m|l])ice$/,                                                   '$1ouse');
      this.singular(/(bus)es$/,                                                      '$1');
      this.singular(/(o)es$/,                                                        '$1');
      this.singular(/(shoe)s$/,                                                      '$1');
      this.singular(/(cris|ax|test)es$/,                                             '$1is');
      this.singular(/(octop|vir)i$/,                                                 '$1us');
      this.singular(/(alias|status)es$/,                                             '$1');
      this.singular(/^(ox)en/,                                                       '$1');
      this.singular(/(vert|ind)ices$/,                                               '$1ex');
      this.singular(/(matr)ices$/,                                                   '$1ix');
      this.singular(/(quiz)zes$/,                                                    '$1');
      this.singular(/(database)s$/,                                                  '$1');

      this.irregular('person', 'people');
      this.irregular('man',    'men');
      this.irregular('child',  'children');
      this.irregular('sex',    'sexes');
      this.irregular('move',   'moves');
      this.irregular('cow',    'kine');

      this.uncountable('equipment');
      this.uncountable('information');
      this.uncountable('rice');
      this.uncountable('money');
      this.uncountable('species');
      this.uncountable('series');
      this.uncountable('fish');
      this.uncountable('sheep');
      this.uncountable('jeans');

      return this;
    }
  };

  /**
   * Lodash integration
   */
  _.mixin(inflector.resetInflections());

  return inflector;
});
