module Spec
  module Matchers
    class Matcher
      include Spec::Matchers::Pretty
      
      attr_reader :expected, :actual
      
      def initialize(name, *expected, &declarations)
        @name     = name
        @expected = expected
        @actual   = nil
        @diffable = false
        @messages = {
          :description => lambda {"#{name_to_sentence}#{expected_to_sentence}"},
          :failure_message_for_should => lambda {|actual| "expected #{actual.inspect} to #{name_to_sentence}#{expected_to_sentence}"},
          :failure_message_for_should_not => lambda {|actual| "expected #{actual.inspect} not to #{name_to_sentence}#{expected_to_sentence}"}
        }
        making_declared_methods_public do
          instance_exec(*@expected, &declarations)
        end
      end
      
      def matches?(actual)
        @actual = actual
        instance_exec(@actual,    &@match_block)
      end
      
      def description(&block)
        cache_or_call_cached(:description, &block)
      end
      
      def failure_message_for_should(&block)
        cache_or_call_cached(:failure_message_for_should, @actual, &block)
      end
      
      def failure_message_for_should_not(&block)
        cache_or_call_cached(:failure_message_for_should_not, @actual, &block)
      end
      
      def match(&block)
        @match_block = block
      end
      
      def diffable?
        @diffable
      end
      
      def diffable
        @diffable = true
      end
            
    private

      def making_declared_methods_public # :nodoc:
        # Our home-grown instance_exec in ruby 1.8.6 results in any methods
        # declared in the block eval'd by instance_exec in the block to which we
        # are yielding here are scoped private. This is NOT the case for Ruby
        # 1.8.7 or 1.9.
        # 
        # Also, due some crazy scoping that I don't understand, these methods
        # are actually available in the specs (something about the matcher being
        # defined in the scope of Spec::Matchers or within an example), so not
        # doing the following will not cause specs to fail, but they *will*
        # cause features to fail and that will make users unhappy. So don't.
        orig_private_methods = private_methods
        yield
        st = (class << self; self; end)
        (private_methods - orig_private_methods).each {|m| st.__send__ :public, m}
      end

      def cache_or_call_cached(key, actual=nil, &block)
        block ? @messages[key] = block : 
                actual.nil? ? @messages[key].call : @messages[key].call(actual)
      end
    
      def name_to_sentence
        split_words(@name)
      end
      
      def expected_to_sentence
        to_sentence(@expected)
      end
    
    end
  end
end