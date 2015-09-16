console.log('loop/element2 1');
(function (loop) {
console.log('loop/element2 2');
loop.fn2 = function (arg1, arg2) {
    return arg1 + arg2;
};
})(require('../loop'));
console.log('loop/element2 3');