require 'stringio'

module Rhino

  # ==Overview
  #  All Javascript must be executed in a context which represents the execution environment in
  #  which scripts will run. The environment consists of the standard javascript objects
  #  and functions like Object, String, Array, etc... as well as any objects or functions which
  #  have been defined in it. e.g.
  #
  #   Context.open do |cxt|
  #     cxt['num'] = 5
  #     cxt.eval('num + 5') #=> 10
  #   end
  #
  # == Multiple Contexts.
  # The same object may appear in any number of contexts, but only one context may be executing javascript code
  # in any given thread. If a new context is opened in a thread in which a context is already opened, the second
  # context will "mask" the old context e.g.
  #
  #   six = 6
  #   Context.open do |cxt|
  #     cxt['num'] = 5
  #     cxt.eval('num') # => 5
  #     Context.open do |cxt|
  #       cxt['num'] = 10
  #       cxt.eval('num') # => 10
  #       cxt.eval('++num') # => 11
  #     end
  #     cxt.eval('num') # => 5
  #   end
  #
  # == Notes
  # While there are many similarities between Rhino::Context and Java::org.mozilla.javascript.Context, they are not
  # the same thing and should not be confused.
  class Context

    class << self

      # initalize a new context with a fresh set of standard objects. All operations on the context
      # should be performed in the block that is passed.
      def open(options = {}, &block)
        new(options).open(&block)
      end

      def eval(javascript)
        new.eval(javascript)
      end

    end

    @@default_factory = nil
    def self.default_factory
      @@default_factory ||= ContextFactory.new
    end

    def self.default_factory=(factory)
      @@default_factory = factory
    end

    @@default_optimization_level = java.lang.Integer.getInteger('rhino.opt.level')
    def self.default_optimization_level
      @@default_optimization_level
    end

    def self.default_optimization_level=(level)
      @@default_optimization_level = level
    end

    @@default_javascript_version = java.lang.System.getProperty('rhino.js.version')
    def self.default_javascript_version
      @@default_javascript_version
    end

    def self.default_javascript_version=(version)
      @@default_javascript_version = version
    end

    attr_reader :scope

    # Create a new javascript environment for executing javascript and ruby code.
    # * <tt>:sealed</tt> - if this is true, then the standard objects such as Object, Function, Array will not be able to be modified
    # * <tt>:with</tt> - use this ruby object as the root scope for all javascript that is evaluated
    # * <tt>:java</tt> - if true, java packages will be accessible from within javascript
    def initialize(options = {}) #:nodoc:
      factory = options[:factory] ||
        (options[:restrictable] ? RestrictableContextFactory.instance : self.class.default_factory)
      @options = options
      factory.call(self) # org.mozilla.javascript.ContextAction (invokes #run)
      if optimization_level = options[:optimization_level] || self.class.default_optimization_level
        self.optimization_level = optimization_level
      end
      if javascript_version = options[:javascript_version] || self.class.default_javascript_version
        self.javascript_version = javascript_version
      end
      yield(self) if block_given?
    end

    include JS::ContextAction

    # org.mozilla.javascript.ContextAction public Object run(Context context);
    def run(context) # :nodoc:
      @native = context
      @global = @native.initStandardObjects(nil, @options[:sealed] == true)
      if with = @options[:with]
        @scope = Rhino.to_javascript(with)
        @scope.setParentScope(@global)
      else
        @scope = @global
      end
      unless @options[:java]
        @global.delete('Packages')
        @global.delete('java'); @global.delete('javax')
        @global.delete('org'); @global.delete('com')
        @global.delete('edu'); @global.delete('net')
      end
    end

    # Returns the ContextFactory used while creating this context.
    def factory
      @native.getFactory
    end

    # Read a value from the global scope of this context
    def [](key)
      @scope[key]
    end

    # Set a value in the global scope of this context. This value will be visible to all the
    # javascript that is executed in this context.
    def []=(key, val)
      @scope[key] = val
    end

    # Evaluate a string of javascript in this context:
    # * <tt>source</tt> - the javascript source code to evaluate. This can be either a string or an IO object.
    # * <tt>source_name</tt> - associated name for this source code. Mainly useful for backtraces.
    # * <tt>line_number</tt> - associate this number with the first line of executing source. Mainly useful for backtraces
    def eval(source, source_name = "<eval>", line_number = 1)
      open do
        if IO === source || StringIO === source
          result = @native.evaluateReader(@scope, IOReader.new(source), source_name, line_number, nil)
        else
          result = @native.evaluateString(@scope, source.to_s, source_name, line_number, nil)
        end
        Rhino.to_ruby(result)
      end
    end

    def evaluate(*args) # :nodoc:
      eval(*args) # an alias
    end

    # Read the contents of <tt>filename</tt> and evaluate it as javascript. Returns the result of evaluating the
    # javascript. e.g.
    #
    # Context.open do |cxt|
    #   cxt.load("path/to/some/lib.js")
    # end
    #
    def load(filename)
      File.open(filename) do |file|
        evaluate file, filename, 1
      end
    end

    # Returns true if this context supports restrictions.
    def restrictable?
      @native.is_a?(RestrictableContextFactory::Context)
    end

    def instruction_limit
      restrictable? ? @native.instruction_limit : false
    end

    # Set the maximum number of instructions that this context will execute.
    # If this instruction limit is exceeded, then a #Rhino::RunawayScriptError
    # will be raised.
    def instruction_limit=(limit)
      if restrictable?
        @native.instruction_limit = limit
      else
        raise "setting an instruction_limit has no effect on this context, use " <<
              "Context.open(:restrictable => true) to gain a restrictable instance"
      end
    end

    def timeout_limit
      restrictable? ? @native.timeout_limit : false
    end

    # Set the duration (in seconds e.g. 1.5) this context is allowed to execute.
    # After the timeout passes (no matter if any JS has been evaluated) and this
    # context is still attempted to run code, a #Rhino::ScriptTimeoutError will
    # be raised.
    def timeout_limit=(limit)
      if restrictable?
        @native.timeout_limit = limit
      else
        raise "setting an timeout_limit has no effect on this context, use " <<
              "Context.open(:restrictable => true) to gain a restrictable instance"
      end
    end

    def optimization_level
      @native.getOptimizationLevel
    end

    # Set the optimization level that this context will use. This is sometimes necessary
    # in Rhino, if the bytecode size of the compiled javascript exceeds the 64KB limit.
    # By using the -1 optimization level, you tell Rhino to run in interpretative mode,
    # taking a hit to performance but escaping the Java bytecode limit.
    def optimization_level=(level)
      if JS::Context.isValidOptimizationLevel(level)
        @native.setOptimizationLevel(level)
        level
      else
        @native.setOptimizationLevel(0)
        nil
      end
    end

    # Get the JS interpreter version.
    # Returns a number e.g. 1.7, nil if unknown and 0 for default.
    def javascript_version
      case const_value = @native.getLanguageVersion
        when -1 then nil # VERSION_UNKNOWN
        when  0 then 0 # VERSION_DEFAULT
        else const_value / 100.0 # VERSION_1_1 (1.1 = 110 / 100)
      end
    end
    alias :version :javascript_version

    # Sets interpreter mode a.k.a. JS language version e.g. 1.7 (if supported).
    def javascript_version=(version)
      const = version.to_s.gsub('.', '_').upcase
      const = "VERSION_#{const}" if const[0, 7] != 'VERSION'
      if JS::Context.constants.find { |c| c.to_s == const }
        const_value = JS::Context.const_get(const)
        @native.setLanguageVersion(const_value)
        const_value
      else
        @native.setLanguageVersion(JS::Context::VERSION_DEFAULT)
        nil
      end
    end
    alias :version= :javascript_version=

    # Enter this context for operations.
    # Some methods such as eval() will fail unless the context is open.
    def open(&block)
      do_open(&block)
    rescue JS::RhinoException => e
      if code_generation_error?(e)
        Rhino.warn "[INFO] Rhino byte-code generation failed forcing #{@native} into interpreted mode"
        self.optimization_level = -1
        retry
      end
      raise Rhino::JSError.new(e)
    end

    private

    def do_open # :nodoc
      factory.enterContext(@native)
      begin
        yield self
      ensure
        factory.exit
      end
    end

    CODE_GENERATION_ERROR_MESSAGE = 'generated bytecode for method exceeds 64K limit' # :nodoc

    CODE_GENERATION_TRACE_CLASS_NAME = 'org.mozilla.javascript.optimizer.Codegen' # :nodoc
    CODE_GENERATION_TRACE_METHOD_NAME = 'reportClassFileFormatException' # :nodoc
    # at org.mozilla.javascript.optimizer.Codegen.reportClassFileFormatException

    def code_generation_error?(exception) # :nodoc
      if ( exception.is_a?(NativeException) rescue nil ) # JRuby 1.6 wrapping
        exception = exception.cause
      end
      if exception.class == Rhino::JS::EvaluatorException
        if exception.message.index(CODE_GENERATION_ERROR_MESSAGE)
          return true
        end
        # NOTE: unfortunately Rhino localizes the error messages!
        # and the ClassFileFormatException is not kept as a cause
        class_name = CODE_GENERATION_TRACE_CLASS_NAME
        method_name = CODE_GENERATION_TRACE_METHOD_NAME
        for trace in exception.getStackTrace()
          if class_name == trace.class_name && method_name == trace.method_name
            return true
          end
        end
      end
      false
    end

  end

  class IOReader < java.io.Reader # :nodoc:

    def initialize(io)
      @io = io
    end

    # implement int Reader#read(char[] buffer, int offset, int length)
    def read(buffer, offset, length)
      str = nil
      begin
        str = @io.read(length)
      rescue StandardError => e
        raise java.io.IOException.new("failed reading from ruby IO object")
      end
      if str.nil?
        return -1
      else
        jstr = str.to_java
        for i in 0 .. jstr.length - 1
          buffer[i + offset] = jstr.charAt(i)
        end
        return jstr.length
      end
    end

  end

  ContextFactory = JS::ContextFactory # :nodoc: backward compatibility

  class RestrictableContextFactory < ContextFactory # :nodoc:

    @@instance = nil
    def self.instance
      @@instance ||= new
    end

    # protected Context makeContext()
    def makeContext
      Context.new(self)
    end

    # protected void observeInstructionCount(Context context, int instructionCount)
    def observeInstructionCount(context, count)
      context.check!(count) if context.is_a?(Context)
    end

    # protected Object doTopCall(Callable callable, Context context,
    #                            Scriptable scope, Scriptable thisObj, Object[] args)
    def doTopCall(callable, context, scope, this, args)
      context.reset! if context.is_a?(Context)
      super
    end

    class Context < JS::Context # :nodoc:

      def initialize(factory)
        super(factory)
        reset!
      end

      attr_reader :instruction_limit

      def instruction_limit=(limit)
        treshold = getInstructionObserverThreshold
        if limit && (treshold == 0 || treshold > limit)
          setInstructionObserverThreshold(limit)
        end
        @instruction_limit = limit
      end

      attr_reader :instruction_count

      TIMEOUT_INSTRUCTION_TRESHOLD = 42

      attr_reader :timeout_limit

      def timeout_limit=(limit) # in seconds
        treshold = getInstructionObserverThreshold
        if limit && (treshold == 0 || treshold > TIMEOUT_INSTRUCTION_TRESHOLD)
          setInstructionObserverThreshold(TIMEOUT_INSTRUCTION_TRESHOLD)
        end
        @timeout_limit = limit
      end

      attr_reader :start_time

      def check!(count = nil)
        @instruction_count += count if count
        check_instruction_limit!
        check_timeout_limit!(count)
      end

      def check_instruction_limit!
        if instruction_limit && instruction_count > instruction_limit
          raise RunawayScriptError, "script exceeded allowable instruction count: #{instruction_limit}"
        end
      end

      def check_timeout_limit!(count = nil)
        if timeout_limit
          elapsed_time = Time.now.to_f - start_time.to_f
          if elapsed_time > timeout_limit
            raise ScriptTimeoutError, "script exceeded timeout: #{timeout_limit} seconds"
          end
          # adapt instruction treshold as needed :
          if count
            treshold = getInstructionObserverThreshold
            if elapsed_time * 2 < timeout_limit
              next_treshold_guess = treshold * 2
              if instruction_limit && instruction_limit < next_treshold_guess
                setInstructionObserverThreshold(instruction_limit)
              else
                setInstructionObserverThreshold(next_treshold_guess)
              end
            end
          end
        end
      end

      def reset!
        @instruction_count = 0
        @start_time = Time.now
        self
      end

    end

  end

  class ContextError < StandardError # :nodoc:
  end

  class RunawayScriptError < ContextError # :nodoc:
  end

  class ScriptTimeoutError < ContextError # :nodoc:
  end

end
