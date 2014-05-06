# -*- ruby encoding: utf-8 -*-

require 'mime/types'

class TestMIMETypesCache < Minitest::Test
  def setup
    require 'fileutils'
    @cache_file = File.expand_path('../cache.tst', __FILE__)
    ENV['RUBY_MIME_TYPES_CACHE'] = @cache_file
    clear_cache_file
  end

  def teardown
    clear_cache_file
    ENV.delete('RUBY_MIME_TYPES_CACHE')
  end

  def reset_mime_types
    MIME::Types.instance_variable_set(:@__types__, nil)
    MIME::Types.send(:load_mime_types)
  end

  def clear_cache_file
    FileUtils.rm @cache_file if File.exist? @cache_file
  end

  def test_uses_correct_cache_file
    assert_equal(@cache_file, MIME::Types.cache_file)
  end

  def test_does_not_use_cache_when_unset
    ENV.delete('RUBY_MIME_TYPES_CACHE')
    assert_equal(nil, MIME::Types.send(:load_mime_types_from_cache))
  end

  def test_raises_exception_when_load_forced_without_cache_file
    assert_raises(ArgumentError) {
      ENV.delete('RUBY_MIME_TYPES_CACHE')
      MIME::Types.send(:load_mime_types_from_cache!)
    }
  end

  def test_does_not_use_cache_when_missing
    assert_equal(false, MIME::Types.send(:load_mime_types_from_cache))
  end

  def test_does_not_create_cache_when_unset
    ENV.delete('RUBY_MIME_TYPES_CACHE')
    assert_equal(nil, MIME::Types.send(:write_mime_types_to_cache))
  end

  def test_raises_exception_when_write_forced_without_cache_file
    assert_raises(ArgumentError) {
      ENV.delete('RUBY_MIME_TYPES_CACHE')
      MIME::Types.send(:write_mime_types_to_cache!)
    }
  end

  def test_creates_cache
    assert_equal(false, File.exist?(@cache_file))
    MIME::Types.send(:write_mime_types_to_cache)
    assert_equal(true, File.exist?(@cache_file))
  end

  def test_uses_cache
    html = MIME::Types['text/html'].first
    html.extensions << 'hex'
    MIME::Types.send(:write_mime_types_to_cache)
    MIME::Types.instance_variable_set(:@__types__, nil)

    assert_equal(true, MIME::Types.send(:load_mime_types_from_cache))
    html = MIME::Types['text/html'].first
    assert_includes(html.extensions, 'hex')

    reset_mime_types
  end
end
