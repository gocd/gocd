require 'test_helper'
require 'zip/filesystem'

class ZipFsDirectoryTest < MiniTest::Test
  TEST_ZIP = "test/data/generated/zipWithDirs_copy.zip"

  def setup
    FileUtils.cp("test/data/zipWithDirs.zip", TEST_ZIP)
  end

  def test_delete
    ::Zip::File.open(TEST_ZIP) {
      |zf|
      assert_raises(Errno::ENOENT, "No such file or directory - NoSuchFile.txt") {
        zf.dir.delete("NoSuchFile.txt")
      }
      assert_raises(Errno::EINVAL, "Invalid argument - file1") {
        zf.dir.delete("file1")
      }
      assert(zf.file.exists?("dir1"))
      zf.dir.delete("dir1")
      assert(! zf.file.exists?("dir1"))
    }
  end

  def test_mkdir
    ::Zip::File.open(TEST_ZIP) {
      |zf|
      assert_raises(Errno::EEXIST, "File exists - dir1") {
        zf.dir.mkdir("file1")
      }
      assert_raises(Errno::EEXIST, "File exists - dir1") {
        zf.dir.mkdir("dir1")
      }
      assert(!zf.file.exists?("newDir"))
      zf.dir.mkdir("newDir")
      assert(zf.file.directory?("newDir"))
      assert(!zf.file.exists?("newDir2"))
      zf.dir.mkdir("newDir2", 3485)
      assert(zf.file.directory?("newDir2"))
    }
  end

  def test_pwd_chdir_entries
    ::Zip::File.open(TEST_ZIP) {
      |zf|
      assert_equal("/", zf.dir.pwd)

      assert_raises(Errno::ENOENT, "No such file or directory - no such dir") {
        zf.dir.chdir "no such dir"
      }

      assert_raises(Errno::EINVAL, "Invalid argument - file1") {
        zf.dir.chdir "file1"
      }

      assert_equal(["dir1", "dir2", "file1"].sort, zf.dir.entries(".").sort)
      zf.dir.chdir "dir1"
      assert_equal("/dir1", zf.dir.pwd)
      assert_equal(["dir11", "file11", "file12"], zf.dir.entries(".").sort)

      zf.dir.chdir "../dir2/dir21"
      assert_equal("/dir2/dir21", zf.dir.pwd)
      assert_equal(["dir221"].sort, zf.dir.entries(".").sort)
    }
  end

  def test_foreach
    ::Zip::File.open(TEST_ZIP) {
      |zf|

      blockCalled = false
      assert_raises(Errno::ENOENT, "No such file or directory - noSuchDir") {
        zf.dir.foreach("noSuchDir") { |e| blockCalled = true }
      }
      assert(! blockCalled)

      assert_raises(Errno::ENOTDIR, "Not a directory - file1") {
        zf.dir.foreach("file1") { |e| blockCalled = true }
      }
      assert(! blockCalled)

      entries = []
      zf.dir.foreach(".") { |e| entries << e }
      assert_equal(["dir1", "dir2", "file1"].sort, entries.sort)

      entries = []
      zf.dir.foreach("dir1") { |e| entries << e }
      assert_equal(["dir11", "file11", "file12"], entries.sort)
    }
  end

  def test_chroot
    ::Zip::File.open(TEST_ZIP) {
      |zf|
      assert_raises(NotImplementedError) {
        zf.dir.chroot
      }
    }
  end

  # Globbing not supported yet
  #def test_glob
  #  # test alias []-operator too
  #  fail "implement test"
  #end

  def test_open_new
    ::Zip::File.open(TEST_ZIP) {
      |zf|

      assert_raises(Errno::ENOTDIR, "Not a directory - file1") {
        zf.dir.new("file1")
      }

      assert_raises(Errno::ENOENT, "No such file or directory - noSuchFile") {
        zf.dir.new("noSuchFile")
      }

      d = zf.dir.new(".")
      assert_equal(["file1", "dir1", "dir2"].sort, d.entries.sort)
      d.close

      zf.dir.open("dir1") {
        |dir|
        assert_equal(["dir11", "file11", "file12"].sort, dir.entries.sort)
      }
    }
  end

end
