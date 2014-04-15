module RSpec
  module Mocks
    # @private
    class MockExpectationError < Exception
    end
    
    # @private
    class AmbiguousReturnError < StandardError
    end
  end
end

