require "helper"

module Nokogiri
  module XML
    class TestSyntaxError < Nokogiri::TestCase
      def test_new
        error = Nokogiri::XML::SyntaxError.new 'hello'
        assert_equal 'hello', error.message
      end

      def test_pushing_to_array
        reader = Nokogiri::XML::Reader(StringIO.new('&bogus;'))
        assert_raises(SyntaxError) {
          reader.read
        }
        assert_equal [SyntaxError], reader.errors.map(&:class) unless Nokogiri.jruby? # needs investigation
      end

      def test_pushing_to_non_array
        reader = Nokogiri::XML::Reader(StringIO.new('&bogus;'))
        def reader.errors
          1
        end
        assert_raises(TypeError) {
          reader.read
        }
      end unless Nokogiri.jruby? # which does not internally call `errors`
    end
  end
end
