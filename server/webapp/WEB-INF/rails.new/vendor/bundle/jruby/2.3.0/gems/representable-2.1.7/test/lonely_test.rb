require 'test_helper'

require 'representable/json/hash'

class LonelyRepresenterTest < MiniTest::Spec

  # test ::items without arguments, render-only.
  for_formats(
    :hash => [Representable::Hash::Collection, [{"name"=>"Resist Stance"}, {"name"=>"Suffer"}]],
    :json => [Representable::JSON::Collection, "[{\"name\":\"Resist Stance\"},{\"name\":\"Suffer\"}]"],
    :xml  => [Representable::XML::Collection, "<array><song><name>Resist Stance</name></song><song><name>Suffer</name></song></array>"],
  ) do |format, mod, output, input|

    describe "[#{format}] lonely collection, render-only" do # TODO: introduce :representable option?
      let (:format) { format }

      representer!(module: mod) do
        items do
          property :name
        end
      end

      let (:album) { [Song.new("Resist Stance"), Song.new("Suffer")].extend(representer) }

      it "calls #to_hash on song instances, nothing else" do
        render(album).must_equal_document(output)
      end
    end
  end



  module SongRepresenter
    include Representable::JSON

    property :name
  end

  let (:decorator) { rpr = representer; Class.new(Representable::Decorator) { include rpr } }

  describe "JSON::Collection" do
    let (:songs) { [Song.new("Days Go By"), Song.new("Can't Take Them All")] }
    let (:json)  { "[{\"name\":\"Days Go By\"},{\"name\":\"Can't Take Them All\"}]" }

    describe "with contained objects" do
      let (:representer) {
        Module.new do
          include Representable::JSON::Collection
          items :class => Song, :extend => SongRepresenter
        end
      }


      it "renders array" do
        assert_json json, songs.extend(representer).to_json
      end

      it "renders array with decorator xxx" do
        assert_json json, decorator.new(songs).to_json
      end

      it "parses array" do
        [].extend(representer).from_json(json).must_equal songs
      end

      it "parses array with decorator" do
        decorator.new([]).from_json(json).must_equal songs
      end
    end

    describe "with inline representer" do
      representer!(:module => Representable::JSON::Collection) do
        items :class => Song do
          property :name
        end
      end

      it { songs.extend(representer).to_json.must_equal json }
      it { [].extend(representer).from_json(json).must_equal songs }
    end

    describe "with contained text" do
      let (:representer) {
        Module.new do
          include Representable::JSON::Collection
        end
      }
      let (:songs) { ["Days Go By", "Can't Take Them All"] }
      let (:json)  { "[\"Days Go By\",\"Can't Take Them All\"]" }

      it "renders contained items #to_json" do
        assert_json json, songs.extend(representer).to_json
      end

      it "returns objects array from #from_json" do
        [].extend(representer).from_json(json).must_equal songs
      end
    end
  end


  describe "JSON::Hash" do  # TODO: move to HashTest.
    describe "with contained objects" do
      let (:representer) {
        Module.new do
          include Representable::JSON::Hash
          values :class => Song, :extend => SongRepresenter
        end
      }
      let (:json)  { "{\"one\":{\"name\":\"Days Go By\"},\"two\":{\"name\":\"Can't Take Them All\"}}" }
      let (:songs) { {"one" => Song.new("Days Go By"), "two" => Song.new("Can't Take Them All")} }

      describe "#to_json" do
        it "renders hash" do
          songs.extend(representer).to_json.must_equal json
        end

        it "renders hash with decorator" do
          decorator.new(songs).to_json.must_equal json
        end

        it "respects :exclude" do
          assert_json "{\"two\":{\"name\":\"Can't Take Them All\"}}", {:one => Song.new("Days Go By"), :two => Song.new("Can't Take Them All")}.extend(representer).to_json(:exclude => [:one])
        end

        it "respects :include" do
          assert_json "{\"two\":{\"name\":\"Can't Take Them All\"}}", {:one => Song.new("Days Go By"), :two => Song.new("Can't Take Them All")}.extend(representer).to_json(:include => [:two])
        end
      end

      describe "#from_json" do
        it "returns objects array" do
          {}.extend(representer).from_json(json).must_equal songs
        end

        it "parses hash with decorator" do
          decorator.new({}).from_json(json).must_equal songs
        end

        it "respects :exclude" do
          assert_equal({"two" => Song.new("Can't Take Them All")}, {}.extend(representer).from_json(json, :exclude => [:one]))
        end

        it "respects :include" do
          assert_equal({"one" => Song.new("Days Go By")}, {}.extend(representer).from_json(json, :include => [:one]))
        end
      end


      describe "with inline representer" do
        representer!(:module => Representable::JSON::Hash) do
          values :class => Song do
            property :name
          end
        end

        it { songs.extend(representer).to_json.must_equal json }
        it { {}.extend(representer).from_json(json).must_equal songs }
      end
    end


    describe "with contained text" do
      before do
        @songs_representer = Module.new do
          include Representable::JSON::Collection
        end
      end

      it "renders contained items #to_json" do
        assert_json "[\"Days Go By\",\"Can't Take Them All\"]", ["Days Go By", "Can't Take Them All"].extend(@songs_representer).to_json
      end

      it "returns objects array from #from_json" do
        assert_equal ["Days Go By", "Can't Take Them All"], [].extend(@songs_representer).from_json("[\"Days Go By\",\"Can't Take Them All\"]")
      end
    end
  end
end
