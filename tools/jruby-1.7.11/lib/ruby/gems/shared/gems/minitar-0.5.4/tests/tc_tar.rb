#!/usr/bin/env ruby

$LOAD_PATH.unshift("#{File.dirname(__FILE__)}/../lib") if __FILE__ == $0

require 'archive/tar/minitar'
require 'test/unit'
require 'stringio'
require 'yaml'
require 'zlib'

module TarTester
private
  def assert_headers_equal(h1, h2)
    fields = %w(name 100 mode 8 uid 8 gid 8 size 12 mtime 12 checksum 8
                typeflag 1 linkname 100 magic 6 version 2 uname 32 gname 32
                devmajor 8 devminor 8 prefix 155)
    offset = 0
    until fields.empty?
      name = fields.shift
      length = fields.shift.to_i
      if name == "checksum"
        chksum_off = offset
        offset += length
        next
      end
      assert_equal(h1[offset, length], h2[offset, length], 
                   "Field #{name} of the tar header differs.")
      offset += length
    end
    assert_equal(h1[chksum_off, 8], h2[chksum_off, 8], "Checksumes differ.")
  end

  def tar_file_header(fname, dname, mode, length)
    h = header("0", fname, dname, length, mode)
    checksum = calc_checksum(h)
    header("0", fname, dname, length, mode, checksum)
  end

  def tar_dir_header(name, prefix, mode)
    h = header("5", name, prefix, 0, mode)
    checksum = calc_checksum(h)
    header("5", name, prefix, 0, mode, checksum)
  end

  def header(type, fname, dname, length, mode, checksum = nil)
    checksum ||= " " * 8
    arr = [ASCIIZ(fname, 100), Z(to_oct(mode, 7)), Z(to_oct(nil, 7)),
           Z(to_oct(nil, 7)), Z(to_oct(length, 11)), Z(to_oct(0, 11)),
           checksum, type, "\0" * 100, "ustar\0", "00", ASCIIZ("", 32),
           ASCIIZ("", 32), Z(to_oct(nil, 7)), Z(to_oct(nil, 7)),
           ASCIIZ(dname, 155) ]
    arr = arr.join("").split(//).map{ |x| x[0] }
    h = arr.pack("C100C8C8C8C12C12C8CC100C6C2C32C32C8C8C155")
    ret = h + "\0" * (512 - h.size)
    assert_equal(512, ret.size)
    ret
  end
    
  def calc_checksum(header)
    sum = header.unpack("C*").inject { |s, a| s + a }
    SP(Z(to_oct(sum, 6)))
  end
    
  def to_oct(n, pad_size)
    if n.nil?
      "\0" * pad_size
    else
      "%0#{pad_size}o" % n
    end
  end

  def ASCIIZ(str, length)
    str + "\0" * (length - str.length)
  end

  def SP(s)
    s + " "
  end

  def Z(s)
    s + "\0"
  end

  def SP_Z(s)
    s + " \0"
  end
end

class TC_Tar__Header < Test::Unit::TestCase
  include Archive::Tar::Minitar
  include TarTester
    
  def test_arguments_are_checked
    e = ArgumentError
    assert_raises(e) { Archive::Tar::PosixHeader.new(:name => "", :size => "", :mode => "") }
    assert_raises(e) { Archive::Tar::PosixHeader.new(:name => "", :size => "", :prefix => "") }
    assert_raises(e) { Archive::Tar::PosixHeader.new(:name => "", :prefix => "", :mode => "") }
    assert_raises(e) { Archive::Tar::PosixHeader.new(:prefix => "", :size => "", :mode => "") }
  end

  def test_basic_headers
    header = { :name => "bla", :mode => 012345, :size => 10, :prefix => "", :typeflag => "0" }
    assert_headers_equal(tar_file_header("bla", "", 012345, 10),
                         Archive::Tar::PosixHeader.new(header).to_s)
    header = { :name => "bla", :mode => 012345, :size => 0, :prefix => "", :typeflag => "5" }
    assert_headers_equal(tar_dir_header("bla", "", 012345),
                         Archive::Tar::PosixHeader.new(header).to_s)
  end

  def test_long_name_works
    header = { :name => "a" * 100, :mode => 012345, :size => 10, :prefix => "" }
    assert_headers_equal(tar_file_header("a" * 100, "", 012345, 10),
                         Archive::Tar::PosixHeader.new(header).to_s)
    header = { :name => "a" * 100, :mode => 012345, :size => 10, :prefix => "bb" * 60 }
    assert_headers_equal(tar_file_header("a" * 100, "bb" * 60, 012345, 10),
                         Archive::Tar::PosixHeader.new(header).to_s)
  end

  def test_new_from_stream
    header = tar_file_header("a" * 100, "", 012345, 10)
    h = nil
    header = StringIO.new(header)
    assert_nothing_raised { h = Archive::Tar::PosixHeader.new_from_stream(header) }
    assert_equal("a" * 100, h.name)
    assert_equal(012345, h.mode)
    assert_equal(10, h.size)
    assert_equal("", h.prefix)
    assert_equal("ustar", h.magic)
  end
    
  def test_new_from_stream_with_evil_name
    header = tar_file_header("a \0" + "\0" * 97, "", 012345, 10)
    h = nil
    header = StringIO.new(header)
    assert_nothing_raised{ h = Archive::Tar::PosixHeader.new_from_stream header }
    assert_equal("a ", h.name)
  end
end

class TC_Tar__Writer < Test::Unit::TestCase
  include Archive::Tar::Minitar
  include TarTester

  class DummyIO
    attr_reader :data

    def initialize
      @data = ""
    end

    def write(dat)
      data << dat
      dat.size
    end

    def reset
      @data = ""
    end
  end

  def setup
    @data = "a" * 10
    @dummyos = DummyIO.new
    @os = Writer.new(@dummyos)
  end

  def teardown
    @os.close
  end

  def test_add_file_simple
    @dummyos.reset

    Writer.open(@dummyos) do |os|
      os.add_file_simple("lib/foo/bar", :mode => 0644, :size => 10) do |f|
        f.write "a" * 10
      end
      os.add_file_simple("lib/bar/baz", :mode => 0644, :size => 100) do |f|
        f.write "fillme"
      end
    end

    assert_headers_equal(tar_file_header("lib/foo/bar", "", 0644, 10),
                         @dummyos.data[0, 512])
    assert_equal("a" * 10 + "\0" * 502, @dummyos.data[512, 512])
    assert_headers_equal(tar_file_header("lib/bar/baz", "", 0644, 100),
                        @dummyos.data[512 * 2, 512])
    assert_equal("fillme" + "\0" * 506, @dummyos.data[512 * 3, 512])
    assert_equal("\0" * 512, @dummyos.data[512 * 4, 512])
    assert_equal("\0" * 512, @dummyos.data[512 * 5, 512])
  end

  def test_write_operations_fail_after_closed
    @dummyos.reset
    @os.add_file_simple("sadd", :mode => 0644, :size => 20) { |f| }
    @os.close
    assert_raises(ClosedStream) { @os.flush }
    assert_raises(ClosedStream) { @os.add_file("dfdsf", :mode => 0644) {} }
    assert_raises(ClosedStream) { @os.mkdir "sdfdsf", :mode => 0644 }
  end

  def test_file_name_is_split_correctly
      # test insane file lengths, and: a{100}/b{155}, etc
    @dummyos.reset
    names = [ "#{'a' * 155}/#{'b' * 100}", "#{'a' * 151}/#{'qwer/' * 19}bla" ]
    o_names = [ "#{'b' * 100}", "#{'qwer/' * 19}bla" ]
    o_prefixes = [ "a" * 155, "a" * 151 ]
    names.each do |name|
      @os.add_file_simple(name, :mode => 0644, :size => 10) { }
    end
    o_names.each_with_index do |nam, i|
      assert_headers_equal(tar_file_header(nam, o_prefixes[i], 0644, 10),
                           @dummyos.data[2 * i * 512, 512])
    end
    assert_raises(FileNameTooLong) do
      @os.add_file_simple(File.join("a" * 152, "b" * 10, "a" * 92),
                                    :mode => 0644, :size => 10) { }
    end
    assert_raises(FileNameTooLong) do
      @os.add_file_simple(File.join("a" * 162, "b" * 10),
                                    :mode => 0644, :size => 10) { }
    end
    assert_raises(FileNameTooLong) do
      @os.add_file_simple(File.join("a" * 10, "b" * 110),
                                    :mode => 0644, :size => 10) { }
    end
  end

  def test_add_file
    dummyos = StringIO.new
    class << dummyos
      def method_missing(meth, *a)
        self.string.send(meth, *a)
      end
    end
    os = Writer.new dummyos
    content1 = ('a'..'z').to_a.join("")  # 26
    content2 = ('aa'..'zz').to_a.join("") # 1352
    Writer.open(dummyos) do |os|
      os.add_file("lib/foo/bar", :mode => 0644) { |f, opts| f.write "a" * 10 }
      os.add_file("lib/bar/baz", :mode => 0644) { |f, opts| f.write content1 }
      os.add_file("lib/bar/baz", :mode => 0644) { |f, opts| f.write content2 }
      os.add_file("lib/bar/baz", :mode => 0644) { |f, opts| }
    end
    assert_headers_equal(tar_file_header("lib/foo/bar", "", 0644, 10),
                         dummyos[0, 512])
    assert_equal(%Q(#{"a" * 10}#{"\0" * 502}), dummyos[512, 512])
    offset = 512 * 2
    [content1, content2, ""].each do |data|
      assert_headers_equal(tar_file_header("lib/bar/baz", "", 0644,
                                          data.size), dummyos[offset, 512])
      offset += 512
      until !data || data == ""
        chunk = data[0, 512]
        data[0, 512] = ""
        assert_equal(chunk + "\0" * (512 - chunk.size), 
        dummyos[offset, 512])
        offset += 512
      end
    end
    assert_equal("\0" * 1024, dummyos[offset, 1024])
  end

  def test_add_file_tests_seekability
    assert_raises(Archive::Tar::Minitar::NonSeekableStream) do 
      @os.add_file("libdfdsfd", :mode => 0644) { |f| }
    end
  end

  def test_write_header
    @dummyos.reset
    @os.add_file_simple("lib/foo/bar", :mode => 0644, :size => 0) { |f|  }
    @os.flush
    assert_headers_equal(tar_file_header("lib/foo/bar", "", 0644, 0),
                        @dummyos.data[0, 512])
    @dummyos.reset
    @os.mkdir("lib/foo", :mode => 0644)
    assert_headers_equal(tar_dir_header("lib/foo", "", 0644),
                        @dummyos.data[0, 512])
    @os.mkdir("lib/bar", :mode => 0644)
    assert_headers_equal(tar_dir_header("lib/bar", "", 0644),
                        @dummyos.data[512 * 1, 512])
  end

  def test_write_data
    @dummyos.reset
    @os.add_file_simple("lib/foo/bar", :mode => 0644, :size => 10) do |f|
      f.write @data
    end
    @os.flush
    assert_equal(@data + ("\0" * (512-@data.size)),
    @dummyos.data[512, 512])
  end

  def test_file_size_is_checked
    @dummyos.reset
    assert_raises(Archive::Tar::Minitar::Writer::BoundedStream::FileOverflow) do 
      @os.add_file_simple("lib/foo/bar", :mode => 0644, :size => 10) do |f|
        f.write "1" * 100
      end
    end
    assert_nothing_raised do
      @os.add_file_simple("lib/foo/bar", :mode => 0644, :size => 10) {|f| }
    end
  end
end

class TC_Tar__Reader < Test::Unit::TestCase
  include Archive::Tar::Minitar
  include TarTester

  def setup
  end

  def teardown
  end

  def test_multiple_entries
    str = tar_file_header("lib/foo", "", 010644, 10) + "\0" * 512
    str += tar_file_header("bar", "baz", 0644, 0)
    str += tar_dir_header("foo", "bar", 012345)
    str += "\0" * 1024
    names = %w[lib/foo bar foo]
    prefixes = ["", "baz", "bar"]
    modes = [010644, 0644, 012345]
    sizes = [10, 0, 0]
    isdir = [false, false, true]
    isfile = [true, true, false]
    Reader.new(StringIO.new(str)) do |is|
      i = 0
      is.each_entry do |entry|
        assert_kind_of(Reader::EntryStream, entry)
        assert_equal(names[i], entry.name)
        assert_equal(prefixes[i], entry.prefix)
        assert_equal(sizes[i], entry.size)
        assert_equal(modes[i], entry.mode)
        assert_equal(isdir[i], entry.directory?)
        assert_equal(isfile[i], entry.file?)
        if prefixes[i] != ""
          assert_equal(File.join(prefixes[i], names[i]), entry.full_name)
        else
          assert_equal(names[i], entry.name)
        end
        i += 1
      end
      assert_equal(names.size, i) 
    end
  end

  def test_rewind_entry_works
    content = ('a'..'z').to_a.join(" ")
    str = tar_file_header("lib/foo", "", 010644, content.size) + content + 
    "\0" * (512 - content.size)
    str << "\0" * 1024
    Reader.new(StringIO.new(str)) do |is|
      is.each_entry do |entry|
        3.times do 
          entry.rewind
          assert_equal(content, entry.read)
          assert_equal(content.size, entry.pos)
        end
      end
    end
  end

  def test_rewind_works
    content = ('a'..'z').to_a.join(" ")
    str = tar_file_header("lib/foo", "", 010644, content.size) + content + 
    "\0" * (512 - content.size)
    str << "\0" * 1024
    Reader.new(StringIO.new(str)) do |is|
      3.times do
        is.rewind
        i = 0
        is.each_entry do |entry|
          assert_equal(content, entry.read)
          i += 1
        end
        assert_equal(1, i)
      end
    end
  end

  def test_read_works
    contents = ('a'..'z').inject(""){|s, x| s << x * 100}
    str = tar_file_header("lib/foo", "", 010644, contents.size) + contents 
    str += "\0" * (512 - (str.size % 512))
    Reader.new(StringIO.new(str)) do |is|
      is.each_entry do |entry|
        assert_kind_of(Reader::EntryStream, entry)
        data = entry.read(3000) # bigger than contents.size
        assert_equal(contents, data)
        assert_equal(true, entry.eof?)
      end
    end
    Reader.new(StringIO.new(str)) do |is|
      is.each_entry do |entry|
        assert_kind_of(Reader::EntryStream, entry)
        data = entry.read(100)
        (entry.size - data.size).times {|i| data << entry.getc.chr }
        assert_equal(contents, data)
        assert_equal(nil, entry.read(10))
        assert_equal(true, entry.eof?)
      end
    end
    Reader.new(StringIO.new(str)) do |is|
      is.each_entry do |entry|
        assert_kind_of(Reader::EntryStream, entry)
        data = entry.read
        assert_equal(contents, data)
        assert_equal(nil, entry.read(10))
        assert_equal(nil, entry.read)
        assert_equal(nil, entry.getc)
        assert_equal(true, entry.eof?)
      end
    end
  end

  def test_eof_works
    str = tar_file_header("bar", "baz", 0644, 0)
    Reader.new(StringIO.new(str)) do |is|
      is.each_entry do |entry|
        assert_kind_of(Reader::EntryStream, entry)
        data = entry.read
        assert_equal(nil, data)
        assert_equal(nil, entry.read(10))
        assert_equal(nil, entry.read)
        assert_equal(nil, entry.getc)
        assert_equal(true, entry.eof?)
      end
    end
    str = tar_dir_header("foo", "bar", 012345)
    Reader.new(StringIO.new(str)) do |is|
      is.each_entry do |entry|
        assert_kind_of(Reader::EntryStream, entry)
        data = entry.read
        assert_equal(nil, data)
        assert_equal(nil, entry.read(10))
        assert_equal(nil, entry.read)
        assert_equal(nil, entry.getc)
        assert_equal(true, entry.eof?)
      end
    end
    str = tar_dir_header("foo", "bar", 012345)
    str += tar_file_header("bar", "baz", 0644, 0)
    str += tar_file_header("bar", "baz", 0644, 0)
    Reader.new(StringIO.new(str)) do |is|
      is.each_entry do |entry|
        assert_kind_of(Reader::EntryStream, entry)
        data = entry.read
        assert_equal(nil, data)
        assert_equal(nil, entry.read(10))
        assert_equal(nil, entry.read)
        assert_equal(nil, entry.getc)
        assert_equal(true, entry.eof?)
      end
    end
  end
end

class TC_Tar__Input < Test::Unit::TestCase
  include Archive::Tar::Minitar
  include TarTester

  require 'rbconfig'

  TEST_TGZ = "\037\213\010\000\001B1A\000\vKI,I\324+I,\322K\257b\240\0250\000\002sSS\254\342 `dj\306``nnnbndbjd\000\0247336`P0\240\231\213\220@i1\320\367@+\351a\327 \004\362\335\034\f\313\034\r\035\031\270\337Ns\344b2\344q\335\375M\304\266QM1W\357\321>\221U\021\005\246\306\367\356\367u3\262;\212\004\265\236\\\334}\351,\377\037;\217\223\301e\247\030\024\\\236\211\277\347\346sii\265\010\330\355\234\240\362\274\371[\202\361\366\302S\316\335o&~m\237r\355\377\303\230\365\352WNW\334\266_\373\273\237\347Q\315t?\263{\377?\006\271\337?\367\207\325\346]\371\376y\307_\234~d\3772\265\346\261}\323\317\373\315\352\377O\376\271/\305\377?X\253\324\303S\373\361\347\277\372^)\267\377\363\03460\331\311\\wW|\031\203\300@\207\325p\004i\2319\251\3064\266\203P\376702B\313\377\246\246\006&\243\371\237\036 $#\263X\001\210@\351@\301XO\201\227k\240]4\nF\301(\030\005\243\200\036\000\000\004\330t\023\000\f\000\000"
  TEST_LONG_ENTRY_TGZ = "\037\213\b\000\022LAL\000\003\355\323Kn\3020\020\006`\2579EN\020l\3428\333\036 w\210\fq!\305\261\301\017H8=\345QJ\027\024\251*\251\250\376o1\036yF\262ek\322q:~)\255\231\227\215Y\222\307\240'\267V\232\361\374\222\037\367\031\313EA\222\362A\367\371\"\372 ]\222\020gm\370\256\357^\375I\371\270R\256\252\217\361:W]p\322\272\2721\322\365\225SR\353\276\332(w\016>Nu\323\352KENg\265z\235/\232\267\245n\215]\255\235\017q\263\355\372]\025\224\017\237a\350\363\322\320\375\307o\3735C\374\307a\250\005\347\207\031gEN\257\3273A\030\343\023:\341y\221\275\367e\2023A\022:\304\003|\314\177\260Q\267J\232[}\367\352Oj\241\264\266\243\277\276\005\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\374\324\036\373\275\021\004\000(\000\000"
  FILETIMES = Time.utc(2004,"jan",1,5,0,0).to_i

  TEST_CONTENTS = [
    [ "data.tar.gz", 174, 0755 ],
    [ "file3",        18, 0755 ],
  ]

  TEST_DATA_CONTENTS = [
    [ "data",          0, 040755 ],
    [ "data/file1",   16, 010644 ],
    [ "data/file2",   16, 010644 ],
    [ "data/__dir__",  0, 010644 ],
  ]

  def setup
    FileUtils.mkdir_p("data__")
  end

  def teardown
    FileUtils.rm_rf("data__")
  end

  def test_each_works
    gzr = Zlib::GzipReader.new(StringIO.new(TEST_TGZ))
    Input.open(gzr) do |is|
      ii = 0
      is.each_with_index do |entry, ii|
        assert_kind_of(Reader::EntryStream, entry)
        assert_equal(TEST_CONTENTS[ii][0], entry.name)
        assert_equal(TEST_CONTENTS[ii][1], entry.size)
        assert_equal(TEST_CONTENTS[ii][2], entry.mode)
        assert_equal(FILETIMES, entry.mtime)

        if 0 == ii
          gzr2 = Zlib::GzipReader.new(StringIO.new(entry.read))
          Input.open(gzr2) do |is2|
            jj = 0
            is2.each_with_index do |entry2, jj|
              assert_kind_of(Reader::EntryStream, entry2)
              assert_equal(TEST_DATA_CONTENTS[jj][0], entry2.name)
              assert_equal(TEST_DATA_CONTENTS[jj][1], entry2.size)
              assert_equal(TEST_DATA_CONTENTS[jj][2], entry2.mode)
              assert_equal(FILETIMES, entry2.mtime)
            end
            assert_equal(3, jj)
          end
        end
      end
      assert_equal(1, ii)
    end
  end

  def test_extract_entry_works
    gzr = Zlib::GzipReader.new(StringIO.new(TEST_TGZ))
    Input.open(gzr) do |is|
      ii = 0
      is.each_with_index do |entry, ii|
        is.extract_entry("data__", entry)
        name = File.join("data__", entry.name)

        if entry.directory?
          assert(File.directory?(name))
        else
          assert(File.file?(name))

          assert_equal(TEST_CONTENTS[ii][1], File.stat(name).size)
        end
        assert_equal(TEST_CONTENTS[ii][2], File.stat(name).mode & 0777) unless RUBY_PLATFORM =~ /win32/

        if 0 == ii
        begin
          ff    = File.open(name, "rb")
          gzr2  = Zlib::GzipReader.new(ff)
          Input.open(gzr2) do |is2|
            jj = 0
            is2.each_with_index do |entry2, jj|
              is2.extract_entry("data__", entry2)
              name2 = File.join("data__", entry2.name)

              if entry2.directory?
                assert(File.directory?(name2))
              else
                assert(File.file?(name2))
                assert_equal(TEST_DATA_CONTENTS[jj][1], File.stat(name2).size, name2)
              end
              assert_equal(TEST_DATA_CONTENTS[jj][2], File.stat(name2).mode, name2) unless RUBY_PLATFORM =~ /win32/
            end
          end
        ensure
          ff.close unless ff.closed?
        end
        end
      end
      assert_equal(1, ii)
    end
  end
  
  def test_extract_long_file_name_entry_works
     gzr = Zlib::GzipReader.new(StringIO.new(TEST_LONG_ENTRY_TGZ))
     Input.open(gzr) do |is|
       ii = 0
       is.each_with_index do |entry, ii|
         assert_equal("super_duper_super_duper_extraordinary_really_very_very_sublimly_really_abcdefghijklmnopqrstuvwxyz_test_test_testsuper_duper_super_duper_extraordinary_really_very_very_sublimly_really_abcdefghijklmnopqrstuvwxyz_test_test_test.txt", entry.name)
         is.extract_entry("data__", entry)
         name = File.join("data__", entry.name)
         assert(File.file?(name))
       end
       assert_equal(0, ii)
     end
  end
end

class TC_Tar__Output < Test::Unit::TestCase
  include Archive::Tar::Minitar
  include TarTester

  def setup
    FileUtils.mkdir_p("data__")
    %w(a b c).each do |filename|
      name = File.join("data__", filename)
      File.open(name, "wb") { |f| f.puts "#{name}: 123456789012345678901234567890" }
    end
    @tarfile = "data__/bla2.tar"
  end

  def teardown
    FileUtils.rm_rf("data__")
  end

  def test_file_looks_good
    Output.open(@tarfile) do |os|
      Dir.chdir("data__") do
        %w(a b c).each do |name|
          stat = File.stat(name)
          opts = { :size => stat.size, :mode => 0644 }
          os.tar.add_file_simple(name, opts) do |ss|
            File.open(name, "rb") { |ff| ss.write(ff.read(4096)) until ff.eof? }
          end
        end
      end
    end
    ff = File.open(@tarfile, "rb")
    Reader.open(ff) do |is|
      ii = 0
      is.each do |entry|
        case ii
        when 0
          assert_equal("a", entry.name)
        when 1
          assert_equal("b", entry.name)
        when 2
          assert_equal("c", entry.name)
        end
        ii += 1
      end
      assert_equal(3, ii)
    end
  ensure
    ff.close if ff
  end
end
