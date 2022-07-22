/*
 *  Copyright 2016 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.example.nativewebrtcexample;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.socket.client.Socket;

/**
 * Implementation of AppRTCClient that uses direct TCP connection as the signaling channel.
 * This eliminates the need for an external server. This class does not support loopback
 * connections.
 */
public class myRTCClient implements AppRTCClient {
  private static final String TAG = "DirectRTCClient";
  private static final int DEFAULT_PORT = 8888;

  // Regex pattern used for checking if room id looks like an IP.
//  static final Pattern IP_PATTERN = Pattern.compile("("
//      // IPv4
//      + "((\\d+\\.){3}\\d+)|"
//      // IPv6
//      + "\\[((([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?::"
//      + "(([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?)\\]|"
//      + "\\[(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})\\]|"
//      // IPv6 without []
//      + "((([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?::(([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?)|"
//      + "(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})|"
//      // Literals
//      + "localhost"
//      + ")"
//      // Optional port number
//      + "(:(\\d+))?");

//  private final ExecutorService executor;
  private final SignalingEvents events;
  @Nullable
  private SocketIO_Utils mSocketUtils;
  private RoomConnectionParameters connectionParameters;

  private enum ConnectionState { NEW, CONNECTED, CLOSED, ERROR }

  // All alterations of the room state should be done from inside the looper thread.
  private ConnectionState roomState;

  private MyService service;
  private Boolean isInitiator;
  private Boolean isChannelReady;
  public  boolean isStarted;


  public myRTCClient(SignalingEvents events) {
    this.events = events;

//    executor = Executors.newSingleThreadExecutor();
    roomState = ConnectionState.NEW;

    mSocketUtils = new SocketIO_Utils();
    mSocketUtils.init();   //사실 URL은 init함수 안에 들어가 있음
    mSocketUtils.socket.on("msg-v1",(packet_) -> {
      JSONObject packet = (JSONObject) packet_[0];
      Log.i(TAG,"Packet from Peer: "+ packet);
      //함수로 만들어서 넣기
      onFromPeerMessage(packet);
    });

    mSocketUtils.socket.on("created", (room) -> {
      Log.i(TAG,"Created room " + room);
      isInitiator = true;
    });

    mSocketUtils.socket.on("full", (room) -> {
      Log.i(TAG,"Room " + room + " is full");
    });

    mSocketUtils.socket.on("join", (room) -> {
      Log.i(TAG,"Another peer made a request to join room " + room);
      Log.i(TAG,"This peer is the initiator of room " + room + "!");
      isChannelReady = true;
//            RTCClient.RoomConnectionParameters.roomId = room;
    });

    mSocketUtils.socket.on("joined", (room) -> {
      Log.i(TAG,"joined: " + room[0]);
      isChannelReady = true;

      if(service.getDescription().equals("Streamer")) {
        //gotStream() in robot_viewer.js
        Log.i(TAG,"Adding local stream."); //??
        mSocketUtils.sendToPeer("connection request");
      }
    });

    mSocketUtils.socket.on("log", (array) -> {
      Log.i(TAG, String.valueOf(array));
    });

    mSocketUtils.socket.on(Socket.EVENT_CONNECT,(socket) -> {
      mSocketUtils.joinService(this.service);
    });


  }

  /**
   * Connects to the room, roomId in connectionsParameters is required. roomId must be a valid
   * IP address matching IP_PATTERN.
   */
  @Override
  public void connectToRoom(RoomConnectionParameters connectionParameters) {
    this.connectionParameters = connectionParameters;

    if (connectionParameters.loopback) {
      reportError("Loopback connections aren't supported by DirectRTCClient.");
    }

//    executor.execute(new Runnable() {
//      @Override
//      public void run() {
        connectToRoomInternal();
//      }
//    });
  }

  @Override
  public void disconnectFromRoom() {
//    executor.execute(new Runnable() {
//      @Override
//      public void run() {
        disconnectFromRoomInternal();
//      }
//    });
  }

  /**
   * Connects to the room.
   *
   * Runs on the looper thread.
   */
  private void connectToRoomInternal() {
    this.roomState = ConnectionState.NEW;
    this.service = new MyService(connectionParameters.profile);

//    String endpoint = connectionParameters.roomId;

//    Matcher matcher = IP_PATTERN.matcher(endpoint);
//    if (!matcher.matches()) {
//      reportError("roomId must match IP_PATTERN for DirectRTCClient.");
//      return;
//    }

//    String ip = matcher.group(1);
//    String portStr = matcher.group(matcher.groupCount());
//    int port;

//    if (portStr != null) {
//      try {
//        port = Integer.parseInt(portStr);
//      } catch (NumberFormatException e) {
//        reportError("Invalid port number: " + portStr);
//        return;
//      }
//    } else {
//      port = DEFAULT_PORT;
//    }
    mSocketUtils.socket.connect();
//    tcpClient = new TCPChannelClient(executor, this, ip, port);
  }

  /**
   * Disconnects from the room.
   *
   * Runs on the looper thread.
   */
  private void disconnectFromRoomInternal() {
    roomState = ConnectionState.CLOSED;

    if (mSocketUtils.socket != null) {
      mSocketUtils.closeClient();
//      tcpClient.disconnect();
//      tcpClient = null;
    }
//    executor.shutdown();
  }

//  @Override
  public void sendOfferSdp(final SessionDescription sdp) {
//    executor.execute(new Runnable() {
//      @Override
//      public void run() {
        if (roomState != ConnectionState.CONNECTED) {
          reportError("Sending offer SDP in non connected state.");
          return;
        }
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "offer");
        sendMessage(json);
//      }
//    });
  }

