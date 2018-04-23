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

import static nokogiri.XmlNode.setDocumentAndDecorate;
import static nokogiri.internals.NokogiriHelpers.getNokogiriClass;
import static nokogiri.internals.NokogiriHelpers.nodeListToRubyArray;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class for Nokogiri::XML::NodeSet
 *
 * @author sergio
 * @author Yoko Harada <yokolet@gmail.com>
 */
@JRubyClass(name="Nokogiri::XML::NodeSet")
public class XmlNodeSet extends RubyObject implements NodeList {

    RubyArray nodes;
    
    public XmlNodeSet(Ruby ruby, RubyClass klazz) {
        super(ruby, klazz);
    }

    public static XmlNodeSet create(final Ruby runtime) {
        return (XmlNodeSet) NokogiriService.XML_NODESET_ALLOCATOR.allocate(runtime, getNokogiriClass(runtime, "Nokogiri::XML::NodeSet"));
    }

    public static XmlNodeSet newEmptyNodeSet(ThreadContext context) {
        return create(context.getRuntime());
    }

    public static XmlNodeSet newXmlNodeSet(final Ruby runtime, RubyArray nodes) {
        XmlNodeSet xmlNodeSet = create(runtime);
        xmlNodeSet.setNodes(nodes);
        return xmlNodeSet;
    }

