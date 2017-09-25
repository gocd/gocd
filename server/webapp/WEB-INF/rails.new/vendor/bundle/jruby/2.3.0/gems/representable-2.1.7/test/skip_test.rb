require 'test_helper'

class SkipParseTest < MiniTest::Spec
  representer! do
    property :title
    property :band,
      skip_parse: lambda { |fragment, opts| opts[:skip?] and fragment["name"].nil? }, class: OpenStruct do
        property :name
    end

    collection :airplays,
      skip_parse: lambda { |fragment, opts| puts fragment.inspect; opts[:skip?] and fragment["station"].nil? }, class: OpenStruct do
        property :station
    end
  end

  let (:song) { OpenStruct.new.extend(representer) }

  # do parse.
  it { song.from_hash({"band" => {"name" => "Mute 98"}}, skip?: true).band.name.must_equal "Mute 98" }
  it { song.from_hash({"airplays" => [{"station" => "JJJ"}]}, skip?: true).airplays[0].station.must_equal "JJJ" }

  # skip parsing.
  it { song.from_hash({"band" => {}}, skip?: true).band.must_equal nil }
  # skip_parse is _per item_.
  let (:airplay) { OpenStruct.new(station: "JJJ") }
  it { song.from_hash({"airplays" => [{"station" => "JJJ"}, {}]}, skip?: true).airplays.must_equal [airplay] }

  # it skips parsing of items as if they hadn't been in the document.
  it { song.from_hash({"airplays" => [{"station" => "JJJ"}, {}, {"station" => "JJJ"}]}, skip?: true).airplays.must_equal [airplay, airplay] }
end

class SkipRenderTest < MiniTest::Spec
  representer! do
    property :title
    property :band,
      skip_render: lambda { |object, opts| opts[:skip?] and object.name == "Rancid" } do
        property :name
    end

    collection :airplays,
      skip_render: lambda { |object, opts| puts object.inspect; opts[:skip?] and object.station == "Radio Dreyeckland" } do
        property :station
    end
  end

  let (:song)      { OpenStruct.new(title: "Black Night", band: OpenStruct.new(name: "Time Again")).extend(representer) }
  let (:skip_song) { OpenStruct.new(title: "Time Bomb",   band: OpenStruct.new(name: "Rancid")).extend(representer) }

  # do render.
  it { song.to_hash(skip?: true).must_equal({"title"=>"Black Night", "band"=>{"name"=>"Time Again"}}) }
  # skip.
  it { skip_song.to_hash(skip?: true).must_equal({"title"=>"Time Bomb"}) }

  # do render all collection items.
  it do
    song = OpenStruct.new(airplays: [OpenStruct.new(station: "JJJ"), OpenStruct.new(station: "ABC")]).extend(representer)
    song.to_hash(skip?: true).must_equal({"airplays"=>[{"station"=>"JJJ"}, {"station"=>"ABC"}]})
  end

  # skip middle item.
  it do
    song = OpenStruct.new(airplays: [OpenStruct.new(station: "JJJ"), OpenStruct.new(station: "Radio Dreyeckland"), OpenStruct.new(station: "ABC")]).extend(representer)
    song.to_hash(skip?: true).must_equal({"airplays"=>[{"station"=>"JJJ"}, {"station"=>"ABC"}]})
  end
end