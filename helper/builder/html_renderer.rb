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

class HtmlRenderer
  
	def initialize output
		@out = output
	end
	
	def close
		@out.close
	end
	
	def HtmlRenderer.open file, &block
		@out = File.new(file)
		yield
		close
	end

	# outputs an html element with name and attributes. If isInline
	# will not start a new line for the element. 
	def element(name, attributes=nil, isInline=false)
		@out << "\n" unless isInline
		@out <<  "<#{name}"
		print_attributes(attributes) unless nil == attributes
		@out << ">"
		yield
		@out << "</#{name}>"
		@out << "\n" unless isInline
	end
	def non_close_element(name, attributes=nil, isInline=false)
    		@out << "\n" unless isInline
    		@out <<  "<#{name}"
    		print_attributes(attributes) unless nil == attributes
    		@out << ">"
    		yield
    		@out << "\n" unless isInline
    	end
	
	def print_attributes attributes
		attributes.each do |key, value| 
			@out << " #{key} = '#{substituteXmlEntities(value.to_s)}'" unless value.nil?
		end
	end
	
	def head(&block)
		element "HEAD", nil, false, &block
	end
	
	def title(text)
		element("TITLE") {print text}
	end
	
	def body(&block)
		element "BODY", {'bgcolor' => 'white'}, false, &block
	end
	
	def text arg
		@out <<  arg
	end
	
	def << arg
		@out << arg
	end
	
	def cdata arg
		@out << substituteXmlEntities(arg)
	end
	
	def substituteXmlEntities aString
		result = aString.gsub "&", "&amp;"
		result.gsub! "<", "&lt;"
		return result
	end
	
	def p(css_class = nil, &block)
		attr = class_attr css_class
		element "p", attr, false, &block
	end
	
	def pre(css_class = nil, &block)
		attr = class_attr css_class
		element "pre", attr, false, &block
	end
	
	def h(level, css_class = nil, &block)
	  attr = class_attr css_class
		element("h" + level.to_s, attr, false, &block)
	end

	def hr css_class = nil
		attr = class_attr css_class
		element('hr', attr) {}
	end

	def span(css_class = nil, &block)
		attr = class_attr css_class
		element "span", attr, true, &block
	end

	def a_ref(href,target, &block)
    if href
      element"a", {'href' => href,'target' => target}, true, &block
    else
      yield
    end
	end

	def a_name(name) 
		element('a', {'name' => name}, false) {}
	end

	def b(&block)
		element 'b', nil, true, &block
	end

	def i(&block)
		element 'i', nil, true, &block
	end

	def span css_class, &block
		attr = class_attr css_class		
		element 'span', attr, true, &block
	end

	def page_title title
		hr
		h 1 do
			text title
		end
		hr
	end

	def table(css_class = nil, &block)
		attr = class_attr css_class
		element 'table', attr, false, &block
	end
	
	def tr(css_class = nil,&block)
		attr = class_attr css_class
		element 'tr', attr, false, &block
	end
	
	def td(css_class = nil,&block)
		attr = class_attr css_class
		element 'td', attr, true, &block
	end

	def col (width_percent = nil, &block)
		attrs = {'valign' => 'top'}
		attrs['width'] = width_percent.to_s + '%' if width_percent
		element "TD", attrs, true, &block
	end

	def error aString
		text "*** #{aString} ***"
		$stderr.puts "WARNING: " + aString
	end
	
	def lf
			text "<BR/>"
	end

	def ul(css_class = nil, &block)
	  attr = class_attr css_class
		element 'ul', attr, false, &block
	end
	
	def ol(css_class = nil, &block)
	  attr = class_attr css_class
		element 'ol', attr, false, &block
	end	
	
	def li(&block)
		element 'li', nil, false, &block
	end

	def include fileName
		File.foreach(fileName) {|line| self << line}
	end
	
	def div css_class = nil, &block
		attr = class_attr css_class
		element "div", attr, false, &block		
	end
	
	def class_attr value
		return (value)  ? {'class' => value} : nil
	end
	 
end