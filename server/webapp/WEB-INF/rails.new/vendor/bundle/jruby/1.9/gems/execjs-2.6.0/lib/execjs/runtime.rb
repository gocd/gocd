require "execjs/encoding"

module ExecJS
  # Abstract base class for runtimes
  class Runtime
    class Context
      include Encoding

      def initialize(runtime, source = "")
      end

      def exec(source, options = {})
        raise NotImplementedError
      end

      def eval(source, options = {})
        raise NotImplementedError
      end

      def call(properties, *args)
        raise NotImplementedError
      end
    end

    def name
      raise NotImplementedError
    end

    def context_class
      self.class::Context
    end

    def exec(source)
      context = context_class.new(self)
      context.exec(source)
    end

    def eval(source)
      context = context_class.new(self)
      context.eval(source)
    end

    def compile(source)
      context_class.new(self, source)
    end

    def deprecated?
      false
    end

    def available?
      raise NotImplementedError
    end
  end
end
