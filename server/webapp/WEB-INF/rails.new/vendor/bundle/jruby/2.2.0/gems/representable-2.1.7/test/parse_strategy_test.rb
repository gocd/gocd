require 'test_helper'

# parse_strategy: :sync
# parse_strategy: :replace
# parse_strategy: :find_or_instantiate ("expand" since we don't delete existing unmatched in target)


class ParseStrategySyncTest < BaseTest
  for_formats(
    :hash => [Representable::Hash, {"song"=>{"title"=>"Resist Stance"}}, {"song"=>{"title"=>"Suffer"}}],
    :xml  => [Representable::XML, "<open_struct><song><title>Resist Stance</title></song></open_struct>", "<open_struct><song><title>Suffer</title></song></open_struct>",],
    :yaml => [Representable::YAML, "---\nsong:\n  title: Resist Stance\n", "---\nsong:\n  title: Suffer\n"],
  ) do |format, mod, output, input|

    describe "[#{format}] property with parse_strategy: :sync" do # TODO: introduce :representable option?
      let (:format) { format }

      representer!(:module => mod, :name => :song_representer) do
        property :title
        self.representation_wrap = :song if format == :xml
      end

      representer!(:inject => :song_representer, :module => mod) do
        property :song, :parse_strategy => :sync, :extend => song_representer
      end

      let (:hit) { hit = OpenStruct.new(:song => song).extend(representer) }

      it "calls #to_hash on song instance, nothing else" do
        render(hit).must_equal_document(output)
      end


      it "calls #from_hash on the existing song instance, nothing else" do
        song_id = hit.song.object_id

        parse(hit, input)

        hit.song.title.must_equal "Suffer"
        hit.song.object_id.must_equal song_id
      end
    end
  end

  # FIXME: there's a bug with XML and the collection name!
  for_formats(
    :hash => [Representable::Hash, {"songs"=>[{"title"=>"Resist Stance"}]}, {"songs"=>[{"title"=>"Suffer"}]}],
    #:json => [Representable::JSON, "{\"song\":{\"name\":\"Alive\"}}", "{\"song\":{\"name\":\"You've Taken Everything\"}}"],
    :xml  => [Representable::XML, "<open_struct><song><title>Resist Stance</title></song></open_struct>", "<open_struct><songs><title>Suffer</title></songs></open_struct>"],
    :yaml => [Representable::YAML, "---\nsongs:\n- title: Resist Stance\n", "---\nsongs:\n- title: Suffer\n"],
  ) do |format, mod, output, input|

    describe "[#{format}] collection with :parse_strategy: :sync" do # TODO: introduce :representable option?
      let (:format) { format }
      representer!(:module => mod, :name => :song_representer) do
        property :title
        self.representation_wrap = :song if format == :xml
      end

      representer!(:inject => :song_representer, :module => mod) do
        collection :songs, :parse_strategy => :sync, :extend => song_representer
      end

      let (:album) { OpenStruct.new(:songs => [song]).extend(representer) }

      it "calls #to_hash on song instances, nothing else" do
        render(album).must_equal_document(output)
      end

      it "calls #from_hash on the existing song instance, nothing else" do
        collection_id = album.songs.object_id
        song          = album.songs.first
        song_id       = song.object_id

        parse(album, input)

        album.songs.first.title.must_equal "Suffer"
        song.title.must_equal "Suffer"
        #album.songs.object_id.must_equal collection_id # TODO: don't replace!
        song.object_id.must_equal song_id
      end
    end
  end


  # Sync errors, when model and incoming are not in sync.
  describe ":sync with error" do
    representer! do
      property :song, :parse_strategy => :sync do
        property :title
      end
    end

    # object.song is nil whereas the document contains one.
    it do
      assert_raises Representable::DeserializeError do
        OpenStruct.new.extend(representer).from_hash({"song" => {"title" => "Perpetual"}})
      end
    end
  end



  # Lonely Collection
  for_formats(
    :hash => [Representable::Hash::Collection, [{"title"=>"Resist Stance"}], [{"title"=>"Suffer"}]],
    # :xml  => [Representable::XML, "<open_struct><song><title>Resist Stance</title></song></open_struct>", "<open_struct><songs><title>Suffer</title></songs></open_struct>"],
  ) do |format, mod, output, input|

    describe "[#{format}] lonely collection with :parse_strategy: :sync" do # TODO: introduce :representable option?
      let (:format) { format }
      representer!(:module => Representable::Hash, :name => :song_representer) do
        property :title
        self.representation_wrap = :song if format == :xml
      end

      representer!(:inject => :song_representer, :module => mod) do
        items :parse_strategy => :sync, :extend => song_representer
      end

      let (:album) { [song].extend(representer) }

      it "calls #to_hash on song instances, nothing else" do
        render(album).must_equal_document(output)
      end

      it "calls #from_hash on the existing song instance, nothing else" do
        #collection_id = album.object_id
        song          = album.first
        song_id       = song.object_id

        parse(album, input)

        album.first.title.must_equal "Suffer"
        song.title.must_equal "Suffer"
        song.object_id.must_equal song_id
      end
    end
  end
