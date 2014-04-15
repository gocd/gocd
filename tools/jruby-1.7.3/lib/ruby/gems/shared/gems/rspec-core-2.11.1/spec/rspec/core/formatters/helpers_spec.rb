require 'spec_helper'
require 'rspec/core/formatters/helpers'

describe RSpec::Core::Formatters::Helpers do
  let(:helper) { Object.new.extend(RSpec::Core::Formatters::Helpers) }

  describe "format duration" do
    context '> 60 and < 120' do
      it "returns 'x minute xx seconds' formatted string" do
        helper.format_duration(70.14).should eq("1 minute 10.14 seconds")
      end
    end

    context '> 120' do
      it "returns 'x minutes xx seconds' formatted string" do
        helper.format_duration(135.14).should eq("2 minutes 15.14 seconds")
      end
    end

    context '< 60' do
      it "returns 'xx seconds' formatted string" do
        helper.format_duration(45.5).should eq("45.5 seconds")
      end
    end
  end

  describe "format seconds" do
    context "sub second times" do
      it "returns 5 digits of precision" do
        helper.format_seconds(0.000006).should eq("0.00001")
      end

      it "strips off trailing zeroes beyond sub-second precision" do
        helper.format_seconds(0.020000).should eq("0.02")
      end

      context "0" do
        it "strips off trailing zeroes" do
          helper.format_seconds(0.00000000001).should eq("0")
        end
      end

      context "> 1" do
        it "strips off trailing zeroes" do
          helper.format_seconds(1.00000000001).should eq("1")
        end
      end
    end

    context "second and greater times" do

      it "returns 2 digits of precision" do
        helper.format_seconds(50.330340).should eq("50.33")
      end

      it "returns human friendly elasped time" do
        helper.format_seconds(50.1).should eq("50.1")
        helper.format_seconds(5).should eq("5")
        helper.format_seconds(5.0).should eq("5")
      end

    end
  end


end
