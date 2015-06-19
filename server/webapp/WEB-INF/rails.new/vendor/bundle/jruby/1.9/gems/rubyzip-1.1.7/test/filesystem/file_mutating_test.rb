require 'test_helper'
require 'zip/filesystem'

class ZipFsFileMutatingTest < MiniTest::Test
  TEST_ZIP = "test/data/generated/zipWithDirs_copy.zip"
  def setup
    FileUtils.cp("test/data/zipWithDirs.zip", TEST_ZIP)
  end

  def teardown
  end

  def test_delete
    do_test_delete_or_unlink(:delete)
  end

  def test_unlink
    do_test_delete_or_unlink(:unlink)
  end

  def test_open_write
    ::Zip::File.open(TEST_ZIP) {
      |zf|

      zf.file.open("test_open_write_entry", "w") {
        |f|
        f.write "This is what I'm writing"
      }
      assert_equal("This is what I'm writing",
                    zf.file.read("test_open_write_entry"))

      # Test with existing entry
      zf.file.open("file1", "wb") { #also check that 'b' option is ignored
        |f|
        f.write "This is what I'm writing too"
      }
      assert_equal("This is what I'm writing too",
                    zf.file.read("file1"))
    }
  end

  def test_rename
    ::Zip::File.open(TEST_ZIP) {
      |zf|
      assert_raises(Errno::ENOENT, "") {
        zf.file.rename("NoSuchFile", "bimse")
      }
      zf.file.rename("file1", "newNameForFile1")
    }

    ::Zip::File.open(TEST_ZIP) {
      |zf|
      assert(! zf.file.exists?("file1"))
      assert(zf.file.exists?("newNameForFile1"))
    }
  end

  def test_chmod
    ::Zip::File.open(TEST_ZIP) {
      |zf|

      zf.file.chmod(0765, "file1")
    }

    ::Zip::File.open(TEST_ZIP) {
      |zf|
      assert_equal(0100765,  zf.file.stat("file1").mode)
    }
  end

  def do_test_delete_or_unlink(symbol)
    ::Zip::File.open(TEST_ZIP) {
      |zf|
      assert(zf.file.exists?("dir2/dir21/dir221/file2221"))
      zf.file.send(symbol, "dir2/dir21/dir221/file2221")
      assert(! zf.file.exists?("dir2/dir21/dir221/file2221"))

      assert(zf.file.exists?("dir1/file11"))
      assert(zf.file.exists?("dir1/file12"))
      zf.file.send(symbol, "dir1/file11", "dir1/file12")
      assert(! zf.file.exists?("dir1/file11"))
      assert(! zf.file.exists?("dir1/file12"))

      assert_raises(Errno::ENOENT) { zf.file.send(symbol, "noSuchFile") }
      assert_raises(Errno::EISDIR) { zf.file.send(symbol, "dir1/dir11") }
      assert_raises(Errno::EISDIR) { zf.file.send(symbol, "dir1/dir11/") }
    }

    ::Zip::File.open(TEST_ZIP) {
      |zf|
      assert(! zf.file.exists?("dir2/dir21/dir221/file2221"))
      assert(! zf.file.exists?("dir1/file11"))
      assert(! zf.file.exists?("dir1/file12"))

      assert(zf.file.exists?("dir1/dir11"))
      assert(zf.file.exists?("dir1/dir11/"))
    }
  end

end
