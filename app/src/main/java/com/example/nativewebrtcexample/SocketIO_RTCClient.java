/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.example.nativewebrtcexample;

//import static com.example.nativewebrtcexample.SocketIO_Utils.mSocket;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import androidx.annotation.Nullable;
import com.example.nativewebrtcexample.RoomParametersFetcher.RoomParametersFetcherEvents;
import com.example.nativewebrtcexample.WebSocketChannelClient.WebSocketChannelEvents;
import com.example.nativewebrtcexample.WebSocketChannelClient.WebSocketConnectionState;
import com.example.nativewebrtcexample.util.AsyncHttpURLConnection;
import com.example.nativewebrtcexample.util.AsyncHttpURLConnection.AsyncHttpEvents;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Negotiates signaling for chatting with https://appr.tc "rooms".
 * Uses the client<->server specifics of the apprtc AppEngine webapp.
 *
 * <p>To use: create an instance of this object (registering a message handler) and
 * call connectToRoom().  Once room connection is established
 * onConnectedToRoom() callback with room parameters is invoked.
 * Messages to other party (with local Ice candidates and answer SDP) can
 * be sent after WebSocket connection is established.
 */
public class SocketIO_RTCClient implements AppRTCClient {
  private static final String TAG = "SOCKETIO_RTCClient";
  private static final String ROOM_JOIN = "join";
  private static final String ROOM_MESSAGE = "message";
  private static final String ROOM_LEAVE = "leave";

  public enum ConnectionState { NEW, CONNECTED, CLOSED, ERROR }

  private enum MessageType { MESSAGE, LEAVE }

  private final Handler handler;
  private boolean initiator;
  private SignalingEvents events;
  private WebSocketChannelClient wsClient;
  private ConnectionState roomState;    //=>준화 코드에서 isChannelReady
  private RoomConnectionParameters connectionParameters;
  private String messageUrl;
  private String leaveUrl;
  private Socket socket;
  private SocketIO_Utils mSocketUtils;
  private MyService service;
  private RoomParametersFetcher roomFetcher;

//  public  boolean isInitiator;
  public  boolean isStarted;
//  public  boolean isChannelReady; //방에 들어갔는지

  public SocketIO_RTCClient(SignalingEvents events) {   //call Activity가 SignalingEvent 구현한것
    this.events = events;
    roomState = ConnectionState.NEW;
    final HandlerThread handlerThread = new HandlerThread(TAG);
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  // --------------------------------------------------------------------
  // AppRTCClient interface implementation.
  // Asynchronously connect to an AppRTC room URL using supplied connection
  // parameters, retrieves room parameters and connect to WebSocket server.
  @Override
  //callActivity의 startCall()에서 실행
  public void connectToRoom(RoomConnectionParameters connectionParameters) {
    //이 RoomConnectionParameter는 connectActivity에서 callActvity부를 때 같이 넘기도록 하자
    this.connectionParameters = connectionParameters; //RoomConnectionParameters(roomUri.toString(), roomId, loopback, urlParameters)

    handler.post(new Runnable() {
      @Override
      public void run() {
        connectToRoomInternal();
      }
    });
  }

  @Override
  public void disconnectFromRoom() {
    handler.post(new Runnable() {
      @Override
      public void run() {
        disconnectFromRoomInternal();
        handler.getLooper().quit();
      }
    });
  }

  // Connects to room - function runs on a local looper thread.
  private void connectToRoomInternal() {
//    String connectionUrl = getConnectionUrl(connectionParameters);
    String roomUri = connectionParameters.roomUrl;
    String roomId = connectionParameters.roomId;
    Log.d(TAG, "Connect to room: " + roomId + " of " + roomUri);
    roomState = ConnectionState.NEW;
//    wsClient = new WebSocketChannelClient(handler, this);
    isStarted = false;
    mSocketUtils = new SocketIO_Utils();
    mSocketUtils.init();   //사실 URL은 init함수 안에 들어가 있음

    mSocketUtils.socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        // your code...
        Log.i(TAG, "@RTCClient, socketID is " + mSocketUtils.socket.id());
//                mSocketUtils.socket.emit("connectReceive", "OK");
        mSocketUtils.id = mSocketUtils.socket.id();
        Log.i(TAG, "SocketUtils ID is " + mSocketUtils.id);

        roomFetcher.makeRequest();
      }
    });

    mSocketUtils.socket.on("msg-v1",(packet)->{
      String from = null;
      String to = null;
      JSONObject message = null;
      try {
        from = ((JSONObject) packet[0]).getString("from");
        to = ((JSONObject) packet[0]).getString("to");
        message = ((JSONObject) packet[0]).getJSONObject("message");
      } catch (JSONException e) {
        e.printStackTrace();
      }
      Log.i(TAG,"msg-v1 :" + packet);

//      if(!isStarted) {
//        roomFetcher.msgResponseParse((JSONObject) packet[0]);
//      }
      if(isStarted) {
        onMessageFromPeer(message);
      }
    });

    mSocketUtils.socket.connect();

    service = new MyService(connectionParameters.profile);
