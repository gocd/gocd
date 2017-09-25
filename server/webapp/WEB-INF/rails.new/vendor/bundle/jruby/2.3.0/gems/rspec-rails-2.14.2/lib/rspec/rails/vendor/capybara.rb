begin
  require 'capybara/rspec'
rescue LoadError
end

begin
  require 'capybara/rails'
rescue LoadError
end

if defined?(Capybara)
  RSpec.configure do |c|
    if defined?(Capybara::DSL)
      c.include Capybara::DSL, :type => :controller
      c.include Capybara::DSL, :type => :feature
    end

    if defined?(Capybara::RSpecMatchers)
      c.include Capybara::RSpecMatchers, :type => :view
      c.include Capybara::RSpecMatchers, :type => :helper
      c.include Capybara::RSpecMatchers, :type => :mailer
      c.include Capybara::RSpecMatchers, :type => :controller
      c.include Capybara::RSpecMatchers, :example_group => {
        :file_path => c.escaped_path(%w[spec features])
      }
    end

    unless defined?(Capybara::RSpecMatchers) || defined?(Capybara::DSL)
      c.include Capybara, :type => :request
      c.include Capybara, :type => :controller
    end
  end
end
