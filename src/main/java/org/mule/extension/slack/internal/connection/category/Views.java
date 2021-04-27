package org.mule.extension.slack.internal.connection.category;

import static org.mule.extension.slack.internal.connection.SlackConnection.ifPresent;
import static org.mule.extension.slack.internal.connection.SlackMethods.API_URI;
import static org.mule.extension.slack.internal.connection.SlackMethods.DIALOG_OPEN;
import static org.mule.extension.slack.internal.connection.SlackMethods.VIEWS_OPEN;
import static org.mule.extension.slack.internal.connection.SlackMethods.VIEWS_PUBLISH;
import static org.mule.extension.slack.internal.connection.SlackMethods.VIEWS_PUSH;
import static org.mule.extension.slack.internal.connection.SlackMethods.VIEWS_UPDATE;

import org.mule.extension.slack.internal.connection.SlackConnection;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public class Views {

    private SlackConnection slackConnection;

    public Views(SlackConnection slackConnection) {
        this.slackConnection = slackConnection;
    }

    public CompletableFuture<HttpResponse> open(InputStream view,
                                                String triggerId) {
        MultiMap<String, String> parameterMap = new MultiMap<>();
        parameterMap.put("trigger_id", triggerId);
        parameterMap.put("view", IOUtils.toString(view));

        return slackConnection.sendAsyncRequest(API_URI + VIEWS_OPEN, parameterMap);
    }

    public CompletableFuture<HttpResponse> push(InputStream view,
                                                String triggerId) {
        MultiMap<String, String> parameterMap = new MultiMap<>();
        parameterMap.put("trigger_id", triggerId);
        parameterMap.put("view", IOUtils.toString(view));

        return slackConnection.sendAsyncRequest(API_URI + VIEWS_PUSH, parameterMap);
    }

    public CompletableFuture<HttpResponse> update(InputStream view,
                                                String externalId,
                                                  String hash,
                                                  String viewId) {
        MultiMap<String, String> parameterMap = new MultiMap<>();
        parameterMap.put("view", IOUtils.toString(view));
        ifPresent(externalId, (h) -> parameterMap.put("external_id", h));
        ifPresent(hash, (h) -> parameterMap.put("hash", h));
        ifPresent(viewId, (h) -> parameterMap.put("view_id", h));

        return slackConnection.sendAsyncRequest(API_URI + VIEWS_UPDATE, parameterMap);
    }

    public CompletableFuture<HttpResponse> publish(InputStream view,
                                                  String userId,
                                                  String hash) {
        MultiMap<String, String> parameterMap = new MultiMap<>();
        parameterMap.put("view", IOUtils.toString(view));
        parameterMap.put("user_id", userId);
        ifPresent(hash, (h) -> parameterMap.put("hash", h));

        return slackConnection.sendAsyncRequest(API_URI + VIEWS_PUBLISH, parameterMap);
    }
}
