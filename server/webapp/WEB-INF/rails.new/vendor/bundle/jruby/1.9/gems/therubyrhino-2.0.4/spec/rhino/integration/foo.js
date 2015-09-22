//console.log('foo.js 1');
var Bar = require('./bar').Bar;
//console.log('foo.js 2 ' + Bar + " : " + Bar.puts);
Bar.puts('Hello Bar!');
//console.log('foo.js 3');
exports.foo = { 'Bar': Bar };
//console.log('foo.js 4');