//    JSONObject profile = connectionParameters.profile;

    RoomParametersFetcherEvents callbacks = new RoomParametersFetcherEvents() {
      @Override
      public void onSignalingParametersReady(final SignalingParameters params) {
        /**
         * Callback fired once the room's signaling parameters
         * SignalingParameters are extracted.
         */
        SocketIO_RTCClient.this.handler.post(new Runnable() {
          @Override
          public void run() {
            SocketIO_RTCClient.this.signalingParametersReady(params);
//            mSocketUtils.sendToPeer("asdasfdfasdf");
            //대충 signaling parameter가 준비되면 이 함수를 실행하라는건데 signalingParameter를 어디서..?
            //roomParameterFetcher에서 가져오는군 여기를 고쳐야겠는데
          }
        });
      }

      @Override
      public void onSignalingParametersError(String description) {
        SocketIO_RTCClient.this.reportError(description);
      }
    };
// RoomPrarmetersFetcher가 받는 인자에 socket 추가 그리고 connectionUrl대신 RoomId
//    new RoomParametersFetcher(connectionUrl, null, callbacks).makeRequest();
    //지금 mSocketUtils.init이 끝나기전에 makerequest 함수가 돌아버리는것 같음
    //makeRequest()를 socket onConnect에 넣음;
    roomFetcher = new RoomParametersFetcher(service, isStarted, roomId, null, callbacks, mSocketUtils);




  }

  // Disconnect from room and send bye messages - runs on a local looper thread.
  private void disconnectFromRoomInternal() {
    Log.d(TAG, "Disconnect. Room state: " + roomState);
    if (roomState == ConnectionState.CONNECTED) {
      Log.d(TAG, "Closing room.");
      sendPostMessage(MessageType.LEAVE, leaveUrl, null);
    }
    roomState = ConnectionState.CLOSED;
    if (wsClient != null) {
      wsClient.disconnect(true);
    }
  }

  // Helper functions to get connection, post message and leave message URLs
  private String getConnectionUrl(RoomConnectionParameters connectionParameters) {
    return connectionParameters.roomUrl + "/" + ROOM_JOIN + "/" + connectionParameters.roomId
        + getQueryString(connectionParameters);
  }

  private String getMessageUrl(
      RoomConnectionParameters connectionParameters, SignalingParameters signalingParameters) {
    return connectionParameters.roomUrl + "/" + ROOM_MESSAGE + "/" + connectionParameters.roomId
        + "/" + signalingParameters.clientId + getQueryString(connectionParameters);
  }

  private String getLeaveUrl(
      RoomConnectionParameters connectionParameters, SignalingParameters signalingParameters) {
    return connectionParameters.roomUrl + "/" + ROOM_LEAVE + "/" + connectionParameters.roomId + "/"
        + signalingParameters.clientId + getQueryString(connectionParameters);
  }

  private String getQueryString(RoomConnectionParameters connectionParameters) {
    if (connectionParameters.urlParameters != null) {
      return "?" + connectionParameters.urlParameters;
    } else {
      return "";
    }
  }

  // Callback issued when room parameters are extracted. Runs on local
  // looper thread.
  private void signalingParametersReady(final SignalingParameters signalingParameters) {
    Log.d(TAG, "Room connection completed.");
    if (connectionParameters.loopback
        && (!signalingParameters.initiator || signalingParameters.offerSdp != null)) {
      reportError("Loopback room is busy.");
      return;
    }
    if (!connectionParameters.loopback && !signalingParameters.initiator
        && signalingParameters.offerSdp == null) {
      Log.w(TAG, "No offer SDP in room response.");
    }
    initiator = signalingParameters.initiator;
    messageUrl = getMessageUrl(connectionParameters, signalingParameters);  //to:
    leaveUrl = getLeaveUrl(connectionParameters, signalingParameters);      //from:
    Log.d(TAG, "Message URL: " + messageUrl);
    Log.d(TAG, "Leave URL: " + leaveUrl);
    roomState = ConnectionState.CONNECTED;

    // Fire connection and signaling parameters events.
    //얘는 call activity에 있을듯
    //onConnectedToRoomInternal()로 연결됨
    //signalingParams를 받아서 local device에 맞는 PeerConnection 및 그 설정을 만드는듯
    // initiator인 상황에서는 offerSdp, iceCandidate유무에 따라 관련 PC함수 실행을 하기도 함.
    events.onConnectedToRoom(signalingParameters);


    // Connect and register WebSocket client.
    //웹소켓 주소설정 우리는 필요없을듯
//    wsClient.connect(signalingParameters.wssUrl, signalingParameters.wssPostUrl);
    //위의 주소에 연결된 웹소켓에서 특정 room 및 client에 등록
    //내가만든 sendToPeer()함수처럼 기능할수 있게 하는늒?ㅣㅁ
//    wsClient.register(connectionParameters.roomId, signalingParameters.clientId);
  }

  // Send local offer SDP to the other participant.
  @Override
  public void sendOfferSdp(final SessionDescription sdp) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (roomState != ConnectionState.CONNECTED) {
          reportError("Sending offer SDP in non connected state.");
          return;
        }
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "offer");
//        sendPostMessage(MessageType.MESSAGE, messageUrl, json.toString());
        mSocketUtils.sendToPeer(json.toString());
        if (connectionParameters.loopback) {
          // In loopback mode rename this offer to answer and route it back.
          SessionDescription sdpAnswer = new SessionDescription(
              SessionDescription.Type.fromCanonicalForm("answer"), sdp.description);
          events.onRemoteDescription(sdpAnswer);
        }
      }
    });
  }

  // Send local answer SDP to the other participant.
  @Override
  public void sendAnswerSdp(final SessionDescription sdp) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (connectionParameters.loopback) {
          Log.e(TAG, "Sending answer in loopback mode.");
          return;
        }
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "answer");
          //sendPostMessage를 안쓰고 이걸 쓴이유는..?
        // 얘들은 방 정보를 긁어 오고 나서 WS connection을 만들어서 그전에 offer보낼수도 있는 것들은
        //http 통신으로 한듯?
