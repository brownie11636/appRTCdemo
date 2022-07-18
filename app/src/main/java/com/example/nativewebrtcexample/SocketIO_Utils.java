package com.example.nativewebrtcexample;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private static final String TAG = "SocketIO_Utils";
    public static IO.Options options = new IO.Options();
    public static Dispatcher dispatcher;
    public static String uri = "https://192.168.0.11:3333";
    public static SSLContext sslContext = null;

    public static boolean isInitiator;
    public static boolean isStarted;
    public static boolean isChannelReady;
//    public SocketIO_RTCClient RTCClient;


    public static io.socket.client.Socket mSocket;

//    public SocketIO_Utils(Manager io, String nsp, Manager.Options opts) {
//        super(io, nsp, opts);
//    }

    public static synchronized Socket init() {
        options = new IO.Options();
        options.transports = new String[]{WebSocket.NAME};

        dispatcher = new Dispatcher();

        setSSLOkHttp(options, sslContext, dispatcher);

        if(mSocket == null) {
            try {
                mSocket = IO.socket(uri, options);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        mSocket.on("msg-v1", packet -> {
//            Log.i(TAG,"packet from Peer: " + packet);
//            Log.i(TAG, "type of packet: " + packet.getClass().getName());
//            Log.i(TAG, "packet[0]: " + packet[0]);

            try {
                Log.i(TAG, "received from: " + ((JSONObject)packet[0]).getString("from") +" to: "+ ((JSONObject)packet[0]).getString("to"));
                JSONObject message = (JSONObject)((JSONObject) packet[0]).get("message");
                Log.i(TAG,"message: " + message);
                Log.i(TAG, "type: " + message.getString("type"));

//                RTCClient.RoomConnectionParameters.roomUrl = uri;
//                RTCClient.RoomConnectionParameters.roomId = uri;
//                RTCClient.RoomConnectionParameters.roomUrl = uri;
//                RTCClient.RoomConnectionParameters.roomUrl = uri;

                if (message.get("type").equals("offer")){
                    if(!isInitiator && !isStarted){
//                        maybeStart();         ///////////////
                    }


                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        mSocket.on("created", (room) -> {
            Log.i(TAG,"Created room " + room);
            isInitiator = true;
        });

        mSocket.on("full", (room) -> {
            Log.i(TAG,"Room " + room + " is full");
        });

        mSocket.on("join", (room) -> {
            Log.i(TAG,"Another peer made a request to join room " + room);
            Log.i(TAG,"This peer is the initiator of room " + room + "!");
            isChannelReady = true;
//            RTCClient.RoomConnectionParameters.roomId = room;
        });

        mSocket.on("joined", (room) -> {
            Log.i(TAG,"joined: " + room[0]);
            isChannelReady = true;
        });

        mSocket.on("log", (array) -> {
            Log.i(TAG, String.valueOf(array));
        });

        // Socket서버에 connect 된 후, 서버로부터 전달받은 'socket.EVENT_CONNECT' Event 처리.
        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                // your code...
                Log.i(TAG,"onConnect? ");

                Log.i(TAG,"onConnect socketID is " + mSocket.id());
                mSocket.emit("connectReceive", "OK");
            }
        });

//        JSONObject query = new JSONObject();
        JSONArray query = new JSONArray();
        try {
            query.put(new JSONObject().put("header","ServiceList").put("filter", null));
//            query.put(new JSONObject().put("filter", null));
            Log.i(TAG,"request query:" + query);
            mSocket.emit("q_service",query);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mSocket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
//                options.auth.put("authorization", "bearer 1234");
                Log.d(TAG, "EVENT_CONNECT_ERROR " + mSocket.id());
                mSocket.connect();
            }
        });

        mSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.i(TAG,"disconnected "+ mSocket.id()); // null
            }
        });

        mSocket.on("socketID", socketID -> {
            Log.i(TAG,"ID is "+ socketID);
        });

        return mSocket;
    }

    private static void maybeStart() {
//        Log.i(TAG,">>>>>>>maybeStart()"+isStarted+LocalStream+isChannelReady);
    }

    public static void sendToPeer(String message) {
        try {
            JSONObject packet = new JSONObject().put("from",mSocket.id())
                    .put("to",null).put("message",message);
            mSocket.emit("msg-v1",packet);
            Log.i(TAG,"send message to Peer: " + packet);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"fail to create a packet");
        }
    }

    public static void sendToPeer(Object packet) {
        mSocket.emit("msg-v1",packet);
        Log.i(TAG,"send message to Peer: " + packet);
    }

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
    public static void closeClient(){
        mSocket.disconnect();
        dispatcher.executorService().shutdown();
    }



}