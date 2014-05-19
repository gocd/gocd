# -*- ruby encoding: utf-8 -*-

require 'mime/types'
require 'minitest_helper'

class TestMIMETypesLoader < Minitest::Test
  def setup
    @path   = File.expand_path('../fixture', __FILE__)
    @loader = MIME::Types::Loader.new(@path)
  end

  def assert_correctly_loaded(types)
    assert_includes(types, 'application/1d-interleaved-parityfec')
    assert_includes(types['application/acad'].first.references, 'LTSW')
    assert_equal([%w(WebM http://www.webmproject.org/code/specs/container/)],
                 types['audio/webm'].first.urls)
    assert_equal(%w(webm), types['audio/webm'].first.extensions)
    refute(types['audio/webm'].first.registered?)

    assert_equal("Fixes a bug with IE6 and progressive JPEGs",
                 types['image/pjpeg'].first.docs)

    assert(types['application/x-apple-diskimage'].first.system?)
    assert_equal(/mac/, types['application/x-apple-diskimage'].first.system)

    assert(types['audio/vnd.qcelp'].first.obsolete?)
    assert_equal(%w(audio/QCELP),
                 types['audio/vnd.qcelp'].first.use_instead)
  end

  def test_load_yaml
    assert_correctly_loaded(@loader.load_yaml)
  end

  def test_load_json
    assert_correctly_loaded(@loader.load_json)
  end

  def test_load_v1
    assert_correctly_loaded(@loader.load_v1)
  end
end
