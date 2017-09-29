begin
  require 'capybara/rspec'
rescue LoadError
end

begin
  require 'capybara/rails'
rescue LoadError
end

if defined?(Capybara)
  module RSpec::Rails::DeprecatedCapybaraDSL
    include ::Capybara::DSL

    def self.included(mod)
      mod.extend(ClassMethods)
      super
    end

    module ClassMethods
      def include(mod)
        if mod == ::Capybara::DSL
          class_variable_set(:@@_rspec_capybara_included_explicitly, true)
        end

        super
      end
    end

    ::Capybara::DSL.instance_methods(false).each do |method|
      # capybara internally calls `page`, skip to avoid a duplicate
      # deprecation warning
      next if method.to_s == 'page'

      define_method method do |*args, &blk|
        unless self.class.class_variable_defined?(:@@_rspec_capybara_included_explicitly)
          RSpec.deprecate "Using the capybara method `#{method}` in controller specs",
            :replacement => "feature specs (spec/features)"
        end
        super(*args, &blk)
      end
    end
  end

  RSpec.configure do |c|
    if defined?(Capybara::DSL)
      c.include ::RSpec::Rails::DeprecatedCapybaraDSL, :type => :controller
      c.include Capybara::DSL, :type => :feature
    end

    if defined?(Capybara::RSpecMatchers)
      c.include Capybara::RSpecMatchers, :type => :view
      c.include Capybara::RSpecMatchers, :type => :helper
      c.include Capybara::RSpecMatchers, :type => :mailer
      c.include Capybara::RSpecMatchers, :type => :controller
      c.include Capybara::RSpecMatchers, :type => :feature
    end

    unless defined?(Capybara::RSpecMatchers) || defined?(Capybara::DSL)
      c.include Capybara, :type => :request
      c.include Capybara, :type => :controller
    end
  end
end
