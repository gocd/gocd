require 'roar/json'
require 'roar/decorator'

module Roar
  module JSON
    module JSONAPI
      def self.included(base)
        base.class_eval do
          include Representable::JSON
          include Roar::JSON::JSONAPI::Singular
          include Roar::JSON::JSONAPI::Resource
          include Roar::JSON::JSONAPI::Document

          extend ForCollection

          representable_attrs[:resource_representer] = Class.new(Resource::Representer)
        end
      end

      module ForCollection
        def for_collection # same API as representable. TODO: we could use ::collection_representer! here.
          singular = self # e.g. Song::Representer

          # this basically does Module.new { include Hash::Collection .. }
          build_inline(nil, [Representable::Hash::Collection, Document::Collection, Roar::JSON], "", {}) do
            items extend: singular, :parse_strategy => :sync

            representable_attrs[:resource_representer] = singular.representable_attrs[:resource_representer]
            representable_attrs[:meta_representer]     = singular.representable_attrs[:meta_representer] # DISCUSS: do we need that?
            representable_attrs[:_wrap] = singular.representable_attrs[:_wrap]
          end
        end
      end


      module Singular
        def to_hash(options={})
          # per resource:
          super(options.merge(:exclude => [:links])).tap do |hash|
            hash["links"] = hash.delete("_links") if hash["_links"]
          end
        end

        def from_hash(hash, options={})
          hash["_links"] = hash["links"]
          super
        end
      end


      module Resource
        # ::link is delegated to Representer which handles the hypermedia (rendering
        # and parsing links).
        class Representer < Roar::Decorator
          include Roar::JSON
          include Roar::Hypermedia

          def self.links_definition_options
            {
              :extend       => LinkCollectionRepresenter,
              :exec_context => :decorator
            }
          end
        end

        def self.included(base)
          base.extend Declarative # inject our ::link.
        end

        # New API for JSON-API representers.
        module Declarative
          def type(name=nil)
            return super unless name # original name.
            representable_attrs[:_wrap] = name.to_s
          end

          # Define global document links for the links: directive.
          def link(*args, &block)
            representable_attrs[:resource_representer].link(*args, &block)
          end

          # Per-model links.
          def links(&block)
            nested(:_links, :inherit => true, &block)
          end

          # TODO: always create _links.
          def has_one(name)
            property :_links, :inherit => true, :use_decorator => true do # simply extend the Decorator _links.
              property "#{name}_id", :as => name
            end
          end

          def has_many(name)
            property :_links, :inherit => true, :use_decorator => true do # simply extend the Decorator _links.
              collection "#{name.to_s.sub(/s$/, "")}_ids", :as => name
            end
          end

          def compound(&block)
            nested(:linked, &block)
          end

          def meta(&block)
            representable_attrs[:meta_representer] = Class.new(Roar::Decorator, &block)
          end
        end
      end


      # TODO: don't use Document for singular+wrap AND singular in collection (this way, we can get rid of the only_body)
      module Document
        def to_hash(options={})
          # per resource:
          res = super # render single resource or collection.
          return res if options[:only_body]
          # this is the only "dirty" part: this module is always included in the Singular document representer, when used in collection, we don't want it to do the extra work. this mechanism here might be changed soon.

          to_document(res, options)
        end

        def from_hash(hash, options={})

          return super(hash, options) if options[:only_body] # singular

          super(from_document(hash)) # singular
        end

      private
        def to_document(res, options)
          links = render_links
          meta  = render_meta(options)
          # FIXME: provide two different #to_document

          if res.is_a?(Array)
            compound = collection_compound!(res, {})
          else
            compound = compile_compound!(res.delete("linked"), {})
          end

          {representable_attrs[:_wrap] => res}.tap do |doc|
            doc.merge!(links)
            doc.merge!(meta)
            doc.merge!("linked" => compound) if compound && compound.size > 0 # FIXME: make that like the above line.
          end
        end

        def from_document(hash)
          hash[representable_attrs[:_wrap]]
        end

        # Compiles the linked: section for compound objects in the document.
        def collection_compound!(collection, compound)
          collection.each { |res|
            kv = res.delete("linked") or next

            compile_compound!(kv, compound)
          }

          compound
        end

        # Go through {"album"=>{"title"=>"Hackers"}, "musicians"=>[{"name"=>"Eddie Van Halen"}, ..]} from linked:
        # and wrap every item in an array.
        def compile_compound!(linked, compound)
          return unless linked

          linked.each { |k,v| # {"album"=>{"title"=>"Hackers"}, "musicians"=>[{"name"=>"Eddie Van Halen"}, {"name"=>"Greg Howe"}]}
            compound[k] ||= []

            if v.is_a?(::Hash) # {"title"=>"Hackers"}
              compound[k] << v
            else
              compound[k].push(*v) # [{"name"=>"Eddie Van Halen"}, {"name"=>"Greg Howe"}]
            end

            compound[k] = compound[k].uniq
          }

          compound
        end

        def render_links
          representable_attrs[:resource_representer].new(represented).to_hash # creates links: section.
        end

        def render_meta(options)
          # TODO: this will call collection.page etc, directly on the collection. we could allow using a "meta"
          # object to hold this data.
          # `meta call_meta: true` or something
          return {"meta" => options["meta"]} if options["meta"]
          return {} unless representer = representable_attrs[:meta_representer]
          {"meta" => representer.new(represented).extend(Representable::Hash).to_hash}
        end


        module Collection
          include Document

          def to_hash(options={})
            res = super(options.merge(:only_body => true))
            to_document(res, options)
          end

          def from_hash(hash, options={})
            hash = from_document(hash)
            super(hash, options.merge(:only_body => true))
          end
        end
      end


      module LinkRepresenter
        include Roar::JSON

        property :href
        property :type
      end

      require 'representable/json/hash'
      module LinkCollectionRepresenter
        include Representable::JSON::Hash

        values :extend => LinkRepresenter # TODO: parsing.
      end
    end
  end
end
