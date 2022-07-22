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

import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;
import com.example.nativewebrtcexample.AppRTCClient.SignalingParameters;
import com.example.nativewebrtcexample.util.AsyncHttpURLConnection;
import com.example.nativewebrtcexample.util.AsyncHttpURLConnection.AsyncHttpEvents;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import io.socket.client.Socket;

/**
 * AsyncTask that converts an AppRTC room URL into the set of signaling
 * parameters to use with that room.
 */
public class RoomParametersFetcher {    //makeRequest 함수를 위한 class
  private static final String TAG = "RoomRTCClient";
  private static final int TURN_HTTP_TIMEOUT_MS = 5000;
  private final RoomParametersFetcherEvents events;
  private final String roomId;
//  private final String roomUrl;
  private final String roomMessage;
  private  String roomMessag;

  private JSONObject profile;
  private MyService service;
//  private Socket socket;
  private SocketIO_Utils socketUtils;
  public boolean isStarted;

  private List<IceCandidate> iceCandidates = null;
  private SessionDescription offerSdp = null;

  /**
   * Room parameters fetcher callbacks.
   */
  public interface RoomParametersFetcherEvents {
    /**
     * Callback fired once the room's signaling parameters
     * SignalingParameters are extracted.
     */
    void onSignalingParametersReady(final SignalingParameters params);

    /**
     * Callback for room parameters extraction error.
     */
    void onSignalingParametersError(final String description);
  }

  public RoomParametersFetcher(
      MyService service, boolean isStarted ,String roomId, String roomMessage, final RoomParametersFetcherEvents events, SocketIO_Utils socketUtils) {
    this.roomId = roomId;
    this.roomMessage = roomMessage;
    this.events = events;
    this.socketUtils = socketUtils;
//    this.socket = socketUtils.mSocket;
    this.service = service;
    this.isStarted = isStarted;
  }

