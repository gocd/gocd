module Spec
  module Matchers
    class AutotestMappingMatcher
      def initialize(specs)
        @specs = specs
      end
  
      def to(file)
        @file = file
        self
      end
  
      def matches?(autotest)
        @autotest = prepare(autotest)
        @actual = autotest.test_files_for(@file)
        @actual == @specs
      end
  
      def failure_message
        "expected #{@autotest.class} to map #{@specs.inspect} to #{@file.inspect}\ngot #{@actual.inspect}"
      end
  
    private

      def prepare(autotest)
        find_order = @specs.dup << @file
        autotest.instance_eval { @find_order = find_order }
        autotest
      end

    end
    
    def map_specs(specs)
      AutotestMappingMatcher.new(specs)
    end
    
  end
end