package org.mule.modules.slack.client.rtm;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;

/**
 * Created by estebanwasinger on 8/1/15.
 */
public class SlackMessageHandler implements MessageHandler.Whole<String> {

    private String webSocketUrl;
    private Thread connectionMonitoringThread = null;
    private Session websocketSession;
    private long lastPingSent = 0;
    private volatile long lastPingAck = 0;
    private boolean reconnectOnDisconnection = true;
    private long messageId = 0;
    public EventHandler messageHandler;

    public SlackMessageHandler(String webSocketUrl) {
        this.webSocketUrl = webSocketUrl;
    }

    public void connect() throws IOException, DeploymentException, InterruptedException {
        ClientManager client = ClientManager.createClient();
        client.getProperties().put(ClientProperties.LOG_HTTP_UPGRADE, true);
        final MessageHandler handler = this;
        websocketSession = client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig config) {
                session.addMessageHandler(handler);
            }

        }, URI.create(webSocketUrl));
        startConnectionMonitoring();
        while (true) {
            Thread.sleep(2000);
        }
    }

    public Session getWebsocketSession() {
        return websocketSession;
    }

    public void onMessage(String message) {
        if (message.contains("{\"type\":\"pong\",\"reply_to\"")) {
            int rightBracketIdx = message.indexOf('}');
            String toParse = message.substring(26, rightBracketIdx);
            lastPingAck = Integer.parseInt(toParse);
        }
        if (message.contains("{\"type\":\"hello\"})")) {
            //ignore
        }
        else {
            messageHandler.onMessage(message);
        }
    }

    private synchronized long getNextMessageId() {
        return messageId++;
    }

    private void startConnectionMonitoring() {
        connectionMonitoringThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (lastPingSent != lastPingAck) {
                            // disconnection happened
                            websocketSession.close();
                            lastPingSent = 0;
                            lastPingAck = 0;
                            if (reconnectOnDisconnection) {
                                connect();
                                continue;
                            }
                        } else {
                            lastPingSent = getNextMessageId();
                            websocketSession.getBasicRemote().sendText("{\"type\":\"ping\",\"id\":" + lastPingSent + "}");
                        }
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (DeploymentException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        connectionMonitoringThread.start();
    }
}
