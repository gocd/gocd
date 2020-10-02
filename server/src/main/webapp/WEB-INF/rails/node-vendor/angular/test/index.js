/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
