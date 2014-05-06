module Spec
  module Matchers
    # :call-seq:
    #   should include(expected)
    #   should_not include(expected)
    #
    # Passes if actual includes expected. This works for
    # collections and Strings. You can also pass in multiple args
    # and it will only pass if all args are found in collection.
    #
    # == Examples
    #
    #   [1,2,3].should include(3)
    #   [1,2,3].should include(2,3) #would pass
    #   [1,2,3].should include(2,3,4) #would fail
    #   [1,2,3].should_not include(4)
    #   "spread".should include("read")
    #   "spread".should_not include("red")
    def include(*expected)
      Matcher.new :include, *expected do |*_expected_|
        match do |actual|
          helper(actual, *_expected_)
        end
        
        def helper(actual, *_expected_)
          _expected_.each do |expected|
            if actual.is_a?(Hash)
              if expected.is_a?(Hash)
                expected.each_pair do |k,v|
                  return false unless actual[k] == v
                end
              else
                return false unless actual.has_key?(expected)
              end
            else
              return false unless actual.include?(expected)
            end
          end
          true
        end
      end
    end
  end
end
