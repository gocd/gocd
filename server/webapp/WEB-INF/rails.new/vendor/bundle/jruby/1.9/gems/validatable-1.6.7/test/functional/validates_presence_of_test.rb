require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

module Functional
  class ValidatesPresenceOfTest < Test::Unit::TestCase
    test "given no name, when validated, then error is in the objects error collection" do
      klass = Class.new do
        include Validatable
        attr_accessor :name
        validates_presence_of :name
      end
      instance = klass.new
      instance.valid?
      assert_equal "can't be empty", instance.errors.on(:name)
    end
  end
end