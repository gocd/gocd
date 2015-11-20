require 'test_helper'

class ZipCentralDirectoryTest < MiniTest::Test

  def teardown
    ::Zip.reset!
  end

  def test_read_from_stream
    ::File.open(TestZipFile::TEST_ZIP2.zip_name, "rb") {
        |zipFile|
      cdir = ::Zip::CentralDirectory.read_from_stream(zipFile)

      assert_equal(TestZipFile::TEST_ZIP2.entry_names.size, cdir.size)
      assert(cdir.entries.sort.compare_enumerables(TestZipFile::TEST_ZIP2.entry_names.sort) {
          |cdirEntry, testEntryName|
        cdirEntry.name == testEntryName
      })
      assert_equal(TestZipFile::TEST_ZIP2.comment, cdir.comment)
    }
  end

  def test_readFromInvalidStream
    File.open("test/data/file2.txt", "rb") {
        |zipFile|
      cdir = ::Zip::CentralDirectory.new
      cdir.read_from_stream(zipFile)
    }
    fail "ZipError expected!"
  rescue ::Zip::Error
  end

  def test_ReadFromTruncatedZipFile
    fragment=""
    File.open("test/data/testDirectory.bin", "rb") { |f| fragment = f.read }
    fragment.slice!(12) # removed part of first cdir entry. eocd structure still complete
    fragment.extend(IOizeString)
    entry = ::Zip::CentralDirectory.new
    entry.read_from_stream(fragment)
    fail "ZipError expected"
  rescue ::Zip::Error
  end

  def test_write_to_stream
    entries = [::Zip::Entry.new("file.zip", "flimse", "myComment", "somethingExtra"),
               ::Zip::Entry.new("file.zip", "secondEntryName"),
               ::Zip::Entry.new("file.zip", "lastEntry.txt", "Has a comment too")]
    cdir = ::Zip::CentralDirectory.new(entries, "my zip comment")
    File.open("test/data/generated/cdirtest.bin", "wb") { |f| cdir.write_to_stream(f) }
    cdirReadback = ::Zip::CentralDirectory.new
    File.open("test/data/generated/cdirtest.bin", "rb") { |f| cdirReadback.read_from_stream(f) }

    assert_equal(cdir.entries.sort, cdirReadback.entries.sort)
  end

  def test_write64_to_stream
    ::Zip.write_zip64_support = true
    entries = [::Zip::Entry.new("file.zip", "file1-little", "comment1", "", 200, 101, ::Zip::Entry::STORED, 200),
               ::Zip::Entry.new("file.zip", "file2-big", "comment2", "", 18000000000, 102, ::Zip::Entry::DEFLATED, 20000000000),
               ::Zip::Entry.new("file.zip", "file3-alsobig", "comment3", "", 15000000000, 103, ::Zip::Entry::DEFLATED, 21000000000),
               ::Zip::Entry.new("file.zip", "file4-little", "comment4", "", 100, 104, ::Zip::Entry::DEFLATED, 121)]
    [0, 250, 18000000300, 33000000350].each_with_index do |offset, index|
      entries[index].local_header_offset = offset
    end
    cdir = ::Zip::CentralDirectory.new(entries, "zip comment")
    File.open("test/data/generated/cdir64test.bin", "wb") { |f| cdir.write_to_stream(f) }
    cdirReadback = ::Zip::CentralDirectory.new
    File.open("test/data/generated/cdir64test.bin", "rb") { |f| cdirReadback.read_from_stream(f) }

    assert_equal(cdir.entries.sort, cdirReadback.entries.sort)
    assert_equal(::Zip::VERSION_NEEDED_TO_EXTRACT_ZIP64, cdirReadback.instance_variable_get(:@version_needed_for_extract))
  end

  def test_equality
    cdir1 = ::Zip::CentralDirectory.new([::Zip::Entry.new("file.zip", "flimse", nil,
                                                          "somethingExtra"),
                                         ::Zip::Entry.new("file.zip", "secondEntryName"),
                                         ::Zip::Entry.new("file.zip", "lastEntry.txt")],
                                        "my zip comment")
    cdir2 = ::Zip::CentralDirectory.new([::Zip::Entry.new("file.zip", "flimse", nil,
                                                          "somethingExtra"),
                                         ::Zip::Entry.new("file.zip", "secondEntryName"),
                                         ::Zip::Entry.new("file.zip", "lastEntry.txt")],
                                        "my zip comment")
    cdir3 = ::Zip::CentralDirectory.new([::Zip::Entry.new("file.zip", "flimse", nil,
                                                          "somethingExtra"),
                                         ::Zip::Entry.new("file.zip", "secondEntryName"),
                                         ::Zip::Entry.new("file.zip", "lastEntry.txt")],
                                        "comment?")
    cdir4 = ::Zip::CentralDirectory.new([::Zip::Entry.new("file.zip", "flimse", nil,
                                                          "somethingExtra"),
                                         ::Zip::Entry.new("file.zip", "lastEntry.txt")],
                                        "comment?")
    assert_equal(cdir1, cdir1)
    assert_equal(cdir1, cdir2)

    assert(cdir1 != cdir3)
    assert(cdir2 != cdir3)
    assert(cdir2 != cdir3)
    assert(cdir3 != cdir4)

    assert(cdir3 != "hello")
  end
end
