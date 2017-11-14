require 'test_helper'
require 'roar/json/collection_json'

class CollectionJsonTest < MiniTest::Spec
  let(:song) { OpenStruct.new(:title => "scarifice", :length => 43) }

  representer_for([Roar::JSON::CollectionJSON]) do
    version "1.0"
    href { "//songs/" }

    link(:feed) { "//songs/feed" }

    items(:class => Song) do
      href { "//songs/scarifice" }

      property :title, :prompt => "Song title"
      property :length, :prompt => "Song length"

      link(:download) { "//songs/scarifice.mp3" }
      link(:stats) { "//songs/scarifice/stats" }
    end

    template do
      property :title, :prompt => "Song title"
      property :length, :prompt => "Song length"
    end

    queries do
      link :search do
        {:href => "//search", :data => [{:name => "q", :value => ""}]}
      end
    end
  end

  describe "#to_json" do
    it "renders document" do
      [song].extend(rpr).to_hash.must_equal(
      {
        "collection"=>{
          "version"=>"1.0",
          "href"=>"//songs/",

          "template"=>{
            :data=>[
              {:name=>"title", :value=>nil},
              {:name=>"length", :value=>nil}
            ]
          },

          "queries"=>[
            {"rel"=>"search", "href"=>"//search",
              "data"=>[
                {:name=>"q", :value=>""}
              ]
            }
          ],

          "items"=>[
            {
              "links"=>[
                {"rel"=>"download", "href"=>"//songs/scarifice.mp3"},
                {"rel"=>"stats",    "href"=>"//songs/scarifice/stats"}
              ],
              "href"=>"//songs/scarifice",
              :data=>[
                {:name=>"title", :value=>"scarifice"},
                {:name=>"length", :value=>43}
              ]
            }
          ],

          "links"=>[
            {"rel"=>"feed", "href"=>"//songs/feed"}
          ]
        }
      })# %{{"collection":{"version":"1.0","href":"//songs/","items":[{"href":"//songs/scarifice","links":[{"rel":"download","href":"//songs/scarifice.mp3"},{"rel":"stats","href":"//songs/scarifice/stats"}],"data":[{"name":"title","value":"scarifice"},{"name":"length","value":43}]}],"template":{"data":[{"name":"title","value":null},{"name":"length","value":null}]},"queries":[{"rel":"search","href":"//search","data":[{"name":"q","value":""}]}],"links":[{"rel":"feed","href":"//songs/feed"}]}}}
    end
  end

  describe "#from_json" do
    subject { [].extend(rpr).from_json [song].extend(rpr).to_json }

    it "provides #version" do
      subject.version.must_equal "1.0"
    end

    it "provides #href" do
      subject.href.must_equal link(:href => "//songs/")
    end

    it "provides #template" do
      # DISCUSS: this might return a Template instance, soon.
      subject.template.must_equal([
        {"name"=>"title", "value"=>nil},
        {"name"=>"length", "value"=>nil}])
    end

    it "provides #queries" do
      # DISCUSS: this might return CollectionJSON::Hyperlink instances that support some kind of substitution operation for the :data attribute.
      # FIXME: this is currently _not_ parsed!
      subject.queries.must_equal([link(:rel => :search, :href=>"//search", :data=>[{:name=>"q", :value=>""}])])
    end

    it "provides #items" do
      subject.items.must_equal([Song.new(:title => "scarifice", :length => "43")])
      song = subject.items.first
      song.title.must_equal "scarifice"
      song.length.must_equal 43
      song.links.must_equal("download" => link({:rel=>"download", :href=>"//songs/scarifice.mp3"}), "stats" => link({:rel=>"stats", :href=>"//songs/scarifice/stats"}))
      song.href.must_equal link(:href => "//songs/scarifice")
    end

    it "provides #links" do
      subject.links.must_equal({"feed" => link(:rel => "feed", :href => "//songs/feed")})
    end
  end

  describe "template_representer#from_json" do
    it "parses object" do
      song = OpenStruct.new.extend(rpr.template_representer).from_hash(
        "template"=>{
          "data"=>[
            {"name"=>"title",  "value"=>"Black Star"},
            {"name"=>"length", "value"=>"4.53"}
          ]
        })
      song.title.must_equal "Black Star"
    end
  end
end