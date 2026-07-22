package ru.untriedduck.weatherforecast.updates;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface UpdateApiService {

    @Streaming
    @GET
    suspend fun downloadUpdateApk(@Url fileUrl: String): Response<ResponseBody>

}
