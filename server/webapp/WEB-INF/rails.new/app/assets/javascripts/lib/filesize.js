"use strict";

/**
 * filesize
 *
 * @copyright 2016 Jason Mulligan <jason.mulligan@avoidwork.com>
 * @license BSD-3-Clause
 * @version 3.2.1
 */
(function (global) {
	var b = /^(b|B)$/;
	var symbol = {
		bits: ["b", "Kb", "Mb", "Gb", "Tb", "Pb", "Eb", "Zb", "Yb"],
		bytes: ["B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"]
	};

	/**
  * filesize
  *
  * @method filesize
  * @param  {Mixed}   arg        String, Int or Float to transform
  * @param  {Object}  descriptor [Optional] Flags
  * @return {String}             Readable file size String
  */
	function filesize(arg) {
		var descriptor = arguments.length <= 1 || arguments[1] === undefined ? {} : arguments[1];

		var result = [],
		    val = 0,
		    e = undefined,
		    base = undefined,
		    bits = undefined,
		    ceil = undefined,
		    neg = undefined,
		    num = undefined,
		    output = undefined,
		    round = undefined,
		    unix = undefined,
		    spacer = undefined,
		    symbols = undefined;

		if (isNaN(arg)) {
			throw new Error("Invalid arguments");
		}

		bits = descriptor.bits === true;
		unix = descriptor.unix === true;
		base = descriptor.base || 2;
		round = descriptor.round !== undefined ? descriptor.round : unix ? 1 : 2;
		spacer = descriptor.spacer !== undefined ? descriptor.spacer : unix ? "" : " ";
		symbols = descriptor.symbols || descriptor.suffixes || {};
		output = descriptor.output || "string";
		e = descriptor.exponent !== undefined ? descriptor.exponent : -1;
		num = Number(arg);
		neg = num < 0;
		ceil = base > 2 ? 1000 : 1024;

		// Flipping a negative number to determine the size
		if (neg) {
			num = -num;
		}

		// Zero is now a special case because bytes divide by 1
		if (num === 0) {
			result[0] = 0;
			result[1] = unix ? "" : !bits ? "B" : "b";
		} else {
			// Determining the exponent
			if (e === -1 || isNaN(e)) {
				e = Math.floor(Math.log(num) / Math.log(ceil));

				if (e < 0) {
					e = 0;
				}
			}

			// Exceeding supported length, time to reduce & multiply
			if (e > 8) {
				e = 8;
			}

			val = base === 2 ? num / Math.pow(2, e * 10) : num / Math.pow(1000, e);

			if (bits) {
				val = val * 8;

				if (val > ceil && e < 8) {
					val = val / ceil;
					e++;
				}
			}

			result[0] = Number(val.toFixed(e > 0 ? round : 0));
			result[1] = base === 10 && e === 1 ? bits ? "kb" : "kB" : symbol[bits ? "bits" : "bytes"][e];

			if (unix) {
				result[1] = result[1].charAt(0);

				if (b.test(result[1])) {
					result[0] = Math.floor(result[0]);
					result[1] = "";
				}
			}
		}

		// Decorating a 'diff'
		if (neg) {
			result[0] = -result[0];
		}

		// Applying custom suffix
		result[1] = symbols[result[1]] || result[1];

		// Returning Array, Object, or String (default)
		if (output === "array") {
			return result;
		}

		if (output === "exponent") {
			return e;
		}

		if (output === "object") {
			return { value: result[0], suffix: result[1], symbol: result[1] };
		}

		return result.join(spacer);
	}

	// CommonJS, AMD, script tag
	if (typeof exports !== "undefined") {
		module.exports = filesize;
	} else if (typeof define === "function" && define.amd) {
		define(function () {
			return filesize;
		});
	} else {
		global.filesize = filesize;
	}
})(typeof window !== "undefined" ? window : global);
