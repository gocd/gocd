require 'spec_helper'

module RSpec
  describe Matchers do

    let(:sample_matchers) do
      [:be,
       :be_close,
       :be_instance_of,
       :be_kind_of]
    end

    context "once required" do
      include TestUnitIntegrationSupport

      it "includes itself in Test::Unit::TestCase" do
        with_test_unit_loaded do
          test_unit_case = Test::Unit::TestCase.allocate
          sample_matchers.each do |sample_matcher|
              expect(test_unit_case).to respond_to(sample_matcher)
          end
        end
      end

      it "includes itself in MiniTest::Unit::TestCase", :if => defined?(MiniTest) do
        with_test_unit_loaded do
          minitest_case = MiniTest::Unit::TestCase.allocate
          sample_matchers.each do |sample_matcher|
              expect(minitest_case).to respond_to(sample_matcher)
          end
        end
      end

    end

  end
end
