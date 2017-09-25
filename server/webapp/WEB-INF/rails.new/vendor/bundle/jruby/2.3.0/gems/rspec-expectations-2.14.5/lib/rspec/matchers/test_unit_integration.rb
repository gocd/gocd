# Include Matchers for other test frameworks.  Note that MiniTest _must_
# come before TU because on ruby 1.9, T::U::TC is a subclass of MT::U::TC
# and a 1.9 bug can lead to infinite recursion from the `super` call in our
# method_missing hook.  See this gist for more info:
# https://gist.github.com/845896
if defined?(MiniTest::Unit::TestCase)
  MiniTest::Unit::TestCase.send(:include, RSpec::Matchers)
end
if defined?(Test::Unit::TestCase)
  Test::Unit::TestCase.send(:include, RSpec::Matchers)
end
