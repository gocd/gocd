
# The base class for all JavaScript objects.
class Java::OrgMozillaJavascript::ScriptableObject
  
  include_package "org.mozilla.javascript"
  
  # get a property from this javascript object, where +k+ is a string or symbol
  # corresponding to the property name e.g.
  # 
  #     jsobject = Context.open do |cxt|
  #       cxt.eval('({foo: 'bar', 'Take me to': 'a funky town'})')
  #     end
  #     jsobject[:foo] # => 'bar'
  #     jsobject['foo'] # => 'bar'
  #     jsobject['Take me to'] # => 'a funky town'
  #
  def [](name)
    Rhino.to_ruby ScriptableObject.getProperty(self, name.to_s)
  end

  # set a property on the javascript object, where +k+ is a string or symbol corresponding
  # to the property name, and +v+ is the value to set. e.g.
  #
  #     jsobject = eval_js "new Object()"
  #     jsobject['foo'] = 'bar'
  #     Context.open(:with => jsobject) do |cxt|
  #       cxt.eval('foo') # => 'bar'
  #     end
  #
  def []=(key, value)
    scope = self
    ScriptableObject.putProperty(self, key.to_s, Rhino.to_javascript(value, scope))
  end
  
  # enumerate the key value pairs contained in this javascript object. e.g.
  #
  #     eval_js("{foo: 'bar', baz: 'bang'}").each do |key,value|
  #       puts "#{key} -> #{value} "
  #     end
  #
  # outputs foo -> bar baz -> bang
  #
  def each
    each_raw { |key, val| yield key, Rhino.to_ruby(val) }
  end
  
  def each_key
    each_raw { |key, val| yield key }
  end

  def each_value
    each_raw { |key, val| yield Rhino.to_ruby(val) }
  end
  
  def each_raw
    for id in getAllIds do
      yield id, get(id, self)
    end
  end
  
  def keys
    keys = []
    each_key { |key| keys << key }
    keys
  end
  
  def values
    vals = []
    each_value { |val| vals << val }
    vals    
  end
  
  # Converts the native object to a hash. This isn't really a stretch since it's
  # pretty much a hash in the first place.
  def to_h
    hash = {}
    each do |key, val|
      hash[key] = val.is_a?(ScriptableObject) ? val.to_h : val
    end
    hash
  end

  def ==(other)
    equivalentValues(other) == true # JS ==
  end

  def eql?(other)
    self.class == other.class && self.==(other)
  end

  # Convert this javascript object into a json string.
  def to_json(*args)
    to_h.to_json(*args)
  end
  
  # make sure inspect prints the same as to_s (on --1.8)
  # otherwise JRuby might play it a little smart e.g. :
  # "#<#<Class:0xd790a8>:0x557c15>" instead of "Error: bar"
  def inspect
    toString
  end

  # Delegate methods to JS object if possible when called from Ruby.
  def method_missing(name, *args)
    name_str = name.to_s
    if name_str[-1, 1] == '=' && args.size == 1 # writer -> JS put
      self[ name_str[0...-1] ] = args[0]
    else
      if property = self[name_str]
        if property.is_a?(Rhino::JS::Function)
          begin
            context = Rhino::JS::Context.enter
            scope = current_scope(context)
            js_args = Rhino.args_to_javascript(args, self) # scope == self
            Rhino.to_ruby property.__call__(context, scope, self, js_args)
          ensure
            Rhino::JS::Context.exit
          end
        else
          if args.size > 0
            raise ArgumentError, "can't call '#{name_str}' with args: #{args.inspect} as it's a property"
          end
          Rhino.to_ruby property
        end
      else
        super
      end
    end
  end
  
  protected
  
    def current_scope(context)
      getParentScope || context.initStandardObjects
    end
  
end

class Java::OrgMozillaJavascript::NativeObject
  
  include_package "org.mozilla.javascript"
  
  def [](name)
    value = Rhino.to_ruby(ScriptableObject.getProperty(self, s_name = name.to_s))
    # handle { '5': 5 }.keys() ... [ 5 ] not [ '5' ] !
    if value.nil? && (i_name = s_name.to_i) != 0
      value = Rhino.to_ruby(ScriptableObject.getProperty(self, i_name))
    end
    value
  end
  
  # re-implement unsupported Map#put
  def []=(key, value)
    scope = self
    ScriptableObject.putProperty(self, key.to_s, Rhino.to_javascript(value, scope))
  end
  
  def ==(other)
    return true if super
    if other.is_a?(Hash) || other.is_a?(java.util.Map)
      for key, val in other
        return false if self[key] != val
      end
      return true
    end
    false
  end

  # NOTE: need to re-implement this as JRuby 1.7.1 seems to be not routing to super
  def eql?(other) # :nodoc
    self.class == other.class && self.==(other)
  end

