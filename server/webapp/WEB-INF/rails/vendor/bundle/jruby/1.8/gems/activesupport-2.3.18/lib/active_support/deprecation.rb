require 'yaml'

module ActiveSupport
  module Deprecation #:nodoc:
    mattr_accessor :debug
    self.debug = false

    # Choose the default warn behavior according to RAILS_ENV.
    # Ignore deprecation warnings in production.
    DEFAULT_BEHAVIORS = {
      'test'        => Proc.new { |message, callstack|
                         $stderr.puts(message)
                         $stderr.puts callstack.join("\n  ") if debug
                       },
      'development' => Proc.new { |message, callstack|
                         logger = (defined?(Rails) && Rails.logger) ? Rails.logger : Logger.new($stderr)
                         logger.warn message
                         logger.debug callstack.join("\n  ") if debug
                       }
    }

    class << self
      def warn(message = nil, callstack = caller)
        behavior.call(deprecation_message(callstack, message), callstack) if behavior && !silenced?
      end

      def default_behavior
        if defined?(RAILS_ENV)
          DEFAULT_BEHAVIORS[RAILS_ENV.to_s]
        else
          DEFAULT_BEHAVIORS['test']
        end
      end

      # Have deprecations been silenced?
      def silenced?
        @silenced = false unless defined?(@silenced)
        @silenced
      end

      # Silence deprecation warnings within the block.
      def silence
        old_silenced, @silenced = @silenced, true
        yield
      ensure
        @silenced = old_silenced
      end

      attr_writer :silenced


      private
        def deprecation_message(callstack, message = nil)
          message ||= "You are using deprecated behavior which will be removed from the next major or minor release."
          message += '.' unless message =~ /\.$/
          "DEPRECATION WARNING: #{message} #{deprecation_caller_message(callstack)}"
        end

        def deprecation_caller_message(callstack)
          file, line, method = extract_callstack(callstack)
          if file
            if line && method
              "(called from #{method} at #{file}:#{line})"
            else
              "(called from #{file}:#{line})"
            end
          end
        end

        def extract_callstack(callstack)
          if md = callstack.first.match(/^(.+?):(\d+)(?::in `(.*?)')?/)
            md.captures
          else
            callstack.first
          end
        end
    end

    # Behavior is a block that takes a message argument.
    mattr_accessor :behavior
    self.behavior = default_behavior

    # Warnings are not silenced by default.
    self.silenced = false

    module ClassMethods #:nodoc:
      # Declare that a method has been deprecated.
      def deprecate(*method_names)
        options = method_names.extract_options!
        method_names = method_names + options.keys
        method_names.each do |method_name|
          alias_method_chain(method_name, :deprecation) do |target, punctuation|
            class_eval(<<-EOS, __FILE__, __LINE__ + 1)
              def #{target}_with_deprecation#{punctuation}(*args, &block)          # def generate_secret_with_deprecation(*args, &block)
                ::ActiveSupport::Deprecation.warn(                                 #   ::ActiveSupport::Deprecation.warn(
                  self.class.deprecated_method_warning(                            #     self.class.deprecated_method_warning(
                    :#{method_name},                                               #       :generate_secret,
                    #{options[method_name].inspect}),                              #       "You should use ActiveSupport::SecureRandom.hex(64)"),
                  caller                                                           #     caller
                )                                                                  #   )
                send(:#{target}_without_deprecation#{punctuation}, *args, &block)  #   send(:generate_secret_without_deprecation, *args, &block)
              end                                                                  # end
            EOS
          end
        end
      end

      def deprecated_method_warning(method_name, message=nil)
        warning = "#{method_name} is deprecated and will be removed from Rails #{deprecation_horizon}"
        case message
          when Symbol then "#{warning} (use #{message} instead)"
          when String then "#{warning} (#{message})"
          else warning
        end
      end

      def deprecation_horizon
        '2.3'
      end
    end

    class DeprecationProxy #:nodoc:
      def self.new(*args, &block)
        object = args.first
        
        return object unless object
        super
      end
      
      silence_warnings do
        instance_methods.each { |m| undef_method m unless m =~ /^__/ }
      end

      # Don't give a deprecation warning on inspect since test/unit and error
      # logs rely on it for diagnostics.
      def inspect
        target.inspect
      end

      private
        def method_missing(called, *args, &block)
          warn caller, called, args
          target.__send__(called, *args, &block)
        end
    end

    class DeprecatedObjectProxy < DeprecationProxy
      def initialize(object, message)
        @object = object
        @message = message
      end

      private
        def target
          @object
        end

        def warn(callstack, called, args)
          ActiveSupport::Deprecation.warn(@message, callstack)
        end
    end

    # Stand-in for <tt>@request</tt>, <tt>@attributes</tt>, <tt>@params</tt>, etc.
    # which emits deprecation warnings on any method call (except +inspect+).
    class DeprecatedInstanceVariableProxy < DeprecationProxy #:nodoc:
      def initialize(instance, method, var = "@#{method}")
        @instance, @method, @var = instance, method, var
      end

      private
        def target
          @instance.__send__(@method)
        end

        def warn(callstack, called, args)
          ActiveSupport::Deprecation.warn("#{@var} is deprecated! Call #{@method}.#{called} instead of #{@var}.#{called}. Args: #{args.inspect}", callstack)
        end
    end

    class DeprecatedConstantProxy < DeprecationProxy #:nodoc:
      def initialize(old_const, new_const)
        @old_const = old_const
        @new_const = new_const
      end

      def class
        target.class
      end

      private
        def target
          @new_const.to_s.constantize
        end

        def warn(callstack, called, args)
          ActiveSupport::Deprecation.warn("#{@old_const} is deprecated! Use #{@new_const} instead.", callstack)
        end
    end
  end
end

class Module
  include ActiveSupport::Deprecation::ClassMethods
end
