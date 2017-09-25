# -*- ruby encoding: utf-8 -*-

# The namespace for MIME applications, tools, and libraries.
module MIME
  # Reflects a MIME Content-Type which is in invalid format (e.g., it isn't
  # in the form of type/subtype).
  class InvalidContentType < RuntimeError; end

  # The definition of one MIME content-type.
  #
  # == Usage
  #  require 'mime/types'
  #
  #  plaintext = MIME::Types['text/plain'].first
  #  # returns [text/plain, text/plain]
  #  text      = plaintext.first
  #  print text.media_type           # => 'text'
  #  print text.sub_type             # => 'plain'
  #
  #  puts text.extensions.join(" ")  # => 'asc txt c cc h hh cpp'
  #
  #  puts text.encoding              # => 8bit
  #  puts text.binary?               # => false
  #  puts text.ascii?                # => true
  #  puts text == 'text/plain'       # => true
  #  puts MIME::Type.simplified('x-appl/x-zip') # => 'appl/zip'
  #
  #  puts MIME::Types.any? { |type|
  #    type.content_type == 'text/plain'
  #  }                               # => true
  #  puts MIME::Types.all?(&:registered?)
  #                                  # => false
  #
  class Type
    # The released version of Ruby MIME::Types
    VERSION = '1.25.1'

    include Comparable

    MEDIA_TYPE_RE = %r{([-\w.+]+)/([-\w.+]*)}o
    UNREG_RE      = %r{[Xx]-}o
    ENCODING_RE   = %r{(?:base64|7bit|8bit|quoted\-printable)}o
    PLATFORM_RE   = %r|#{RUBY_PLATFORM}|o

    SIGNATURES    = %w(application/pgp-keys application/pgp
                       application/pgp-signature application/pkcs10
                       application/pkcs7-mime application/pkcs7-signature
                       text/vcard)

    IANA_URL      = "http://www.iana.org/assignments/media-types/%s/%s"
    RFC_URL       = "http://rfc-editor.org/rfc/rfc%s.txt"
    DRAFT_URL     = "http://datatracker.ietf.org/public/idindex.cgi?command=id_details&filename=%s"
    LTSW_URL      = "http://www.ltsw.se/knbase/internet/%s.htp"
    CONTACT_URL   = "http://www.iana.org/assignments/contact-people.htm#%s"

    # Returns +true+ if the simplified type matches the current
    def like?(other)
      if other.respond_to?(:simplified)
        @simplified == other.simplified
      else
        @simplified == Type.simplified(other)
      end
    end

    # Compares the MIME::Type against the exact content type or the
    # simplified type (the simplified type will be used if comparing against
    # something that can be treated as a String with #to_s). In comparisons,
    # this is done against the lowercase version of the MIME::Type.
    def <=>(other)
      if other.respond_to?(:content_type)
        @content_type.downcase <=> other.content_type.downcase
      elsif other.respond_to?(:to_s)
        @simplified <=> Type.simplified(other.to_s)
      else
        @content_type.downcase <=> other.downcase
      end
    end

    # Compares the MIME::Type based on how reliable it is before doing a
    # normal <=> comparison. Used by MIME::Types#[] to sort types. The
    # comparisons involved are:
    #
    # 1. self.simplified <=> other.simplified (ensures that we
    #    don't try to compare different types)
    # 2. IANA-registered definitions < other definitions.
    # 3. Generic definitions < platform definitions.
    # 3. Complete definitions < incomplete definitions.
    # 4. Current definitions < obsolete definitions.
    # 5. Obselete with use-instead references < obsolete without.
    # 6. Obsolete use-instead definitions are compared.
    def priority_compare(other)
      pc = simplified <=> other.simplified

      if pc.zero?
        pc = if registered? != other.registered?
               registered? ? -1 : 1 # registered < unregistered
             elsif platform? != other.platform?
               platform? ? 1 : -1 # generic < platform
             elsif complete? != other.complete?
               complete? ? -1 : 1 # complete < incomplete
             elsif obsolete? != other.obsolete?
               obsolete? ? 1 : -1 # current < obsolete
             else
               0
             end

        if pc.zero? and obsolete? and (use_instead != other.use_instead)
          pc = if use_instead.nil?
                 -1
               elsif other.use_instead.nil?
                 1
               else
                 use_instead <=> other.use_instead
               end
        end
      end

      pc
    end

    # Returns +true+ if the other object is a MIME::Type and the content
    # types match.
    def eql?(other)
      other.kind_of?(MIME::Type) and self == other
    end

    # Returns the whole MIME content-type string.
    #
    #   text/plain        => text/plain
    #   x-chemical/x-pdb  => x-chemical/x-pdb
    attr_reader :content_type
    # Returns the media type of the simplified MIME type.
    #
    #   text/plain        => text
    #   x-chemical/x-pdb  => chemical
    attr_reader :media_type
    # Returns the media type of the unmodified MIME type.
    #
    #   text/plain        => text
    #   x-chemical/x-pdb  => x-chemical
    attr_reader :raw_media_type
    # Returns the sub-type of the simplified MIME type.
    #
    #   text/plain        => plain
    #   x-chemical/x-pdb  => pdb
    attr_reader :sub_type
    # Returns the media type of the unmodified MIME type.
    #
    #   text/plain        => plain
    #   x-chemical/x-pdb  => x-pdb
    attr_reader :raw_sub_type
    # The MIME types main- and sub-label can both start with <tt>x-</tt>,
    # which indicates that it is a non-registered name. Of course, after
    # registration this flag can disappear, adds to the confusing
    # proliferation of MIME types. The simplified string has the <tt>x-</tt>
    # removed and are translated to lowercase.
    #
    #   text/plain        => text/plain
    #   x-chemical/x-pdb  => chemical/pdb
    attr_reader :simplified
    # The list of extensions which are known to be used for this MIME::Type.
    # Non-array values will be coerced into an array with #to_a. Array
    # values will be flattened and +nil+ values removed.
    attr_accessor :extensions
    remove_method :extensions= ;
    def extensions=(ext) #:nodoc:
      @extensions = [ext].flatten.compact
    end

    # The encoding (7bit, 8bit, quoted-printable, or base64) required to
    # transport the data of this content type safely across a network, which
    # roughly corresponds to Content-Transfer-Encoding. A value of +nil+ or
    # <tt>:default</tt> will reset the #encoding to the #default_encoding
    # for the MIME::Type. Raises ArgumentError if the encoding provided is
    # invalid.
    #
    # If the encoding is not provided on construction, this will be either
    # 'quoted-printable' (for text/* media types) and 'base64' for eveything
    # else.
    attr_accessor :encoding
    remove_method :encoding= ;
    def encoding=(enc) #:nodoc:
      if enc.nil? or enc == :default
        @encoding = self.default_encoding
      elsif enc =~ ENCODING_RE
        @encoding = enc
      else
        raise ArgumentError, "The encoding must be nil, :default, base64, 7bit, 8bit, or quoted-printable."
      end
    end

    # The regexp for the operating system that this MIME::Type is specific
    # to.
    attr_accessor :system
    remove_method :system= ;
    def system=(os) #:nodoc:
      if os.nil? or os.kind_of?(Regexp)
        @system = os
      else
        @system = %r|#{os}|
      end
    end
    # Returns the default encoding for the MIME::Type based on the media
    # type.
    attr_reader :default_encoding
    remove_method :default_encoding
    def default_encoding
      (@media_type == 'text') ? 'quoted-printable' : 'base64'
    end

    # Returns the media type or types that should be used instead of this
    # media type, if it is obsolete. If there is no replacement media type,
    # or it is not obsolete, +nil+ will be returned.
    attr_reader :use_instead
    remove_method :use_instead
    def use_instead
      return nil unless @obsolete
      @use_instead
    end

    # Returns +true+ if the media type is obsolete.
    def obsolete?
      @obsolete ? true : false
    end
    # Sets the obsolescence indicator for this media type.
    attr_writer :obsolete

    # The documentation for this MIME::Type. Documentation about media
    # types will be found on a media type definition as a comment.
    # Documentation will be found through #docs.
    attr_accessor :docs
    remove_method :docs= ;
    def docs=(d)
      if d
        a = d.scan(%r{use-instead:#{MEDIA_TYPE_RE}})

        if a.empty?
          @use_instead = nil
        else
          @use_instead = a.map { |el| "#{el[0]}/#{el[1]}" }
        end
      end
      @docs = d
    end

    # The encoded URL list for this MIME::Type. See #urls for more
    # information.
    attr_accessor :url
    # The decoded URL list for this MIME::Type.
    # The special URL value IANA will be translated into:
    #   http://www.iana.org/assignments/media-types/<mediatype>/<subtype>
    #
    # The special URL value RFC### will be translated into:
    #   http://www.rfc-editor.org/rfc/rfc###.txt
    #
    # The special URL value DRAFT:name will be translated into:
    #   https://datatracker.ietf.org/public/idindex.cgi?
    #       command=id_detail&filename=<name>
    #
    # The special URL value LTSW will be translated into:
    #   http://www.ltsw.se/knbase/internet/<mediatype>.htp
    #
    # The special URL value [token] will be translated into:
    #   http://www.iana.org/assignments/contact-people.htm#<token>
    #
    # These values will be accessible through #urls, which always returns an
    # array.
    def urls
      @url.map do |el|
        case el
        when %r{^IANA$}
          IANA_URL % [ @media_type, @sub_type ]
        when %r{^RFC(\d+)$}
          RFC_URL % $1
        when %r{^DRAFT:(.+)$}
          DRAFT_URL % $1
        when %r{^LTSW$}
          LTSW_URL % @media_type
        when %r{^\{([^=]+)=([^\}]+)\}}
          [$1, $2]
        when %r{^\[([^=]+)=([^\]]+)\]}
          [$1, CONTACT_URL % $2]
        when %r{^\[([^\]]+)\]}
          CONTACT_URL % $1
        else
          el
        end
      end
    end

    class << self
      # The MIME types main- and sub-label can both start with <tt>x-</tt>,
      # which indicates that it is a non-registered name. Of course, after
      # registration this flag can disappear, adds to the confusing
      # proliferation of MIME types. The simplified string has the
      # <tt>x-</tt> removed and are translated to lowercase.
      def simplified(content_type)
        matchdata = MEDIA_TYPE_RE.match(content_type)

        if matchdata.nil?
          simplified = nil
        else
          media_type = matchdata.captures[0].downcase.gsub(UNREG_RE, '')
          subtype = matchdata.captures[1].downcase.gsub(UNREG_RE, '')
          simplified = "#{media_type}/#{subtype}"
        end
        simplified
      end

      # Creates a MIME::Type from an array in the form of:
      #   [type-name, [extensions], encoding, system]
      #
      # +extensions+, +encoding+, and +system+ are optional.
      #
      #   MIME::Type.from_array("application/x-ruby", ['rb'], '8bit')
      #   MIME::Type.from_array(["application/x-ruby", ['rb'], '8bit'])
      #
      # These are equivalent to:
      #
      #   MIME::Type.new('application/x-ruby') do |t|
      #     t.extensions  = %w(rb)
      #     t.encoding    = '8bit'
      #   end
      def from_array(*args) #:yields MIME::Type.new:
        # Dereferences the array one level, if necessary.
        args = args.first if args.first.kind_of? Array

        unless args.size.between?(1, 8)
          raise ArgumentError, "Array provided must contain between one and eight elements."
        end

        MIME::Type.new(args.shift) do |t|
          t.extensions, t.encoding, t.system, t.obsolete, t.docs, t.url,
            t.registered = *args
          yield t if block_given?
        end
      end

      # Creates a MIME::Type from a hash. Keys are case-insensitive,
      # dashes may be replaced with underscores, and the internal Symbol
      # of the lowercase-underscore version can be used as well. That is,
      # Content-Type can be provided as content-type, Content_Type,
      # content_type, or :content_type.
      #
      # Known keys are <tt>Content-Type</tt>,
      # <tt>Content-Transfer-Encoding</tt>, <tt>Extensions</tt>, and
      # <tt>System</tt>.
      #
      #   MIME::Type.from_hash('Content-Type' => 'text/x-yaml',
      #                        'Content-Transfer-Encoding' => '8bit',
      #                        'System' => 'linux',
      #                        'Extensions' => ['yaml', 'yml'])
      #
      # This is equivalent to:
      #
      #   MIME::Type.new('text/x-yaml') do |t|
      #     t.encoding    = '8bit'
      #     t.system      = 'linux'
      #     t.extensions  = ['yaml', 'yml']
      #   end
      def from_hash(hash) #:yields MIME::Type.new:
        type = {}
        hash.each_pair do |k, v|
          type[k.to_s.tr('A-Z', 'a-z').gsub(/-/, '_').to_sym] = v
        end

        MIME::Type.new(type[:content_type]) do |t|
          t.extensions  = type[:extensions]
          t.encoding    = type[:content_transfer_encoding]
          t.system      = type[:system]
          t.obsolete    = type[:obsolete]
          t.docs        = type[:docs]
          t.url         = type[:url]
          t.registered  = type[:registered]

          yield t if block_given?
        end
      end

      # Essentially a copy constructor.
      #
      #   MIME::Type.from_mime_type(plaintext)
      #
      # is equivalent to:
      #
      #   MIME::Type.new(plaintext.content_type.dup) do |t|
      #     t.extensions  = plaintext.extensions.dup
      #     t.system      = plaintext.system.dup
      #     t.encoding    = plaintext.encoding.dup
      #   end
      def from_mime_type(mime_type) #:yields the new MIME::Type:
        MIME::Type.new(mime_type.content_type.dup) do |t|
          t.extensions = mime_type.extensions.map { |e| e.dup }
          t.url = mime_type.url && mime_type.url.map { |e| e.dup }

          mime_type.system && t.system = mime_type.system.dup
          mime_type.encoding && t.encoding = mime_type.encoding.dup

          t.obsolete = mime_type.obsolete?
          t.registered = mime_type.registered?

          mime_type.docs && t.docs = mime_type.docs.dup

          yield t if block_given?
        end
      end
    end

    # Builds a MIME::Type object from the provided MIME Content Type value
    # (e.g., 'text/plain' or 'applicaton/x-eruby'). The constructed object
    # is yielded to an optional block for additional configuration, such as
    # associating extensions and encoding information.
    def initialize(content_type) #:yields self:
      matchdata = MEDIA_TYPE_RE.match(content_type)

      if matchdata.nil?
        raise InvalidContentType, "Invalid Content-Type provided ('#{content_type}')"
      end

      @content_type = content_type
      @raw_media_type = matchdata.captures[0]
      @raw_sub_type = matchdata.captures[1]

      @simplified = MIME::Type.simplified(@content_type)
      matchdata = MEDIA_TYPE_RE.match(@simplified)
      @media_type = matchdata.captures[0]
      @sub_type = matchdata.captures[1]

      self.extensions   = nil
      self.encoding     = :default
      self.system       = nil
      self.registered   = true
      self.url          = nil
      self.obsolete     = nil
      self.docs         = nil

      yield self if block_given?
    end

    # MIME content-types which are not regestered by IANA nor defined in
    # RFCs are required to start with <tt>x-</tt>. This counts as well for
    # a new media type as well as a new sub-type of an existing media
    # type. If either the media-type or the content-type begins with
    # <tt>x-</tt>, this method will return +false+.
    def registered?
      if (@raw_media_type =~ UNREG_RE) || (@raw_sub_type =~ UNREG_RE)
        false
      else
        @registered
      end
    end
    attr_writer :registered #:nodoc:

    # MIME types can be specified to be sent across a network in particular
    # formats. This method returns +true+ when the MIME type encoding is set
    # to <tt>base64</tt>.
    def binary?
      @encoding == 'base64'
    end

    # MIME types can be specified to be sent across a network in particular
    # formats. This method returns +false+ when the MIME type encoding is
    # set to <tt>base64</tt>.
    def ascii?
      not binary?
    end

    # Returns +true+ when the simplified MIME type is in the list of known
    # digital signatures.
    def signature?
      SIGNATURES.include?(@simplified.downcase)
    end

    # Returns +true+ if the MIME::Type is specific to an operating system.
    def system?
      not @system.nil?
    end

    # Returns +true+ if the MIME::Type is specific to the current operating
    # system as represented by RUBY_PLATFORM.
    def platform?
      system? and (RUBY_PLATFORM =~ @system)
    end

    # Returns +true+ if the MIME::Type specifies an extension list,
    # indicating that it is a complete MIME::Type.
    def complete?
      not @extensions.empty?
    end

    # Returns the MIME type as a string.
    def to_s
      @content_type
    end

    # Returns the MIME type as a string for implicit conversions.
    def to_str
      @content_type
    end

    # Returns the MIME type as an array suitable for use with
    # MIME::Type.from_array.
    def to_a
      [ @content_type, @extensions, @encoding, @system, @obsolete, @docs,
        @url, registered? ]
    end

    # Returns the MIME type as an array suitable for use with
    # MIME::Type.from_hash.
    def to_hash
      { 'Content-Type'              => @content_type,
        'Content-Transfer-Encoding' => @encoding,
        'Extensions'                => @extensions,
        'System'                    => @system,
        'Obsolete'                  => @obsolete,
        'Docs'                      => @docs,
        'URL'                       => @url,
        'Registered'                => registered?,
      }
    end
  end

  # = MIME::Types
  # MIME types are used in MIME-compliant communications, as in e-mail or
  # HTTP traffic, to indicate the type of content which is transmitted.
  # MIME::Types provides the ability for detailed information about MIME
  # entities (provided as a set of MIME::Type objects) to be determined and
  # used programmatically. There are many types defined by RFCs and vendors,
  # so the list is long but not complete; don't hesitate to ask to add
  # additional information. This library follows the IANA collection of MIME
  # types (see below for reference).
  #
  # == Description
  # MIME types are used in MIME entities, as in email or HTTP traffic. It is
  # useful at times to have information available about MIME types (or,
  # inversely, about files). A MIME::Type stores the known information about
  # one MIME type.
  #
  # == Usage
  #  require 'mime/types'
  #
  #  plaintext = MIME::Types['text/plain']
  #  print plaintext.media_type           # => 'text'
  #  print plaintext.sub_type             # => 'plain'
  #
  #  puts plaintext.extensions.join(" ")  # => 'asc txt c cc h hh cpp'
  #
  #  puts plaintext.encoding              # => 8bit
  #  puts plaintext.binary?               # => false
  #  puts plaintext.ascii?                # => true
  #  puts plaintext.obsolete?             # => false
  #  puts plaintext.registered?           # => true
  #  puts plaintext == 'text/plain'       # => true
  #  puts MIME::Type.simplified('x-appl/x-zip') # => 'appl/zip'
  #
  # This module is built to conform to the MIME types of RFCs 2045 and 2231.
  # It follows the official IANA registry at
  # http://www.iana.org/assignments/media-types/ and
  # ftp://ftp.iana.org/assignments/media-types with some unofficial types
  # added from the the collection at
  # http://www.ltsw.se/knbase/internet/mime.htp
  class Types
    # The released version of Ruby MIME::Types
    VERSION      = MIME::Type::VERSION
    DATA_VERSION = (VERSION.to_f * 100).to_i

    # The data version.
    attr_reader :data_version

    class HashWithArrayDefault < Hash # :nodoc:
      def initialize
        super { |h, k| h[k] = [] }
      end

      def marshal_dump
        {}.merge(self)
      end

      def marshal_load(hash)
        self.merge!(hash)
      end
    end

    class CacheContainer # :nodoc:
      attr_reader :version, :data
      def initialize(version, data)
        @version, @data = version, data
      end
    end

    def initialize(data_version = DATA_VERSION)
      @type_variants    = HashWithArrayDefault.new
      @extension_index  = HashWithArrayDefault.new
      @data_version     = data_version
    end

    def add_type_variant(mime_type) #:nodoc:
      @type_variants[mime_type.simplified] << mime_type
    end

    def index_extensions(mime_type) #:nodoc:
      mime_type.extensions.each { |ext| @extension_index[ext] << mime_type }
    end

    def defined_types #:nodoc:
      @type_variants.values.flatten
    end

    # Returns the number of known types. A shortcut of MIME::Types[//].size.
    # (Keep in mind that this is memory intensive, cache the result to spare
    # resources)
    def count
      defined_types.size
    end

    def each
     defined_types.each { |t| yield t }
    end

    @__types__  = nil

    # Returns a list of MIME::Type objects, which may be empty. The optional
    # flag parameters are :complete (finds only complete MIME::Type objects)
    # and :platform (finds only MIME::Types for the current platform). It is
    # possible for multiple matches to be returned for either type (in the
    # example below, 'text/plain' returns two values -- one for the general
    # case, and one for VMS systems.
    #
    #   puts "\nMIME::Types['text/plain']"
    #   MIME::Types['text/plain'].each { |t| puts t.to_a.join(", ") }
    #
    #   puts "\nMIME::Types[/^image/, :complete => true]"
    #   MIME::Types[/^image/, :complete => true].each do |t|
    #     puts t.to_a.join(", ")
    #   end
    #
    # If multiple type definitions are returned, returns them sorted as
    # follows:
    #   1. Complete definitions sort before incomplete ones;
    #   2. IANA-registered definitions sort before LTSW-recorded
    #      definitions.
    #   3. Generic definitions sort before platform-specific ones;
    #   4. Current definitions sort before obsolete ones;
    #   5. Obsolete definitions with use-instead clauses sort before those
    #      without;
    #   6. Obsolete definitions use-instead clauses are compared.
    #   7. Sort on name.
    def [](type_id, flags = {})
      matches = case type_id
                when MIME::Type
                  @type_variants[type_id.simplified]
                when Regexp
                  match(type_id)
                else
                  @type_variants[MIME::Type.simplified(type_id)]
                end

      prune_matches(matches, flags).sort { |a, b| a.priority_compare(b) }
    end

    # Return the list of MIME::Types which belongs to the file based on its
    # filename extension. If +platform+ is +true+, then only file types that
    # are specific to the current platform will be returned.
    #
    # This will always return an array.
    #
    #   puts "MIME::Types.type_for('citydesk.xml')
    #     => [application/xml, text/xml]
    #   puts "MIME::Types.type_for('citydesk.gif')
    #     => [image/gif]
    def type_for(filename, platform = false)
      ext = filename.chomp.downcase.gsub(/.*\./o, '')
      list = @extension_index[ext]
      list.delete_if { |e| not e.platform? } if platform
      list
    end

    # A synonym for MIME::Types.type_for
    def of(filename, platform = false)
      type_for(filename, platform)
    end

    # Add one or more MIME::Type objects to the set of known types. Each
    # type should be experimental (e.g., 'application/x-ruby'). If the type
    # is already known, a warning will be displayed.
    #
    # <strong>Please inform the maintainer of this module when registered
    # types are missing.</strong>
    def add(*types)
      types.each do |mime_type|
        if mime_type.kind_of? MIME::Types
          add(*mime_type.defined_types)
        else
          if @type_variants.include?(mime_type.simplified)
            if @type_variants[mime_type.simplified].include?(mime_type)
              warn "Type #{mime_type} already registered as a variant of #{mime_type.simplified}." unless defined? MIME::Types::LOAD
            end
          end
          add_type_variant(mime_type)
          index_extensions(mime_type)
        end
      end
    end

    private
    def prune_matches(matches, flags)
      matches.delete_if { |e| not e.complete? } if flags[:complete]
      matches.delete_if { |e| not e.platform? } if flags[:platform]
      matches
    end

    def match(pattern)
      matches = @type_variants.select { |k, v| k =~ pattern }
      if matches.respond_to? :values
        matches.values.flatten
      else
        matches.map { |m| m.last }.flatten
      end
    end

    class << self
      def add_type_variant(mime_type) #:nodoc:
        __types__.add_type_variant(mime_type)
      end

      def index_extensions(mime_type) #:nodoc:
        __types__.index_extensions(mime_type)
      end

      # The regular expression used to match a file-based MIME type
      # definition.
      TEXT_FORMAT_RE = %r{
        \A
        \s*
        ([*])?                                 # 0: Unregistered?
        (!)?                                   # 1: Obsolete?
        (?:(\w+):)?                            # 2: Platform marker
        #{MIME::Type::MEDIA_TYPE_RE}?          # 3,4: Media type
        (?:\s+@([^\s]+))?                      # 5: Extensions
        (?:\s+:(#{MIME::Type::ENCODING_RE}))?  # 6: Encoding
        (?:\s+'(.+))?                          # 7: URL list
        (?:\s+=(.+))?                          # 8: Documentation
        (?:\s*([#].*)?)?
        \s*
        \z
      }x

      # Build the type list from a file in the format:
      #
      #   [*][!][os:]mt/st[<ws>@ext][<ws>:enc][<ws>'url-list][<ws>=docs]
      #
      # == *
      # An unofficial MIME type. This should be used if and only if the MIME type
      # is not properly specified (that is, not under either x-type or
      # vnd.name.type).
      #
      # == !
      # An obsolete MIME type. May be used with an unofficial MIME type.
      #
      # == os:
      # Platform-specific MIME type definition.
      #
      # == mt
      # The media type.
      #
      # == st
      # The media subtype.
      #
      # == <ws>@ext
      # The list of comma-separated extensions.
      #
      # == <ws>:enc
      # The encoding.
      #
      # == <ws>'url-list
      # The list of comma-separated URLs.
      #
      # == <ws>=docs
      # The documentation string.
      #
      # That is, everything except the media type and the subtype is optional. The
      # more information that's available, though, the richer the values that can
      # be provided.
      def load_from_file(filename) #:nodoc:
        if defined? ::Encoding
          data = File.open(filename, 'r:UTF-8:-') { |f| f.read }
        else
          data = File.open(filename) { |f| f.read }
        end
        data = data.split($/)
        mime = MIME::Types.new
        data.each_with_index { |line, index|
          item = line.chomp.strip
          next if item.empty?

          begin
            m = TEXT_FORMAT_RE.match(item).captures
          rescue Exception
            puts "#{filename}:#{index}: Parsing error in MIME type definitions."
            puts "=> #{line}"
            raise
          end

          unregistered, obsolete, platform, mediatype, subtype, extensions,
            encoding, urls, docs, comment = *m

          if mediatype.nil?
            if comment.nil?
              puts "#{filename}:#{index}: Parsing error in MIME type definitions."
              puts "=> #{line}"
              raise RuntimeError
            end

            next
          end

          extensions &&= extensions.split(/,/)
          urls &&= urls.split(/,/)

          mime_type = MIME::Type.new("#{mediatype}/#{subtype}") do |t|
            t.extensions  = extensions
            t.encoding    = encoding
            t.system      = platform
            t.obsolete    = obsolete
            t.registered  = false if unregistered
            t.docs        = docs
            t.url         = urls
          end

          mime.add(mime_type)
        }
        mime
      end

      # Returns a list of MIME::Type objects, which may be empty. The
      # optional flag parameters are :complete (finds only complete
      # MIME::Type objects) and :platform (finds only MIME::Types for the
      # current platform). It is possible for multiple matches to be
      # returned for either type (in the example below, 'text/plain' returns
      # two values -- one for the general case, and one for VMS systems.
      #
      #   puts "\nMIME::Types['text/plain']"
      #   MIME::Types['text/plain'].each { |t| puts t.to_a.join(", ") }
      #
      #   puts "\nMIME::Types[/^image/, :complete => true]"
      #   MIME::Types[/^image/, :complete => true].each do |t|
      #     puts t.to_a.join(", ")
      #   end
      def [](type_id, flags = {})
        __types__[type_id, flags]
      end

      include Enumerable

      def count
        __types__.count
      end

      def each
        __types__.each {|t| yield t }
      end

      # Return the list of MIME::Types which belongs to the file based on
      # its filename extension. If +platform+ is +true+, then only file
      # types that are specific to the current platform will be returned.
      #
      # This will always return an array.
      #
      #   puts "MIME::Types.type_for('citydesk.xml')
      #     => [application/xml, text/xml]
      #   puts "MIME::Types.type_for('citydesk.gif')
      #     => [image/gif]
      def type_for(filename, platform = false)
        __types__.type_for(filename, platform)
      end

      # A synonym for MIME::Types.type_for
      def of(filename, platform = false)
        __types__.type_for(filename, platform)
      end

      # Add one or more MIME::Type objects to the set of known types. Each
      # type should be experimental (e.g., 'application/x-ruby'). If the
      # type is already known, a warning will be displayed.
      #
      # <strong>Please inform the maintainer of this module when registered
      # types are missing.</strong>
      def add(*types)
        __types__.add(*types)
      end

      # Returns the currently defined cache file, if any.
      def cache_file
        ENV['RUBY_MIME_TYPES_CACHE']
      end

      private
      def load_mime_types_from_cache
        load_mime_types_from_cache! if cache_file
      end

      def load_mime_types_from_cache!
        raise ArgumentError, "No RUBY_MIME_TYPES_CACHE set." unless cache_file
        return false unless File.exists? cache_file

        begin
          data      = File.read(cache_file)
          container = Marshal.load(data)

          if container.version == VERSION
            @__types__ = Marshal.load(container.data)
            true
          else
            false
          end
        rescue => e
          warn "Could not load MIME::Types cache: #{e}"
          false
        end
      end

      def write_mime_types_to_cache
        write_mime_types_to_cache! if cache_file
      end

      def write_mime_types_to_cache!
        raise ArgumentError, "No RUBY_MIME_TYPES_CACHE set." unless cache_file

        File.open(cache_file, 'w') do |f|
          cache = MIME::Types::CacheContainer.new(VERSION,
                                                  Marshal.dump(__types__))
          f.write Marshal.dump(cache)
        end

        true
      end

      def load_and_parse_mime_types
        const_set(:LOAD, true) unless $DEBUG
        Dir[File.join(File.dirname(__FILE__), 'types', '*')].sort.each { |f|
          add(load_from_file(f))
        }
        remove_const :LOAD if defined? LOAD
      end

      def lazy_load?
        (lazy = ENV['RUBY_MIME_TYPES_LAZY_LOAD']) && (lazy != 'false')
      end

      def __types__
        load_mime_types unless @__types__
        @__types__
      end

      def load_mime_types
        @__types__ = new(VERSION)
        unless load_mime_types_from_cache
          load_and_parse_mime_types
          write_mime_types_to_cache
        end
      end
    end

    load_mime_types unless lazy_load?
  end
end

# vim: ft=ruby
