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
    private  final String TAG = "SocketIO_Utils";
    public  IO.Options options = new IO.Options();
    public  Dispatcher dispatcher;
    public  String uri = "https://192.168.0.11:3333";
    public  SSLContext sslContext = null;

    public  boolean isInitiator;
    public  boolean isStarted;
    public  boolean isChannelReady;
//    public SocketIO_RTCClient RTCClient;


    public Socket mSocket;

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
                String type = message.getString("type");

//                if (type.equals("candidate")) {
//                    events.onRemoteIceCandidate(toJavaCandidate(json));
//                } else if (type.equals("remove-candidates")) {    //준화코드는 remove-candidates은 없음
//                    JSONArray candidateArray = json.getJSONArray("candidates");
//                    IceCandidate[] candidates = new IceCandidate[candidateArray.length()];
//                    for (int i = 0; i < candidateArray.length(); ++i) {
//                        candidates[i] = toJavaCandidate(candidateArray.getJSONObject(i));
//                    }
//                    events.onRemoteIceCandidatesRemoved(candidates);
//                } else if (type.equals("answer")) {
//                    if (initiator) {
//                        SessionDescription sdp = new SessionDescription(
//                                SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));
//                        events.onRemoteDescription(sdp);
//                    } else {
//                        reportError("Received answer for call initiator: " + msg);
//                    }
//                } else if (type.equals("offer")) {
//                    if (!initiator) {
//                        SessionDescription sdp = new SessionDescription(
//                                SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));     //java 내부의 SessionDescription class에 타입이 이미 OFFER, PRANSWER, ANSWER, ROLLBACK으로 정의되어있음
//                        events.onRemoteDescription(sdp);
//                    } else {
//                        reportError("Received offer for call receiver: " + msg);
//                    }
//                } else if (type.equals("bye")) {
//                    events.onChannelClose();
//                } else {
//                    reportError("Unexpected WebSocket message: " + msg);
//                }

//                if (message.get("type").equals("offer")){
//                    if(!isInitiator && !isStarted){
////                        maybeStart();         ///////////////
//                    }
//                }
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

    public  void sendToPeer(String message) {
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

    public  void sendToPeer(Object packet) {
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
    public void closeClient(Socket socket){
        socket.disconnect();
        dispatcher.executorService().shutdown();
    }

}