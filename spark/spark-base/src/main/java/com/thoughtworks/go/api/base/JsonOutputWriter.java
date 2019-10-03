/*
 * Copyright 2019 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.api.base;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.internal.bind.util.ISO8601Utils;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.spark.RequestContext;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Consumer;

public class JsonOutputWriter {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final JsonFactory JSON_FACTORY = new JsonFactory(OBJECT_MAPPER)
        .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
        .enable(JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION);

    protected final Writer writer;
    private final RequestContext requestContext;
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    public JsonOutputWriter(Writer writer, RequestContext requestContext) {
        this.writer = writer;
        this.requestContext = requestContext;
    }

    public JsonOutputWriter forTopLevelObject(Consumer<OutputWriter> consumer) {
        bufferWriterAndFlushWhenDone(writer, bufferedWriter -> {
            try (JsonOutputWriterUsingJackson jacksonOutputWriter = new JsonOutputWriterUsingJackson(bufferedWriter, requestContext)) {
                jacksonOutputWriter.forTopLevelObject(consumer);
            }
        });

        return this;
    }

    public JsonOutputWriter forTopLevelArray(Consumer<OutputListWriter> consumer) {
        bufferWriterAndFlushWhenDone(writer, bufferedWriter -> {
            try (JsonOutputWriterUsingJackson jacksonOutputWriter = new JsonOutputWriterUsingJackson(writer, requestContext)) {
                jacksonOutputWriter.forTopLevelArray(consumer);
            }
        });

        return this;
    }

    private void bufferWriterAndFlushWhenDone(Writer writer, Consumer<BufferedWriter> consumer) {
        BufferedWriter bufferedWriter = (writer instanceof BufferedWriter) ? (BufferedWriter) writer : new BufferedWriter(writer, 32 * 1024);
        try {
            try {
                consumer.accept(bufferedWriter);
            } finally {
                bufferedWriter.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class JsonOutputWriterUsingJackson implements OutputWriter {

        private final RequestContext requestContext;
        private final JsonGenerator jacksonWriter;

        private JsonOutputWriterUsingJackson(Writer writer, RequestContext requestContext) {
            this.requestContext = requestContext;
            try {
                jacksonWriter = JSON_FACTORY.createGenerator(writer);
                jacksonWriter.useDefaultPrettyPrinter();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public JsonOutputWriterUsingJackson add(String key, String value) {
            return withExceptionHandling((jacksonWriter) -> {
                jacksonWriter.writeStringField(key, value);
            });
        }

        @Override
        public JsonOutputWriterUsingJackson add(String key, Double value) {
            return withExceptionHandling((jacksonWriter) -> {
                jacksonWriter.writeNumberField(key,value);
            });
        }

        @Override
        public JsonOutputWriterUsingJackson add(String key, CaseInsensitiveString value) {
            return withExceptionHandling((jacksonWriter) -> {
                if (value == null) {
                   renderNull(key);
                }
                else {
                    jacksonWriter.writeStringField(key, value.toString());
                }
            });
        }

        @Override
        public JsonOutputWriterUsingJackson addIfNotNull(String key, String value) {
            return withExceptionHandling((jacksonWriter) -> {
                if (value != null) {
                    add(key, value);
                }
            });
        }

        @Override
        public JsonOutputWriterUsingJackson addIfNotNull(String key, Long value) {
            return withExceptionHandling((jacksonWriter) -> {
                if (value != null) {
                    add(key, value);
                }
            });
        }

        @Override
        public JsonOutputWriterUsingJackson addIfNotNull(String key, CaseInsensitiveString value) {
            return withExceptionHandling((jacksonWriter) -> {
                if (value != null) {
                    add(key, value);
                }
            });
        }

        @Override
        public JsonOutputWriterUsingJackson addIfNotNull(String key, Double value) {
            return withExceptionHandling((jacksonWriter) -> {
                if (value != null) {
                    add(key, value);
                }
            });
        }


        @Override
        public JsonOutputWriterUsingJackson addWithDefaultIfBlank(String key, String value, String defaultValue) {
            return withExceptionHandling((jacksonWriter) -> {
                if (StringUtils.isNotBlank(value)) {
                    add(key, value);
                }
                else {
                    add(key, defaultValue);
                }
            });
        }

        @Override
        public JsonOutputWriterUsingJackson add(String key, int value) {
            return withExceptionHandling((jacksonWriter) -> {
                jacksonWriter.writeNumberField(key, value);
            });
        }

        @Override
        public JsonOutputWriterUsingJackson add(String key, boolean value) {
            return withExceptionHandling((jacksonWriter) -> {
                jacksonWriter.writeBooleanField(key, value);
            });
        }

        @Override
        public JsonOutputWriterUsingJackson add(String key, long value) {
            return withExceptionHandling((jacksonWriter) -> {
                jacksonWriter.writeNumberField(key, value);
            });
        }

        @Override
        public JsonOutputWriterUsingJackson add(String key, Date value) {
            return withExceptionHandling((jacksonWriter) -> {
                jacksonWriter.writeStringField(key, jsonDate(value));
            });
        }

        @Override
        public JsonOutputWriterUsingJackson addIfNotNull(String key, Date value) {
            return withExceptionHandling((jacksonWriter) -> {
                if (value != null) {
                    add(key, value);
                }
            });
        }

        @Override
        public OutputWriter addChild(String key, Consumer<OutputWriter> consumer) {
            return new JsonOutputChildWriter(key, this).body(consumer);
        }

        @Override
        public OutputWriter addChildList(String key, Consumer<OutputListWriter> consumer) {
            return new JsonOutputListWriter(this).body(key, consumer);
        }

        @Override
        public OutputWriter addChildList(String key, Collection<String> values) {
            return new JsonOutputListWriter(this).body(key, listWriter -> values.forEach(listWriter::value));
        }

        @Override
        public OutputWriter addLinks(Consumer<OutputLinkWriter> consumer) {
            if (null == requestContext) return this;

            return withExceptionHandling((jacksonWriter) -> {
                addChild("_links", (childWriter) -> {
                    consumer.accept(new JsonOutputLinkWriter(childWriter));
                });
            });
        }

        @Override
        public OutputWriter addEmbedded(Consumer<OutputWriter> consumer) {
            return new JsonOutputChildWriter("_embedded", this).body(consumer);
        }

        @Override
        public OutputWriter add(String key, JsonNode jsonNode) {
            return withExceptionHandling((jacksonWriter) -> {
                    jacksonWriter.writeFieldName(key);
                    jacksonWriter.writeTree(jsonNode);
                }
            );
        }

        @Override
        public void renderNull(String key) {
            withExceptionHandling((jacksonWriter) -> {
                        jacksonWriter.writeFieldName(key);
                        jacksonWriter.writeTree(null);
                    }
            );
        }

        private JsonOutputWriterUsingJackson withExceptionHandling(ConsumerWhichThrows consumerWhichThrows) {
            consumerWhichThrows.accept(this.jacksonWriter);
            return this;
        }

        @FunctionalInterface
        interface ConsumerWhichThrows extends Consumer<JsonGenerator> {
            void acceptWhichThrows(JsonGenerator writer) throws Exception;

            @Override
            default void accept(JsonGenerator writer) {
                try {
                    acceptWhichThrows(writer);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void forTopLevelObject(Consumer<OutputWriter> consumer) {
            try {
                withExceptionHandling(writer -> {
                    writer.writeStartObject();
                    consumer.accept(this);
                    writer.writeEndObject();
                });
            } catch (Exception e) {
                makeOutputAnInvalidJSON();
                throw e;
            }
        }

        private void forTopLevelArray(Consumer<OutputListWriter> consumer) {
            try {
                withExceptionHandling(writer -> {
                    new JsonOutputListWriter(this).startArrayWithoutName(consumer);
                });
            } catch (Exception e) {
                makeOutputAnInvalidJSON();
                throw e;
            }
        }

        private void makeOutputAnInvalidJSON() {
            try {
                // we perform a writeRaw because the writer does not allow emitting things that will
                // otherwise generate bad json
                this.jacksonWriter.writeRaw("{");
                this.jacksonWriter.writeRaw("\"Failed due to an exception. Please check the logs.\"");
            } catch (IOException ignored) {
            }
        }

        @Override
        public void close() {
            if (!this.jacksonWriter.isClosed()) {
                withExceptionHandling(JsonGenerator::close);
            }
        }

        public class JsonOutputChildWriter {
            private String key;
            private JsonOutputWriterUsingJackson parentWriter;

            JsonOutputChildWriter(String key, JsonOutputWriterUsingJackson parentWriter) {
                this.key = key;
                this.parentWriter = parentWriter;
            }

            public JsonOutputWriterUsingJackson body(Consumer<OutputWriter> consumer) {
                return parentWriter.withExceptionHandling((jacksonWriter) -> {
                    jacksonWriter.writeFieldName(key);
                    jacksonWriter.writeStartObject();
                    consumer.accept(parentWriter);
                    jacksonWriter.writeEndObject();
                });
            }
        }


        public class JsonOutputListWriter implements OutputListWriter {
            private final JsonOutputWriterUsingJackson parentWriter;

            JsonOutputListWriter(JsonOutputWriterUsingJackson parentWriter) {
                this.parentWriter = parentWriter;
            }

            private JsonOutputWriterUsingJackson body(String key, Consumer<OutputListWriter> consumer) {
                return parentWriter.withExceptionHandling((jacksonWriter) -> {
                    jacksonWriter.writeFieldName(key);
                    startArrayWithoutName(consumer);
                });
            }

            private void startArrayWithoutName(Consumer<OutputListWriter> consumer) {
                parentWriter.withExceptionHandling(jacksonWriter -> {
                    jacksonWriter.writeStartArray();
                    consumer.accept(this);
                    jacksonWriter.writeEndArray();
                });
            }

            @Override
            public JsonOutputListWriter value(String value) {
                parentWriter.withExceptionHandling((jacksonWriter) -> jacksonWriter.writeString(value));
                return this;
            }

            @Override
            public JsonOutputListWriter addChild(Consumer<OutputWriter> consumer) {
                parentWriter.withExceptionHandling((jacksonWriter) -> {
                    jacksonWriter.writeStartObject();
                    consumer.accept(parentWriter);
                    jacksonWriter.writeEndObject();
                });
                return this;
            }
        }


        public class JsonOutputLinkWriter implements OutputLinkWriter {
            private OutputWriter parentWriter;

            JsonOutputLinkWriter(OutputWriter parentWriter) {
                this.parentWriter = parentWriter;
            }

            @Override
            public JsonOutputLinkWriter addLink(String key, String href) {
                return addAbsoluteLink(key, requestContext.build(key, href).getHref());
            }

            @Override
            public JsonOutputLinkWriter addAbsoluteLink(String key, String href) {
                parentWriter.addChild(key, innerChildWriter -> {
                    innerChildWriter.add("href", href);
                });
                return this;
            }
        }
    }

    public static String jsonDate(Date value) {
        return value == null ? null : ISO8601Utils.format(value, false, UTC);
    }

}
