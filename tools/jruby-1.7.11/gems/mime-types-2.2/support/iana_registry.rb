# -*- ruby encoding: utf-8 -*-

$LOAD_PATH.unshift File.expand_path('../../lib', __FILE__)

require 'open-uri'
require 'nokogiri'
require 'cgi'
require 'pathname'
require 'yaml'

ENV['RUBY_MIME_TYPES_LAZY_LOAD'] = 'yes'
require 'mime/types'

class IANARegistry
  DEFAULTS = {
    url: %q(https://www.iana.org/assignments/media-types/media-types.xml),
    to: Pathname(__FILE__).join('../../type-lists')
  }.freeze.each_value(&:freeze)

  def self.download(options = {})
    dest = Pathname(options[:to] || DEFAULTS[:to]).expand_path
    url  = options.fetch(:url, DEFAULTS[:url])

    puts "Downloading IANA MIME type assignments."
    puts "\t#{url}"
    xml  = Nokogiri::XML(open(url) { |f| f.read })

    xml.css('registry registry').each do |registry|
      next if registry.at_css('title').text == 'example'
      new(registry: registry, to: dest) do |parser|
        puts "Extracting #{parser.type}/*."
        parser.parse
        parser.save
      end
    end
  end

  attr_reader :type

  def initialize(options = {})
    @registry = options.fetch(:registry)
    @to       = Pathname(options.fetch(:to)).expand_path
    @type     = @registry.at_css('title').text
    @name     = "#{@type}.yaml"
    @file     = @to.join(@name)
    @types    = mime_types_for(@file)

    yield self if block_given?
  end

  ASSIGNMENT_FILE_REF = "{%s=http://www.iana.org/assignments/media-types/%s}"

  def parse
    @registry.css('record').each do |record|
      subtype = record.at_css('name').text
      refs    = record.css('xref').map do |xref|
        case xref["type"]
        when 'person'
          "[#{xref["data"]}]"
        when 'rfc'
          xref["data"].upcase
        when 'draft'
          "DRAFT:#{xref["data"].sub(/^RFC-/, 'draft-')}"
        when 'rfc-errata'
          "{RFC Errata #{xref["data"]}=http://www.rfc-editor.org/errata_search.php?eid=#{xref["data"]}}"
        when 'uri'
          # Fix a couple of known-broken links:
          case xref["data"]
          when /contact-people.htmll#Dolan\z/
            "[Dolan]"
          when /contact-people.htmll#Rottmann?\z/
            "[Frank_Rottman]"
          else
            "{#{xref["data"]}}"
          end
        when 'text'
          xref["data"]
        end
      end

      xrefs   = MIME::Types::Container.new
      record.css('xref').map do |xref|
        type, data = xref["type"], xref["data"]

        case type
        when 'uri'
          case data
          when /contact-people.htmll#Dolan\z/
            type, data = "person", "Dolan"
          when /contact-people.htmll#Rottmann?\z/
            type, data = "person", "Frank_Rottman"
          end
        end

        xrefs[type] << data
      end

      record.css('file').each do |file|
        if file["type"] == "template"
          refs << (ASSIGNMENT_FILE_REF % [ file.text, file.text ])
        end

        xrefs[file["type"]] << file.text
      end

      content_type  = [ @type, subtype ].join('/')
      obsolete      = record.at_css('obsolete')
      use_instead   = record.at_css('deprecated').text rescue nil

      types         = @types.select { |t|
        (t.content_type == content_type)
      }

      if types.empty?
        MIME::Type.new(content_type) do |mt|
          mt.references  = %w(IANA) + refs
          mt.xrefs       = xrefs
          mt.registered  = true
          mt.obsolete    = obsolete if obsolete
          mt.use_instead = use_instead if use_instead
          @types << mt
        end
      else
        types.each { |mt|
          mt.references  = %w(IANA) + refs
          mt.registered  = true
          mt.xrefs       = xrefs
          mt.obsolete    = obsolete if obsolete
          mt.use_instead = use_instead if use_instead
        }
      end
    end
  end

  def save
    @to.mkpath
    File.open(@file, 'wb') { |f| f.puts @types.map.to_a.sort.to_yaml }
  end

  private
  def mime_types_for(file)
    if file.exist?
      MIME::Types::Loader.load_from_yaml(file)
    else
      MIME::Types.new
    end
  end
end
