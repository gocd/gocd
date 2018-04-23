require 'test/unit'
require 'rubygems'
require 'mocha'
require 'dust'
require 'set'
require 'expectations'

require File.dirname(__FILE__) + '/../lib/validatable'

class << Test::Unit::TestCase
  def expect(expected_value, &block)
    define_method :"test_#{caller.first.split("/").last}" do
      begin
        assert_equal expected_value, instance_eval(&block)
      rescue Exception => ex
        raise ex unless expected_value.is_a?(Class) && ex.is_a?(expected_value)
        assert_equal expected_value, ex.class
      end
    end 
  end
end

class Test::Unit::TestCase
  def assert_array_equal a, b
    assert_equal Set.new(a), Set.new(b)
  end
end

class Array
  def to_blank_options_hash
    self.inject({}) {|hash, value| hash[value] = nil; hash }
  end
end