require 'test_helper'

class DecoratorTest < MiniTest::Spec
  # TODO: Move to global place since it's used twice.
  class SongRepresentation < Representable::Decorator
    include Representable::JSON
    property :name
  end

  class AlbumRepresentation < Representable::Decorator
    include Representable::JSON

    collection :songs, :class => Song, :extend => SongRepresentation
  end

  let (:song) { Song.new("Mama, I'm Coming Home") }
  let (:album) { Album.new([song]) }

  describe "inheritance" do
    let (:inherited_decorator) do
      Class.new(AlbumRepresentation) do
        property :best_song
      end.new(Album.new([song], "Stand Up"))
    end

    it { inherited_decorator.to_hash.must_equal({"songs"=>[{"name"=>"Mama, I'm Coming Home"}], "best_song"=>"Stand Up"}) }
  end

  let (:decorator) { AlbumRepresentation.new(album) }

  it "renders" do
    decorator.to_hash.must_equal({"songs"=>[{"name"=>"Mama, I'm Coming Home"}]})
    album.wont_respond_to :to_hash
    song.wont_respond_to :to_hash # DISCUSS: weak test, how to assert blank slate?
    # no @representable_attrs in decorated objects
    song.instance_variable_get(:@representable_attrs).must_equal nil
  end

  describe "#from_hash" do
    it "returns represented" do
      decorator.from_hash({"songs"=>[{"name"=>"Mama, I'm Coming Home"}]}).must_equal album
    end

    it "parses" do
      decorator.from_hash({"songs"=>[{"name"=>"Atomic Garden"}]})
      album.songs.first.must_be_kind_of Song
      album.songs.must_equal [Song.new("Atomic Garden")]
      album.wont_respond_to :to_hash
      song.wont_respond_to :to_hash # DISCUSS: weak test, how to assert blank slate?
    end
  end

  describe "#decorated" do
    it "is aliased to #represented" do
      AlbumRepresentation.prepare(album).decorated.must_equal album
    end
  end


  describe "inline decorators" do
    representer!(decorator: true) do
      collection :songs, :class => Song do
        property :name
      end
    end

    it "does not pollute represented" do
      representer.new(album).from_hash({"songs"=>[{"name"=>"Atomic Garden"}]})

      # no @representable_attrs in decorated objects
      song.instance_variable_get(:@representable_attrs).must_equal nil
      album.instance_variable_get(:@representable_attrs).must_equal nil
    end
  end
end
