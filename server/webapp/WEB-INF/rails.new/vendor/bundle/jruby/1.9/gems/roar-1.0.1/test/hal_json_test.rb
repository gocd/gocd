require 'test_helper'
require 'roar/json/hal'

class HalJsonTest < MiniTest::Spec
  let(:rpr) do
    Module.new do
      include Roar::JSON
      include Roar::JSON::HAL

      links :self do
        [{:lang => "en", :href => "http://en.hit"},
         {:lang => "de", :href => "http://de.hit"}]
      end

      link :next do
        "http://next"
      end
    end
  end

  subject { Object.new.extend(rpr) }

  describe "links" do
    describe "parsing" do
      it "parses link array" do # TODO: remove me.
        obj = subject.from_json("{\"_links\":{\"self\":[{\"lang\":\"en\",\"href\":\"http://en.hit\"},{\"lang\":\"de\",\"href\":\"http://de.hit\"}]}}")
        obj.links.must_equal "self" => [link("rel" => "self", "href" => "http://en.hit", "lang" => "en"), link("rel" => "self", "href" => "http://de.hit", "lang" => "de")]
      end

      it "parses single links" do # TODO: remove me.
        obj = subject.from_json("{\"_links\":{\"next\":{\"href\":\"http://next\"}}}")
        obj.links.must_equal "next" => link("rel" => "next", "href" => "http://next")
      end

      it "parses link and link array" do
        obj = subject.from_json("{\"_links\":{\"next\":{\"href\":\"http://next\"}, \"self\":[{\"lang\":\"en\",\"href\":\"http://en.hit\"},{\"lang\":\"de\",\"href\":\"http://de.hit\"}]}}")
        obj.links.must_equal "next" => link("rel" => "next", "href" => "http://next"), "self" => [link("rel" => "self", "href" => "http://en.hit", "lang" => "en"), link("rel" => "self", "href" => "http://de.hit", "lang" => "de")]
      end

      it "parses empty link array" do
        subject.from_json("{\"_links\":{\"self\":[]}}").links[:self].must_equal []
      end

      it "parses non-existent link array" do
        subject.from_json("{\"_links\":{}}").links[:self].must_equal nil # DISCUSS: should this be []?
      end

      it "rejects single links declared as array" do
        assert_raises TypeError do
          subject.from_json("{\"_links\":{\"self\":{\"href\":\"http://next\"}}}")
        end
      end
    end

    describe "rendering" do
      it "renders link and link array" do
        subject.to_json.must_equal "{\"_links\":{\"self\":[{\"lang\":\"en\",\"href\":\"http://en.hit\"},{\"lang\":\"de\",\"href\":\"http://de.hit\"}],\"next\":{\"href\":\"http://next\"}}}"
      end

      it "renders empty link array" do
        rpr = Module.new do
          include Roar::JSON::HAL

          links :self do [] end
        end
        subject = Object.new.extend(rpr)

        subject.to_json.must_equal "{\"_links\":{\"self\":[]}}"
      end
    end
  end

  # describe "#prepare_links!" do
  #   it "should map link arrays correctly" do
  #     subject.send :prepare_links!, {}
  #     subject.links.must_equal :self => [link("rel" => "self", "href" => "http://en.hit", "lang" => "en"),link("rel" => :self, "href" => "http://de.hit", "lang" => "de")], "next" => link("href" => "http://next", "rel" => "next")
  #   end
  # end

  describe "#link_array_rels" do
    it "returns list of rels for array links" do
      subject.send(:link_array_rels).must_equal [:self]
    end
  end


  describe "HAL/JSON" do
    Bla = Module.new do
      include Roar::JSON::HAL
      property :title
      link :self do
        "http://songs/#{title}"
      end
    end

    representer_for([Roar::JSON::HAL]) do
      property :id
      collection :songs, :class => Song, :extend => Bla, :embedded => true
      link :self do
        "http://albums/#{id}"
      end
    end

    before do
      @album = Album.new(:songs => [Song.new(:title => "Beer")], :id => 1).extend(rpr)
    end

    it "render links and embedded resources according to HAL" do
      assert_equal "{\"id\":1,\"_embedded\":{\"songs\":[{\"title\":\"Beer\",\"_links\":{\"self\":{\"href\":\"http://songs/Beer\"}}}]},\"_links\":{\"self\":{\"href\":\"http://albums/1\"}}}", @album.to_json
    end

    it "parses links and resources following the mighty HAL" do
      @album.from_json("{\"id\":2,\"_embedded\":{\"songs\":[{\"title\":\"Coffee\",\"_links\":{\"self\":{\"href\":\"http://songs/Coffee\"}}}]},\"_links\":{\"self\":{\"href\":\"http://albums/2\"}}}")
      assert_equal 2, @album.id
      assert_equal "Coffee", @album.songs.first.title
      assert_equal "http://songs/Coffee", @album.songs.first.links[:self].href
      assert_equal "http://albums/2", @album.links[:self].href
    end

    it "doesn't require _links and _embedded to be present" do
      @album.from_json("{\"id\":2}")
      assert_equal 2, @album.id

      # in newer representables, this is not overwritten to an empty [] anymore.
      assert_equal ["Beer"], @album.songs.map(&:title)
      @album.links.must_equal nil
    end
  end
end

class LinkCollectionTest < MiniTest::Spec
  subject { Roar::JSON::HAL::LinkCollection.new([:self, "next"]) }
  describe "#is_array?" do
    it "returns true for array link" do
      subject.is_array?(:self).must_equal true
      subject.is_array?("self").must_equal true
    end

    it "returns false otherwise" do
      subject.is_array?("prev").must_equal false
    end
  end
end


class HalCurieTest < MiniTest::Spec
  representer!([Roar::JSON::HAL]) do
    link "doc:self" do
      "/"
    end

    curies do
      [{:name => :doc,
        :href => "//docs/{rel}",
        :templated => true}]
    end
  end

  it { Object.new.extend(rpr).to_hash.must_equal({"_links"=>{"doc:self"=>{"href"=>"/"}, :curies=>[{"name"=>:doc, "href"=>"//docs/{rel}", "templated"=>true}]}}) }
end