    static XmlNodeSet newXmlNodeSet(ThreadContext context, RubyArray nodes) {
        return newXmlNodeSet(context.getRuntime(), nodes);
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

    void setNodes(RubyArray array) {
        this.nodes = array;

        IRubyObject first = array.first();
        initialize(array.getRuntime(), first);
    }

    private void setReference(XmlNodeSet reference) {
        this.nodes = null;
        IRubyObject first = reference.nodes.first();
        initialize(reference.getRuntime(), first);
    }
    
    public void setNodeList(NodeList nodeList) {
        setNodes(nodeListToRubyArray(getRuntime(), nodeList));
    }

    final void initialize(Ruby runtime, IRubyObject refNode) {
        if (refNode instanceof XmlNode) {
            IRubyObject doc = ((XmlNode) refNode).document(runtime);
            setDocumentAndDecorate(runtime.getCurrentContext(), this, doc);
        }
    }

    public int length() {
        if (nodes == null) return 0;
        return nodes.size();
    }

    public void relink_namespace(ThreadContext context) {
        List<?> n = nodes.getList();

        for (int i = 0; i < n.size(); i++) {
            if (n.get(i) instanceof XmlNode) {
                ((XmlNode) n.get(i)).relink_namespace(context);
            }
        }
    }

    @JRubyMethod(name="&")
    public IRubyObject and(ThreadContext context, IRubyObject nodeSet) {
        if (nodes == null) setNodes(RubyArray.newEmptyArray(context.getRuntime()));
        return newXmlNodeSet(context, (RubyArray) nodes.op_and(getNodes(context, nodeSet)));
    }

    @JRubyMethod
    public IRubyObject delete(ThreadContext context, IRubyObject node_or_namespace) {
        if (nodes == null) return context.getRuntime().getNil();
        if (node_or_namespace instanceof XmlNamespace) {
            ((XmlNamespace) node_or_namespace).deleteHref();
        }
        return nodes.delete(context, asXmlNodeOrNamespace(context, node_or_namespace), Block.NULL_BLOCK);
    }

    @JRubyMethod
    public IRubyObject dup(ThreadContext context){
        if (nodes == null) return newEmptyNodeSet(context);
        return newXmlNodeSet(context, nodes.aryDup());
    }

    @JRubyMethod(name = "include?")
    public IRubyObject include_p(ThreadContext context, IRubyObject node_or_namespace) {
        node_or_namespace = asXmlNodeOrNamespace(context, node_or_namespace);
        if (nodes == null) return context.getRuntime().getFalse();
        return nodes.include_p(context, node_or_namespace);
    }

    @JRubyMethod(name = {"length", "size"})
    public IRubyObject length(ThreadContext context) {
        return nodes != null ? nodes.length() : context.getRuntime().newFixnum(0);
    }

    @JRubyMethod(name="-")
    public IRubyObject op_diff(ThreadContext context, IRubyObject nodeSet) {
        XmlNodeSet xmlNodeSet = newXmlNodeSet(context, this);
        if (nodes == null) setNodes(RubyArray.newEmptyArray(context.getRuntime()));
        xmlNodeSet.setNodes((RubyArray) nodes.op_diff(getNodes(context, nodeSet)));
        return xmlNodeSet;
    }

    @JRubyMethod(name={"|", "+"})
    public IRubyObject op_or(ThreadContext context, IRubyObject nodeSet) {
        if (nodes == null) setNodes(RubyArray.newEmptyArray(context.getRuntime()));
        return newXmlNodeSet(context, (RubyArray) nodes.op_or(getNodes(context, nodeSet)));
    }

    @JRubyMethod(name = {"push", "<<"})
    public IRubyObject push(ThreadContext context, IRubyObject node_or_namespace) {
        if (nodes == null) setNodes(RubyArray.newArray(context.getRuntime()));
        nodes.append(asXmlNodeOrNamespace(context, node_or_namespace));
        return this;
    }

    @JRubyMethod(name={"[]", "slice"})
    public IRubyObject slice(ThreadContext context, IRubyObject indexOrRange){
        if (nodes == null) return context.getRuntime().getNil();
        IRubyObject result = nodes.aref19(indexOrRange);
        if (result instanceof RubyArray) {
            return newXmlNodeSet(context, (RubyArray) result);
        }
        return result;
    }

    @JRubyMethod(name={"[]", "slice"})
    public IRubyObject slice(ThreadContext context, IRubyObject start, IRubyObject length){
        if (nodes == null) return context.getRuntime().getNil();
        IRubyObject result = nodes.aref19(start, length);
        if (result instanceof RubyArray) {
            return newXmlNodeSet(context, (RubyArray) result);
        }
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = {"to_a", "to_ary"})
    public IRubyObject to_a(ThreadContext context) {
        return nodes;
    }

    @JRubyMethod(name = {"unlink", "remove"})
    public IRubyObject unlink(ThreadContext context){
        if (nodes == null) return this;
        IRubyObject[] arr = nodes.toJavaArrayUnsafe();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] instanceof XmlNode) {
                ((XmlNode) arr[i] ).unlink(context);
            }
        }
        return this;
    }

    private static XmlNodeSet newXmlNodeSet(ThreadContext context, XmlNodeSet reference) {
        XmlNodeSet xmlNodeSet = create(context.getRuntime());
        xmlNodeSet.setReference(reference);
        return xmlNodeSet;
    }

    private static IRubyObject asXmlNodeOrNamespace(ThreadContext context, IRubyObject possibleNode) {
        if (possibleNode instanceof XmlNode || possibleNode instanceof XmlNamespace) {
            return possibleNode;
        }
        throw context.getRuntime().newArgumentError("node must be a Nokogiri::XML::Node or Nokogiri::XML::Namespace");
    }

    private static RubyArray getNodes(ThreadContext context, IRubyObject possibleNodeSet) {
        if (possibleNodeSet instanceof XmlNodeSet) {
            RubyArray nodes = ((XmlNodeSet) possibleNodeSet).nodes;
            return nodes == null ? RubyArray.newEmptyArray(context.getRuntime()) : nodes;
        }
        throw context.getRuntime().newArgumentError("node must be a Nokogiri::XML::NodeSet");
    }
    
    public int getLength() {
        return nodes == null ? 0 : nodes.size();
    }
    
    public Node item(int index) {
        if (nodes == null) return null;
        Object n = nodes.get(index);
        if (n instanceof XmlNode) return ((XmlNode)n).node;
        if (n instanceof XmlNamespace) return ((XmlNamespace)n).getNode();
        return null;
    }
}
