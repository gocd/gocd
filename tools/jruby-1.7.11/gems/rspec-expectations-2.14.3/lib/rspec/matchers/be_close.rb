module RSpec
  module Matchers
    # @deprecated use +be_within+ instead.
    def be_close(expected, delta)
      RSpec.deprecate("be_close(#{expected}, #{delta})", :replacement => "be_within(#{delta}).of(#{expected})")
      be_within(delta).of(expected)
    end
  end
end
