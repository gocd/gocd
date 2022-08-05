var test = require('tape'),
	path = require('path'),
	angular = require('angular'),
	inject = angular.injector(['ng']).invoke;

test('core', function (t) {

	test('init', function (t) {
		t.true(angular, 'Angular instance is defined');
		t.end();
	});

	test('version', function (t) {
		t.equal(angular.version.full, require(path.resolve('./package.json')).version, 'Angular and package versions match');
		t.end();
	});

	t.end();
});



test('injector', function (t) {
	var el;

	test('should compile a binding', function (t) {

		inject(function ($rootScope, $compile) {
			el = angular.element('<div>{{ 2 + 2 }}</div>');
			el = $compile(el)($rootScope);
			$rootScope.$digest();
		})

		t.equal(+el.html(), 4, 'simple binding compiled');

		t.end();
	});

	t.end();
});
