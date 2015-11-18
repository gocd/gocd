# encoding: ASCII-8BIT

require 'common'
require 'net/ssh/buffer'

class TestBuffer < Test::Unit::TestCase
  def test_constructor_should_initialize_buffer_to_empty_by_default
    buffer = new
    assert buffer.empty?
    assert_equal 0, buffer.position
  end

  def test_constructor_with_string_should_initialize_buffer_to_the_string
    buffer = new("hello")
    assert !buffer.empty?
    assert_equal "hello", buffer.to_s
    assert_equal 0, buffer.position
  end

  def test_from_should_require_an_even_number_of_arguments
    assert_raises(ArgumentError) { Net::SSH::Buffer.from("this") }
  end

  def test_from_should_build_new_buffer_from_definition
    buffer = Net::SSH::Buffer.from(:byte, 1, :long, 2, :int64, 3, :string, "4", :bool, true, :bool, false, :bignum, OpenSSL::BN.new("1234567890", 10), :raw, "something")
    assert_equal "\1\0\0\0\2\0\0\0\0\0\0\0\3\0\0\0\0014\1\0\000\000\000\004I\226\002\322something", buffer.to_s
  end

  def test_from_with_array_argument_should_write_multiple_of_the_given_type
    buffer = Net::SSH::Buffer.from(:byte, [1,2,3,4,5])
    assert_equal "\1\2\3\4\5", buffer.to_s
  end

  def test_from_should_measure_bytesize_of_utf_8_string_correctly
    buffer = Net::SSH::Buffer.from(:string, "\u2603") # Snowman is 3 bytes
    assert_equal "\0\0\0\3\u2603", buffer.to_s
  end

  def test_read_without_argument_should_read_to_end
    buffer = new("hello world")
    assert_equal "hello world", buffer.read
    assert buffer.eof?
    assert_equal 11, buffer.position
  end

  def test_read_with_argument_that_is_less_than_length_should_read_that_many_bytes
    buffer = new "hello world"
    assert_equal "hello", buffer.read(5)
    assert_equal 5, buffer.position
  end

  def test_read_with_argument_that_is_more_than_length_should_read_no_more_than_length
    buffer = new "hello world"
    assert_equal "hello world", buffer.read(500)
    assert_equal 11, buffer.position
  end

  def test_read_at_eof_should_return_empty_string
    buffer = new "hello"
    buffer.position = 5
    assert_equal "", buffer.read
  end

  def test_consume_without_argument_should_resize_buffer_to_start_at_position
    buffer = new "hello world"
    buffer.read(5)
    assert_equal 5, buffer.position
    assert_equal 11, buffer.length
    buffer.consume!
    assert_equal 0, buffer.position
    assert_equal 6, buffer.length
    assert_equal " world", buffer.to_s
  end

  def test_consume_with_argument_should_resize_buffer_starting_at_n
    buffer = new "hello world"
    assert_equal 0, buffer.position
    buffer.consume!(5)
    assert_equal 0, buffer.position
    assert_equal 6, buffer.length
    assert_equal " world", buffer.to_s
  end

  def test_read_bang_should_read_and_consume_and_return_read_portion
    buffer = new "hello world"
    assert_equal "hello", buffer.read!(5)
    assert_equal 0, buffer.position
    assert_equal 6, buffer.length
    assert_equal " world", buffer.to_s
  end

  def test_available_should_return_length_after_position_to_end_of_string
    buffer = new "hello world"
    buffer.read(5)
    assert_equal 6, buffer.available
  end

  def test_clear_bang_should_reset_buffer_contents_and_counters
    buffer = new "hello world"
    buffer.read(5)
    buffer.clear!
    assert_equal 0, buffer.length
    assert_equal 0, buffer.position
    assert_equal "", buffer.to_s
  end

  def test_append_should_append_argument_without_changing_position_and_should_return_self
    buffer = new "hello world"
    buffer.read(5)
    buffer.append(" again")
    assert_equal 5, buffer.position
    assert_equal 12, buffer.available
    assert_equal 17, buffer.length
    assert_equal "hello world again", buffer.to_s
  end

  def test_remainder_as_buffer_should_return_a_new_buffer_filled_with_the_text_after_the_current_position
    buffer = new "hello world"
    buffer.read(6)
    b2 = buffer.remainder_as_buffer
    assert_equal 6, buffer.position
    assert_equal 0, b2.position
    assert_equal "world", b2.to_s
  end

  def test_read_int64_should_return_8_byte_integer
    buffer = new "\xff\xee\xdd\xcc\xbb\xaa\x99\x88"
    assert_equal 0xffeeddccbbaa9988, buffer.read_int64
    assert_equal 8, buffer.position
  end

  def test_read_int64_should_return_nil_on_partial_read
    buffer = new "\0\0\0\0\0\0\0"
    assert_nil buffer.read_int64
    assert buffer.eof?
  end

  def test_read_long_should_return_4_byte_integer
    buffer = new "\xff\xee\xdd\xcc\xbb\xaa\x99\x88"
    assert_equal 0xffeeddcc, buffer.read_long
    assert_equal 4, buffer.position
  end

  def test_read_long_should_return_nil_on_partial_read
    buffer = new "\0\0\0"
    assert_nil buffer.read_long
    assert buffer.eof?
  end

  def test_read_byte_should_return_single_byte_integer
    buffer = new "\xfe\xdc"
    assert_equal 0xfe, buffer.read_byte
    assert_equal 1, buffer.position
  end

  def test_read_byte_should_return_nil_at_eof
    assert_nil new.read_byte
  end

  def test_read_string_should_read_length_and_data_from_buffer
    buffer = new "\0\0\0\x0bhello world"
    assert_equal "hello world", buffer.read_string
  end

  def test_read_string_should_return_nil_if_4_byte_length_cannot_be_read
    assert_nil new("\0\1").read_string
  end

  def test_read_bool_should_return_true_if_non_zero_byte_is_read
    buffer = new "\1\2\3\4\5\6"
    6.times { assert_equal true, buffer.read_bool }
  end

  def test_read_bool_should_return_false_if_zero_byte_is_read
    buffer = new "\0"
    assert_equal false, buffer.read_bool
  end

  def test_read_bool_should_return_nil_at_eof
    assert_nil new.read_bool
  end

  def test_read_bignum_should_read_openssl_formatted_bignum
    buffer = new("\000\000\000\004I\226\002\322")
    assert_equal OpenSSL::BN.new("1234567890", 10), buffer.read_bignum
  end

  def test_read_bignum_should_return_nil_if_length_cannot_be_read
    assert_nil new("\0\1\2").read_bignum
  end

  def test_read_key_blob_should_read_dsa_keys
    random_dss { |buffer| buffer.read_keyblob("ssh-dss") }
  end

  def test_read_key_blob_should_read_rsa_keys
    random_rsa { |buffer| buffer.read_keyblob("ssh-rsa") }
  end

  def test_read_key_should_read_dsa_key_type_and_keyblob
    random_dss do |buffer|
      b2 = Net::SSH::Buffer.from(:string, "ssh-dss", :raw, buffer)
      b2.read_key
    end
  end

  def test_read_key_should_read_rsa_key_type_and_keyblob
    random_rsa do |buffer|
      b2 = Net::SSH::Buffer.from(:string, "ssh-rsa", :raw, buffer)
      b2.read_key
    end
  end

  def test_read_buffer_should_read_a_string_and_return_it_wrapped_in_a_buffer
    buffer = new("\0\0\0\x0bhello world")
    b2 = buffer.read_buffer
    assert_equal 0, b2.position
    assert_equal 11, b2.length
    assert_equal "hello world", b2.read
  end

  def test_read_to_should_return_nil_if_pattern_does_not_exist_in_buffer
    buffer = new("one two three")
    assert_nil buffer.read_to("\n")
  end

  def test_read_to_should_grok_string_patterns
    buffer = new("one two three")
    assert_equal "one tw", buffer.read_to("tw")
    assert_equal 6, buffer.position
  end

  def test_read_to_should_grok_regex_patterns
    buffer = new("one two three")
    assert_equal "one tw", buffer.read_to(/tw/)
    assert_equal 6, buffer.position
  end

  def test_read_to_should_grok_fixnum_patterns
    buffer = new("one two three")
    assert_equal "one tw", buffer.read_to(?w)
    assert_equal 6, buffer.position
  end

  def test_reset_bang_should_reset_position_to_0
    buffer = new("hello world")
    buffer.read(5)
    assert_equal 5, buffer.position
    buffer.reset!
    assert_equal 0, buffer.position
  end

  def test_write_should_write_arguments_directly_to_end_buffer
    buffer = new("start")
    buffer.write "hello", " ", "world"
    assert_equal "starthello world", buffer.to_s
    assert_equal 0, buffer.position
  end

  def test_write_int64_should_write_arguments_as_8_byte_integers_to_end_of_buffer
    buffer = new("start")
    buffer.write_int64 0xffeeddccbbaa9988, 0x7766554433221100
    assert_equal "start\xff\xee\xdd\xcc\xbb\xaa\x99\x88\x77\x66\x55\x44\x33\x22\x11\x00", buffer.to_s
  end

  def test_write_long_should_write_arguments_as_4_byte_integers_to_end_of_buffer
    buffer = new("start")
    buffer.write_long 0xffeeddcc, 0xbbaa9988
    assert_equal "start\xff\xee\xdd\xcc\xbb\xaa\x99\x88", buffer.to_s
  end

  def test_write_byte_should_write_arguments_as_1_byte_integers_to_end_of_buffer
    buffer = new("start")
    buffer.write_byte 1, 2, 3, 4, 5
    assert_equal "start\1\2\3\4\5", buffer.to_s
  end

  def test_write_bool_should_write_arguments_as_1_byte_boolean_values_to_end_of_buffer
    buffer = new("start")
    buffer.write_bool nil, false, true, 1, Object.new
    assert_equal "start\0\0\1\1\1", buffer.to_s
  end

  def test_write_bignum_should_write_arguments_as_ssh_formatted_bignum_values_to_end_of_buffer
    buffer = new("start")
    buffer.write_bignum OpenSSL::BN.new('1234567890', 10)
    assert_equal "start\000\000\000\004I\226\002\322", buffer.to_s
  end

  def test_write_dss_key_should_write_argument_to_end_of_buffer
    buffer = new("start")

    key = OpenSSL::PKey::DSA.new
    key.p = 0xffeeddccbbaa9988
    key.q = 0x7766554433221100
    key.g = 0xffddbb9977553311
    key.pub_key = 0xeeccaa8866442200

    buffer.write_key(key)
