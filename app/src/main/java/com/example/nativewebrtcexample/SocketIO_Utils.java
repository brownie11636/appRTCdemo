package com.example.nativewebrtcexample;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

//import de.tavendo.autobahn.WebSocket;

public class SocketIO_Utils {

    //    public static socket socket;
    private final String TAG = "SocketIO_Utils";
    public IO.Options options = new IO.Options();
    public Dispatcher dispatcher;
    public String uri = "https://192.168.0.11:3333";
    public SSLContext sslContext = null;
    public String id = null;

    public  boolean isInitiator;
//    public  boolean isStarted;
    public  boolean isChannelReady; //방에 들어갔는지
//    public SocketIO_RTCClient RTCClient;


    public Socket socket;

/////////from webSockerRTCClient
////    private static final String TAG = "SOCKETIO_RTCClient";
//    private static final String ROOM_JOIN = "join";
//    private static final String ROOM_MESSAGE = "message";
//    private static final String ROOM_LEAVE = "leave";
//
//    private enum ConnectionState { NEW, CONNECTED, CLOSED, ERROR }
//
//    private enum MessageType { MESSAGE, LEAVE }
//
////    private final Handler handler;
//    private boolean initiator;
//    private SignalingEvents events;
//    private WebSocketChannelClient wsClient;
//    private ConnectionState roomState;
//    private RoomConnectionParameters connectionParameters;
//    private String messageUrl;
//    private String leaveUrl;

//    public SocketIO_Utils(SignalingEvents events) {   //call Activity가 SignalingEvent 구현한것
//        this.events = events;
//        roomState = ConnectionState.NEW;
////        final HandlerThread handlerThread = new HandlerThread(TAG);
////        handlerThread.start();
////        handler = new Handler(handlerThread.getLooper());
//    }

    public Socket init() {
        options = new IO.Options();
        options.transports = new String[]{WebSocket.NAME};

        dispatcher = new Dispatcher();

        setSSLOkHttp(options, sslContext, dispatcher);

        if(socket == null) {
            try {
                socket = IO.socket(uri, options);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
//        this.id = mSocket.id();

//        mSocket.on("msg-v1", packet -> {
//            Log.i(TAG,"packet from Peer: " + packet);
//            Log.i(TAG, "type of packet: " + packet.getClass().getName());
//            Log.i(TAG, "packet[0]: " + packet[0]);
//                });




        // Socket서버에 connect 된 후, 서버로부터 전달받은 'socket.EVENT_CONNECT' Event 처리.
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                // your code...
                Log.i(TAG,"onConnect? ");

                Log.i(TAG,"onConnect socketID is " + socket.id());
                socket.emit("connectReceive", "OK");
                id = socket.id();
                Log.i(TAG,"SocketUtils ID is " + id);

            }
        });



        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
//                options.auth.put("authorization", "bearer 1234");
                Log.d(TAG, "EVENT_CONNECT_ERROR " + socket.id());
                socket.connect();
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.i(TAG,"disconnected "+ socket.id()); // null
            }
        });

        socket.on("socketID", socketID -> {
            Log.i(TAG,"ID is "+ socketID[0]);
        });


        return socket;
    }

    private static void maybeStart() {
//        Log.i(TAG,">>>>>>>maybeStart()"+isStarted+LocalStream+isChannelReady);
    }

    public String id(){
        return this.id;
    }

    public void joinService(MyService service) {
        if(service == null) {
            Log.e(TAG,"Target service is null!!");
        } else{
            try {
                socket.emit("Join_Service", service.profile.getJSONObject("service"));
                Log.i(TAG, "send Join_Service message: " + service.profile.getJSONObject("service"));
                socket.emit("msg-v1", new JSONObject().put("from", socket.id()));
//      socketUtils.socket.emit("msg-v1", new JSONObject().put("from", socketUtils.socket.id()));
                Log.i(TAG, "send message to Peer from " + socket.id());
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "fail to send json socketId");
            }
        }
    }
    public void sendToPeer(String message) {
        try {
            JSONObject packet = new JSONObject().put("from",socket.id())
                    .put("to",null).put("message",message);
            socket.emit("msg-v1",packet);
            Log.i(TAG,"send message to Peer: " + packet);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"fail to create a packet");
        }
    }

    public void sendToPeer(JSONObject message) {
        try {
            JSONObject packet = new JSONObject().put("from",socket.id())
                    .put("to",null).put("message",message);
            socket.emit("msg-v1",packet);
            Log.i(TAG,"send message to Peer: " + packet);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"fail to create a packet");
        }
    }

//    public  void sendToPeer(JSONObject packet) {
//        socket.emit("msg-v1",packet);
//        Log.i(TAG,"send message to Peer: " + packet);
//    }

    static HostnameVerifier hostnameVerifier = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession sslSession) {
            return true;
        }
    };

    static X509TrustManager trustManager = new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    public static IO.Options setSSLOkHttp(IO.Options options, SSLContext sslContext, Dispatcher dispatcher) {
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e("mySOCKET", e.toString());
        }
        try {
            sslContext.init(null, new TrustManager[] { trustManager }, null);
        } catch (KeyManagementException e) {
            e.printStackTrace();
            Log.e("SOCKETL", e.toString());
        }

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .hostnameVerifier(hostnameVerifier)
                .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
//                .readTimeout(1, TimeUnit.MINUTES) // important for HTTP long-polling
                .readTimeout(1, TimeUnit.MINUTES) // important for HTTP long-polling
//                .minWebSocketMessageToCompress(2048)  //for websocket connections
                .build();

        // default settings for all sockets
        IO.setDefaultOkHttpWebSocketFactory((okhttp3.WebSocket.Factory) okHttpClient);        //for websocket connections
//        IO.setDefaultOkHttpCallFactory((Call.Factory) okHttpClient);         //for HTTP long-polling requests

        // set as an option
        options.transports = new String[] {WebSocket.NAME};
        options.webSocketFactory = (okhttp3.WebSocket.Factory) okHttpClient;        //for websocket connections
//        options.callFactory = (Call.Factory) okHttpClient;        //for HTTP long-polling requests
//        options.transports = new String[] {Polling.NAME};       //for polling

        return options;

    }


    //    https://socketio.github.io/socket.io-client-java/faq.html#How_to_properly_close_a_client
    public void closeClient(){
        socket.disconnect();
//        socket = null;
        dispatcher.executorService().shutdown();
    }

//    public interface socket_connec

}