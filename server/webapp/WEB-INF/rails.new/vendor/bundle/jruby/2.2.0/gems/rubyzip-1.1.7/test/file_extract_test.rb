require 'test_helper'

class ZipFileExtractTest < MiniTest::Test
  include CommonZipFileFixture
  EXTRACTED_FILENAME = "test/data/generated/extEntry"
  ENTRY_TO_EXTRACT, *REMAINING_ENTRIES = TEST_ZIP.entry_names.reverse

  def setup
    super
    ::File.delete(EXTRACTED_FILENAME) if ::File.exist?(EXTRACTED_FILENAME)
  end

  def test_extract
    ::Zip::File.open(TEST_ZIP.zip_name) {
        |zf|
      zf.extract(ENTRY_TO_EXTRACT, EXTRACTED_FILENAME)

      assert(File.exist?(EXTRACTED_FILENAME))
      AssertEntry::assert_contents(EXTRACTED_FILENAME,
                                   zf.get_input_stream(ENTRY_TO_EXTRACT) { |is| is.read })


      ::File.unlink(EXTRACTED_FILENAME)

      entry = zf.get_entry(ENTRY_TO_EXTRACT)
      entry.extract(EXTRACTED_FILENAME)

      assert(File.exist?(EXTRACTED_FILENAME))
      AssertEntry::assert_contents(EXTRACTED_FILENAME,
                                   entry.get_input_stream() { |is| is.read })

    }
  end

  def test_extractExists
    writtenText = "written text"
    ::File.open(EXTRACTED_FILENAME, "w") { |f| f.write(writtenText) }

    assert_raises(::Zip::DestinationFileExistsError) {
      ::Zip::File.open(TEST_ZIP.zip_name) { |zf|
        zf.extract(zf.entries.first, EXTRACTED_FILENAME)
      }
    }
    File.open(EXTRACTED_FILENAME, "r") { |f|
      assert_equal(writtenText, f.read)
    }
  end

  def test_extractExistsOverwrite
    writtenText = "written text"
    ::File.open(EXTRACTED_FILENAME, "w") { |f| f.write(writtenText) }

    gotCalledCorrectly = false
    ::Zip::File.open(TEST_ZIP.zip_name) {
        |zf|
      zf.extract(zf.entries.first, EXTRACTED_FILENAME) {
          |entry, extractLoc|
        gotCalledCorrectly = zf.entries.first == entry &&
            extractLoc == EXTRACTED_FILENAME
        true
      }
    }

    assert(gotCalledCorrectly)
    ::File.open(EXTRACTED_FILENAME, "r") {
        |f|
      assert(writtenText != f.read)
    }
  end

  def test_extractNonEntry
    zf = ::Zip::File.new(TEST_ZIP.zip_name)
    assert_raises(Errno::ENOENT) { zf.extract("nonExistingEntry", "nonExistingEntry") }
  ensure
    zf.close if zf
  end

  def test_extractNonEntry2
    outFile = "outfile"
    assert_raises(Errno::ENOENT) {
      zf = ::Zip::File.new(TEST_ZIP.zip_name)
      nonEntry = "hotdog-diddelidoo"
      assert(!zf.entries.include?(nonEntry))
      zf.extract(nonEntry, outFile)
      zf.close
    }
    assert(!File.exist?(outFile))
  end

end
