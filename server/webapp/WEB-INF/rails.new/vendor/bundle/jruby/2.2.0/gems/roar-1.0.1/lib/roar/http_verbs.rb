require 'roar/transport/net_http'

module Roar
  # Gives HTTP-power to representers. They can serialize, send, process and deserialize HTTP-requests.
  module HttpVerbs

    class << self
      attr_accessor :transport_engine

      def included(base)
        base.extend ClassMethods
      end
    end
    self.transport_engine = ::Roar::Transport::NetHTTP


    module ClassMethods
      # GETs +url+ with +format+ and returns deserialized represented object.
      def get(*args)
        new.get(*args)
      end
    end


    attr_writer :transport_engine
    def transport_engine
      @transport_engine || HttpVerbs.transport_engine
    end

    # Serializes the object, POSTs it to +url+ with +format+, deserializes the returned document
    # and updates properties accordingly.
    def post(options={}, &block)
      response = http.post_uri(options.merge(:body => serialize), &block)
      handle_response(response)
    end

    # GETs +url+ with +format+, deserializes the returned document and updates properties accordingly.
    def get(options={}, &block)
      response = http.get_uri(options, &block)
      handle_response(response)
    end

    # Serializes the object, PUTs it to +url+ with +format+, deserializes the returned document
    # and updates properties accordingly.
    def put(options={}, &block)
      response = http.put_uri(options.merge(:body => serialize), &block)
      handle_response(response)
      self
    end

    def patch(options={}, &block)
      response = http.patch_uri(options.merge(:body => serialize), &block)
      handle_response(response)
      self
    end

    def delete(options, &block)
      http.delete_uri(options, &block)
      self
    end

  private
    def handle_response(response)
      document = response.body
      deserialize(document)
    end

    def http
      transport_engine.new
    end

    def handle_deprecated_args(body, *args) # TODO: remove in 1.0.
      options = args.first

      if args.size > 1
        warn %{DEPRECATION WARNING: #get, #post, #put, #delete and #patch no longer accept positional arguments. Please call them as follows:
 get(uri: "http://localhost/songs", as: "application/json")
post(uri: "http://localhost/songs", as: "application/json")
Thank you and have a beautiful day.}
        options = {:uri => args[0], :as => args[1]} if args.size == 2
        options = {:uri => args[0], :as => args[2]}
      end

      options[:body] = body
      options
    end
  end
end
