package com.example.herbai;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Retrofit API interface for image upload.
 */
public interface ApiService {
    @Multipart
    @POST("/predict")
    Call<ResponseBody> uploadImage(@Part MultipartBody.Part file);
}
