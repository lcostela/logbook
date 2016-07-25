package org.zalando.logbook.httpclient;

/*
 * #%L
 * Logbook: HTTP Client
 * %%
 * Copyright (C) 2015 - 2016 Zalando SE
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;
import org.zalando.logbook.Correlator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

public final class LogbookHttpAsyncResponseConsumer<T> extends ForwardingHttpAsyncResponseConsumer<T> {

    private final HttpAsyncResponseConsumer<T> consumer;
    private HttpResponse response;

    public LogbookHttpAsyncResponseConsumer(final HttpAsyncResponseConsumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    protected HttpAsyncResponseConsumer<T> delegate() {
        return consumer;
    }

    @Override
    public void responseReceived(final HttpResponse response) throws IOException, HttpException {
        this.response = response;
        delegate().responseReceived(response);
    }

    @Override
    public void responseCompleted(final HttpContext context) {
        findCorrelator(context).ifPresent(correlator -> {
            try {
                correlator.write(new RemoteResponse(response));
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        delegate().responseCompleted(context);
    }

    private Optional<Correlator> findCorrelator(final HttpContext context) {
        return Optional.ofNullable(context.getAttribute(Attributes.CORRELATOR)).map(Correlator.class::cast);
    }

}
