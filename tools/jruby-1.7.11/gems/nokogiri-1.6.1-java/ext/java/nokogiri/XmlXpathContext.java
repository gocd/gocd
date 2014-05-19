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

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import nokogiri.internals.NokogiriNamespaceContext;
import nokogiri.internals.NokogiriXPathFunctionResolver;
import nokogiri.internals.NokogiriXPathVariableResolver;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFloat;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.NodeList;

/**
 * Class for Nokogiri::XML::XpathContext
 *
 * @author sergio
 * @author Yoko Harada <yokolet@gmail.com>
 */
@JRubyClass(name="Nokogiri::XML::XPathContext")
public class XmlXpathContext extends RubyObject {
    private XmlNode context;
    private XPath xpath;
    
    public XmlXpathContext(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }
    
    public void setNode(XmlNode node) {
        context = node;
        xpath.setNamespaceContext(NokogiriNamespaceContext.create());
        xpath.setXPathVariableResolver(NokogiriXPathVariableResolver.create());
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

    @JRubyMethod(name = "new", meta = true)
    public static IRubyObject rbNew(ThreadContext thread_context, IRubyObject klazz, IRubyObject node) {
        XmlNode xmlNode = (XmlNode)node;
        XmlXpathContext xmlXpathContext = (XmlXpathContext) NokogiriService.XML_XPATHCONTEXT_ALLOCATOR.allocate(thread_context.getRuntime(), (RubyClass)klazz);
        xmlXpathContext.xpath = XPathFactory.newInstance().newXPath();
        xmlXpathContext.setNode(xmlNode);
        return xmlXpathContext;
    }

    @JRubyMethod
    public IRubyObject evaluate(ThreadContext thread_context, IRubyObject expr, IRubyObject handler) {
        String src = (String) expr.toJava(String.class);
        try {
            if(!handler.isNil()) {
            	if (!isContainsPrefix(src)) {
                    Set<String> methodNames = handler.getMetaClass().getMethods().keySet();
                    for (String name : methodNames) {
                        src = src.replaceAll(name, NokogiriNamespaceContext.NOKOGIRI_PREFIX+":"+name);
                    }
                }
                xpath.setXPathFunctionResolver(NokogiriXPathFunctionResolver.create(handler));
            }
            XPathExpression xpathExpression = xpath.compile(src);
            return node_set(thread_context, xpathExpression);
        } catch (XPathExpressionException xpee) {
            xpee = new XPathExpressionException(src);
            RubyException e = XmlSyntaxError.createXPathSyntaxError(getRuntime(), xpee);
            throw new RaiseException(e);
        }
    }

    protected IRubyObject node_set(ThreadContext thread_context, XPathExpression xpathExpression) {
        XmlNodeSet result = null;
        try {  
            result = tryGetNodeSet(thread_context, xpathExpression);
            return result;
        } catch (XPathExpressionException xpee) {
            try {
                return tryGetOpaqueValue(xpathExpression);
            } catch (XPathExpressionException xpee_opaque) {
                 RubyException e = XmlSyntaxError.createXPathSyntaxError(getRuntime(), xpee_opaque);
                 throw new RaiseException(e);
            }
        }
    }
    
    private XmlNodeSet tryGetNodeSet(ThreadContext thread_context, XPathExpression xpathExpression) throws XPathExpressionException {
        NodeList nodeList = (NodeList)xpathExpression.evaluate(context.node, XPathConstants.NODESET);
        XmlNodeSet xmlNodeSet = (XmlNodeSet) NokogiriService.XML_NODESET_ALLOCATOR.allocate(getRuntime(), getNokogiriClass(getRuntime(), "Nokogiri::XML::NodeSet"));
        xmlNodeSet.setNodeList(nodeList);
        xmlNodeSet.initialize(thread_context.getRuntime(), context);
        return xmlNodeSet;    
    }

    private static Pattern boolean_pattern = Pattern.compile("true|false");
    
    private IRubyObject tryGetOpaqueValue(XPathExpression xpathExpression) throws XPathExpressionException {
        String string = (String)xpathExpression.evaluate(context.node, XPathConstants.STRING);
        Double value = null;
        if ((value = getDoubleValue(string)) != null) {
            return new RubyFloat(getRuntime(), value);
        }
        if (doesMatch(boolean_pattern, string.toLowerCase())) return RubyBoolean.newBoolean(getRuntime(), Boolean.parseBoolean(string));
        return RubyString.newString(getRuntime(), string);
    }
    
    private Double getDoubleValue(String value) {
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private boolean doesMatch(Pattern pattern, String string) {
        Matcher m = pattern.matcher(string);
        return m.matches();
    }

    private boolean isContainsPrefix(String str) {
        Set<String> prefixes = ((NokogiriNamespaceContext)xpath.getNamespaceContext()).getAllPrefixes();
        for (String prefix : prefixes) {
            if (str.contains(prefix + ":")) {
                return true;
            }
        }
        return false;
    }


    @JRubyMethod
    public IRubyObject evaluate(ThreadContext context, IRubyObject expr) {
        return this.evaluate(context, expr, context.getRuntime().getNil());
    }

    @JRubyMethod
    public IRubyObject register_ns(ThreadContext context, IRubyObject prefix, IRubyObject uri) {
        ((NokogiriNamespaceContext) xpath.getNamespaceContext()).registerNamespace((String)prefix.toJava(String.class), (String)uri.toJava(String.class));
        return this;
    }

    @JRubyMethod
    public IRubyObject register_variable(ThreadContext context, IRubyObject name, IRubyObject value) {
        ((NokogiriXPathVariableResolver) xpath.getXPathVariableResolver()).
            registerVariable((String)name.toJava(String.class), (String)value.toJava(String.class));
        return this;
    }
}
