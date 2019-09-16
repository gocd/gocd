!function(root, name, make) {
  typeof module != 'undefined' && module.exports ? module.exports = make() : root[name] = make()
}(this, 'often', function() {

  var model = often.prototype

  function often(fn) {
    return (this instanceof often ? this : new often).init(fn)
  }

  function start(o, delay) {
    o.clear()
    o._timer = setTimeout(recur(o), delay > 0 ? delay : 0)
    return o
  }

  function recur(o) {
      return function() {
        if (o._recur) {
          o._trial++
          o._function()
          if (o._recur) start(o, o._wait)
        }
      }
  }

  model.init = function(fn) {
    return this.clean().wait(0).use(fn)
  }

  model.use = function(fn) {
    this._function = fn
    return this
  }

  model.wait = function(ms) {
    this._wait = ms
    return this
  }

  model.start = function(delay) {
    this._recur = true
    return start(this, delay)
  }

  model.clear = function() {
    this._timer == null || clearTimeout(this._timer)
    this._timer = null
    return this
  }

  model.clean = function() {
    this._function = this._recur = this._trial = this._timer = this._wait = null
    return this
  }

  model.stop = function() {
    this._recur = false
    this._trial = null
    return this.clear()
  }

  model.done = function() {
    return this.clear().clean()
  }

  return often
});
