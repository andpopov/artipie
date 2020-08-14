/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.headers.Authorization;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsStatus;
import com.artipie.metrics.Metrics;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Slice reporting response metrics.
 * Requests and responses are forwarded to origin {@link Slice}.
 * Returned response reported to metrics as success or error if it's status code is 4xx or 5xx.
 *
 * @since 0.9
 * @todo #231:30min Report exceptions as errors in `ResponseMetricsSlice`.
 *  In case of exceptions in origin slice handling or sending to `Connection` nothing is reported
 *  to `Metrics`. It needs to be fixed, so if origin slice fails or we failed to send response
 *  then error is sent to `Metrics`.
 */
public final class ResponseMetricsSlice implements Slice {

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Metrics for reporting.
     */
    private final Metrics metrics;

    /**
     * Ctor.
     *
     * @param origin Origin slice.
     * @param metrics Metrics for reporting.
     */
    public ResponseMetricsSlice(final Slice origin, final Metrics metrics) {
        this.origin = origin;
        this.metrics = metrics;
    }

    @Override
    public Response response(
        final String rqline,
        final Iterable<Map.Entry<String, String>> rqheaders,
        final Publisher<ByteBuffer> rqbody) {
        return connection -> this.origin.response(rqline, rqheaders, rqbody).send(
            (rsstatus, rsheaders, rsbody) -> {
                this.report(rqline, rqheaders, rsstatus);
                return connection.accept(rsstatus, rsheaders, rsbody);
            }
        );
    }

    /**
     * Report response to metrics.
     *
     * @param rqline Request line.
     * @param rqheaders Request headers.
     * @param rsstatus Response status.
     * @todo #231:30min Remove ad-hoc error detection from `ResponseMetricsSlice.report()`.
     *  As soon as issue https://github.com/artipie/http/issues/196 is complete and we have
     *  method in `RsStatus` to detect errors we may use this method instead of RegEx usage.
     */
    private void report(
        final String rqline,
        final Iterable<Map.Entry<String, String>> rqheaders,
        final RsStatus rsstatus
    ) {
        if (rsstatus.code().matches("^[45].+$")) {
            this.report(rqline, "error");
            if (rsstatus.equals(RsStatus.UNAUTHORIZED)) {
                this.reportUnauthorized(rqline, rqheaders);
            }
        } else {
            this.report(rqline, "success");
        }
    }

    /**
     * Report unauthorized response to metrics.
     *
     * @param rqline Request line.
     * @param rqheaders Request headers.
     */
    private void reportUnauthorized(
        final String rqline,
        final Iterable<Map.Entry<String, String>> rqheaders
    ) {
        if (new RqHeaders(rqheaders, Authorization.NAME).isEmpty()) {
            this.report(rqline, "error.no-auth");
        } else {
            this.report(rqline, "error.bad-auth");
        }
    }

    /**
     * Report response to metrics.
     *
     * @param rqline Request line.
     * @param result Response result string.
     */
    private void report(final String rqline, final String result) {
        final String name = String.format(
            "%s.%s",
            new RequestLineFrom(rqline).method().value().toLowerCase(Locale.getDefault()),
            result
        );
        this.metrics.counter(name).inc();
    }
}
