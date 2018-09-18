+function (window, jasmine, $) {
    "use strict";
    beforeEach(function () {
        jasmine.addMatchers({
                toBeReadonly: function () {
                    return {
                        compare: function (actual) {
                            return {pass: !!$(actual).attr('readonly') }
                        }
                    }
                }
            }
        )
    })
}(window, window.jasmine, window.jQuery);

