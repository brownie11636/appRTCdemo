/*
 *  Copyright 2013 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.example.nativewebrtcexample;

import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.List;

/**
 * AppRTCClient is the interface representing an AppRTC client.
 */
public interface AppRTCClient {
  /**
   * Struct holding the connection parameters of an AppRTC room.
   */
  class RoomConnectionParameters {
    public final String roomUrl;    //홈페이지 주소
    public final String roomId;     //룸 넘버 socketId로 쓰면 될듯
    public final boolean loopback;  //이건 버리지 뭐
    public final String urlParameters;    //AppRTC 사이트랑 https통신할때 url 뒤에 붙여서 parameter들 조절하는 String 나는 쓸일없네 https://github.com/webrtc/samples/wiki/AppRTC-URL-parameter-guide
    public JSONObject profile = null;
    public RoomConnectionParameters(
        JSONObject profile, String roomUrl, String roomId, boolean loopback, String urlParameters) {
      this.profile = profile;

      this.roomUrl = roomUrl;
      this.roomId = roomId;
      this.loopback = loopback;
      this.urlParameters = urlParameters;
    }
    public RoomConnectionParameters(JSONObject profile,String roomUrl, String roomId, boolean loopback) {
      this(profile, roomUrl, roomId, loopback, null /* urlParameters */);
    }
  }

  /**
   * Asynchronously connect to an AppRTC room URL using supplied connection
   * parameters. Once connection is established onConnectedToRoom()
   * callback with room parameters is invoked.
   */
  void connectToRoom(RoomConnectionParameters connectionParameters);

  /**
   * Send offer SDP to the other participant.
   */
  void sendOfferSdp(final SessionDescription sdp);

  /**
   * Send answer SDP to the other participant.
   */
  void sendAnswerSdp(final SessionDescription sdp);

  /**
   * Send Ice candidate to the other participant.
   */
  void sendLocalIceCandidate(final IceCandidate candidate);

  /**
   * Send removed ICE candidates to the other participant.
   */
  void sendLocalIceCandidateRemovals(final IceCandidate[] candidates);

  /**
   * Disconnect from room.
   */
  void disconnectFromRoom();

  /**
   * Struct holding the signaling parameters of an AppRTC room.
   */
  class SignalingParameters {
    public final List<PeerConnection.IceServer> iceServers;
    public final boolean initiator;
    public final String clientId;   //clientId 내 SocketId로 대체하지 뭐
    public final String wssUrl;     //웹소켓 서버
    public final String wssPostUrl;     //http 서버 말하는듯? 없어도 되지 뭐
    public final SessionDescription offerSdp;
    public final List<IceCandidate> iceCandidates;

    public SignalingParameters(List<PeerConnection.IceServer> iceServers, boolean initiator,
        String clientId, String wssUrl, String wssPostUrl, SessionDescription offerSdp,
        List<IceCandidate> iceCandidates) {
      this.iceServers = iceServers;
      this.initiator = initiator;
      this.clientId = clientId;
      this.wssUrl = wssUrl;
      this.wssPostUrl = wssPostUrl;
      this.offerSdp = offerSdp;
      this.iceCandidates = iceCandidates;
    }
  }

  /**
   * Callback interface for messages delivered on signaling channel.
   *
   * <p>Methods are guaranteed to be invoked on the UI thread of `activity`.
   */
  interface SignalingEvents {
    /**
     * Callback fired once the room's signaling parameters
     * SignalingParameters are extracted.
     */
    void onConnectedToRoom(final SignalingParameters params);

    /**
     * Callback fired once remote SDP is received.
     */
    void onRemoteDescription(final SessionDescription sdp);

    /**
     * Callback fired once remote Ice candidate is received.
     */
    void onRemoteIceCandidate(final IceCandidate candidate);

    /**
     * Callback fired once remote Ice candidate removals are received.
     */
    void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates);

    /**
     * Callback fired once channel is closed.
     */
    void onChannelClose();

    /**
     * Callback fired once channel error happened.
     */
    void onChannelError(final String description);
  }
}
