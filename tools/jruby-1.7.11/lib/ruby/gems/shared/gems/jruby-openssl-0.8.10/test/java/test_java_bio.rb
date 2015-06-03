module PKCS7Test
  class TestJavaBIO < Test::Unit::TestCase
    def test_string_bio_simple
      bio = BIO::from_string("abc")
      arr = Java::byte[20].new
      read = bio.gets(arr, 10)
      assert_equal 3, read
      assert_equal "abc".to_java_bytes.to_a, arr.to_a[0...read]
    end

    def test_string_bio_simple_with_newline
      bio = BIO::from_string("abc\n")
      arr = Java::byte[20].new
      read = bio.gets(arr, 10)
      assert_equal 4, read
      assert_equal "abc\n".to_java_bytes.to_a, arr.to_a[0...read]
    end

    def test_string_bio_simple_with_newline_and_more_data
      bio = BIO::from_string("abc\nfoo\n\nbar")
      arr = Java::byte[20].new
      read = bio.gets(arr, 10)
      assert_equal 4, read
      assert_equal "abc\n".to_java_bytes.to_a, arr.to_a[0...read]

      read = bio.gets(arr, 10)
      assert_equal 4, read
      assert_equal "foo\n".to_java_bytes.to_a, arr.to_a[0...read]
    
      read = bio.gets(arr, 10)
      assert_equal 1, read
      assert_equal "\n".to_java_bytes.to_a, arr.to_a[0...read]

      read = bio.gets(arr, 10)
      assert_equal 3, read
      assert_equal "bar".to_java_bytes.to_a, arr.to_a[0...read]

      read = bio.gets(arr, 10)
      assert_equal 0, read
    end
  end
end
