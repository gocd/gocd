require 'rack/mime'

module Sprockets
  module Mime
    # Returns a `Hash` of registered mime types registered on the
    # environment and those part of `Rack::Mime`.
    #
    # If an `ext` is given, it will lookup the mime type for that extension.
    def mime_types(ext = nil)
      if ext.nil?
        Rack::Mime::MIME_TYPES.merge(@mime_types)
      else
        ext = Sprockets::Utils.normalize_extension(ext)
        @mime_types[ext] || Rack::Mime::MIME_TYPES[ext]
      end
    end

    # Returns a `Hash` of explicitly registered mime types.
    def registered_mime_types
      @mime_types.dup
    end

    if {}.respond_to?(:key)
      def extension_for_mime_type(type)
        mime_types.key(type)
      end
    else
      def extension_for_mime_type(type)
        mime_types.index(type)
      end
    end

    # Register a new mime type.
    def register_mime_type(mime_type, ext)
      ext = Sprockets::Utils.normalize_extension(ext)
      @mime_types[ext] = mime_type
    end

    if defined? Encoding
      # Returns the correct encoding for a given mime type, while falling
      # back on the default external encoding, if it exists.
      def encoding_for_mime_type(type)
        encoding = Encoding::BINARY if type =~ %r{^(image|audio|video)/}
        encoding ||= default_external_encoding if respond_to?(:default_external_encoding)
        encoding
      end
    end
  end
end
