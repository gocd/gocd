require 'test_helper'

class GetterSetterTest < BaseTest
  representer! do
    property :name, # key under :name.
      :getter => lambda { |args| "#{args[:welcome]} #{song_name}" },
      :setter => lambda { |val, args| self.song_name = "#{args[:welcome]} #{val}" }
  end

  subject { Struct.new(:song_name).new("Mony Mony").extend(representer) }

  it "uses :getter when rendering" do
    subject.instance_eval { def name; raise; end }
    subject.to_hash(:welcome => "Hi").must_equal({"name" => "Hi Mony Mony"})
  end

  it "does not call original reader when rendering" do
    subject.instance_eval { def name; raise; end; self }.to_hash({})
  end

  it "uses :setter when parsing" do
    subject.from_hash({"name" => "Eyes Without A Face"}, :welcome => "Hello").song_name.must_equal "Hello Eyes Without A Face"
  end

  it "does not call original writer when parsing" do
    subject.instance_eval { def name=(*); raise; end; self }.from_hash({"name"=>"Dana D And Talle T"}, {})
  end
end