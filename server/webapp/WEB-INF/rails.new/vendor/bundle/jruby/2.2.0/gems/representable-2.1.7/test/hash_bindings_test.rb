require 'test_helper'

class HashBindingTest < MiniTest::Spec
  module SongRepresenter
    include Representable::JSON
    property :name
  end

  class SongWithRepresenter < ::Song
    include Representable
    include SongRepresenter
  end


  describe "PropertyBinding" do
    describe "#read" do
      before do
        @property = Representable::Hash::Binding.new(Representable::Definition.new(:song), nil, nil)
      end

      it "returns fragment if present" do
        assert_equal "Stick The Flag Up Your Goddamn Ass, You Sonofabitch", @property.read({"song" => "Stick The Flag Up Your Goddamn Ass, You Sonofabitch"})
        assert_equal "", @property.read({"song" => ""})
        assert_equal nil, @property.read({"song" => nil})
      end

      it "returns FRAGMENT_NOT_FOUND if not in document" do
        assert_equal Representable::Binding::FragmentNotFound, @property.read({})
      end

    end
  end


  describe "CollectionBinding" do
    describe "with plain text items" do
      before do
        @property = Representable::Hash::Binding::Collection.new(Representable::Definition.new(:songs, :collection => true), Album.new, nil)
      end

      it "extracts with #read" do
        assert_equal ["The Gargoyle", "Bronx"], @property.read("songs" => ["The Gargoyle", "Bronx"])
      end

      it "inserts with #write" do
        doc = {}
        assert_equal(["The Gargoyle", "Bronx"], @property.write(doc, ["The Gargoyle", "Bronx"]))
        assert_equal({"songs"=>["The Gargoyle", "Bronx"]}, doc)
      end
    end
  end




  describe "HashBinding" do
    describe "with plain text items" do
      before do
        @property = Representable::Hash::Binding::Hash.new(Representable::Definition.new(:songs, :hash => true), nil, nil)
      end

      it "extracts with #read" do
        assert_equal({"first" => "The Gargoyle", "second" => "Bronx"} , @property.read("songs" => {"first" => "The Gargoyle", "second" => "Bronx"}))
      end

      it "inserts with #write" do
        doc = {}
        assert_equal({"first" => "The Gargoyle", "second" => "Bronx"}, @property.write(doc, {"first" => "The Gargoyle", "second" => "Bronx"}))
        assert_equal({"songs"=>{"first" => "The Gargoyle", "second" => "Bronx"}}, doc)
      end
    end

    describe "with objects" do
      before do
        @property = Representable::Hash::Binding::Hash.new(Representable::Definition.new(:songs, :hash => true, :class => Song, :extend => SongRepresenter), nil, nil)
      end

      it "doesn't change the represented hash in #write" do
        song = Song.new("Better Than That")
        hash = {"first" => song}
        @property.write({}, hash)
        assert_equal({"first" => song}, hash)
      end
    end

  end
end
