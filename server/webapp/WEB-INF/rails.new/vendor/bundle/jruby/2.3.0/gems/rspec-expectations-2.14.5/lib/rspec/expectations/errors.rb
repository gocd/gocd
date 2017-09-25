module RSpec
  module Expectations
    if defined?(Test::Unit::AssertionFailedError)
      class ExpectationNotMetError < Test::Unit::AssertionFailedError; end
    else
      class ExpectationNotMetError < ::StandardError; end
    end
  end
end