end

# The base class for all JavaScript function objects.
class Java::OrgMozillaJavascript::BaseFunction
  
  # Object call(Context context, Scriptable scope, Scriptable this, Object[] args)
  alias_method :__call__, :call
  
  # make JavaScript functions callable Ruby style e.g. `fn.call('42')`
  # 
  # NOTE: That invoking #call does not have the same semantics as
  # JavaScript's Function#call but rather as Ruby's Method#call !
  # Use #apply or #bind before calling to achieve the same effect.
  def call(*args)
    context = Rhino::JS::Context.enter; scope = current_scope(context)
    # calling as a (var) stored function - no this === undefined "use strict"
    # TODO can't pass Undefined.instance as this - it's not a Scriptable !?
    this = Rhino::JS::ScriptRuntime.getGlobal(context)
    js_args = Rhino.args_to_javascript(args, scope)
    Rhino.to_ruby __call__(context, scope, this, js_args)
  rescue Rhino::JS::JavaScriptException => e
    raise Rhino::JSError.new(e)
  ensure
    Rhino::JS::Context.exit
  end
  
  # bind a JavaScript function into the given (this) context
  def bind(this, *args)
    context = Rhino::JS::Context.enter; scope = current_scope(context)
    args = Rhino.args_to_javascript(args, scope)
    Rhino::JS::BoundFunction.new(context, scope, self, Rhino.to_javascript(this), args)
  ensure
    Rhino::JS::Context.exit    
  end
  
  # use JavaScript functions constructors from Ruby as `fn.new`
  def new(*args)
    context = Rhino::JS::Context.enter; scope = current_scope(context)
    construct(context, scope, Rhino.args_to_javascript(args, scope))
  rescue Rhino::JS::JavaScriptException => e
    raise Rhino::JSError.new(e)
  ensure
    Rhino::JS::Context.exit
  end
  
  # apply a function with the given context and (optional) arguments 
  # e.g. `fn.apply(obj, 1, 2)`
  # 
  # NOTE: That #call from Ruby does not have the same semantics as
  # JavaScript's Function#call but rather as Ruby's Method#call !
  def apply(this, *args)
    context = Rhino::JS::Context.enter; scope = current_scope(context)
    args = Rhino.args_to_javascript(args, scope)
    __call__(context, scope, Rhino.to_javascript(this), args)
  rescue Rhino::JS::JavaScriptException => e
    raise Rhino::JSError.new(e)
  ensure
    Rhino::JS::Context.exit
  end
  alias_method :methodcall, :apply # V8::Function compatibility
  
end

class Java::OrgMozillaJavascript::ScriptStackElement
  
  def file_name; fileName; end # public final String fileName;
  def function_name; functionName; end # public final String functionName;
  def line_number; lineNumber; end # public final int lineNumber;
  
  def to_s
    str = "at #{fileName}"
    str << ':' << lineNumber.to_s if lineNumber > -1
    str << " (#{functionName})" if functionName
    str
  end
  
end

class Java::OrgMozillaJavascript::Context
  
  CACHE = java.util.WeakHashMap.new
  
  def reset_cache!
    CACHE[self] = java.util.WeakHashMap.new
  end

  def enable_cache!
    CACHE[self] = nil unless CACHE[self]
  end

  def disable_cache!
    CACHE[self] = false
  end
  
  # Support for caching JS data per context.
  # e.g. to get === comparison's working ...
  # 
  # NOTE: the cache only works correctly for keys following Java identity !
  #       (implementing #equals & #hashCode e.g. RubyStrings will work ...)
  #
  def cache(key)
    return yield if (cache = CACHE[self]) == false
    cache = reset_cache! unless cache
    fetch(key, cache) || store(key, yield, cache)
  end
    
  private

    def fetch(key, cache = CACHE[self])
      ref = cache.get(key)
      ref ? ref.get : nil
    end

    def store(key, value, cache = CACHE[self])
      cache.put(key, java.lang.ref.WeakReference.new(value))
      value
    end
    
end
