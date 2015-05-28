#Process.setrlimit(Process::RLIMIT_CORE, Process::RLIM_INFINITY) unless RUBY_PLATFORM =~ /(java|mswin|mingw)/i
$VERBOSE = true
require 'minitest/autorun'
require 'minitest/pride'
require 'fileutils'
require 'tempfile'
require 'pp'

require 'nokogiri'

warn "#{__FILE__}:#{__LINE__}: version info: #{Nokogiri::VERSION_INFO.inspect}"

module Nokogiri
  class TestCase < MiniTest::Spec
    ASSETS_DIR          = File.expand_path File.join(File.dirname(__FILE__), 'files')
    ADDRESS_SCHEMA_FILE = File.join(ASSETS_DIR, 'address_book.rlx')
    ADDRESS_XML_FILE    = File.join(ASSETS_DIR, 'address_book.xml')
    ENCODING_HTML_FILE  = File.join(ASSETS_DIR, 'encoding.html')
    ENCODING_XHTML_FILE = File.join(ASSETS_DIR, 'encoding.xhtml')
    EXML_FILE           = File.join(ASSETS_DIR, 'exslt.xml')
    EXSLT_FILE          = File.join(ASSETS_DIR, 'exslt.xslt')
    HTML_FILE           = File.join(ASSETS_DIR, 'tlm.html')
    METACHARSET_FILE    = File.join(ASSETS_DIR, 'metacharset.html')
    NICH_FILE           = File.join(ASSETS_DIR, '2ch.html')
    NOENCODING_FILE     = File.join(ASSETS_DIR, 'noencoding.html')
    PO_SCHEMA_FILE      = File.join(ASSETS_DIR, 'po.xsd')
    PO_XML_FILE         = File.join(ASSETS_DIR, 'po.xml')
    SHIFT_JIS_HTML      = File.join(ASSETS_DIR, 'shift_jis.html')
    SHIFT_JIS_NO_CHARSET= File.join(ASSETS_DIR, 'shift_jis_no_charset.html')
    SHIFT_JIS_XML       = File.join(ASSETS_DIR, 'shift_jis.xml')
    SNUGGLES_FILE       = File.join(ASSETS_DIR, 'snuggles.xml')
    XML_FILE            = File.join(ASSETS_DIR, 'staff.xml')
    XML_XINCLUDE_FILE   = File.join(ASSETS_DIR, 'xinclude.xml')
    XML_ATOM_FILE       = File.join(ASSETS_DIR, 'atom.xml')
    XSLT_FILE           = File.join(ASSETS_DIR, 'staff.xslt')
    XPATH_FILE          = File.join(ASSETS_DIR, 'slow-xpath.xml')

    def teardown
      if ENV['NOKOGIRI_GC']
        STDOUT.putc '!'
        if RUBY_PLATFORM =~ /java/
          require 'java'
          java.lang.System.gc
        else
          GC.start
        end
      end
    end

    def stress_memory_while &block
      # force the test to explicitly declare a skip
      raise "JRuby doesn't do GC" if Nokogiri.jruby?

      old_stress = GC.stress
      begin
        GC.stress = true
        yield
      ensure
        GC.stress = old_stress
      end
    end

    def assert_indent amount, doc, message = nil
      nodes = []
      doc.traverse do |node|
        nodes << node if node.text? && node.blank?
      end
      assert nodes.length > 0
      nodes.each do |node|
        len = node.content.gsub(/[\r\n]/, '').length
        assert_equal(0, len % amount, message)
      end
    end

    def util_decorate(document, decorator_module)
      document.decorators(XML::Node) << decorator_module
      document.decorators(XML::NodeSet) << decorator_module
      document.decorate!
    end

    #
    #  Test::Unit backwards compatibility section
    #
    alias :assert_no_match      :refute_match
    alias :assert_not_nil       :refute_nil
    alias :assert_raise         :assert_raises
    alias :assert_not_equal     :refute_equal

    def assert_not_send send_ary, m = nil
      recv, msg, *args = send_ary
      m = message(m) {
        "Expected #{mu_pp(recv)}.#{msg}(*#{mu_pp(args)}) to return false" }
      assert !recv.__send__(msg, *args), m
    end unless method_defined?(:assert_not_send)
  end

  module SAX
    class TestCase < Nokogiri::TestCase
      class Doc < XML::SAX::Document
        attr_reader :start_elements, :start_document_called
        attr_reader :end_elements, :end_document_called
        attr_reader :data, :comments, :cdata_blocks, :start_elements_namespace
        attr_reader :errors, :warnings, :end_elements_namespace
        attr_reader :xmldecls
        attr_reader :processing_instructions

        def xmldecl version, encoding, standalone
          @xmldecls = [version, encoding, standalone].compact
          super
        end

        def start_document
          @start_document_called = true
          super
        end

        def end_document
          @end_document_called = true
          super
        end

        def error error
          (@errors ||= []) << error
          super
        end

        def warning warning
          (@warning ||= []) << warning
          super
        end

        def start_element *args
          (@start_elements ||= []) << args
          super
        end

        def start_element_namespace *args
          (@start_elements_namespace ||= []) << args
          super
        end

        def end_element *args
          (@end_elements ||= []) << args
          super
        end

        def end_element_namespace *args
          (@end_elements_namespace ||= []) << args
          super
        end

        def characters string
          @data ||= []
          @data += [string]
          super
        end

        def comment string
          @comments ||= []
          @comments += [string]
          super
        end

        def cdata_block string
          @cdata_blocks ||= []
          @cdata_blocks += [string]
          super
        end

        def processing_instruction name, content
          @processing_instructions ||= []
          @processing_instructions << [name, content]
        end
      end
    end
  end
end
