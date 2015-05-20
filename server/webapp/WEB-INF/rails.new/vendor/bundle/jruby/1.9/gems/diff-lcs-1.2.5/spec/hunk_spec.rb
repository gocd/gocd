# -*- ruby encoding: utf-8 -*-

require 'spec_helper'

def h(v)
  v.to_s.bytes.to_a.map { |e| "%02x" % e }.join
end

describe "Diff::LCS::Hunk" do
  if String.method_defined?(:encoding)

    let(:old_data) { ["Tu avec carté {count} itém has".encode('UTF-16LE')] }
    let(:new_data) { ["Tu avec carte {count} item has".encode('UTF-16LE')] }
    let(:pieces)   { Diff::LCS.diff old_data, new_data }
    let(:hunk)     { Diff::LCS::Hunk.new(old_data, new_data, pieces[0], 3, 0) }

    it 'should be able to produce a unified diff from the two pieces' do
      expected = (<<-EOD.gsub(/^\s+/,'').encode('UTF-16LE').chomp)
        @@ -1,2 +1,2 @@
        -Tu avec carté {count} itém has
        +Tu avec carte {count} item has
      EOD
      expect(hunk.diff(:unified).to_s == expected).to eql true
    end

    it 'should be able to produce a context diff from the two pieces' do
      expected = (<<-EOD.gsub(/^\s+/,'').encode('UTF-16LE').chomp)
        ***************
        *** 1,2 ****
        !Tu avec carté {count} itém has
        --- 1,2 ----
        !Tu avec carte {count} item has
      EOD

      expect(hunk.diff(:context).to_s == expected).to eql true
    end

    it 'should be able to produce an old diff from the two pieces' do
      expected = (<<-EOD.gsub(/^ +/,'').encode('UTF-16LE').chomp)
        1,2c1,2
        < Tu avec carté {count} itém has
        ---
        > Tu avec carte {count} item has

      EOD
      expect(hunk.diff(:old).to_s == expected).to eql true
    end

    it 'should be able to produce a reverse ed diff from the two pieces' do
      expected = (<<-EOD.gsub(/^ +/,'').encode('UTF-16LE').chomp)
        c1,2
        Tu avec carte {count} item has
        .

      EOD
      expect(hunk.diff(:reverse_ed).to_s == expected).to eql true
    end

    context 'with empty first data set' do
      let(:old_data) { [] }

      it 'should be able to produce a unified diff' do
        expected = (<<-EOD.gsub(/^\s+/,'').encode('UTF-16LE').chomp)
          @@ -1 +1,2 @@
          +Tu avec carte {count} item has
        EOD
        expect(hunk.diff(:unified).to_s == expected).to eql true
      end
    end

  end
end
