##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

class ElementHandler

	def initialize output, root
		@html = output  #an instance of HtmlRenderer
		@root = root  #root of this document
		@copy_set = [] #these elements will just be copied to the output
		@apply_set = [] #these elements will get the recursive apply
		if defined?(REXML::Formatters)
		  @formatter = REXML::Formatters::Default.new
		end
	end

	def render
		handle @root
	end

	def handle aNode
		if aNode.kind_of? REXML::CData
			handleCdataNode(aNode)
		elsif aNode.kind_of? REXML::Text
			handleTextNode(aNode)
		elsif aNode.kind_of? REXML::Element
			handle_element aNode  
		else
			return #ignore comments and processing instructions
		end
	end
	
	def handle_element anElement	  
		handler_method = "handle_" + anElement.name.tr("-","_")
		if self.respond_to? handler_method
			self.send(handler_method, anElement)
		else
			if @copy_set.include? anElement.name
				copy anElement
			elsif @apply_set.include? anElement.name
				apply anElement
			else
				default_handler anElement
			end
		end
	end

	def default_handler anElement
		apply anElement
	end

	def copy anElement
		attr = nil
		if anElement.has_attributes?
			attr = {}
			anElement.attributes.each {|name, value| attr[name] = value}
		end
		@html.element(anElement.name, attr) {apply anElement}
	end

	def apply anElement
		# Handles all children. Equivalent to xslt apply-templates
		anElement.each {|e| handle(e)} if anElement
	end
	
	def handleTextNode aNode
		#HACKTAG this is an ugly hack to replace &apos; with ' since
		#IE cannot handle &apos. I haven't yet looked for a clean
		#way to do this nicely
		if aNode.to_s =~ /\S/
			output = ""
			if @formatter
			  @formatter.write(aNode, output)
			else
			  aNode.write(output)
			end
			output.gsub!("&apos;", "'")
			@html << output
		end
	end
	
	def handleCdataNode aNode
		# This did have a problem compressing whitespace, but seems to work now.
		output = ""
		output = aNode.to_s
		@html.cdata(output)
	end

end
