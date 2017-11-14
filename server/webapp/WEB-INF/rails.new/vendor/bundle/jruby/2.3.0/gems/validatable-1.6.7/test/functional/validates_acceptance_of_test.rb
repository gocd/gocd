require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

module Functional
  class ValidatesAcceptanceOfTest < Test::Unit::TestCase
    test "given no acceptance, when validated, then error is in the objects error collection" do
      klass = Class.new do
        include Validatable
        attr_accessor :name
        validates_acceptance_of :name
      end
      instance = klass.new
      instance.valid?
      assert_equal "must be accepted", instance.errors.on(:name)
    end
  end
end