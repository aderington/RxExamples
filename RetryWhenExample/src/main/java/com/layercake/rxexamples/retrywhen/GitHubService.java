package com.layercake.rxexamples.retrywhen;


import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface GitHubService {

    @GET("/users/{user}")
    Observable<User> user(@Path("user") String user);

}