  public void makeRequest() {

    Log.d(TAG, "Connecting to room: " + roomId);
//    AsyncHttpURLConnection httpConnection =
//        new AsyncHttpURLConnection("POST", roomUrl, roomMessage, new AsyncHttpEvents() {
//          @Override
//          public void onHttpError(String errorMessage) {
//            Log.e(TAG, "Room connection error: " + errorMessage);
//            events.onSignalingParametersError(errorMessage);
//          }
//
//          @Override
//          public void onHttpComplete(String response) {
//            roomHttpResponseParse(response);
//          }
//        });
    socketUtils.socket.on("msg-v1", packet -> {
      if(!isStarted) {
        msgResponseParse((JSONObject) packet[0]);
      }
    });

//    httpConnection.send();
    socketUtils.joinService(this.service);



    if(service.getDescription().equals("Streamer")) {
      //gotStream() in robot_viewer.js
      Log.i(TAG,"Adding local stream."); //??
      socketUtils.sendToPeer("connection request");
    }
  }


//룸을 만드는 입장일때 쓰는듯
//private void roomHttpResponseParse(String response) {
public void msgResponseParse(JSONObject roomJson) {
//  Log.d(TAG, "Room response: " + response);

  Log.d(TAG, "Room response: " + roomJson);
    try {

      //appRTC에서는 이게 한번만 돌았던거 같은데 난 아니니까
      // 더 밖으로 빼서 roomFetcher가 선언될때 null로 만들어야겠음
//      List<IceCandidate> iceCandidates = null;
//      SessionDescription offerSdp = null;
//      JSONObject roomJson = new JSONObject(response);

//      String result = roomJson.getString("result");
//      if (!result.equals("SUCCESS")) {
//        events.onSignalingParametersError("Room response error: " + result);
//        return;
//      }
//      response = roomJson.getString("params");
//      roomJson = new JSONObject(roomJson.getString("message"));
      String roomId = roomJson.getString("from");
      String clientId = roomJson.getString("from");
      String wssUrl = roomJson.getString("from");
      String wssPostUrl = roomJson.getString("from");
//      boolean initiator = (roomJson.getBoolean("is_initiator"));
      boolean initiator = false;    //우리는 어차피 viewer입장에서만 코드 작성

      if (!initiator) {
        iceCandidates = new ArrayList<>();
        JSONObject message = roomJson.getJSONObject("message");
//        String messagesString = roomJson.getString("messages");
//        JSONArray messages = new JSONArray(messagesString);
//        for (int i = 0; i < messages.length(); ++i) {
//          String messageString = messages.getString(i);
//          JSONObject message = new JSONObject(messageString);
          String messageType = message.getString("type");
//          Log.d(TAG, "GAE->C #" + i + " : " + messageString);
          if (messageType.equals("offer")) {
            offerSdp = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(messageType), message.getString("sdp"));
          } else if (messageType.equals("candidate")) {
            IceCandidate candidate = new IceCandidate(
                message.getString("id"), message.getInt("label"), message.getString("candidate"));
            iceCandidates.add(candidate);
          } else {
            Log.e(TAG, "Unknown message: " + message);
          }
//        }
      }
      Log.d(TAG, "RoomId: " + roomId + ". ClientId: " + clientId);
      Log.d(TAG, "Initiator: " + initiator);
      Log.d(TAG, "WSS url: " + wssUrl);
      Log.d(TAG, "WSS POST url: " + wssPostUrl);

      List<PeerConnection.IceServer> iceServers =
//      iceServersFromPCConfigJSON(roomJson.getString("pc_config"));    ////"pc_config도 우리가 그냥 집어넣음 ㅋㅋ
      iceServersFromPCConfigJSON(new JSONObject()
        .put("iceServers",new JSONArray()
          .put(new JSONObject()
            .put("urls","turn:3.38.108.27")
            .put("username","usr")
            .put("credential","pass"))));    ////"pc_config도 우리가 그냥 집어넣음 ㅋㅋ
      boolean isTurnPresent = false;
      for (PeerConnection.IceServer server : iceServers) {
        Log.d(TAG, "IceServer: " + server);
        for (String uri : server.urls) {
          if (uri.startsWith("turn:")) {
            isTurnPresent = true;
            break;
          }
        }
      }
      // Request TURN servers.
      if (!isTurnPresent && !roomJson.optString("ice_server_url").isEmpty()) {
        List<PeerConnection.IceServer> turnServers =
            requestTurnServers(roomJson.getString("ice_server_url"));
        for (PeerConnection.IceServer turnServer : turnServers) {
          Log.d(TAG, "TurnServer: " + turnServer);
          iceServers.add(turnServer);
        }
      }

      SignalingParameters params = new SignalingParameters(
          iceServers, initiator, clientId, wssUrl, wssPostUrl, offerSdp, iceCandidates);
      events.onSignalingParametersReady(params);
    } catch (JSONException e) {
      events.onSignalingParametersError("Room JSON parsing error: " + e.toString());
    } catch (IOException e) {
      events.onSignalingParametersError("Room IO error: " + e.toString());
    }
  }

  // Requests & returns a TURN ICE Server based on a request URL.  Must be run
  // off the main thread!
  @SuppressWarnings("UseNetworkAnnotations")
  private List<PeerConnection.IceServer> requestTurnServers(String url)
      throws IOException, JSONException {
    List<PeerConnection.IceServer> turnServers = new ArrayList<>();
    Log.d(TAG, "Request TURN from: " + url);
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setDoOutput(true);
    //url from Junhwa's code
    connection.setRequestProperty("REFERER", "https://computeengineondemand.appspot.com/turn?username=41784574&key=4080218913");
    connection.setConnectTimeout(TURN_HTTP_TIMEOUT_MS);
    connection.setReadTimeout(TURN_HTTP_TIMEOUT_MS);
    int responseCode = connection.getResponseCode();
    if (responseCode != 200) {
      throw new IOException("Non-200 response when requesting TURN server from " + url + " : "
          + connection.getHeaderField(null));
    }
    InputStream responseStream = connection.getInputStream();
    String response = drainStream(responseStream);
    connection.disconnect();
    Log.d(TAG, "TURN response: " + response);
    JSONObject responseJSON = new JSONObject(response);
    JSONArray iceServers = responseJSON.getJSONArray("iceServers");
    for (int i = 0; i < iceServers.length(); ++i) {
      JSONObject server = iceServers.getJSONObject(i);
      JSONArray turnUrls = server.getJSONArray("urls");
      String username = server.has("username") ? server.getString("username") : "";
      String credential = server.has("credential") ? server.getString("credential") : "";
      for (int j = 0; j < turnUrls.length(); j++) {
        String turnUrl = turnUrls.getString(j);
        PeerConnection.IceServer turnServer =
            PeerConnection.IceServer.builder(turnUrl)
              .setUsername(username)
              .setPassword(credential)
              .createIceServer();
        turnServers.add(turnServer);
      }
    }
    return turnServers;
  }

  // Return the list of ICE servers described by a WebRTCPeerConnection
  // configuration string.
//  private List<PeerConnection.IceServer> iceServersFromPCConfigJSON(String pcConfig)
  private List<PeerConnection.IceServer> iceServersFromPCConfigJSON(JSONObject pcConfig)
      throws JSONException {
//    JSONObject json = new JSONObject(pcConfig);
//    JSONArray servers = json.getJSONArray("iceServers");
    JSONArray servers = pcConfig.getJSONArray("iceServers");
    List<PeerConnection.IceServer> ret = new ArrayList<>();
    for (int i = 0; i < servers.length(); ++i) {
      JSONObject server = servers.getJSONObject(i);
      String url = server.getString("urls");
      String credential = server.has("credential") ? server.getString("credential") : "";
        PeerConnection.IceServer turnServer =
            PeerConnection.IceServer.builder(url)
              .setPassword(credential)
              .createIceServer();
      ret.add(turnServer);
    }
    return ret;
  }

  // Return the contents of an InputStream as a String.
  private static String drainStream(InputStream in) {
    Scanner s = new Scanner(in, "UTF-8").useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }
}