//        wsClient.send(json.toString());
        mSocketUtils.sendToPeer(json.toString());
      }
    });
  }

  // Send Ice candidate to the other participant.
  @Override
  public void sendLocalIceCandidate(final IceCandidate candidate) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "candidate");
        jsonPut(json, "label", candidate.sdpMLineIndex);
        jsonPut(json, "id", candidate.sdpMid);
        jsonPut(json, "candidate", candidate.sdp);
        if (initiator) {
          // Call initiator sends ice candidates to GAE server.
          if (roomState != ConnectionState.CONNECTED) {
            reportError("Sending ICE candidate in non connected state.");
            return;
          }
//          sendPostMessage(MessageType.MESSAGE, messageUrl, json.toString());
          mSocketUtils.sendToPeer(json.toString());

          if (connectionParameters.loopback) {
            events.onRemoteIceCandidate(candidate);
          }
        } else {
          // Call receiver sends ice candidates to websocket server.
//          wsClient.send(json.toString());
          mSocketUtils.sendToPeer(json.toString());
        }
      }
    });
  }

  // Send removed Ice candidates to the other participant.
  @Override
  public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "remove-candidates");
        JSONArray jsonArray = new JSONArray();
        for (final IceCandidate candidate : candidates) {
          jsonArray.put(toJsonCandidate(candidate));
        }
        jsonPut(json, "candidates", jsonArray);
        if (initiator) {
          // Call initiator sends ice candidates to GAE server.
          if (roomState != ConnectionState.CONNECTED) {
            reportError("Sending ICE candidate removals in non connected state.");
            return;
          }
//          sendPostMessage(MessageType.MESSAGE, messageUrl, json.toString());
          mSocketUtils.sendToPeer(json.toString());
          if (connectionParameters.loopback) {
            events.onRemoteIceCandidatesRemoved(candidates);
          }
        } else {
          // Call receiver sends ice candidates to websocket server.
//          wsClient.send(json.toString());
          mSocketUtils.sendToPeer(json.toString());
        }
      }
    });
  }

  // --------------------------------------------------------------------
  public void onMessageFromPeer(final JSONObject message) {
//    if (wsClient.getState() != WebSocketConnectionState.REGISTERED) {
//      Log.e(TAG, "Got WebSocket message in non registered state.");
//      return;
//    }
    try {
      String type = message.getString("type");     //getString의 경우 키에 해당하는 값이 없는 경우 JsonException을 발생시키는 반면    https://sandn.tistory.com/89
//      String errorText = json.optString("error");   //optString은 ""와 같은 빈 문자열을 반환한다.  예상치 못한 에러를 방지하기 위해서는 optString을 사용하는 것이 좋다.
//      if (msgText.length() > 0) {
//        json = new JSONObject(msgText);     //이거 같은거 보면 msgText도 json형식인듯 json안에 json
//        String type = json.optString("type");
      if (type.equals("candidate")) {
        events.onRemoteIceCandidate(toJavaCandidate(message));
//      } else if (type.equals("remove-candidates")) {    //준화코드는 remove-candidates은 없음
//        JSONArray candidateArray = json.getJSONArray("candidates");
//        IceCandidate[] candidates = new IceCandidate[candidateArray.length()];
//        for (int i = 0; i < candidateArray.length(); ++i) {
//          candidates[i] = toJavaCandidate(candidateArray.getJSONObject(i));
//        }
//        events.onRemoteIceCandidatesRemoved(candidates);
      } else if (type.equals("answer")) {
        if (initiator) {
          SessionDescription sdp = new SessionDescription(
              SessionDescription.Type.fromCanonicalForm(type), message.getString("sdp"));
          events.onRemoteDescription(sdp);
        } else {
          reportError("Received answer for call initiator: " + message);
        }
      } else if (type.equals("offer")) {
        if (!initiator) {
          SessionDescription sdp = new SessionDescription(
              SessionDescription.Type.fromCanonicalForm(type), message.getString("sdp"));     //java 내부의 SessionDescription class에 타입이 이미 OFFER, PRANSWER, ANSWER, ROLLBACK으로 정의되어있음
          events.onRemoteDescription(sdp);
        } else {
          reportError("Received offer for call receiver: " + message);
        }
      } else if (message.toString().equals("bye")) {
        events.onChannelClose();
      } else {
        reportError("Unexpected WebSocket message: " + message);
      }
//      } else {
//        if (errorText != null && errorText.length() > 0) {
//          reportError("WebSocket error message: " + errorText);
//        } else {
//          reportError("Unexpected WebSocket message: " + msg);
//        }
//      }
    } catch (JSONException e) {
      reportError("WebSocket message JSON parsing error: " + e.toString());
    }
  }

  // --------------------------------------------------------------------
  // Helper functions.
  private void reportError(final String errorMessage) {
    Log.e(TAG, errorMessage);
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (roomState != ConnectionState.ERROR) {
          roomState = ConnectionState.ERROR;
          events.onChannelError(errorMessage);
        }
      }
    });
  }

  // Put a `key`->`value` mapping in `json`.
  private static void jsonPut(JSONObject json, String key, Object value) {
    try {
      json.put(key, value);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  // Send SDP or ICE candidate to a room server.
  private void sendPostMessage(
      final MessageType messageType, final String url, @Nullable final String message) {
    String logInfo = url;
    if (message != null) {
      logInfo += ". Message: " + message;
    }
    Log.d(TAG, "C->GAE: " + logInfo);
    AsyncHttpURLConnection httpConnection =
        new AsyncHttpURLConnection("POST", url, message, new AsyncHttpEvents() {
          @Override
          public void onHttpError(String errorMessage) {
            reportError("GAE POST error: " + errorMessage);
          }

          @Override
          public void onHttpComplete(String response) {
            if (messageType == MessageType.MESSAGE) {
              try {
                JSONObject roomJson = new JSONObject(response);
                String result = roomJson.getString("result");
                if (!result.equals("SUCCESS")) {
                  reportError("GAE POST error: " + result);
                }
              } catch (JSONException e) {
                reportError("GAE POST JSON error: " + e.toString());
              }
            }
          }
        });
    httpConnection.send();
  }

  // Converts a Java candidate to a JSONObject.
  private JSONObject toJsonCandidate(final IceCandidate candidate) {
    JSONObject json = new JSONObject();
    jsonPut(json, "label", candidate.sdpMLineIndex);
    jsonPut(json, "id", candidate.sdpMid);
    jsonPut(json, "candidate", candidate.sdp);
    return json;
  }

  // Converts a JSON candidate to a Java object.
  IceCandidate toJavaCandidate(JSONObject json) throws JSONException {
    return new IceCandidate(
        json.getString("id"), json.getInt("label"), json.getString("candidate"));
  }

  public void notifyStarted(){
    this.isStarted = true;
    this.roomFetcher.isStarted = true;
  }
}
