require 'test_helper'

class HypermediaTest < MiniTest::Spec
  describe "inheritance" do
    before do
      module BaseRepresenter
        include Roar::JSON
        include Roar::Hypermedia

        link(:base) { "http://base" }
      end

      module Bar
        include Roar::JSON
        include Roar::Hypermedia

        link(:bar) { "http://bar" }
      end

      module Foo
        include Roar::JSON
        include Roar::Hypermedia
        include BaseRepresenter
        include Bar

        link(:foo) { "http://foo" }
      end
    end

    it "inherits parent links" do
      foo = Object.new.extend(Foo)

      assert_equal "{\"links\":[{\"rel\":\"base\",\"href\":\"http://base\"},{\"rel\":\"bar\",\"href\":\"http://bar\"},{\"rel\":\"foo\",\"href\":\"http://foo\"}]}", foo.to_json
    end

    it "inherits links from all mixed-in representers" do
      skip
      Object.new.extend(BaseRepresenter).extend(Bar).to_json.must_equal "{\"links\":[{\"rel\":\"base\",\"href\":\"http://base\"},{\"rel\":\"bar\",\"href\":\"http://bar\"}]}"
    end
  end

  describe "#links_array" do
    subject { Object.new.extend(rpr) }

    representer_for do
      link(:self) { "//self" }
    end


    describe "#to_json" do
      it "renders" do
        subject.to_json.must_equal "{\"links\":[{\"rel\":\"self\",\"href\":\"//self\"}]}"
      end
    end

    describe "#from_json" do
      it "parses" do
        subject.from_json "{\"links\":[{\"rel\":\"self\",\"href\":\"//self\"}]}"
        subject.links.must_equal({"self" => link("rel" => "self", "href" => "//self")})
      end
    end


    describe "#link" do

      describe "with any options" do
        representer_for do
          link(:rel => :self, :title => "Hey, @myabc") { "//self" }
        end

        it "renders options" do
          subject.to_json.must_equal "{\"links\":[{\"rel\":\"self\",\"title\":\"Hey, @myabc\",\"href\":\"//self\"}]}"
        end
      end

      describe "with string rel" do
        representer_for do
          link("ns:self") { "//self" }
        end

        it "renders rel" do
          subject.to_json.must_equal "{\"links\":[{\"rel\":\"ns:self\",\"href\":\"//self\"}]}"
        end
      end

      describe "passing options to serialize" do
        representer_for do
          link(:self) { |opts| "//self/#{opts[:id]}" }
        end

        it "receives options when rendering" do
          subject.to_json(:id => 1).must_equal "{\"links\":[{\"rel\":\"self\",\"href\":\"//self/1\"}]}"
        end

        describe "in a composition" do
          representer_for do
            property :entity, :extend => self
            link(:self) { |opts| "//self/#{opts[:id]}" }
          end

          it "propagates options" do
            Song.new(:entity => Song.new).extend(rpr).to_json(:id => 1).must_equal "{\"entity\":{\"links\":[{\"rel\":\"self\",\"href\":\"//self/1\"}]},\"links\":[{\"rel\":\"self\",\"href\":\"//self/1\"}]}"
          end
        end
      end

      describe "returning option hash from block" do
        representer_for do
          link(:self) do {:href => "//self", :type => "image/jpg"} end
          link(:other) do |params|
            hash = { :href => "//other" }
            hash.merge!(:type => 'image/jpg') if params[:type]
            hash
          end
        end

        it "is rendered as link attributes" do
          subject.to_json.must_equal "{\"links\":[{\"rel\":\"self\",\"href\":\"//self\",\"type\":\"image/jpg\"},{\"rel\":\"other\",\"href\":\"//other\"}]}"
        end

        it "is rendered according to context" do
          subject.to_json(type: true).must_equal "{\"links\":[{\"rel\":\"self\",\"href\":\"//self\",\"type\":\"image/jpg\"},{\"rel\":\"other\",\"href\":\"//other\",\"type\":\"image/jpg\"}]}"
          subject.to_json.must_equal "{\"links\":[{\"rel\":\"self\",\"href\":\"//self\",\"type\":\"image/jpg\"},{\"rel\":\"other\",\"href\":\"//other\"}]}"
        end
      end

      describe "not calling #link" do
        representer_for {}

        it "still allows rendering" do
          subject.to_json.must_equal "{}"
        end
      end
    end
  end
end

class HyperlinkTest < MiniTest::Spec
  describe "Hyperlink" do
    subject { link(:rel => "self", "href" => "http://self", "data-whatever" => "Hey, @myabc") }

    it "accepts string keys in constructor" do
      assert_equal "Hey, @myabc", subject.send("data-whatever")
    end

    it "responds to #rel" do
      assert_equal "self", subject.rel
    end

    it "responds to #href" do
      assert_equal "http://self", subject.href
    end

    it "responds to #replace with string keys" do
      subject.replace("rel" => "next")
      assert_equal nil, subject.href
      assert_equal "next", subject.rel
    end

    it "responds to #each and implements Enumerable" do
      assert_equal ["rel:self", "href:http://self", "data-whatever:Hey, @myabc"], subject.collect { |k,v| "#{k}:#{v}" }
    end
  end

  describe "Config inheritance" do
      it "doesn't mess up with inheritable_array" do  # FIXME: remove this test when uber is out.
        OpenStruct.new.extend( Module.new do
                  include Roar::JSON
                  include(Module.new do
                                      include Roar::JSON
                                      include Roar::Hypermedia

                                      property :bla

                                      link( :self) {"bo"}

                                      #puts "hey ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
                                      #puts representable_attrs.inheritable_array(:links).inspect
                                    end)


                  #puts representable_attrs.inheritable_array(:links).inspect

                  property :blow
                  include Roar::Hypermedia
                  link(:bla) { "boo" }
                end).to_hash.must_equal({"links"=>[{"rel"=>"self", "href"=>"bo"}, {"rel"=>"bla", "href"=>"boo"}]})
    end
  end
end
