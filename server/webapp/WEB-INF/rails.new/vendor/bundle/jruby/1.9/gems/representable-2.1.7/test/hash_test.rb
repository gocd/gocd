require 'test_helper'

class HashTest < MiniTest::Spec
  def self.hash_representer(&block)
    Module.new do
      include Representable::Hash
      instance_exec &block
    end
  end

  def hash_representer(&block)
    self.class.hash_representer(&block)
  end


  describe "property" do
    let (:hsh) { hash_representer do property :best_song end }

    let (:album) { Album.new.tap do |album|
      album.best_song = "Liar"
    end }

    describe "#to_hash" do
      it "renders plain property" do
        album.extend(hsh).to_hash.must_equal("best_song" => "Liar")
      end
    end


    describe "#from_hash" do
      it "parses plain property" do
        album.extend(hsh).from_hash("best_song" => "This Song Is Recycled").best_song.must_equal "This Song Is Recycled"
      end
    end


    describe "with :class and :extend" do
      hash_song = hash_representer do property :name end
      let (:hash_album) { Module.new do
        include Representable::Hash
        property :best_song, :extend => hash_song, :class => Song
      end }

      let (:album) { Album.new.tap do |album|
        album.best_song = Song.new("Liar")
      end }


      describe "#to_hash" do
        it "renders embedded typed property" do
          album.extend(hash_album).to_hash.must_equal("best_song" => {"name" => "Liar"})
        end
      end

      describe "#from_hash" do
        it "parses embedded typed property" do
          album.extend(hash_album).from_hash("best_song" => {"name" => "Go With Me"}).must_equal Album.new(nil,Song.new("Go With Me"))
        end
      end
    end


    describe "with :extend and :as" do
      hash_song = hash_representer do property :name end

      let (:hash_album) { Module.new do
        include Representable::Hash
        property :song, :extend => hash_song, :class => Song, :as => :hit
      end }

      let (:album) { OpenStruct.new(:song => Song.new("Liar")).extend(hash_album) }

      it { album.to_hash.must_equal("hit" => {"name" => "Liar"}) }
      it { album.from_hash("hit" => {"name" => "Go With Me"}).must_equal OpenStruct.new(:song => Song.new("Go With Me")) }
    end
    # describe "FIXME COMBINE WITH ABOVE with :extend and :as" do
    #   hash_song = Module.new do
    #     include Representable::XML
    #     self.representation_wrap = :song
    #     property :name
    #   end

    #   let (:hash_album) { Module.new do
    #     include Representable::XML
    #     self.representation_wrap = :album
    #     property :song, :extend => hash_song, :class => Song, :as => :hit
    #   end }

    #   let (:album) { OpenStruct.new(:song => Song.new("Liar")).extend(hash_album) }

    #   it { album.to_xml.must_equal_xml("<album><hit><name>Liar</name></hit></album>") }
    #   #it { album.from_hash("hit" => {"name" => "Go With Me"}).must_equal OpenStruct.new(:song => Song.new("Go With Me")) }
    # end
  end


  describe "collection" do
    let (:hsh) { hash_representer do collection :songs end }

    let (:album) { Album.new.tap do |album|
      album.songs = ["Jackhammer", "Terrible Man"]
    end }


    describe "#to_hash" do
      it "renders a block style list per default" do
        album.extend(hsh).to_hash.must_equal("songs" => ["Jackhammer", "Terrible Man"])
      end
    end


    describe "#from_hash" do
      it "parses a block style list" do
        album.extend(hsh).from_hash("songs" => ["Off Key Melody", "Sinking"]).must_equal Album.new(["Off Key Melody", "Sinking"])

      end
    end


    describe "with :class and :extend" do
      hash_song = hash_representer do
        property :name
        property :track
      end
      let (:hash_album) { Module.new do
        include Representable::Hash
        collection :songs, :extend => hash_song, :class => Song
      end }

      let (:album) { Album.new.tap do |album|
        album.songs = [Song.new("Liar", 1), Song.new("What I Know", 2)]
      end }


      describe "#to_hash" do
        it "renders collection of typed property" do
          album.extend(hash_album).to_hash.must_equal("songs" => [{"name" => "Liar", "track" => 1}, {"name" => "What I Know", "track" => 2}])
        end
      end

      describe "#from_hash" do
        it "parses collection of typed property" do
          album.extend(hash_album).from_hash("songs" => [{"name" => "One Shot Deal", "track" => 4},
            {"name" => "Three Way Dance", "track" => 5}]).must_equal Album.new([Song.new("One Shot Deal", 4), Song.new("Three Way Dance", 5)])
        end
      end
    end
  end
end