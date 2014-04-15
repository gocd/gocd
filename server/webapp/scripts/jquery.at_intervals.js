// Author: Jacek Becela
// Website: http://github.com/ncr/at_intervals
// License: cc-by-sa
(function($) {
  $.fn.at_intervals = function(fn, options) {
    var settings = $.extend({}, $.fn.at_intervals.defaults, options);

    return this.each(function() {
      var e = $(this)
      var name = settings.name
      var delay = settings.delay

      var helper = {
        should_stop: function() { // used to completely remove the interval
          return !this.element_in_dom() || this.user_wants_to_stop()
        },
        should_work: function() { // used to pause/resume the interval
          return this.element_visible() && !this.user_wants_to_pause()
        },
        user_wants_to_stop: function() {
          return e.data(name).should_stop == true
        },
        user_wants_to_pause: function() {
          return e.data(name).should_pause == true
        },
        element_in_dom: function() {
          return e.parents("html").length > 0
        },
        element_visible: function() {
          return e.parents("*").andSelf().not(":visible").length == 0
        },
        stop: function(interval_id) {
          clearInterval(interval_id)
          e.removeData(name)
        }
      }

      if(e.data(name)) {
        helper.stop(e.data(name).interval_id) // remove previous executer
      }

      e.data(name, { delay: delay }) // initialize data cache

      if(helper.should_work()) {
        fn() // call fn immediately (setInterval applies the delay before calling fn for the first time)
      }

      var interval_id = setInterval(function() {
        if(helper.should_stop()) {
          helper.stop(interval_id)
        } else {
          if(helper.should_work()){
            fn()
          }
        }
      }, delay)

      e.data(name).interval_id = interval_id
    })
  };

  $.fn.at_intervals.defaults = {
    name:  "at_intervals",
    delay: 1000 // one second
  }
})(jQuery);