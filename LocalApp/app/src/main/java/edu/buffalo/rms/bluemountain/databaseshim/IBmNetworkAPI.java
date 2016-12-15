package edu.buffalo.rms.bluemountain.databaseshim;

import android.content.res.TypedArray;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.*;
import retrofit2.Callback;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

/**
 * @author Ramanpreet Singh Khinda
 *
 * Created by raman on 11/24/16.
 */
public interface IBmNetworkAPI {
    @Multipart
    @POST("/BlueMountain/uploadOperationsLog/")
    Call<Boolean> uploadOperationsLogToServer(
            @Part("description") RequestBody description,
            @Part MultipartBody.Part logFile);

    @Multipart
    @POST("/BlueMountain/uploadMultiDbTable/")
    Call<Boolean> uploadDbTableFileToServer(
            @Part("description") RequestBody description,
            @Part MultipartBody.Part dbTableFile);

    @Multipart
    @POST("/BlueMountain/uploadFileChunk/")
    Call<Boolean> uploadFileChunkToServer(
            @Part("description") RequestBody description,
            @Part MultipartBody.Part chunkByteData,
            @Part("offset") int offset);
}