require 'test_helper'

class FilterPipelineTest < MiniTest::Spec
  let (:block1) { lambda { |value, *| "1: #{value}" } }
  let (:block2) { lambda { |value, *| "2: #{value}" } }

  subject { Representable::Pipeline[block1, block2] }

  it { subject.call(Object, "Horowitz").must_equal "2: 1: Horowitz" }
end


class FilterTest < MiniTest::Spec
  representer! do
    property :title
    property :track,
      :parse_filter  => lambda { |val, doc, options| "#{val.downcase},#{doc},#{options}" },
      :render_filter => lambda { |val, doc, options| "#{val.upcase},#{doc},#{options}" }
  end

  # gets doc and options.
  it {
    song = OpenStruct.new.extend(representer).from_hash("title" => "VULCAN EARS", "track" => "Nine")
    song.title.must_equal "VULCAN EARS"
    song.track.must_equal "nine,{\"title\"=>\"VULCAN EARS\", \"track\"=>\"Nine\"},{}"
  }

  it { OpenStruct.new("title" => "vulcan ears", "track" => "Nine").extend(representer).to_hash.must_equal( {"title"=>"vulcan ears", "track"=>"NINE,{\"title\"=>\"vulcan ears\"},{}"}) }


  describe "#parse_filter" do
    representer! do
      property :track,
        :parse_filter => [
          lambda { |val, doc, options| "#{val}-1" },
          lambda { |val, doc, options| "#{val}-2" }],
        :render_filter => [
          lambda { |val, doc, options| "#{val}-1" },
          lambda { |val, doc, options| "#{val}-2" }]

      # definition[:parse_filter].instance_variable_get(:@value) << lambda { |val, doc, options| "#{val}-1" }
      # property :track, :parse_filter => lambda { |val, doc, options| "#{val}-2" }, :inherit => true
    end

    # order matters.
    it { OpenStruct.new.extend(representer).from_hash("track" => "Nine").track.must_equal "Nine-1-2" }
    it { OpenStruct.new("track" => "Nine").extend(representer).to_hash.must_equal({"track"=>"Nine-1-2"}) }
  end
end


class RenderFilterTest < MiniTest::Spec
  representer! do
    property :track, :render_filter => [lambda { |val, doc, options| "#{val}-1" } ]
    property :track, :render_filter => [lambda { |val, doc, options| "#{val}-2" } ], :inherit => true
  end

  it { OpenStruct.new("track" => "Nine").extend(representer).to_hash.must_equal({"track"=>"Nine-1-2"}) }
end