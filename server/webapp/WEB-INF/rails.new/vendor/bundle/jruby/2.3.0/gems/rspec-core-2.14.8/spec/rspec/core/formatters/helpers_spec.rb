require 'spec_helper'
require 'rspec/core/formatters/helpers'

describe RSpec::Core::Formatters::Helpers do
  let(:helper) { Object.new.extend(RSpec::Core::Formatters::Helpers) }

  describe "format duration" do
    context '< 1' do
      it "returns '0.xxxxx seconds' formatted string" do
        expect(helper.format_duration(0.123456)).to eq("0.12346 seconds")
      end
    end

    context '> 1 and < 60' do
      it "returns 'xx.xx seconds' formatted string" do
        expect(helper.format_duration(45.51)).to eq("45.51 seconds")
      end
    end

    context '> 60 and < 120' do
      it "returns 'x minute xx.xx seconds' formatted string" do
        expect(helper.format_duration(70.14)).to eq("1 minute 10.14 seconds")
      end
    end

    context '> 120 and < 300' do
      it "returns 'x minutes xx.x seconds' formatted string" do
        expect(helper.format_duration(135.14)).to eq("2 minutes 15.1 seconds")
      end
    end

    context '> 300' do
      it "returns 'x minutes xx seconds' formatted string" do
        expect(helper.format_duration(335.14)).to eq("5 minutes 35 seconds")
      end
    end

    context '= 61' do
      it "returns 'x minute x second' formatted string" do
        expect(helper.format_duration(61)).to eq("1 minute 1 second")
      end
    end

    context '= 1' do
      it "returns 'x second' formatted string" do
        expect(helper.format_duration(1)).to eq("1 second")
      end
    end

    context 'with mathn loaded' do
      include MathnIntegrationSupport

      it "returns 'x minutes xx.x seconds' formatted string" do
        with_mathn_loaded do
          expect(helper.format_duration(133.7)).to eq("2 minutes 13.7 seconds")
        end
      end
    end
  end

  describe "format seconds" do
    it "uses passed in precision if specified unless result is 0" do
      expect(helper.format_seconds(0.01234, 2)).to eq("0.01")
    end

    context "sub second times" do
      it "returns 5 digits of precision" do
        expect(helper.format_seconds(0.000006)).to eq("0.00001")
      end

      it "strips off trailing zeroes beyond sub-second precision" do
        expect(helper.format_seconds(0.020000)).to eq("0.02")
      end

      context "0" do
        it "strips off trailing zeroes" do
          expect(helper.format_seconds(0.00000000001)).to eq("0")
        end
      end

      context "> 1" do
        it "strips off trailing zeroes" do
          expect(helper.format_seconds(1.00000000001)).to eq("1")
        end
      end
    end

    context "second and greater times" do

      it "returns 2 digits of precision" do
        expect(helper.format_seconds(50.330340)).to eq("50.33")
      end

      it "returns human friendly elasped time" do
        expect(helper.format_seconds(50.1)).to eq("50.1")
        expect(helper.format_seconds(5)).to eq("5")
        expect(helper.format_seconds(5.0)).to eq("5")
      end

    end
  end


end
