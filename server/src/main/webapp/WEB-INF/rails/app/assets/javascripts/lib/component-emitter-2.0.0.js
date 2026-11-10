function Emitter(object) {
  if (object) {
    return mixin(object);
  }

  this._callbacks = new Map();
}

function mixin(object) {
  Object.assign(object, Emitter.prototype);
  object._callbacks = new Map();
  return object;
}

Emitter.prototype.on = function (event, listener) {
  const callbacks = this._callbacks.get(event) ?? [];
  callbacks.push(listener);
  this._callbacks.set(event, callbacks);
  return this;
};

Emitter.prototype.once = function (event, listener) {
  const on = (...arguments_) => {
    this.off(event, on);
    listener.apply(this, arguments_);
  };

  on.fn = listener;
  this.on(event, on);
  return this;
};

Emitter.prototype.off = function (event, listener) {
  if (event === undefined && listener === undefined) {
    this._callbacks.clear();
    return this;
  }

  if (listener === undefined) {
    this._callbacks.delete(event);
    return this;
  }

  const callbacks = this._callbacks.get(event);
  if (callbacks) {
    for (const [index, callback] of callbacks.entries()) {
      if (callback === listener || callback.fn === listener) {
        callbacks.splice(index, 1);
        break;
      }
    }

    if (callbacks.length === 0) {
      this._callbacks.delete(event);
    } else {
      this._callbacks.set(event, callbacks);
    }
  }

  return this;
};

Emitter.prototype.emit = function (event, ...arguments_) {
  const callbacks = this._callbacks.get(event);
  if (callbacks) {
    // Create a copy of the callbacks array to avoid issues if it's modified during iteration
    const callbacksCopy = [...callbacks];

    for (const callback of callbacksCopy) {
      callback.apply(this, arguments_);
    }
  }

  return this;
};

Emitter.prototype.listeners = function (event) {
  return this._callbacks.get(event) ?? [];
};

Emitter.prototype.listenerCount = function (event) {
  if (event) {
    return this.listeners(event).length;
  }

  let totalCount = 0;
  for (const callbacks of this._callbacks.values()) {
    totalCount += callbacks.length;
  }

  return totalCount;
};

Emitter.prototype.hasListeners = function (event) {
  return this.listenerCount(event) > 0;
};

// Aliases
Emitter.prototype.addEventListener = Emitter.prototype.on;
Emitter.prototype.removeListener = Emitter.prototype.off;
Emitter.prototype.removeEventListener = Emitter.prototype.off;
Emitter.prototype.removeAllListeners = Emitter.prototype.off;

if (typeof module !== 'undefined') {
  module.exports = Emitter;
}
