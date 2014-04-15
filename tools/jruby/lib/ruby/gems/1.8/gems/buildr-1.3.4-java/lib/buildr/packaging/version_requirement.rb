# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.


module Buildr
  
  #
  # See ArtifactNamespace#need
  class VersionRequirement
    
    CMP_PROCS = Gem::Requirement::OPS.dup
    CMP_REGEX = Gem::Requirement::OP_RE.dup
    CMP_CHARS = CMP_PROCS.keys.join
    BOOL_CHARS = '\|\&\!'
    VER_CHARS = '\w\.\-'

    class << self
      # is +str+ a version string?
      def version?(str)
        /^\s*[#{VER_CHARS}]+\s*$/ === str
      end
      
      # is +str+ a version requirement?
      def requirement?(str)
        /[#{BOOL_CHARS}#{CMP_CHARS}\(\)]/ === str
      end
      
      # :call-seq:
      #    VersionRequirement.create(" >1 <2 !(1.5) ") -> requirement
      #
      # parse the +str+ requirement 
      def create(str)
        instance_eval normalize(str)
      rescue StandardError => e
        raise "Failed to parse #{str.inspect} due to: #{e}"
      end

      private
      def requirement(req)
        unless req =~ /^\s*(#{CMP_REGEX})?\s*([#{VER_CHARS}]+)\s*$/
          raise "Invalid requirement string: #{req}"
        end
        comparator, version = $1, $2
        version = Gem::Version.new(0).tap { |v| v.version = version }
        VersionRequirement.new(nil, [$1, version])
      end

      def negate(vreq)
        vreq.negative = !vreq.negative
        vreq
      end
      
      def normalize(str)
        str = str.strip
        if str[/[^\s\(\)#{BOOL_CHARS + VER_CHARS + CMP_CHARS}]/]
          raise "version string #{str.inspect} contains invalid characters"
        end
        str.gsub!(/\s+(and|\&\&)\s+/, ' & ')
        str.gsub!(/\s+(or|\|\|)\s+/, ' | ')
        str.gsub!(/(^|\s*)not\s+/, ' ! ')
        pattern = /(#{CMP_REGEX})?\s*[#{VER_CHARS}]+/
        left_pattern = /[#{VER_CHARS}\)]$/
        right_pattern = /^(#{pattern}|\()/
        str = str.split.inject([]) do |ary, i|
          ary << '&' if ary.last =~ left_pattern  && i =~ right_pattern
          ary << i
        end
        str = str.join(' ')
        str.gsub!(/!([^=])?/, ' negate \1')
        str.gsub!(pattern) do |expr|
          case expr.strip
          when 'not', 'negate' then 'negate '
          else 'requirement("' + expr + '")'
          end
        end
        str.gsub!(/negate\s+\(/, 'negate(')
        str
      end
    end

    def initialize(op, *requirements) #:nodoc:
      @op, @requirements = op, requirements
    end

    # Is this object a composed requirement?
    #   VersionRequirement.create('1').composed? -> false
    #   VersionRequirement.create('1 | 2').composed? -> true
    #   VersionRequirement.create('1 & 2').composed? -> true
    def composed?
      requirements.size > 1
    end

    # Return the last requirement on this object having an = operator.
    def default
      default = nil
      requirements.reverse.find do |r|
        if Array === r
          if !negative && (r.first.nil? || r.first.include?('='))
            default = r.last.to_s
          end
        else
          default = r.default
        end
      end
      default
    end

    # Test if this requirement can be satisfied by +version+
    def satisfied_by?(version)
      return false unless version
      unless version.kind_of?(Gem::Version)
        raise "Invalid version: #{version.inspect}" unless self.class.version?(version)
        version = Gem::Version.new(0).tap { |v| v.version = version.strip }
      end
      message = op == :| ? :any? : :all?
      result = requirements.send message do |req|
        if Array === req
          cmp, rv = *req
          CMP_PROCS[cmp || '='].call(version, rv)
        else
          req.satisfied_by?(version)
        end
      end
      negative ? !result : result
    end

    # Either modify the current requirement (if it's already an or operation)
    # or create a new requirement
    def |(other)
      operation(:|, other)
    end

    # Either modify the current requirement (if it's already an and operation)
    # or create a new requirement
    def &(other)
      operation(:&, other)
    end
    
    # return the parsed expression
    def to_s
      str = requirements.map(&:to_s).join(" " + @op.to_s + " ").to_s
      str = "( " + str + " )" if negative || requirements.size > 1
      str = "!" + str if negative
      str
    end

    attr_accessor :negative
    protected
    attr_reader :requirements, :op
    def operation(op, other)
      @op ||= op 
      if negative == other.negative && @op == op && other.requirements.size == 1
        @requirements << other.requirements.first
        self
      else
        self.class.new(op, self, other)
      end
    end
  end # VersionRequirement
end
