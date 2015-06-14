require 'test_helper'
require 'zip/filesystem'

class ZipFsDirIteratorTest < MiniTest::Test

  FILENAME_ARRAY = [ "f1", "f2", "f3", "f4", "f5", "f6"  ]

  def setup
    @dirIt = ::Zip::FileSystem::ZipFsDirIterator.new(FILENAME_ARRAY)
  end

  def test_close
    @dirIt.close
    assert_raises(IOError, "closed directory") {
      @dirIt.each { |e| p e }
    }
    assert_raises(IOError, "closed directory") {
      @dirIt.read
    }
    assert_raises(IOError, "closed directory") {
      @dirIt.rewind
    }
    assert_raises(IOError, "closed directory") {
      @dirIt.seek(0)
    }
    assert_raises(IOError, "closed directory") {
      @dirIt.tell
    }

  end

  def test_each
    # Tested through Enumerable.entries
    assert_equal(FILENAME_ARRAY, @dirIt.entries)
  end

  def test_read
    FILENAME_ARRAY.size.times {
      |i|
      assert_equal(FILENAME_ARRAY[i], @dirIt.read)
    }
  end

  def test_rewind
    @dirIt.read
    @dirIt.read
    assert_equal(FILENAME_ARRAY[2], @dirIt.read)
    @dirIt.rewind
    assert_equal(FILENAME_ARRAY[0], @dirIt.read)
  end

  def test_tell_seek
    @dirIt.read
    @dirIt.read
    pos = @dirIt.tell
    valAtPos = @dirIt.read
    @dirIt.read
    @dirIt.seek(pos)
    assert_equal(valAtPos, @dirIt.read)
  end

end
