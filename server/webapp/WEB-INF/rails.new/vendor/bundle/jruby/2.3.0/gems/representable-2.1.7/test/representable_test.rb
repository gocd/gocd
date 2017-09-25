require 'test_helper'

class RepresentableTest < MiniTest::Spec
  class Band
    include Representable::Hash
    property :name
    attr_accessor :name
  end

  class PunkBand < Band
    property :street_cred
    attr_accessor :street_cred
  end

  module BandRepresentation
    include Representable

    property :name
  end

  module PunkBandRepresentation
    include Representable
    include BandRepresentation

    property :street_cred
  end


  describe "#representable_attrs" do
    describe "in module" do
      it "allows including the concrete representer module later" do
        vd = class VD
          attr_accessor :name, :street_cred
          include Representable::JSON
          include PunkBandRepresentation
        end.new
        vd.name        = "Vention Dention"
        vd.street_cred = 1
        assert_json "{\"name\":\"Vention Dention\",\"street_cred\":1}", vd.to_json
      end

      #it "allows including the concrete representer module only" do
      #  require 'representable/json'
      #  module RockBandRepresentation
      #    include Representable::JSON
      #    property :name
      #  end
      #  vd = class VH
      #    include RockBandRepresentation
      #  end.new
      #  vd.name        = "Van Halen"
      #  assert_equal "{\"name\":\"Van Halen\"}", vd.to_json
      #end
    end
  end


  describe "inheritance" do
    class CoverSong < OpenStruct
    end

    module SongRepresenter
      include Representable::Hash
      property :name
    end

    module CoverSongRepresenter
      include Representable::Hash
      include SongRepresenter
      property :by
    end

    it "merges properties from all ancestors" do
      props = {"name"=>"The Brews", "by"=>"Nofx"}
      assert_equal(props, CoverSong.new(props).extend(CoverSongRepresenter).to_hash)
    end

    it "allows mixing in multiple representers" do
      require 'representable/json'
      require 'representable/xml'
      class Bodyjar
        include Representable::XML
        include Representable::JSON
        include PunkBandRepresentation

        self.representation_wrap = "band"
        attr_accessor :name, :street_cred
      end

      band = Bodyjar.new
      band.name = "Bodyjar"

      assert_json "{\"band\":{\"name\":\"Bodyjar\"}}", band.to_json
      assert_xml_equal "<band><name>Bodyjar</name></band>", band.to_xml
    end

    it "allows extending with different representers subsequentially" do
      module SongXmlRepresenter
        include Representable::XML
        property :name, :as => "name", :attribute => true
      end

      module SongJsonRepresenter
        include Representable::JSON
        property :name
      end

      @song = Song.new("Days Go By")
      assert_xml_equal "<song name=\"Days Go By\"/>", @song.extend(SongXmlRepresenter).to_xml
      assert_json "{\"name\":\"Days Go By\"}", @song.extend(SongJsonRepresenter).to_json
    end
  end


  describe "#property" do
    it "doesn't modify options hash" do
      options = {}
      representer.property(:title, options)
      options.must_equal({})
    end

    representer! {}

    it "returns the Definition instance" do
      representer.property(:name).must_be_kind_of Representable::Definition
    end
  end

  describe "#collection" do
    class RockBand < Band
      collection :albums
    end

    it "creates correct Definition" do
      assert_equal "albums", RockBand.representable_attrs.get(:albums).name
      assert RockBand.representable_attrs.get(:albums).array?
    end
  end

  describe "#hash" do
    it "also responds to the original method" do
      assert_kind_of Integer, BandRepresentation.hash
    end
  end


  # DISCUSS: i don't like the JSON requirement here, what about some generic test module?
  class PopBand
    include Representable::JSON
    property :name
    property :groupies
    attr_accessor :name, :groupies
  end

  describe "#update_properties_from" do
    before do
      @band = PopBand.new
    end

    it "copies values from document to object" do
      @band.from_hash({"name"=>"No One's Choice", "groupies"=>2})
      assert_equal "No One's Choice", @band.name
      assert_equal 2, @band.groupies
    end


    it "accepts :exclude option" do
      @band.from_hash({"name"=>"No One's Choice", "groupies"=>2}, {:exclude => [:groupies]})
      assert_equal "No One's Choice", @band.name
      assert_equal nil, @band.groupies
    end

    it "accepts :include option" do
      @band.from_hash({"name"=>"No One's Choice", "groupies"=>2}, :include => [:groupies])
      assert_equal 2, @band.groupies
      assert_equal nil, @band.name
    end

    it "ignores non-writeable properties" do
      @band = Class.new(Band) { property :name; collection :founders, :writeable => false; attr_accessor :founders }.new
      @band.from_hash("name" => "Iron Maiden", "groupies" => 2, "founders" => ["Steve Harris"])
      assert_equal "Iron Maiden", @band.name
      assert_equal nil, @band.founders
    end

    it "always returns the represented" do
      assert_equal @band, @band.from_hash({"name"=>"Nofx"})
    end

    it "includes false attributes" do
      @band.from_hash({"groupies"=>false})
      assert_equal false, @band.groupies
    end

    it "ignores properties not present in the incoming document" do
      @band.instance_eval do
        def name=(*); raise "I should never be called!"; end
      end
      @band.from_hash({})
    end

    # FIXME: do we need this test with XML _and_ JSON?
    it "ignores (no-default) properties not present in the incoming document" do
      { Representable::Hash => [:from_hash, {}],
        Representable::XML  => [:from_xml,  xml(%{<band/>}).to_s]
      }.each do |format, config|
        nested_repr = Module.new do # this module is never applied. # FIXME: can we make that a simpler test?
          include format
          property :created_at
        end

        repr = Module.new do
          include format
          property :name, :class => Object, :extend => nested_repr
        end

        @band = Band.new.extend(repr)
        @band.send(config.first, config.last)
        assert_equal nil, @band.name, "Failed in #{format}"
      end
    end

    describe "passing options" do
      class Track
        attr_accessor :nr
      end

      module TrackRepresenter
        include Representable::Hash
        property :nr

        def to_hash(options)
          @nr = options[:nr]
          super
        end
        def from_hash(data, options)
          super.tap do
            @nr = options[:nr]
          end
        end
      end

      representer! do
        property :track, :extend => TrackRepresenter, :class => Track
      end

      describe "#to_hash" do
        it "propagates to nested objects" do
          Song.new("Ocean Song", Track.new).extend(representer).to_hash(:nr => 9).must_equal({"track"=>{"nr"=>9}})
        end
      end

      describe "#from_hash" do
        it "propagates to nested objects" do
          Song.new.extend(representer).from_hash({"track"=>{"nr" => "replace me"}}, :nr => 9).track.nr.must_equal 9
        end
      end
    end
  end

  describe "#create_representation_with" do
    before do
      @band = PopBand.new
      @band.name = "No One's Choice"
      @band.groupies = 2
    end

    it "compiles document from properties in object" do
      assert_equal({"name"=>"No One's Choice", "groupies"=>2}, @band.to_hash)
    end

    it "accepts :exclude option" do
      hash = @band.to_hash({:exclude => [:groupies]})
      assert_equal({"name"=>"No One's Choice"}, hash)
    end

    it "accepts :include option" do
      hash = @band.to_hash({:include => [:groupies]})
      assert_equal({"groupies"=>2}, hash)
    end

    it "ignores non-readable properties" do
      @band = Class.new(Band) { property :name; collection :founder_ids, :readable => false; attr_accessor :founder_ids }.new
      @band.name = "Iron Maiden"
      @band.founder_ids = [1,2,3]

      hash = @band.to_hash
      assert_equal({"name" => "Iron Maiden"}, hash)
    end

    it "does not write nil attributes" do
      @band.groupies = nil
      assert_equal({"name"=>"No One's Choice"}, @band.to_hash)
    end

    it "writes false attributes" do
      @band.groupies = false
      assert_equal({"name"=>"No One's Choice","groupies"=>false}, @band.to_hash)
    end

    describe "when :render_nil is true" do
      it "includes nil attribute" do
        mod = Module.new do
          include Representable::JSON
          property :name
          property :groupies, :render_nil => true
        end

        @band.extend(mod) # FIXME: use clean object.
        @band.groupies = nil
        hash = @band.to_hash
        assert_equal({"name"=>"No One's Choice", "groupies" => nil}, hash)
      end

      it "includes nil attribute without extending" do
        mod = Module.new do
          include Representable::JSON
          property :name
          property :groupies, :render_nil => true, :extend => BandRepresentation
        end

        @band.extend(mod) # FIXME: use clean object.
        @band.groupies = nil
        hash = @band.to_hash
        assert_equal({"name"=>"No One's Choice", "groupies" => nil}, hash)
      end
    end

    it "does not propagate private options to nested objects" do
      cover_rpr = Module.new do
        include Representable::Hash
        property :title
        property :original, :extend => self
      end

      # FIXME: we should test all representable-options (:include, :exclude, ?)

      Class.new(OpenStruct).new(:title => "Roxanne", :original => Class.new(OpenStruct).new(:title => "Roxanne (Don't Put On The Red Light)")).extend(cover_rpr).
        to_hash(:include => [:original]).must_equal({"original"=>{"title"=>"Roxanne (Don't Put On The Red Light)"}})
    end
  end


  describe ":extend and :class" do
    module UpcaseRepresenter
      include Representable
      def to_hash(*); upcase; end
      def from_hash(hsh, *args); replace hsh.upcase; end   # DISCUSS: from_hash must return self.
    end
    module DowncaseRepresenter
      include Representable
      def to_hash(*); downcase; end
      def from_hash(hsh, *args); replace hsh.downcase; end
    end
    class UpcaseString < String; end


    describe "lambda blocks" do
      representer! do
        property :name, :extend => lambda { |name, *| compute_representer(name) }
      end

      it "executes lambda in represented instance context" do
        Song.new("Carnage").instance_eval do
          def compute_representer(name)
            UpcaseRepresenter
          end
          self
        end.extend(representer).to_hash.must_equal({"name" => "CARNAGE"})
      end
    end

    describe ":instance" do
      obj = String.new("Fate")
      mod = Module.new { include Representable; def from_hash(*); self; end }
      representer! do
        property :name, :extend => mod, :instance => lambda { |*| obj }
      end

      it "uses object from :instance but still extends it" do
        song = Song.new.extend(representer).from_hash("name" => "Eric's Had A Bad Day")
        song.name.must_equal obj
        song.name.must_be_kind_of mod
      end
    end

    describe "property with :extend" do
      representer! do
        property :name, :extend => lambda { |name, *| name.is_a?(UpcaseString) ? UpcaseRepresenter : DowncaseRepresenter }, :class => String
      end

      it "uses lambda when rendering" do
        assert_equal({"name" => "you make me thick"}, Song.new("You Make Me Thick").extend(representer).to_hash )
        assert_equal({"name" => "STEPSTRANGER"}, Song.new(UpcaseString.new "Stepstranger").extend(representer).to_hash )
      end

      it "uses lambda when parsing" do
        Song.new.extend(representer).from_hash({"name" => "You Make Me Thick"}).name.must_equal "you make me thick"
        Song.new.extend(representer).from_hash({"name" => "Stepstranger"}).name.must_equal "stepstranger" # DISCUSS: we compare "".is_a?(UpcaseString)
      end

      describe "with :class lambda" do
        representer! do
          property :name, :extend => lambda { |name, *| name.is_a?(UpcaseString) ? UpcaseRepresenter : DowncaseRepresenter },
                          :class  => lambda { |fragment, *| fragment == "Still Failing?" ? String : UpcaseString }
        end

        it "creates instance from :class lambda when parsing" do
          song = OpenStruct.new.extend(representer).from_hash({"name" => "Quitters Never Win"})
          song.name.must_be_kind_of UpcaseString
          song.name.must_equal "QUITTERS NEVER WIN"

          song = OpenStruct.new.extend(representer).from_hash({"name" => "Still Failing?"})
          song.name.must_be_kind_of String
          song.name.must_equal "still failing?"
        end
      end
    end


    describe "collection with :extend" do
      representer! do
        collection :songs, :extend => lambda { |name, *| name.is_a?(UpcaseString) ? UpcaseRepresenter : DowncaseRepresenter }, :class => String
      end

      it "uses lambda for each item when rendering" do
        Album.new([UpcaseString.new("Dean Martin"), "Charlie Still Smirks"]).extend(representer).to_hash.must_equal("songs"=>["DEAN MARTIN", "charlie still smirks"])
      end

      it "uses lambda for each item when parsing" do
        album = Album.new.extend(representer).from_hash("songs"=>["DEAN MARTIN", "charlie still smirks"])
        album.songs.must_equal ["dean martin", "charlie still smirks"] # DISCUSS: we compare "".is_a?(UpcaseString)
      end

      describe "with :class lambda" do
        representer! do
          collection :songs,  :extend => lambda { |name, *| name.is_a?(UpcaseString) ? UpcaseRepresenter : DowncaseRepresenter },
                              :class  => lambda { |fragment, *| fragment == "Still Failing?" ? String : UpcaseString }
        end

        it "creates instance from :class lambda for each item when parsing" do
          album = Album.new.extend(representer).from_hash("songs"=>["Still Failing?", "charlie still smirks"])
          album.songs.must_equal ["still failing?", "CHARLIE STILL SMIRKS"]
        end
      end
    end

    describe ":decorator" do
      let (:extend_rpr) { Module.new { include Representable::Hash; collection :songs, :extend => SongRepresenter } }
      let (:decorator_rpr) { Module.new { include Representable::Hash; collection :songs, :decorator => SongRepresenter } }
      let (:songs) { [Song.new("Bloody Mary")] }

      it "is aliased to :extend" do
        Album.new(songs).extend(extend_rpr).to_hash.must_equal Album.new(songs).extend(decorator_rpr).to_hash
      end
    end

    describe ":binding" do
      representer! do
        class MyBinding < Representable::Binding
          def write(doc, *args)
            doc[:title] = @represented.title
          end
        end
        property :title, :binding => lambda { |*args| MyBinding.new(*args) }
      end

      it "uses the specified binding instance" do
        OpenStruct.new(:title => "Affliction").extend(representer).to_hash.must_equal({:title => "Affliction"})
      end
    end


    # TODO: Move to global place since it's used twice.
    class SongRepresentation < Representable::Decorator
      include Representable::JSON
      property :name
    end
    class AlbumRepresentation < Representable::Decorator
      include Representable::JSON

      collection :songs, :class => Song, :extend => SongRepresentation
    end

    describe "::prepare" do
      let (:song) { Song.new("Still Friends In The End") }
      let (:album) { Album.new([song]) }

      describe "module including Representable" do
        it "uses :extend strategy" do
          album_rpr = Module.new { include Representable::Hash; collection :songs, :class => Song, :extend => SongRepresenter}

          album_rpr.prepare(album).to_hash.must_equal({"songs"=>[{"name"=>"Still Friends In The End"}]})
          album.must_respond_to :to_hash
        end
      end

      describe "Decorator subclass" do
        it "uses :decorate strategy" do
          AlbumRepresentation.prepare(album).to_hash.must_equal({"songs"=>[{"name"=>"Still Friends In The End"}]})
          album.wont_respond_to :to_hash
        end
      end
    end
  end


  describe "#use_decorator" do
    representer! do
      property :title, :use_decorator => true do
        property :lower
      end
    end

    it "uses a Decorator for inline representer" do
      outer = Struct.new(:title, :lower, :band, :bla).new(inner = Struct.new(:lower).new("paper wings"))

      outer.extend(representer).to_hash.must_equal({"title"=>{"lower"=>"paper wings"}})
      outer.must_be_kind_of Representable::Hash
      inner.wont_be_kind_of Representable::Hash
    end
  end


  # using your own Definition class
  class StaticDefinition < Representable::Definition
    def initialize(name, options)
      options[:as] = :Name
      super
    end
  end


  module StaticRepresenter
    def self.build_config
      Representable::Config.new(StaticDefinition)
    end
    include Representable::Hash

    property :title
  end

  # we currently need an intermediate decorator to inherit ::build_config properly.
  class BaseDecorator < Representable::Decorator
    include Representable::Hash
    def self.build_config
      Representable::Config.new(StaticDefinition)
    end
  end

  class StaticDecorator < BaseDecorator
    property :title
  end

  it { OpenStruct.new(:title => "Tarutiri").extend(StaticRepresenter).to_hash.must_equal({"Name"=>"Tarutiri"}) }
  it { StaticDecorator.new(OpenStruct.new(:title => "Tarutiri")).to_hash.must_equal({"Name"=>"Tarutiri"}) }
end
