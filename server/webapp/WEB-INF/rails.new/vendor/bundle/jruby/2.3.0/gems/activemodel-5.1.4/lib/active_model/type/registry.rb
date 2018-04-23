module ActiveModel
  # :stopdoc:
  module Type
    class Registry
      def initialize
        @registrations = []
      end

      def register(type_name, klass = nil, **options, &block)
        block ||= proc { |_, *args| klass.new(*args) }
        registrations << registration_klass.new(type_name, block, **options)
      end

      def lookup(symbol, *args)
        registration = find_registration(symbol, *args)

        if registration
          registration.call(self, symbol, *args)
        else
          raise ArgumentError, "Unknown type #{symbol.inspect}"
        end
      end

      # TODO Change this to private once we've dropped Ruby 2.2 support.
      # Workaround for Ruby 2.2 "private attribute?" warning.
      protected

        attr_reader :registrations

      private

        def registration_klass
          Registration
        end

        def find_registration(symbol, *args)
          registrations.find { |r| r.matches?(symbol, *args) }
        end
    end

    class Registration
      # Options must be taken because of https://bugs.ruby-lang.org/issues/10856
      def initialize(name, block, **)
        @name = name
        @block = block
      end

      def call(_registry, *args, **kwargs)
        if kwargs.any? # https://bugs.ruby-lang.org/issues/10856
          block.call(*args, **kwargs)
        else
          block.call(*args)
        end
      end

      def matches?(type_name, *args, **kwargs)
        type_name == name
      end

      # TODO Change this to private once we've dropped Ruby 2.2 support.
      # Workaround for Ruby 2.2 "private attribute?" warning.
      protected

        attr_reader :name, :block
    end
  end
  # :startdoc:
end
