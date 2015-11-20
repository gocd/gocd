/**
 * (The MIT License)
 *
 * Copyright (c) 2008 - 2014:
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import nokogiri.internals.NokogiriNamespaceContext;
import nokogiri.internals.NokogiriXPathFunctionResolver;
import nokogiri.internals.NokogiriXPathVariableResolver;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.utils.PrefixResolver;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.jaxp.JAXPExtensionsProvider;
import com.sun.org.apache.xpath.internal.jaxp.JAXPPrefixResolver;
import com.sun.org.apache.xpath.internal.jaxp.JAXPVariableStack;
import com.sun.org.apache.xpath.internal.objects.XObject;

/**
 * Class for Nokogiri::XML::XpathContext
 *
 * @author sergio
 * @author Yoko Harada <yokolet@gmail.com>
 * @author John Shahid <jvshahid@gmail.com>
 */
@JRubyClass(name="Nokogiri::XML::XPathContext")
public class XmlXpathContext extends RubyObject {
    public final static String XPATH_CONTEXT = "CACHCED_XPATH_CONTEXT";

    private XmlNode context;
    private final NokogiriXPathFunctionResolver functionResolver;
    private final NokogiriXPathVariableResolver variableResolver;
    private PrefixResolver prefixResolver;
    private XPathContext xpathSupport = null;
    private NokogiriNamespaceContext nsContext;

    public XmlXpathContext(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
        functionResolver = NokogiriXPathFunctionResolver.create(ruby.getCurrentContext().nil);
        variableResolver = NokogiriXPathVariableResolver.create();
    }

    private void setNode(XmlNode node) throws IllegalArgumentException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Node doc = node.getNode().getOwnerDocument();
        if (doc == null) {
            doc = node.getNode();
        }
        xpathSupport = (XPathContext) doc.getUserData(XPATH_CONTEXT);

        if (xpathSupport == null) {
            JAXPExtensionsProvider jep = getProviderInstance();
            xpathSupport = new XPathContext(jep);
            xpathSupport.setVarStack(new JAXPVariableStack(variableResolver));
            doc.setUserData(XPATH_CONTEXT, xpathSupport, null);
        }

        context = node;
        nsContext = NokogiriNamespaceContext.create();
        prefixResolver = new JAXPPrefixResolver(nsContext);
    }

    private JAXPExtensionsProvider getProviderInstance() throws ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<?> clazz = Class.forName("com.sun.org.apache.xpath.internal.jaxp.JAXPExtensionsProvider");
        Constructor[] constructors = clazz.getDeclaredConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Class[] parameterTypes = constructors[i].getParameterTypes();
            if (parameterTypes.length == 2) {
                return (JAXPExtensionsProvider) constructors[i].newInstance(functionResolver, false);
            } else if (parameterTypes.length == 1) {
                return (JAXPExtensionsProvider) constructors[i].newInstance(functionResolver);
            }
        }
        return null;
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
        XPathFactory.newInstance().newXPath();
        try {
            xmlXpathContext.setNode(xmlNode);
        } catch (IllegalArgumentException e) {
            throw thread_context.getRuntime().newRuntimeError(e.getMessage());
        } catch (ClassNotFoundException e) {
            throw thread_context.getRuntime().newRuntimeError(e.getMessage());
        } catch (InstantiationException e) {
            throw thread_context.getRuntime().newRuntimeError(e.getMessage());
        } catch (IllegalAccessException e) {
            throw thread_context.getRuntime().newRuntimeError(e.getMessage());
        } catch (InvocationTargetException e) {
            throw thread_context.getRuntime().newRuntimeError(e.getMessage());
        }
        return xmlXpathContext;
    }

    @JRubyMethod
    public IRubyObject evaluate(ThreadContext thread_context, IRubyObject expr, IRubyObject handler) {
        functionResolver.setHandler(handler);
        String src = (String) expr.toJava(String.class);
        if(!handler.isNil()) {
            if (!isContainsPrefix(src)) {
                Set<String> methodNames = handler.getMetaClass().getMethods().keySet();
                for (String name : methodNames) {
                    src = src.replaceAll(name, NokogiriNamespaceContext.NOKOGIRI_PREFIX+":"+name);
                }
            }
        }
        return node_set(thread_context, src);
    }

    protected IRubyObject node_set(ThreadContext thread_context, String expr) {
        try {
          return tryGetNodeSet(thread_context, expr);
        } catch (XPathExpressionException xpee) {
          RubyException e = XmlSyntaxError.createXPathSyntaxError(getRuntime(), xpee);
          throw new RaiseException(e);
        }
    }

    private IRubyObject tryGetNodeSet(ThreadContext thread_context, String expr) throws XPathExpressionException {
        XObject xobj = null;

        Node contextNode = context.node;

        try {
          com.sun.org.apache.xpath.internal.XPath xpathInternal = new com.sun.org.apache.xpath.internal.XPath (expr, null,
                      prefixResolver, com.sun.org.apache.xpath.internal.XPath.SELECT );

          // We always need to have a ContextNode with Xalan XPath implementation
          // To allow simple expression evaluation like 1+1 we are setting
          // dummy Document as Context Node

          if ( contextNode == null )
              xobj = xpathInternal.execute(xpathSupport, DTM.NULL, prefixResolver);
          else
              xobj = xpathInternal.execute(xpathSupport, contextNode, prefixResolver);

          switch (xobj.getType()) {
          case XObject.CLASS_BOOLEAN:
            return thread_context.getRuntime().newBoolean(xobj.bool());
          case XObject.CLASS_NUMBER:
            return thread_context.getRuntime().newFloat(xobj.num());
          case XObject.CLASS_NODESET:
            NodeList nodeList = xobj.nodelist();
            XmlNodeSet xmlNodeSet = (XmlNodeSet) NokogiriService.XML_NODESET_ALLOCATOR.allocate(getRuntime(), getNokogiriClass(getRuntime(), "Nokogiri::XML::NodeSet"));
            xmlNodeSet.setNodeList(nodeList);
            xmlNodeSet.initialize(thread_context.getRuntime(), context);
            return xmlNodeSet;
          default:
            return thread_context.getRuntime().newString(xobj.str());
          }
        } catch(TransformerException ex) {
          throw new XPathExpressionException(expr);
        }
    }

    private boolean isContainsPrefix(String str) {
        Set<String> prefixes = nsContext.getAllPrefixes();
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
        nsContext.registerNamespace((String)prefix.toJava(String.class), (String)uri.toJava(String.class));
        return this;
    }

    @JRubyMethod
    public IRubyObject register_variable(ThreadContext context, IRubyObject name, IRubyObject value) {
        variableResolver.registerVariable((String)name.toJava(String.class), (String)value.toJava(String.class));
        return this;
    }
}
