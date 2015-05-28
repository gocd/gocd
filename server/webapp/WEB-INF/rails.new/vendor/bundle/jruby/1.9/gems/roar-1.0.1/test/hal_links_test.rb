require 'ostruct'
require 'test_helper'
require 'roar/json/hal'


class HalLinkTest < MiniTest::Spec
  let(:rpr) do
    Module.new do
      include Roar::JSON
      include Roar::JSON::HAL::Links
      link :self do
        "//songs"
      end
    end
  end

  subject { Object.new.extend(rpr) }

  describe "#to_json" do
    it "uses 'links' key" do
      subject.to_json.must_equal "{\"links\":{\"self\":{\"href\":\"//songs\"}}}"
    end
  end

  describe "#from_json" do
    it "uses 'links' key" do
      subject.from_json("{\"links\":{\"self\":{\"href\":\"//lifer\"}}}").links.values.must_equal [link("href" => "//lifer", "rel" => "self")]
    end
  end
end