//  @Override
  public void sendAnswerSdp(final SessionDescription sdp) {
//    executor.execute(new Runnable() {
//      @Override
//      public void run() {
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "answer");
//        sendMessage(json.toString());
//    String message = "{type: " + "answer" +", sdp: " + sdp.description + "}";
        sendMessage(json);

//      }
//    });
  }

//  @Override
  public void sendLocalIceCandidate(final IceCandidate candidate) {
//    executor.execute(new Runnable() {
//      @Override
//      public void run() {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "candidate");
        jsonPut(json, "label", candidate.sdpMLineIndex);
        jsonPut(json, "id", candidate.sdpMid);
        jsonPut(json, "candidate", candidate.sdp);

        if (roomState != ConnectionState.CONNECTED) {
          reportError("Sending ICE candidate in non connected state.");
          return;
        }
        sendMessage(json);
//      }
//    });
  }

  /** Send removed Ice candidates to the other participant. */
//  @Override
  public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
//    executor.execute(new Runnable() {
//      @Override
//      public void run() {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "remove-candidates");
        JSONArray jsonArray = new JSONArray();
        for (final IceCandidate candidate : candidates) {
          jsonArray.put(toJsonCandidate(candidate));
        }
        jsonPut(json, "candidates", jsonArray);

        if (roomState != ConnectionState.CONNECTED) {
          reportError("Sending ICE candidate removals in non connected state.");
          return;
        }
        sendMessage(json);
//      }
//    });
  }

  // -------------------------------------------------------------------
  // TCPChannelClient event handlers

  /**
   * If the client is the server side, this will trigger onConnectedToRoom.
   */
//  @Override
  public void onTCPConnected(boolean isServer) {
    if (isServer) {
      roomState = ConnectionState.CONNECTED;

      SignalingParameters parameters = new SignalingParameters(
          // Ice servers are not needed for direct connections.
          new ArrayList<>(),
          isServer, // Server side acts as the initiator on direct connections.
          null, // clientId
          null, // wssUrl
          null, // wwsPostUrl
          null, // offerSdp
          null // iceCandidates
          );
      events.onConnectedToRoom(parameters);
    }
  }

//  @Override
//  public void onTCPMessage(String msg) {
  public void onFromPeerMessage(JSONObject json) {
    try {
//      JSONObject json = new JSONObject(msg);
      JSONObject message = json.getJSONObject("message");
      String type = message.optString("type");
      if (type.equals("candidate")) {
        events.onRemoteIceCandidate(toJavaCandidate(message));
      } else if (type.equals("remove-candidates")) {
        JSONArray candidateArray = message.getJSONArray("candidates");
        IceCandidate[] candidates = new IceCandidate[candidateArray.length()];
        for (int i = 0; i < candidateArray.length(); ++i) {
          candidates[i] = toJavaCandidate(candidateArray.getJSONObject(i));
        }
        events.onRemoteIceCandidatesRemoved(candidates);
      } else if (type.equals("answer")) {
        SessionDescription sdp = new SessionDescription(
            SessionDescription.Type.fromCanonicalForm(type), message.getString("sdp"));
        events.onRemoteDescription(sdp);
      } else if (type.equals("offer")) {
        SessionDescription sdp = new SessionDescription(
            SessionDescription.Type.fromCanonicalForm(type), message.getString("sdp"));

        SignalingParameters parameters = new SignalingParameters(
            // Ice servers are not needed for direct connections.
            new ArrayList<>(),
            false, // This code will only be run on the client side. So, we are not the initiator.
            null, // clientId
            null, // wssUrl
            null, // wssPostUrl
            sdp, // offerSdp
            null // iceCandidates
            );
        roomState = ConnectionState.CONNECTED;
        events.onConnectedToRoom(parameters);
      } else {
        reportError("Unexpected TCP message: " + json);
      }
    } catch (JSONException e) {
      reportError("TCP message JSON parsing error: " + e.toString());
    }
  }

//  @Override
  public void onTCPError(String description) {
    reportError("TCP connection error: " + description);
  }

//  @Override
  public void onTCPClose() {
    events.onChannelClose();
  }

  // --------------------------------------------------------------------
  // Helper functions.
  private void reportError(final String errorMessage) {
    Log.e(TAG, errorMessage);
//    executor.execute(new Runnable() {
//      @Override
//      public void run() {
        if (roomState != ConnectionState.ERROR) {
          roomState = ConnectionState.ERROR;
          events.onChannelError(errorMessage);
        }
//      }
//    });
  }

  private void sendMessage(final String message) {
//    executor.execute(new Runnable() {
//      @Override
//      public void run() {
//        tcpClient.send(message);
    mSocketUtils.sendToPeer(message);
//      }
//    });
  }

  private void sendMessage(final JSONObject message) {
    mSocketUtils.sendToPeer(message);
  }


    // Put a `key`->`value` mapping in `json`.
  private static void jsonPut(JSONObject json, String key, Object value) {
    try {
      json.put(key, value);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  // Converts a Java candidate to a JSONObject.
  private static JSONObject toJsonCandidate(final IceCandidate candidate) {
    JSONObject json = new JSONObject();
    jsonPut(json, "label", candidate.sdpMLineIndex);
    jsonPut(json, "id", candidate.sdpMid);
    jsonPut(json, "candidate", candidate.sdp);
    return json;
  }

  // Converts a JSON candidate to a Java object.
  private static IceCandidate toJavaCandidate(JSONObject message) throws JSONException {
    return new IceCandidate(
        message.getString("id"), message.getInt("label"), message.getString("candidate"));
  }

  //by Yeosang
  public void notifyStarted(){
    this.isStarted = true;
  }
}

