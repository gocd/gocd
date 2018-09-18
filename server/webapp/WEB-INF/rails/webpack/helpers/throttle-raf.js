/**
 * Throttles a function with window.requestAnimationFrame()
 *
 * Useful for event handlers that may fire very rapidly (e.g. resize, scroll, etc)
 */
function throttleRaf(fn, win=window) {
  let isRunning, self, args;

  function run() {
    isRunning = false;
    fn.apply(self, args);
  }

  return function throttledFn() {
    self = this;
    args = Array.prototype.slice.call(arguments);

    if (isRunning) {
      return;
    }

    isRunning = true;
    win.requestAnimationFrame(run);
  };
}

module.exports = throttleRaf;
