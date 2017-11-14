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
import static org.jruby.javasupport.util.RuntimeHelpers.invoke;
import static nokogiri.internals.NokogiriHelpers.rubyStringToString;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.EnumSet;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

import nokogiri.internals.ClosedStreamException;
import nokogiri.internals.NokogiriBlockingQueueInputStream;
import nokogiri.internals.NokogiriHelpers;
import nokogiri.internals.ParserContext;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Class for Nokogiri::HTML::SAX::PushParser
 *
 * @author 
 * @author Piotr Szmielew <p.szmielew@ava.waw.pl> - based on Nokogiri::XML::SAX::PushParser
 */
@JRubyClass(name="Nokogiri::HTML::SAX::PushParser")
public class HtmlSaxPushParser extends RubyObject {
    ParserContext.Options options;
    IRubyObject optionsRuby;
    IRubyObject saxParser;
    NokogiriBlockingQueueInputStream stream;
    ParserTask parserTask = null;
    FutureTask<HtmlSaxParserContext> futureTask = null;
    ExecutorService executor = null;

    public HtmlSaxPushParser(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    @Override
    public void finalize() {
      terminateTask(null);
    }

    /**
    * Silently skips provided encoding
    *
    */
    @JRubyMethod
    public IRubyObject initialize_native(final ThreadContext context,
                                         IRubyObject saxParser,
                                         IRubyObject fileName,
                                         IRubyObject encoding) {
        optionsRuby
            = invoke(context, context.getRuntime().getClassFromPath("Nokogiri::XML::ParseOptions"), "new");
       
        options = new ParserContext.Options(0);
        this.saxParser = saxParser;
        return this;
    }

    /**
     * Returns an integer.
     */
    @JRubyMethod(name="options")
    public IRubyObject getOptions(ThreadContext context) {
        return invoke(context, optionsRuby, "options");
    }

    /**
     * <code>val</code> is an integer.
     */
    @JRubyMethod(name="options=")
    public IRubyObject setOptions(ThreadContext context, IRubyObject val) {
        invoke(context, optionsRuby, "options=", val);
        options =
            new ParserContext.Options(val.convertToInteger().getLongValue());
        return getOptions(context);
    }

    @JRubyMethod
    public IRubyObject native_write(ThreadContext context, IRubyObject chunk,
                                    IRubyObject isLast) {
        try {
            initialize_task(context);
        } catch (IOException e) {
            throw context.getRuntime().newRuntimeError(e.getMessage());
        }
        final ByteArrayInputStream data = NokogiriHelpers.stringBytesToStream(chunk);
        if (data == null) {
            terminateTask(context);
            throw new RaiseException(XmlSyntaxError.createHTMLSyntaxError(context.runtime)); // Nokogiri::HTML::SyntaxError
        }

        int errorCount0 = parserTask.getErrorCount();

        if (isLast.isTrue()) {
            IRubyObject document = invoke(context, this, "document");
            invoke(context, document, "end_document");
            terminateTask(context);
        } else {
            try {
              Future<Void> task = stream.addChunk(data);
              task.get();
            } catch (ClosedStreamException ex) {
              // this means the stream is closed, ignore this exception
            } catch (Exception e) {
              throw context.getRuntime().newRuntimeError(e.getMessage());
            }

        }

        if (!options.recover && parserTask.getErrorCount() > errorCount0) {
            terminateTask(context);
            throw new RaiseException(parserTask.getLastError(), true);
        }

        return this;
    }

    private void initialize_task(ThreadContext context) throws IOException {
        if (futureTask == null || stream == null) {
            stream = new NokogiriBlockingQueueInputStream();

            parserTask = new ParserTask(context, saxParser);
            futureTask = new FutureTask<HtmlSaxParserContext>(parserTask);
            executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
              @Override
              public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("HtmlSaxPushParser");
                t.setDaemon(true);
                return t;
              }
            });
            executor.submit(futureTask);
        }
    }

    private synchronized void terminateTask(ThreadContext context) {
        try {
          Future<Void> task = stream.addChunk(NokogiriBlockingQueueInputStream.END);
          task.get();
        } catch (ClosedStreamException ex) {
          // ignore this exception, it means the stream was closed
        } catch (Exception e) {
            if (context != null)
              throw context.getRuntime().newRuntimeError(e.getMessage());
        }
        futureTask.cancel(true);
        executor.shutdown();
        executor = null;
        stream = null;
        futureTask = null;
    }
    

    private class ParserTask implements Callable<HtmlSaxParserContext> {
        private final ThreadContext context;
        private final IRubyObject handler;
        private final HtmlSaxParserContext parser;

        private ParserTask(ThreadContext context, IRubyObject handler) {
            RubyClass klazz = getNokogiriClass(context.getRuntime(), "Nokogiri::HTML::SAX::ParserContext");
            this.context = context;
            this.handler = handler;
            this.parser = (HtmlSaxParserContext) HtmlSaxParserContext.parse_stream(context, klazz, stream);
        }

        @Override
        public HtmlSaxParserContext call() throws Exception {
          try {
            parser.parse_with(context, handler);
          } finally {
            // we have to close the stream before exiting, otherwise someone
            // can add a chunk and block on task.get() forever.
            stream.close();
          }
          return parser;
        }

        private synchronized int getErrorCount() {
            // check for null because thread may not have started yet
            if (parser.getNokogiriHandler() == null) return 0;
            else return parser.getNokogiriHandler().getErrorCount();
        }

        private synchronized RubyException getLastError() {
            return (RubyException) parser.getNokogiriHandler().getLastError();
        }
    }
}
