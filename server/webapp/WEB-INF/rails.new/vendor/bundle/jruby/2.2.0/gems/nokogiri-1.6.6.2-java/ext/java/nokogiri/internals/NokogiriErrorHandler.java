/**
 * (The MIT License)
 *
 * Copyright (c) 2008 - 2011:
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

package nokogiri.internals;

import static nokogiri.internals.NokogiriHelpers.getNokogiriClass;

import java.util.ArrayList;
import java.util.List;

import nokogiri.NokogiriService;
import nokogiri.XmlSyntaxError;

import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.xml.sax.ErrorHandler;

/**
 * Super class of error handlers.
 * 
 * XMLErrorHandler is used by nokogiri.internals.HtmlDomParserContext since NekoHtml
 * uses this type of the error handler.
 * 
 * @author sergio
 * @author Yoko Harada <yokolet@gmail.com>
 */
public abstract class NokogiriErrorHandler implements ErrorHandler, XMLErrorHandler {
    protected List<Exception> errors;
    protected boolean noerror;
    protected boolean nowarning;

    public NokogiriErrorHandler(boolean noerror, boolean nowarning) {
        errors = new ArrayList<Exception>();
        this.noerror = noerror;
        this.nowarning = nowarning;
    }

    public List<Exception> getErrors() { return errors; }

    public List<IRubyObject> getErrorsReadyForRuby(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        List<IRubyObject> res = new ArrayList<IRubyObject>();
        for (int i = 0; i < errors.size(); i++) {
            XmlSyntaxError xmlSyntaxError = (XmlSyntaxError) NokogiriService.XML_SYNTAXERROR_ALLOCATOR.allocate(runtime, getNokogiriClass(runtime, "Nokogiri::XML::SyntaxError"));
            xmlSyntaxError.setException(errors.get(i));
            res.add(xmlSyntaxError);
        }
        return res;
    }

    protected boolean usesNekoHtml(String domain) {
        if ("http://cyberneko.org/html".equals(domain)) return true;
        else return false;
    }
}
