class RailsDevTweaks::Configuration

  # By default, we log a notice on each request that has its to_prepare hooks skipped, you can
  # disable that if you choose!
  attr_accessor :log_autoload_notice

  attr_reader :granular_autoload_config

  def initialize
    @log_autoload_notice = true

    @granular_autoload_config = GranularAutoloadConfiguration.new

    # And set our defaults
    self.autoload_rules do
      keep :all

      skip '/favicon.ico'
      skip :assets
      keep :forced
    end
  end

  # Takes a block that configures the granular autoloader's rules.
  def autoload_rules(&block)
    @granular_autoload_config.instance_eval(&block)
  end

  class GranularAutoloadConfiguration

    def initialize
      # Each rule is a simple pair: [:skip, callable], [:keep, callable], etc.
      @rules = []
    end

    def keep(*args, &block)
      self.append_rule(:keep, *args, &block)
    end

    def skip(*args, &block)
      self.append_rule(:skip, *args, &block)
    end

    def append_rule(rule_type, *args, &block)
      unless rule_type == :skip || rule_type == :keep
        raise TypeError, "Rule must be :skip or :keep.  Got: #{rule_type.inspect}"
      end

      # Simple matcher blocks
      if args.size == 0 && block.present?
        @rules.unshift [rule_type, block]
        return self
      end

      # String match shorthand
      args[0] = /^#{args[0]}/ if args.size == 1 && args[0].kind_of?(String)

      # Regex match shorthand
      args = [:path, args[0]] if args.size == 1 && args[0].kind_of?(Regexp)

      if args.size == 0 && block.blank?
        raise TypeError, 'Cannot process autoload rule as specified.  Expecting a named matcher (symbol), path prefix (string) or block'
      end

      # Named matcher
      matcher_class = "RailsDevTweaks::GranularAutoload::Matchers::#{args[0].to_s.classify}Matcher".constantize
      matcher       = matcher_class.new(*args[1..-1])
      raise TypeError, "Matchers must respond to :call. #{matcher.inspect} does not." unless matcher.respond_to? :call

      @rules.unshift [rule_type, matcher]

      self
    end

    def should_reload?(request)
      @rules.each do |rule_type, callable|
        return rule_type == :keep if callable.call(request)
      end

      # We default to reloading to preserve behavior, but we should never get to this unless the configuration is
      # all sorts of horked.
      true
    end

  end

end