end


class ParseStrategyFindOrInstantiateTest < BaseTest
  # parse_strategy: :find_or_instantiate

  Song = Struct.new(:id, :title)
  Song.class_eval do
    def self.find_by(attributes={})
      return new(1, "Resist Stan") if attributes[:id]==1# we should return the same object here
      new
    end
  end

  representer!(:name => :song_representer) do
    property :title
  end


  describe "collection" do
    representer!(:inject => :song_representer) do
      collection :songs, :parse_strategy => :find_or_instantiate, :extend => song_representer, :class => Song
    end

    let (:album) { Struct.new(:songs).new([]).extend(representer) }


    it "replaces the existing collection with a new consisting of existing items or new items" do
      songs_id = album.songs.object_id

      album.from_hash({"songs"=>[{"id" => 1, "title"=>"Resist Stance"}, {"title"=>"Suffer"}]})

      album.songs[0].title.must_equal "Resist Stance" # note how title is updated from "Resist Stan"
      album.songs[0].id.must_equal 1
      album.songs[1].title.must_equal "Suffer"
      album.songs[1].id.must_equal nil

      album.songs.object_id.wont_equal songs_id
    end

    # TODO: test with existing collection
  end


  describe "property" do
    representer!(:inject => :song_representer) do
      property :song, :parse_strategy => :find_or_instantiate, :extend => song_representer, :class => Song
    end

    let (:album) { Struct.new(:song).new.extend(representer) }


    it "finds song by id" do
      album.from_hash({"song"=>{"id" => 1, "title"=>"Resist Stance"}})

      album.song.title.must_equal "Resist Stance" # note how title is updated from "Resist Stan"
      album.song.id.must_equal 1
    end

    it "creates song" do
      album.from_hash({"song"=>{"title"=>"Off The Track"}})

      album.song.title.must_equal "Off The Track"
      album.song.id.must_equal nil
    end
  end


  describe "property with dynamic :class" do
    representer!(:inject => :song_representer) do
      property :song, :parse_strategy => :find_or_instantiate, :extend => song_representer,
        :class => lambda { |fragment, *args| fragment["class"] }
    end

    let (:album) { Struct.new(:song).new.extend(representer) }


    it "finds song by id" do
      album.from_hash({"song"=>{"id" => 1, "title"=>"Resist Stance", "class"=>Song}})

      album.song.title.must_equal "Resist Stance" # note how title is updated from "Resist Stan"
      album.song.id.must_equal 1
    end
  end
end


class ParseStrategyLambdaTest < MiniTest::Spec
  Song = Struct.new(:id, :title)
  Song.class_eval do
    def self.find_by(attributes={})
      return new(1, "Resist Stan") if attributes[:id]==1# we should return the same object here
      new
    end
  end

  representer!(:name => :song_representer) do
    property :title
  end

  # property with instance: lambda, using representable's setter. # TODO: that should be handled better via my api.
  describe "property parse_strategy: lambda, representable: false" do
    representer! do
      property :title,
        :instance      => lambda { |fragment, options| fragment.to_s },  # this will still call song.title= "8675309".
        :representable => false # don't call object.from_hash
    end

    let (:song) { Song.new(nil, nil) }
    it { song.extend(representer).from_hash("title" => 8675309).title.must_equal "8675309" }
  end


  describe "collection" do
    representer!(:inject => :song_representer) do
      collection :songs, :parse_strategy => lambda { |fragment, i, options|
        songs << song = Song.new
        song
      }, :extend => song_representer
    end

    let (:album) { Struct.new(:songs).new([Song.new(1, "A Walk")]).extend(representer) }


    it "adds to existing collection" do
      songs_id = album.songs.object_id

      album.from_hash({"songs"=>[{"title"=>"Resist Stance"}]})

      album.songs[0].title.must_equal "A Walk" # note how title is updated from "Resist Stan"
      album.songs[0].id.must_equal 1
      album.songs[1].title.must_equal "Resist Stance"
      album.songs[1].id.must_equal nil

      album.songs.object_id.must_equal songs_id
    end

    # TODO: test with existing collection
  end
end
