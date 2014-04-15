module RSpec
  module Mocks
    # @api private
    class Space
      def add(obj)
        receivers << obj unless receivers.detect {|m| m.equal? obj}
      end

      def verify_all
        receivers.each do |mock|
          mock.rspec_verify
        end
      end

      def reset_all
        receivers.each do |mock|
          mock.rspec_reset
        end
        receivers.clear
        expectation_ordering.clear
      end

      def expectation_ordering
        @expectation_ordering ||= OrderGroup.new
      end

    private
    
      def receivers
        @receivers ||= []
      end
    end
  end
end
