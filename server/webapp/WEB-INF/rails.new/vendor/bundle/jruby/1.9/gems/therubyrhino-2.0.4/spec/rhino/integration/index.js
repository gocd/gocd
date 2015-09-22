console.log('./loop');
require('./loop');

['element1', 'element2'].forEach(function (n) {
    console.log('require(./loop/' + n + ')');
    require('./loop/' + n);
});