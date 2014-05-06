module Spec
  module Example
    module ArgsAndOptions
      def args_and_options(*args) # :nodoc:
        options = Hash === args.last ? args.pop : {}
        return args, options
      end

      def add_options(args, options={}) # :nodoc:
        args << {} unless Hash === args.last
        args.extend WithOptions
        args.options.merge!(options)
        args.options
      end

      def set_location(options, location) # :nodoc:
        options[:location] ||= location
      end

      module WithOptions # :nodoc:
        def options
          last
        end
      end
    end
  end
end
