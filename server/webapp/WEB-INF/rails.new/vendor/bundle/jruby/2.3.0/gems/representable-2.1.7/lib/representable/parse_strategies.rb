module Representable
  # Parse strategies are just a combination of representable's options. They save you from memoizing the
  # necessary parameters.
  #
  # Feel free to contribute your strategy if you think it's worth sharing!
  class ParseStrategy
    def self.apply!(options)
      return unless strategy = options[:parse_strategy]

      strategy = :proc if strategy.is_a?(::Proc)

      parse_strategies[strategy].apply!(name, options)
    end

    def self.parse_strategies
      {
        :sync                 => Sync,
        :find_or_instantiate  => FindOrInstantiate,
        :proc                 => Proc
      }
    end


    # Using a lambda as parse_strategy does not set the parsed property for you.
    class Proc
      def self.apply!(name, options)
        options[:setter]       = lambda { |*| }
        options[:pass_options] = true
        options[:instance]     = options[:parse_strategy]
      end
    end


    class Sync
      def self.apply!(name, options)
        options[:setter]       = lambda { |*| }
        options[:pass_options] = true
        options[:instance]     = options[:collection] ?
          lambda { |fragment, i, options| options.binding.get[i] } :
          lambda { |fragment, options|    options.binding.get }
      end
    end


    # replaces current collection.
    class FindOrInstantiate
      def self.apply!(name, options)
        options[:pass_options] = true
        options[:instance]     = lambda { |fragment, *args|
          args = args.last # TODO: don't pass i as separate block parameter but in Options.
          object_class = args.binding[:class].evaluate(self, fragment, args)

          object_class.find_by({id: fragment["id"]}) or object_class.new
        }
      end
    end
  end
end
