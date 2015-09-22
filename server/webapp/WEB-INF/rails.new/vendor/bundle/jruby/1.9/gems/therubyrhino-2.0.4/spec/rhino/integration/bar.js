//console.log('bar.js 1');
var util = require('util');
//console.log('bar.js 2');
var Bar = {};
Bar.puts = function (message) {
    util.puts(message);
    return message;
};
//console.log('bar.js 3');
exports.Bar = Bar;
//console.log('bar.js 4');