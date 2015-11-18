module Roar::JSON
  # Implementation of the Collection+JSON media format, http://amundsen.com/media-types/collection/format/
  #
  # When clients want to add or update an item the collection's filled out template is POSTed or PUT. You can parse that using the
  # +SongCollectionRepresenter::template_representer+ module.
  #
  #   Song.new.extend(SongCollectionRepresenter.template_representer).from_json("{template: ...")
  module CollectionJSON
    def self.included(base)
      base.class_eval do
        include Roar::Representer
        include Roar::JSON
        include Roar::Hypermedia

        extend ClassMethods

        self.representation_wrap= :collection # FIXME: move outside of this block for inheritance!

        property :__template, :as => :template, :extend => lambda {|*| representable_attrs.collection_representers[:template] }, :class => Array
        def __template
          OpenStruct.new  # TODO: handle preset values.
        end

        collection :queries, :extend => Roar::JSON::HyperlinkRepresenter, :class => lambda { |fragment,*| ::Hash }
        def queries
          compile_links_for(representable_attrs.collection_representers[:queries].link_configs)
        end
        def queries=(v)
        end


        def items
          self
        end
        def items=(v)
          replace(v)
        end

        property :__version, :as => :version
        def __version
          representable_attrs.collection_representers[:version]
        end

        property :__href, :as => :href
        def __href
          compile_links_for(representable_attrs.collection_representers[:href].link_configs).first.href
        end



        include ClientMethods
      end
    end

    module ClassMethods
      module PropertyWithRenderNil
        def property(name, options={})
          super(name, options.merge!(:render_nil => true))
        end
      end

      # TODO: provide automatic copying from the ItemRepresenter here.
      def template(&block)
        mod = representable_attrs.collection_representers[:object_template] = Module.new do
          include Roar::JSON
          include Roar::JSON::CollectionJSON::DataMethods

          extend PropertyWithRenderNil

          module_exec(&block)

          #self.representation_wrap = :template
          def from_hash(hash, *args)  # overridden in :template representer.
            super(hash["template"])
          end
        end

        representable_attrs.collection_representers[:template] = Module.new do
          include Roar::JSON
          include mod

          #self.representation_wrap = false

          # DISCUSS: currently we skip real deserialization here and just store the :data hash.
          def from_hash(hash, *args)
            replace(hash["data"])
          end
        end
      end
      def template_representer
        representable_attrs.collection_representers[:object_template]
      end

      def queries(&block)
        mod = representable_attrs.collection_representers[:queries] = Module.new do
          include Roar::JSON
          include Roar::Hypermedia

          module_exec(&block)

          def to_hash(*)
            hash = super
            hash["links"] # TODO: make it easier to render collection of links.
          end
        end
      end

      def items(options={}, &block)
        collection :items, { :extend => lambda {|*| representable_attrs.collection_representers[:items] } }.merge!(options)

        mod = representable_attrs.collection_representers[:items] = Module.new do
          include Roar::JSON
          include Roar::Hypermedia
          include Roar::JSON::CollectionJSON::DataMethods
          extend SharedClassMethodsBullshit

          module_exec(&block)

          # TODO: share with main module!
          property :__href, :as => :href
          def __href
            compile_links_for(representable_attrs.collection_representers[:href].link_configs).first.href
          end
          def __href=(v)
            @__href = Roar::Hypermedia::Hyperlink.new(:href => v)
          end
          def href
            @__href
          end
        end
      end

      def version(v)
        representable_attrs.collection_representers[:version] = v
      end

      module SharedClassMethodsBullshit
        def href(&block)
          mod = representable_attrs.collection_representers[:href] = Module.new do
            include Roar::JSON
            include Roar::Hypermedia


            link(:href, &block)
          end
        end

        def representable_attrs
          super.tap do |attrs|
            attrs.instance_eval do # FIXME: of course, this is WIP.
              def collection_representers
                @collection_representers ||= {}
              end
            end
          end
        end
      end
      include SharedClassMethodsBullshit
    end

    module DataMethods
      def to_hash(*)
        hash = super.tap do |hsh|
          data = []
          hsh.keys.each do |n|
            next if ["href", "links"].include?(n)

            v = hsh.delete(n.to_s)
            data << {:name => n, :value => v} # TODO: get :prompt from Definition.
          end
          hsh[:data] = data
        end
      end

      def from_hash(hash, *args)
        data = {}
        hash.delete("data").collect do |item|
          data[item["name"]] = item["value"]
        end
        super(hash.merge!(data))
      end
    end

    module ClientMethods
      def __version=(v)
        @__version = v
      end
      def version
        @__version
      end

      def __href=(v)
        @__href = Roar::Hypermedia::Hyperlink.new(:href => v)
      end
      def href
        @__href
      end

      def __template=(v)
        @__template = v
      end
      # DISCUSS: this might return a Template instance, soon.
      def template
        @__template
      end
    end
  end
end