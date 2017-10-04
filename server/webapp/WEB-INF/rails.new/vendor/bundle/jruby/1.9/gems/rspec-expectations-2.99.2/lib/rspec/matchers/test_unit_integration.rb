# Include Matchers for other test frameworks.  Note that MiniTest _must_
# come before TU because on ruby 1.9, T::U::TC is a subclass of MT::U::TC
# and a 1.9 bug can lead to infinite recursion from the `super` call in our
# method_missing hook.  See this gist for more info:
# https://gist.github.com/845896
if defined?(MiniTest::TestCase)
  MiniTest::TestCase.add_setup_hook do |instance|
    unless ::RSpec::Matchers === instance
      ::RSpec.deprecate("rspec-expectations' built-in integration with minitest < 5.x",
                        :replacement => "`include RSpec::Matchers` from within `Minitest::TestCase`")

      MiniTest::TestCase.send(:include, RSpec::Matchers)
    end
  end
elsif defined?(Test::Unit::TestCase)
  Test::Unit::TestCase.class_eval do
    def setup
      unless ::RSpec::Matchers === self
        ::RSpec.deprecate("rspec-expectations' built-in integration with Test::Unit",
                          :replacement => "`include RSpec::Matchers` from within `Test::Unit::TestCase`")

        Test::Unit::TestCase.send(:include, RSpec::Matchers)
      end

      super if defined?(super)
    end
  end
end
