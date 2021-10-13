package com.example.simplechatapp.Remote;

import com.example.simplechatapp.Model.FCMResponse;
import com.example.simplechatapp.Model.FCMSendData;

import io.reactivex.Observable;
import retrofit2.http.Body;

import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
  @Headers({
          "Content-Type:application/json",
          "Authorization:key=AAAAlcwgNFg:APA91bG1xnLxVnAQIdPYj43YFalo5LK47zGHXD6PvgDE1UrNsb6TCihamm7WH7eJ97sq55q-kjorNhYmH3TQlaPOAMFleVJRPBqiUMtQu7yL6IkIeeuTDVEMKGekTIjneIF9sAYAJXju"
  })
    @POST("fcm/send")
  Observable<FCMResponse> sendNotification(@Body FCMSendData body);
}
