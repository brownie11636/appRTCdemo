package com.example.nativewebrtcexample;

import org.json.JSONException;
import org.json.JSONObject;

public class MyService {
    //어질어질하다  이거 전체를 profile이라함.
    //{"socketId":"YQQB1VqkA2Tzkb7SAAAA",
    // "type":"service",
    // "service":{
    // "socketId":"YQQB1VqkA2Tzkb7SAAAA",
    // "room":"room:YQQB1VqkA2Tzkb7SAAAA",
    // "type":"Device_1",
    // "description":"Streamer",
    // "contents":{
    // "sensor":"{sensor1,sensor2}",
    // "stream":"{video,audio}"
    // }
    // },
    // "target_service":null,
    // "room":"room:YQQB1VqkA2Tzkb7SAAAA"}
    public JSONObject profile;
    public String socketID;
    //    private static String nickname;
    public String description;
    public JSONObject service;

    public MyService(JSONObject profile) {
        this.profile = profile;
        try {
            this.socketID = (String) profile.get("socketId");
            this.description = (String) ((JSONObject) profile.get("service")).get("description");
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        this.nickname = nickname;

//        try {
//            this.service
////                    .put("nickname",nickname)
//                    .put("description",description);
//            this.profile.put("socketId",socketID).put("service",service);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    public JSONObject getProfile (){
        return this.profile;
    }

//    public MyService create(String socketID, JSONObject service) {
////        this.socketID = socketID;
////        this.service = service;
//        try {
////            nickname = (String) service.get("nickname");
//            description = (String) service.get("description");
////            this.serviceProfile.put(socketID).put(service);
//        } catch (JSONException e) {
//            e.printStackTrace();
//            Log.e("serviceProfile","@create 1 : " + e);
//        }
//        return new MyService(socketID,
////                nickname,
//                description);
//    }
//
//    public static MyService create(JSONObject serviceProfile) {
//        String socketID = null;
//        String description = null;
//        try {
//            socketID = (String) serviceProfile.get("socketId");
////            nickname = (String) ((JSONObject) serviceProfile.get("service")).get("nickname");
//            description = (String) ((JSONObject) serviceProfile.get("service")).get("description");;
////            this.serviceProfile.put(socketID).put(service);
//        } catch (JSONException e) {
//            e.printStackTrace();
//            Log.e("serviceProfile","@create 2 : " + e);
//        }
//        return new MyService(socketID,
////                nickname,
//                description);
//    }




    public String getSocketID() {
        return this.socketID;
    }
    //    public String getNickname() {
//        return this.nickname;
//    }
    public String getDescription() { return this.description; }
}