assert_equal "start\0\0\0\7ssh-dss\0\0\0\011\0\xff\xee\xdd\xcc\xbb\xaa\x99\x88\0\0\0\010\x77\x66\x55\x44\x33\x22\x11\x00\0\0\0\011\0\xff\xdd\xbb\x99\x77\x55\x33\x11\0\0\0\011\0\xee\xcc\xaa\x88\x66\x44\x22\x00", buffer.to_s
  end

  def test_write_rsa_key_should_write_argument_to_end_of_buffer
    buffer = new("start")

    key = OpenSSL::PKey::RSA.new
    key.e = 0xffeeddccbbaa9988
    key.n = 0x7766554433221100

    buffer.write_key(key)
    assert_equal "start\0\0\0\7ssh-rsa\0\0\0\011\0\xff\xee\xdd\xcc\xbb\xaa\x99\x88\0\0\0\010\x77\x66\x55\x44\x33\x22\x11\x00", buffer.to_s
  end

  if defined?(OpenSSL::PKey::EC)
    def test_read_key_blob_should_read_ecdsa_sha2_nistp256_keys
      random_ecdsa_sha2_nistp256 { |buffer|
        buffer.read_keyblob("ecdsa-sha2-nistp256")
      }
    end
    def test_read_key_blob_should_read_ecdsa_sha2_nistp384_keys
      random_ecdsa_sha2_nistp384 { |buffer|
        buffer.read_keyblob("ecdsa-sha2-nistp384")
      }
    end
    def test_read_key_blob_should_read_ecdsa_sha2_nistp521_keys
      random_ecdsa_sha2_nistp521 { |buffer|
        buffer.read_keyblob("ecdsa-sha2-nistp521")
      }
    end

    def test_read_key_should_read_ecdsa_sha2_nistp256_key_type_and_keyblob
      random_ecdsa_sha2_nistp256 do |buffer|
        b2 = Net::SSH::Buffer.from(:string, "ecdsa-sha2-nistp256", :raw, buffer)
        b2.read_key
      end
    end
    def test_read_key_should_read_ecdsa_sha2_nistp384_key_type_and_keyblob
      random_ecdsa_sha2_nistp384 do |buffer|
        b2 = Net::SSH::Buffer.from(:string, "ecdsa-sha2-nistp384", :raw, buffer)
        b2.read_key
      end
    end
    def test_read_key_should_read_ecdsa_sha2_nistp521_key_type_and_keyblob
      random_ecdsa_sha2_nistp521 do |buffer|
        b2 = Net::SSH::Buffer.from(:string, "ecdsa-sha2-nistp521", :raw, buffer)
        b2.read_key
      end
    end

    def test_write_ecdsa_sha2_nistp256_key_should_write_argument_to_end_of_buffer
      buffer = new("start")
      key = OpenSSL::PKey::EC.new("-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIISGj5vAJCWt2KPI8NwaWVDSNLl2vbRxDIOkY+n6O0VVoAoGCCqGSM49\nAwEHoUQDQgAEnKbs0yEogTKT4QRu8T9nb2svl2mEWXb6g224oCpD2o6TYNXNw54H\nmWkdCv+kFCqSlfSi5fqFhrXdfEY6zSzQYQ==\n-----END EC PRIVATE KEY-----\n")

      buffer.write_key(key)
      assert_equal "start\000\000\000\023ecdsa-sha2-nistp256\000\000\000\bnistp256\000\000\000A\004\234\246\354\323!(\2012\223\341\004n\361?gok/\227i\204Yv\372\203m\270\240*C\332\216\223`\325\315\303\236\a\231i\035\n\377\244\024*\222\225\364\242\345\372\205\206\265\335|F:\315,\320a", buffer.to_s
    end

    def test_write_ecdsa_sha2_nistp384_key_should_write_argument_to_end_of_buffer
      buffer = new("start")
      key = OpenSSL::PKey::EC.new("-----BEGIN EC PRIVATE KEY-----\nMIGkAgEBBDBAfxJpzhsR7O+wMol6BcDgualR8rJBvYegUDYbBUrDnPzDx2/gD1lZ\nnwG1FuD2s9igBwYFK4EEACKhZANiAATsfiU4Kxyvvj1DdvFYsdDnZIT7loRlan9I\n8geCWPPl6x7NFRP+awrnTaarMgieGqxG8IQaIA0SsDOICfbDBkuatRi0S1Et/in4\nZwVEZvO81Ro5YSrjuUDAsytnI6OXS28=\n-----END EC PRIVATE KEY-----\n")

      buffer.write_key(key)
      assert_equal "start\000\000\000\023ecdsa-sha2-nistp384\000\000\000\bnistp384\000\000\000a\004\354~%8+\034\257\276=Cv\361X\261\320\347d\204\373\226\204ej\177H\362\a\202X\363\345\353\036\315\025\023\376k\n\347M\246\2532\b\236\032\254F\360\204\032 \r\022\2603\210\t\366\303\006K\232\265\030\264KQ-\376)\370g\005Df\363\274\325\0329a*\343\271@\300\263+g#\243\227Ko", buffer.to_s
    end

    def test_write_ecdsa_sha2_nistp521_key_should_write_argument_to_end_of_buffer
      buffer = new("start")
      key = OpenSSL::PKey::EC.new("-----BEGIN EC PRIVATE KEY-----\nMIHbAgEBBEGhnQF/SFo4Vym88HnCfc6BR8WwYqDh9wNTPeqzR8auxIpp0GKQlCG2\nuHzyteJX5/YalV8empYhEzNmNLNn8x7j0aAHBgUrgQQAI6GBiQOBhgAEAYygOgV9\nVI8UyLQ3BDlv+rb3es+ufrIcj++cqcc9QcmRn237NiWRr/1NKy2AKijsEdACtZXo\nxPC0x9Vs9ieC2oR+ANOBubcxPl2giDnBYm8ywAmmlXsP5ByAM17k97CzW5O+Z/uO\nbxGUzzhoXTNcjqpAckhRVKdnh6FL/rKelT0tBYi+\n-----END EC PRIVATE KEY-----\n")

      buffer.write_key(key)
      assert_equal "start\000\000\000\023ecdsa-sha2-nistp521\000\000\000\bnistp521\000\000\000\205\004\001\214\240:\005}T\217\024\310\2647\0049o\372\266\367z\317\256~\262\034\217\357\234\251\307=A\311\221\237m\3736%\221\257\375M+-\200*(\354\021\320\002\265\225\350\304\360\264\307\325l\366'\202\332\204~\000\323\201\271\2671>]\240\2109\301bo2\300\t\246\225{\017\344\034\2003^\344\367\260\263[\223\276g\373\216o\021\224\3178h]3\\\216\252@rHQT\247g\207\241K\376\262\236\225=-\005\210\276", buffer.to_s
    end
  end

  private

    def random_rsa
      n1 = OpenSSL::BN.new(rand(0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF).to_s, 10)
      n2 = OpenSSL::BN.new(rand(0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF).to_s, 10)
      buffer = Net::SSH::Buffer.from(:bignum, [n1, n2])
      key = yield(buffer)
      assert_equal "ssh-rsa", key.ssh_type
      assert_equal n1, key.e
      assert_equal n2, key.n
    end

    def random_dss
      n1 = OpenSSL::BN.new(rand(0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF).to_s, 10)
      n2 = OpenSSL::BN.new(rand(0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF).to_s, 10)
      n3 = OpenSSL::BN.new(rand(0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF).to_s, 10)
      n4 = OpenSSL::BN.new(rand(0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF).to_s, 10)
      buffer = Net::SSH::Buffer.from(:bignum, [n1, n2, n3, n4])
      key = yield(buffer)
      assert_equal "ssh-dss", key.ssh_type
      assert_equal n1, key.p
      assert_equal n2, key.q
      assert_equal n3, key.g
      assert_equal n4, key.pub_key
    end

    if defined?(OpenSSL::PKey::EC)
      def random_ecdsa_sha2_nistp256
        k = OpenSSL::PKey::EC.new("prime256v1").generate_key
        buffer = Net::SSH::Buffer.from(:string, "nistp256",
                                       :string, k.public_key.to_bn.to_s(2))
        key = yield(buffer)
        assert_equal "ecdsa-sha2-nistp256", key.ssh_type
        assert_equal k.public_key, key.public_key
      end

      def random_ecdsa_sha2_nistp384
        k = OpenSSL::PKey::EC.new("secp384r1").generate_key
        buffer = Net::SSH::Buffer.from(:string, "nistp384",
                                       :string, k.public_key.to_bn.to_s(2))
        key = yield(buffer)
        assert_equal "ecdsa-sha2-nistp384", key.ssh_type
        assert_equal k.public_key, key.public_key
      end

      def random_ecdsa_sha2_nistp521
        k = OpenSSL::PKey::EC.new("secp521r1").generate_key
        buffer = Net::SSH::Buffer.from(:string, "nistp521",
                                       :string, k.public_key.to_bn.to_s(2))
        key = yield(buffer)
        assert_equal "ecdsa-sha2-nistp521", key.ssh_type
        assert_equal k.public_key, key.public_key
      end
    end

    def new(*args)
      Net::SSH::Buffer.new(*args)
    end
end
