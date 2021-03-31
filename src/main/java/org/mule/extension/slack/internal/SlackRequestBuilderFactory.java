package org.mule.extension.slack.internal;

import static java.lang.String.valueOf;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import java.util.concurrent.CompletableFuture;

public class SlackRequestBuilderFactory {

    private static final String API_URI = "https://slack.com/api/";
    private static final String AUTHORIZATION = "Authorization";

    private final HttpClient httpClient;
    private final String token;
    private final String authHeader;
    private final int responseTimeout;

    public SlackRequestBuilderFactory(HttpClient httpClient, String token) {
        this(httpClient, token, 5000);
    }

    public SlackRequestBuilderFactory(HttpClient httpClient, String token, int responseTimeout) {
        this.httpClient = httpClient;
        this.token = token;
        this.authHeader = "Bearer " + token;
        this.responseTimeout = responseTimeout;
    }

    public SlackRequestBuilder newRequest(String slackMethod) {
        return new SlackRequestBuilder(slackMethod);
    }

    public class SlackRequestBuilder {
        private final String uri;
        private final MultiMap<String, String> params;

        SlackRequestBuilder(String slackMethod) {
            this.params = new MultiMap<>();
            this.uri = API_URI + slackMethod;
        }

        public SlackRequestBuilder withOptionalParam(String name, Object value) {
            if (value != null) {
                this.params.put(name, valueOf(value));
            }
            return this;
        }

        public CompletableFuture<HttpResponse> sendAsyncRequest() {
            return httpClient.sendAsync(HttpRequest.builder()
                    .method(GET)
                    .uri(uri)
                    .addHeader(AUTHORIZATION, authHeader)
                    .queryParams(params)
                    .build(), responseTimeout, true, null);
        }

    }
}
