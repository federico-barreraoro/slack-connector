package org.mule.extension.slack.internal.connection;

import static java.lang.String.valueOf;
import static org.mule.extension.slack.internal.connection.SlackMethods.API_URI;
import static org.mule.extension.slack.internal.connection.SlackMethods.CONVERSATIONS_LIST;
import static org.mule.extension.slack.internal.connection.SlackMethods.IMS_LIST;
import static org.mule.extension.slack.internal.connection.SlackMethods.RTM_CONNECT;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;

import org.mule.extension.slack.internal.ConversationListingConfiguration;
import org.mule.extension.slack.internal.ConversationTypes;
import org.mule.extension.slack.internal.SlackRequestBuilderFactory;
import org.mule.extension.slack.internal.connection.category.Auth;
import org.mule.extension.slack.internal.connection.category.Channel;
import org.mule.extension.slack.internal.connection.category.Chat;
import org.mule.extension.slack.internal.connection.category.Conversations;
import org.mule.extension.slack.internal.connection.category.Dialog;
import org.mule.extension.slack.internal.connection.category.File;
import org.mule.extension.slack.internal.connection.category.Group;
import org.mule.extension.slack.internal.connection.category.IM;
import org.mule.extension.slack.internal.connection.category.Reactions;
import org.mule.extension.slack.internal.connection.category.User;
import org.mule.extension.slack.internal.connection.category.Views;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.http.api.HttpConstants;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.client.proxy.ProxyConfig;
import org.mule.runtime.http.api.domain.entity.HttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import javax.inject.Inject;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SlackConnection {

    private static final String AUTHORIZATION = "Authorization";
    private final SlackRequestBuilderFactory requestBuilderFactory;
    public Conversations conversations;
    public Channel channel;
    public Chat chat;
    public Dialog dialog;
    public Group group;
    public File file;
    public User user;
    public IM im;
    public Auth auth;
    public Views views;
    public Reactions reactions;
    private HttpClient httpClient;
    private String token;
    private String authHeader;
    @Inject
    private ExpressionManager expressionManager;

    SlackConnection(String token, HttpService httpService, ProxyConfig proxyConfig) {
        this.token = token;
        this.authHeader = "Bearer " + token;
        initHttpClient(httpService, proxyConfig);
        requestBuilderFactory = new SlackRequestBuilderFactory(httpClient, token);
        channel = new Channel(this);
        chat = new Chat(this);
        group = new Group(this);
        file = new File(this);
        user = new User(this);
        im = new IM(this);
        auth = new Auth(this);
        dialog = new Dialog(this);
        views = new Views(this);
        reactions = new Reactions(this);
        conversations = new Conversations(this);
    }

    public static <T> void ifPresent(T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    public CompletableFuture<HttpResponse> imsList(String cursor, Integer limit) {
        MultiMap<String, String> parameterMap = new MultiMap<>();
        parameterMap.put("cursor", cursor);
        parameterMap.put("limit", valueOf(limit));
        return sendAsyncRequest(API_URI + IMS_LIST, parameterMap);
    }

    public CompletableFuture<HttpResponse> conversationList(ConversationTypes conversationTypes, ConversationListingConfiguration listingConfiguration) {
        return requestBuilderFactory.newRequest(CONVERSATIONS_LIST)
                .withOptionalParam("types", conversationTypes.getRequiredTypes())
                .withOptionalParam("exclude_archived", listingConfiguration.isExcludeArchive())
                .withOptionalParam("cursor", listingConfiguration.getCursor())
                .withOptionalParam("limit", listingConfiguration.getLimit())
                .sendAsyncRequest();
    }

    public CompletableFuture<HttpResponse> sendAsyncRequest(String uri, MultiMap<String, String> parameterMap) {
        return sendAsyncRequest(GET, uri, parameterMap, null);
    }

    public CompletableFuture<HttpResponse> sendAsyncRequest(HttpConstants.Method method, String uri, MultiMap<String, String> parameterMap, HttpEntity httpEntity) {

        HttpRequestBuilder builder = HttpRequest.builder();

        if (httpEntity != null) {
            builder.entity(httpEntity);
        }

        return httpClient.sendAsync(builder
                .addHeader(AUTHORIZATION, authHeader)
                .method(method)
                .uri(uri)
                .queryParams(parameterMap)
                .build(), 5000, true, null);
    }

    public CompletableFuture<HttpResponse> getWebSocketURI() {
        MultiMap<String, String> parameterMap = new MultiMap<>();

        return httpClient.sendAsync(HttpRequest.builder()
                .addHeader(AUTHORIZATION, authHeader)
                .method(GET)
                .uri(API_URI + RTM_CONNECT)
                .queryParams(parameterMap)
                .build(), 5000, true, null);
    }

    public void disconnect() {
        httpClient.stop();
    }

    public ExpressionManager getExpressionManager() {
        return expressionManager;
    }

    private void initHttpClient(HttpService httpService, ProxyConfig proxyConfig) {
        HttpClientConfiguration.Builder builder = new HttpClientConfiguration.Builder();
        if (proxyConfig != null) {
            builder.setProxyConfig(proxyConfig);
        }
        builder.setName("slack");
        httpClient = httpService.getClientFactory().create(builder.build());
        httpClient.start();
    }
}
