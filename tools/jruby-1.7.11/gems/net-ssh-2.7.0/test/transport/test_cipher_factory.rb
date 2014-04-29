# encoding: ASCII-8BIT

require 'common'
require 'net/ssh/transport/cipher_factory'

module Transport

  class TestCipherFactory < Test::Unit::TestCase
    def self.if_supported?(name)
      yield if Net::SSH::Transport::CipherFactory.supported?(name)
    end

    def test_lengths_for_none
      assert_equal [0,0], factory.get_lengths("none")
      assert_equal [0,0], factory.get_lengths("bogus")
    end

    def test_lengths_for_blowfish_cbc
      assert_equal [16,8], factory.get_lengths("blowfish-cbc")
    end

    if_supported?("idea-cbc") do
      def test_lengths_for_idea_cbc
        assert_equal [16,8], factory.get_lengths("idea-cbc")
      end
    end

    def test_lengths_for_rijndael_cbc
      assert_equal [32,16], factory.get_lengths("rijndael-cbc@lysator.liu.se")
    end

    def test_lengths_for_cast128_cbc
      assert_equal [16,8], factory.get_lengths("cast128-cbc")
    end

    def test_lengths_for_3des_cbc
      assert_equal [24,8], factory.get_lengths("3des-cbc")
    end

    def test_lengths_for_aes128_cbc
      assert_equal [16,16], factory.get_lengths("aes128-cbc")
    end

    def test_lengths_for_aes192_cbc
      assert_equal [24,16], factory.get_lengths("aes192-cbc")
    end

    def test_lengths_for_aes256_cbc
      assert_equal [32,16], factory.get_lengths("aes256-cbc")
    end

    def test_lengths_for_arcfour
      assert_equal [16,8], factory.get_lengths("arcfour")
    end

    def test_lengths_for_arcfour128
      assert_equal [16,8], factory.get_lengths("arcfour128")
    end
    
    def test_lengths_for_arcfour256
      assert_equal [32,8], factory.get_lengths("arcfour256")
    end
    
    def test_lengths_for_arcfour512
      assert_equal [64,8], factory.get_lengths("arcfour512")
    end

    if_supported?("camellia128-cbc@openssh.org") do
      def test_lengths_for_camellia128_cbc_openssh_org
        assert_equal [16,16], factory.get_lengths("camellia128-cbc@openssh.org")
      end
    end

    if_supported?("camellia192-cbc@openssh.org") do
      def test_lengths_for_camellia192_cbc_openssh_org
        assert_equal [24,16], factory.get_lengths("camellia192-cbc@openssh.org")
      end
    end

    if_supported?("camellia256-cbc@openssh.org") do
      def test_lengths_for_camellia256_cbc_openssh_org
        assert_equal [32,16], factory.get_lengths("camellia256-cbc@openssh.org")
      end
    end

    def test_lengths_for_3des_ctr
      assert_equal [24,8], factory.get_lengths("3des-ctr")
    end

    def test_lengths_for_aes128_ctr
      assert_equal [16,16], factory.get_lengths("aes128-ctr")
    end

    def test_lengths_for_aes192_ctr
      assert_equal [24,16], factory.get_lengths("aes192-ctr")
    end

    def test_lengths_for_aes256_ctr
      assert_equal [32,16], factory.get_lengths("aes256-ctr")
    end

    def test_lengths_for_blowfish_ctr
      assert_equal [16,8], factory.get_lengths("blowfish-ctr")
    end

    def test_lengths_for_cast128_ctr
      assert_equal [16,8], factory.get_lengths("cast128-ctr")
    end

    if_supported?("camellia128-ctr@openssh.org") do
      def test_lengths_for_camellia128_ctr_openssh_org
        assert_equal [16,16], factory.get_lengths("camellia128-ctr@openssh.org")
      end
    end

    if_supported?("camellia192-ctr@openssh.org") do
      def test_lengths_for_camellia192_ctr_openssh_org
        assert_equal [24,16], factory.get_lengths("camellia192-ctr@openssh.org")
      end
    end

    if_supported?("camellia256-ctr@openssh.org") do
      def test_lengths_for_camellia256_ctr_openssh_org
        assert_equal [32,16], factory.get_lengths("camellia256-ctr@openssh.org")
      end
    end

    BLOWFISH_CBC = "\210\021\200\315\240_\026$\352\204g\233\244\242x\332e\370\001\327\224Nv@9_\323\037\252kb\037\036\237\375]\343/y\037\237\312Q\f7]\347Y\005\275%\377\0010$G\272\250B\265Nd\375\342\372\025r6}+Y\213y\n\237\267\\\374^\346BdJ$\353\220Ik\023<\236&H\277=\225"

    def test_blowfish_cbc_for_encryption
      assert_equal BLOWFISH_CBC, encrypt("blowfish-cbc")
    end

    def test_blowfish_cbc_for_decryption
      assert_equal TEXT, decrypt("blowfish-cbc", BLOWFISH_CBC)
    end

    if_supported?("idea-cbc") do
      IDEA_CBC = "W\234\017G\231\b\357\370H\b\256U]\343M\031k\233]~\023C\363\263\177\262-\261\341$\022\376mv\217\322\b\2763\270H\306\035\343z\313\312\3531\351\t\201\302U\022\360\300\354ul7$z\320O]\360g\024\305\005`V\005\335A\351\312\270c\320D\232\eQH1\340\265\2118\031g*\303v"

      def test_idea_cbc_for_encryption
        assert_equal IDEA_CBC, encrypt("idea-cbc")
      end

      def test_idea_cbc_for_decryption
        assert_equal TEXT, decrypt("idea-cbc", IDEA_CBC)
      end
    end

    RIJNDAEL = "$\253\271\255\005Z\354\336&\312\324\221\233\307Mj\315\360\310Fk\241EfN\037\231\213\361{'\310\204\347I\343\271\005\240`\325;\034\346uM>#\241\231C`\374\261\vo\226;Z\302:\b\250\366T\330\\#V\330\340\226\363\374!\bm\266\232\207!\232\347\340\t\307\370\356z\236\343=v\210\206y"

    def test_rijndael_cbc_for_encryption
      assert_equal RIJNDAEL, encrypt("rijndael-cbc@lysator.liu.se")
    end

    def test_rijndael_cbc_for_decryption
      assert_equal TEXT, decrypt("rijndael-cbc@lysator.liu.se", RIJNDAEL)
    end

    CAST128_CBC = "qW\302\331\333P\223t[9 ~(sg\322\271\227\272\022I\223\373p\255>k\326\314\260\2003\236C_W\211\227\373\205>\351\334\322\227\223\e\236\202Ii\032!P\214\035:\017\360h7D\371v\210\264\317\236a\262w1\2772\023\036\331\227\240:\f/X\351\324I\t[x\350\323E\2301\016m"

    def test_cast128_cbc_for_encryption
      assert_equal CAST128_CBC, encrypt("cast128-cbc")
    end

    def test_cast128_cbc_for_decryption
      assert_equal TEXT, decrypt("cast128-cbc", CAST128_CBC)
    end

    TRIPLE_DES_CBC = "\322\252\216D\303Q\375gg\367A{\177\313\3436\272\353%\223K?\257\206|\r&\353/%\340\336 \203E8rY\206\234\004\274\267\031\233T/{\"\227/B!i?[qGaw\306T\206\223\213n \212\032\244%]@\355\250\334\312\265E\251\017\361\270\357\230\274KP&^\031r+r%\370"

    def test_3des_cbc_for_encryption
      assert_equal TRIPLE_DES_CBC, encrypt("3des-cbc")
    end

    def test_3des_cbc_for_decryption
      assert_equal TEXT, decrypt("3des-cbc", TRIPLE_DES_CBC)
    end

    AES128_CBC = "k\026\350B\366-k\224\313\3277}B\035\004\200\035\r\233\024$\205\261\231Q\2214r\245\250\360\315\237\266hg\262C&+\321\346Pf\267v\376I\215P\327\345-\232&HK\375\326_\030<\a\276\212\303g\342C\242O\233\260\006\001a&V\345`\\T\e\236.\207\223l\233ri^\v\252\363\245"

    def test_aes128_cbc_for_encryption
      assert_equal AES128_CBC, encrypt("aes128-cbc")
    end

    def test_aes128_cbc_for_decryption
      assert_equal TEXT, decrypt("aes128-cbc", AES128_CBC)
    end

    AES192_CBC = "\256\017)x\270\213\336\303L\003f\235'jQ\3231k9\225\267\242\364C4\370\224\201\302~\217I\202\374\2167='\272\037\225\223\177Y\r\212\376(\275\n\3553\377\177\252C\254\236\016MA\274Z@H\331<\rL\317\205\323[\305X8\376\237=\374\352bH9\244\0231\353\204\352p\226\326~J\242"

    def test_aes192_cbc_for_encryption
      assert_equal AES192_CBC, encrypt("aes192-cbc")
    end

    def test_aes192_cbc_for_decryption
      assert_equal TEXT, decrypt("aes192-cbc", AES192_CBC)
    end

    AES256_CBC = "$\253\271\255\005Z\354\336&\312\324\221\233\307Mj\315\360\310Fk\241EfN\037\231\213\361{'\310\204\347I\343\271\005\240`\325;\034\346uM>#\241\231C`\374\261\vo\226;Z\302:\b\250\366T\330\\#V\330\340\226\363\374!\bm\266\232\207!\232\347\340\t\307\370\356z\236\343=v\210\206y"

    def test_aes256_cbc_for_encryption
      assert_equal AES256_CBC, encrypt("aes256-cbc")
    end

    def test_aes256_cbc_for_decryption
      assert_equal TEXT, decrypt("aes256-cbc", AES256_CBC)
    end

    ARCFOUR = "\xC1.\x1AdH\xD0+%\xF1CrG\x1C\xCC\xF6\xACho\xB0\x95\\\xBC\x02P\xF9\xAF\n\xBB<\x13\xF3\xCF\xEB\n\b(iO\xFB'\t^?\xA6\xE5a\xE2\x17\f\x97\xCAs\x9E\xFC\xF2\x88\xC93\v\x84\xCA\x82\x0E\x1D\x11\xEA\xE1\x82\x8E\xB3*\xC5\xFB\x8Cmgs\xB0\xFA\xF5\x9C\\\xE2\xB0\x95\x1F>LT"

    def test_arcfour_for_encryption
      assert_equal ARCFOUR, encrypt("arcfour")
    end

    def test_arcfour_for_decryption
      assert_equal TEXT, decrypt("arcfour", ARCFOUR)
    end
    
    ARCFOUR128 = "\n\x90\xED*\xD4\xBE\xCBg5\xA5\a\xEC]\x97\xB7L\x06)6\x12FL\x90@\xF4Sqxqh\r\x11\x1Aq \xC8\xE6v\xC6\x12\xD9<A\xDAZ\xFE\x7F\x88\x19f.\x06\xA7\xFE:\xFF\x93\x9B\x8D\xA0\\\x9E\xCA\x03\x15\xE1\xE2\f\xC0\b\xA2C\xE1\xBD\xB6\x13D\xD1\xB4'g\x89\xDC\xEB\f\x19Z)U"

    def test_arcfour128_for_encryption
      assert_equal ARCFOUR128, encrypt("arcfour128")
    end
    
    def test_arcfour128_for_decryption
      assert_equal TEXT, decrypt("arcfour128", ARCFOUR128)
    end
    
    ARCFOUR256 = "|g\xCCw\xF5\xC1y\xEB\xF0\v\xF7\x83\x14\x03\xC8\xAB\xE8\xC2\xFCY\xDC,\xB8\xD4dVa\x8B\x18%\xA4S\x00\xE0at\x86\xE8\xA6W\xAB\xD2\x9D\xA8\xDE[g\aZy.\xFB\xFC\x82c\x04h\f\xBFYq\xB7U\x80\x0EG\x91\x88\xDF\xA3\xA2\xFA(\xEC\xDB\xA4\xE7\xFE)\x12u\xAF\x0EZ\xA0\xBA\x97\n\xFC"

    def test_arcfour256_for_encryption
      assert_equal ARCFOUR256, encrypt("arcfour256")
    end
    
    def test_arcfour256_for_decryption
      assert_equal TEXT, decrypt("arcfour256", ARCFOUR256)
    end
    
    ARCFOUR512 = "|8\"v\xE7\xE3\b\xA8\x19\x9Aa\xB6Vv\x00\x11\x8A$C\xB6xE\xEF\xF1j\x90\xA8\xFA\x10\xE4\xA1b8\xF6\x04\xF2+\xC0\xD1(8\xEBT]\xB0\xF3/\xD9\xE0@\x83\a\x93\x9D\xCA\x04RXS\xB7A\x0Fj\x94\bE\xEB\x84j\xB4\xDF\nU\xF7\x83o\n\xE8\xF9\x01{jH\xEE\xCDQym\x9E"

    def test_arcfour512_for_encryption
      assert_equal ARCFOUR512, encrypt("arcfour512")
    end
    
    def test_arcfour512_for_decryption
      assert_equal TEXT, decrypt("arcfour512", ARCFOUR512)
    end

    if_supported?("camellia128-cbc@openssh.org") do
      CAMELLIA128_CBC = "\a\b\x83+\xF1\xC5m\a\xE1\xD3\x06\xD2NA\xC3l@\\*M\xFD\x96\xAE\xA8\xB4\xA9\xACm\"8\x8E\xEE<\xC3O[\rK\xFAgu}\xCD\xAC\xF4\x04o\xDB\x94-\xB8\"\xDC\xE7{y\xA9 \x8F=y\x85\x82v\xC8\xCA\x8A\xE9\xE3:\xC4,u=a/\xC0\x05\xDA\xDAk8g\xCB\xD9\xA8\xE6\xFE\xCE_\x8E\x97\xF0\xAC\xB6\xCE"
      def test_camellia128_cbc_for_encryption
        assert_equal CAMELLIA128_CBC, encrypt("camellia128-cbc@openssh.org")
      end
      def test_camellia128_cbc_for_decryption
        assert_equal TEXT, decrypt("camellia128-cbc@openssh.org", CAMELLIA128_CBC)
      end
    end

    if_supported?("camellia192-cbc@openssh.org") do
      CAMELLIA192_CBC = "\x82\xB2\x03\x90\xFA\f2\xA0\xE3\xFA\xF2B\xAB\xDBX\xD5\x04z\xD4G\x19\xB8\xAB\v\x85\x84\xCD:.\xBA\x9Dd\xD5(\xEB.\n\xAA]\xCB\xF3\x0F4\x8Bd\xF8m\xC9!\xE2\xA1=\xEBY\xA6\x83\x86\n\x13\e6\v\x06\xBBNJg\xF2-\x14',[\xC1\xB1.\x85\xF3\xC6\xBF\x1Ff\xCE\x87'\x9C\xB2\xC8!\xF3|\xE2\xD2\x9E\x96\xA1"
      def test_camellia192_cbc_for_encryption
        assert_equal CAMELLIA192_CBC, encrypt("camellia192-cbc@openssh.org")
      end
      def test_camellia192_cbc_for_decryption
        assert_equal TEXT, decrypt("camellia192-cbc@openssh.org", CAMELLIA192_CBC)
      end
    end

    if_supported?("camellia256-cbc@openssh.org") do
      CAMELLIA256_CBC = ",\x80J/\xF5\x8F\xFE4\xF0@\n[2\xFF4\xB6\xA4\xD0\xF8\xF5*\x17I\xF3\xA2\x1F$L\xC6\xA1\x06\xDC\x84f\x1C\x10&\x1C\xC4/R\x859|i\x85ZP\xC8\x94\xED\xE8-\n@ w\x92\xF7\xD4\xAB\xF0\x85c\xC1\x0F\x1E#\xEB\xE5W\x87N!\xC7'/\xE3E8$\x1D\x9B:\xC9\xAF_\x05\xAC%\xD7\x945\xBBDK"
      def test_camellia256_cbc_for_encryption
        assert_equal CAMELLIA256_CBC, encrypt("camellia256-cbc@openssh.org")
      end
      def test_camellia256_cbc_for_decryption
        assert_equal TEXT, decrypt("camellia256-cbc@openssh.org", CAMELLIA256_CBC)
      end
    end

    BLOWFISH_CTR = "\xF5\xA6\x1E{\x8F(\x85G\xFAh\xDB\x19\xDC\xDF\xA2\x9A\x99\xDD5\xFF\xEE\x8BE\xE6\xB5\x92\x82\xE80\x91\x11`\xEF\x10\xED\xE9\xD3\vG\x0E\xAF\xB2K\t\xA4\xA6\x05\xD1\x17\x0Fl\r@E\x8DJ\e\xE63\x04\xB5\x05\x99Y\xCC\xFBb\x8FK+\x8C1v\xE4N\b?B\x06Rz\xA6\xB6N/b\xCE}\x83\x8DY\xD7\x92qU\x0F"

    def test_blowfish_ctr_for_encryption
      assert_equal BLOWFISH_CTR, encrypt("blowfish-ctr")
    end

    def test_blowfish_ctr_for_decryption
      assert_equal TEXT, decrypt("blowfish-ctr", BLOWFISH_CTR)
    end

    CAST128_CTR = "\xB5\xBB\xC3h\x80\x90`{\xD7I\x03\xE9\x80\xC4\xC4U\xE3@\xF1\xE9\xEFX\xDB6\xEE,\x8E\xC2\xE8\x89\x17\xBArf\x81\r\x96\xDC\xB1_'\x83hs\t7\xB8@\x17\xAA\xD9;\xE8\x8E\x94\xBD\xFF\xA4K\xA4\xFA\x8F-\xCD\bO\xD9I`\xE5\xC9H\x99\x14\xC5K\xC8\xEF\xEA#\x1D\xE5\x13O\xE1^P\xDC\x1C^qm\v|c@"

    def test_cast128_ctr_for_encryption
      assert_equal CAST128_CTR, encrypt("cast128-ctr")
    end

    def test_cast128_ctr_for_decryption
      assert_equal TEXT, decrypt("cast128-ctr", CAST128_CTR)
    end

    TRIPLE_DES_CTR = "\x90\xCD\b\xD2\xF1\x15:\x98\xF4sJ\xF0\xC9\xAA\xC5\xE3\xB4\xCFq\x93\xBAB\xF9v\xE1\xE7\x8B<\xBC\x97R\xDF?kK~Nw\xF3\x92`\x90]\xD9\xEF\x16\xC85V\x03C\xE9\x14\xF0\x86\xEB\x19\x85\x82\xF6\x16gz\x9B`\xB1\xCE\x80&?\xC8\xBD\xBC+\x91/)\xA5x\xBB\xCF\x06\x15#\e\xB3\xBD\x9B\x1F\xA7\xE2\xC7\xA3\xFC\x06\xC8"

    def test_3des_ctr_for_encryption
      if defined?(JRUBY_VERSION)
        # on JRuby, this test fails due to JRUBY-6558
        puts "Skipping 3des-ctr tests for JRuby"
      else
        assert_equal TRIPLE_DES_CTR, encrypt("3des-ctr")
      end
    end

    def test_3des_ctr_for_decryption
      if defined?(JRUBY_VERSION)
        # on JRuby, this test fails due to JRUBY-6558
        puts "Skipping 3des-ctr tests for JRuby"
      else
        assert_equal TEXT, decrypt("3des-ctr", TRIPLE_DES_CTR)
      end
    end

    AES128_CTR = "\x9D\xC7]R\x89\x01\xC4\x14\x00\xE7\xCEc`\x80\v\xC7\xF7\xBD\xD5#d\f\xC9\xB0\xDE\xA6\x8Aq\x10p\x8F\xBC\xFF\x8B\xB4\xC5\xB3\xF7,\xF7eO\x06Q]\x0F\x05\x86\xEC\xA6\xC8\x12\xE9\xC4\x9D0\xD3\x9AL\x192\xAA\xDFu\x0E\xECz\x7F~g\xCA\xEA\xBA\x80,\x83V\x10\xF6/\x04\xD2\x8A\x94\x94\xA9T>~\xD2\r\xE6\x0E\xA0q\xEF"

    def test_aes128_ctr_for_encryption
      assert_equal AES128_CTR, encrypt("aes128-ctr")
    end

    def test_aes128_ctr_for_decryption
      assert_equal TEXT, decrypt("aes128-ctr", AES128_CTR)
    end

    AES192_CTR = "\xE2\xE7\x1FJ\xE5\xB09\xE1\xB7/\xB3\x95\xF2S\xCE\x8C\x93\x14mFY\x88*\xCE\b\xA6\x87W\xD7\xEC/\xC9\xB6\x9Ba\a\x8E\x89-\xD7\xB2j\a\xB3\a\x92f\"\x96\x8D\xBF\x01\t\xB8Y\xF3\x92\x01\xCC7\xB6w\xF9\"=u:\xA1\xD5*\n\x9E\xC7p\xDC\x11\a\x1C\x88y\xE8\x87`\xA6[fF\x9B\xACv\xA6\xDA1|#F"

    def test_aes192_ctr_for_encryption
      assert_equal AES192_CTR, encrypt("aes192-ctr")
    end

    def test_aes192_ctr_for_decryption
      assert_equal TEXT, decrypt("aes192-ctr", AES192_CTR)
    end

    AES256_CTR = "2\xB8\xE6\xC9\x95\xB4\x05\xD2\xC7+\x7F\x88\xEB\xD4\xA0\b\"\xBF\x9E\x85t\x19,\e\x90\x11\x04b\xC7\xEE$\xDE\xE6\xC5@G\xFEm\xE1u\x9B\au\xAF\xB5\xB8\x857\x87\x139u\xAC\x1A\xAB\fh\x8FiW~\xB8:\xA4\xA0#~\xC4\x89\xBA5#:\xFC\xC8\xE3\x9B\xF0A2\x87\x980\xD1\xE3\xBC'\xBE\x1E\n\x1A*B\x06\xF3\xCC"

    def test_aes256_ctr_for_encryption
      assert_equal AES256_CTR, encrypt("aes256-ctr")
    end

    def test_aes256_ctr_for_decryption
      assert_equal TEXT, decrypt("aes256-ctr", AES256_CTR)
    end

    CAMELLIA128_CTR = "$\xCDQ\x86\xFD;Eq\x04\xFD\xEF\xC9\x18\xBA\\ZA\xD1\xA6Z\xC7V\xDE\xCDT\xBB\xC9\xB0BW\x9BOb}O\xCANy\xEA\xBB\xC5\x126\xE3\xDF\xB8]|j\x1D\xAE\"i\x8A\xCB\xE06\x01\xC4\xDA\xF6:\xA7\xB2v\xB0\xAE\xA5m\x16\xDB\xEBR\xCC\xB4\xA3\x93\x11;\xF1\x00\xDFS6\xF8\xD0_\b\nl\xA2\x95\x8E\xF2\xB0\xC1"
    if_supported?("camellia128-ctr@openssh.org") do
      def test_camellia128_ctr_openssh_org_for_encryption
        assert_equal CAMELLIA128_CTR, encrypt("camellia128-ctr@openssh.org")
      end
      def test_camellia128_ctr_openssh_org_for_decryption
        assert_equal TEXT, decrypt("camellia128-ctr@openssh.org", CAMELLIA128_CTR)
      end
    end
    if_supported?("camellia128-ctr") do
      def test_camellia128_ctr_for_encryption
        assert_equal CAMELLIA128_CTR, encrypt("camellia128-ctr")
      end
      def test_camellia128_ctr_for_decryption
        assert_equal TEXT, decrypt("camellia128-ctr", CAMELLIA128_CTR)
      end
    end

    CAMELLIA192_CTR = "\xB1O;\xA5\xB9 \xD6\x7Fw\ajz\xAF12\x1C\xF0^\xB2\x13\xA7s\xCB\x1A(3Yw\x8B\"7\xD7}\xC4\xAA\xF7\xDB\xF2\xEEi\x02\xD0\x94BK\xD9l\xBC\xBEbrk\x87\x14h\xE1'\xD2\xE4\x8C\x8D\x87\xCE\xBF\x89\xA9\x9E\xC4\f\xB8\x87(\xFE?\xD9\xEF\xBA5\xD8\xA1\rI\xD6s9\x10\xA9l\xB8S\x93}*\x9A\xB0="
    if_supported?("camellia192-ctr@openssh.org") do
      def test_camellia192_ctr_openssh_org_for_encryption
        assert_equal CAMELLIA192_CTR, encrypt("camellia192-ctr@openssh.org")
      end
      def test_camellia192_ctr_openssh_org_for_decryption
        assert_equal TEXT, decrypt("camellia192-ctr@openssh.org", CAMELLIA192_CTR)
      end
    end
    if_supported?("camellia192-ctr") do
      def test_camellia192_ctr_for_encryption
        assert_equal CAMELLIA192_CTR, encrypt("camellia192-ctr")
      end
      def test_camellia192_ctr_for_decryption
        assert_equal TEXT, decrypt("camellia192-ctr", CAMELLIA192_CTR)
      end
    end

    CAMELLIA256_CTR = "`\x8F#Nqr^m\xB2/i\xF9}\x1E\xD1\xE7X\x99\xAF\x1E\xBA\v\xF3\x8E\xCA\xECZ\xCB\x8A\xC96FW\xB3\x84 bwzRM,P\xC1r\xEFHNr%\xB9\a\xD6\xE6\xE7O\b\xC8?\x98d\x9F\xD3v\x10#\xA6\x87\xB2\x85\x059\xF0-\xF9\xBC\x00V\xB2?\xAE\x1E{\e\xF1\xA9zJ\xC9=1\xB3t73\xEB"
    if_supported?("camellia256-ctr@openssh.org") do
      def test_camellia256_ctr_openssh_org_for_encryption
        assert_equal CAMELLIA256_CTR, encrypt("camellia256-ctr@openssh.org")
      end
      def test_camellia256_ctr_openssh_org_for_decryption
        assert_equal TEXT, decrypt("camellia256-ctr@openssh.org", CAMELLIA256_CTR)
      end
    end
    if_supported?("camellia256-ctr") do
      def test_camellia256_ctr_for_encryption
        assert_equal CAMELLIA256_CTR, encrypt("camellia256-ctr")
      end
      def test_camellia256_ctr_for_decryption
        assert_equal TEXT, decrypt("camellia256-ctr", CAMELLIA256_CTR)
      end
    end

    def test_none_for_encryption
      assert_equal TEXT, encrypt("none").strip
    end

    def test_none_for_decryption
      assert_equal TEXT, decrypt("none", TEXT)
    end
    
    private

      TEXT = "But soft! What light through yonder window breaks? It is the east, and Juliet is the sun!"

      OPTIONS = { :iv => "ABC",
        :key => "abc",
        :digester => OpenSSL::Digest::MD5,
        :shared => "1234567890123456780",
        :hash => '!@#$%#$^%$&^&%#$@$'
      }

      def factory
        Net::SSH::Transport::CipherFactory
      end

      def encrypt(type)
        cipher = factory.get(type, OPTIONS.merge(:encrypt => true))
        padding = TEXT.length % cipher.block_size
        result = cipher.update(TEXT.dup)
        result << cipher.update(" " * (cipher.block_size - padding)) if padding > 0
        result << cipher.final
      end

      def decrypt(type, data)
        cipher = factory.get(type, OPTIONS.merge(:decrypt => true))
        result = cipher.update(data.dup)
        result << cipher.final
        result.strip
      end
  end

end
