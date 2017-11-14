var system = require('system');

console.log('bar ' + system.args[1]);
console.log('bar ' + system.args[2]);
phantom.exit();
