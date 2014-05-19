/**
 * (The MIT License)
 *
 * Copyright (c) 2008 - 2012:
 *
 * * {Aaron Patterson}[http://tenderlovemaking.com]
 * * {Mike Dalessio}[http://mike.daless.io]
 * * {Charles Nutter}[http://blog.headius.com]
 * * {Sergio Arbeo}[http://www.serabe.com]
 * * {Patrick Mahoney}[http://polycrystal.org]
 * * {Yoko Harada}[http://yokolet.blogspot.com]
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * 'Software'), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nokogiri;

import static nokogiri.internals.NokogiriHelpers.getNokogiriClass;
import static nokogiri.internals.NokogiriHelpers.stringOrNil;

import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.xml.sax.SAXParseException;

/**
 * Class for Nokogiri::XML::SyntaxError
 *
 * @author sergio
 * @author Yoko Harada <yokolet@gmail.com>
 */
@JRubyClass(name="Nokogiri::XML::SyntaxError", parent="Nokogiri::SyntaxError")
public class XmlSyntaxError extends RubyException {
    private Exception exception;

    public XmlSyntaxError(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }
    
    /**
     * Create and return a copy of this object.
     *
     * @return a clone of this object
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public XmlSyntaxError(Ruby runtime, RubyClass rubyClass, Exception ex) {
        super(runtime, rubyClass, ex.getMessage());
        this.exception = ex;
    }

    public static XmlSyntaxError createWarning(Ruby runtime, SAXParseException e) {
        XmlSyntaxError xmlSyntaxError = createNokogiriXmlSyntaxError(runtime);
        xmlSyntaxError.setException(runtime, e, 1);
        return xmlSyntaxError;
    }

    public static XmlSyntaxError createError(Ruby runtime, SAXParseException e) {
        XmlSyntaxError xmlSyntaxError = createNokogiriXmlSyntaxError(runtime);
        xmlSyntaxError.setException(runtime, e, 2);
        return xmlSyntaxError;
    }

    public static XmlSyntaxError createFatalError(Ruby runtime, SAXParseException e) {
        XmlSyntaxError xmlSyntaxError = createNokogiriXmlSyntaxError(runtime);
        xmlSyntaxError.setException(runtime, e, 3);
        return xmlSyntaxError;
    }

    public static XmlSyntaxError createNokogiriXmlSyntaxError(Ruby runtime) {
      return (XmlSyntaxError) NokogiriService.XML_SYNTAXERROR_ALLOCATOR.allocate(runtime, getNokogiriClass(runtime, "Nokogiri::XML::SyntaxError"));
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
    
    public void setException(Ruby runtime, Exception exception, int level) {
        this.exception = exception;
        setInstanceVariable("@level", runtime.newFixnum(level));
        setInstanceVariable("@line", runtime.newFixnum(((SAXParseException)exception).getLineNumber()));
        setInstanceVariable("@column", runtime.newFixnum(((SAXParseException)exception).getColumnNumber()));
        setInstanceVariable("@file", stringOrNil(runtime, ((SAXParseException)exception).getSystemId()));
    }

    public static RubyException createXPathSyntaxError(Ruby runtime, Exception e) {
        RubyClass klazz = (RubyClass)runtime.getClassFromPath("Nokogiri::XML::XPath::SyntaxError");
        return new XmlSyntaxError(runtime, klazz, e);
    }

    //@Override
    //"to_s" method was branched in 1.8 and 1.9 since JRuby 1.6.6
    // to support older version of JRuby, the annotation is commented out
    @Override
    @JRubyMethod(name = "to_s", compat = CompatVersion.RUBY1_8)
    public IRubyObject to_s(ThreadContext context) {
        if (exception != null && exception.getMessage() != null)
            return context.getRuntime().newString(exception.getMessage());
        else
            return super.to_s(context);  
    }
    
    //@Override
    //"to_s" method was branched in 1.8 and 1.9 since JRuby 1.6.6
    // to support older version of JRuby, the annotation is commented out
    @JRubyMethod(name = "to_s", compat = CompatVersion.RUBY1_9)
    public IRubyObject to_s19(ThreadContext context) {
        return this.to_s(context);
    }
    
}
