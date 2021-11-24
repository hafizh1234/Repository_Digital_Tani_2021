package id.ac.ipb.mobile.digitani.api

import com.google.gson.JsonObject
import id.ac.ipb.mobile.digitani.model.*
import id.ac.ipb.mobile.digitani.response.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import java.util.*


interface ApiInterface {

    @FormUrlEncoded
    @POST("login")
    fun postLogin(
            @Field("username") username: String,
            @Field("password") password: String
    ): Call<UserLoginToken>

    @POST("register")
    fun postRegister(
    @Body userRegister: UserRegister
    ): Call<UserRegisterResponse>

    @GET("detail")
    fun getDetailUser(
        @Header("Authorization") token: String
    ): Call<UserLoginResponse>

    //Dedication(Pengabdian)

    @GET("dedications")
    fun getAllPengabdian(
        @Header("Authorization") token:String,
    ):Call<PengabdiansResponse>

    @FormUrlEncoded
    @POST("dedications")
    fun postPengabdian(
        @Header("Authorization") token:String,
        @Field("title") title:String,
        @Field("content") content:String,
        @Field("city_id")city_id:String,
        @Field("author_id")author_id:String,
        @Field("type")type:String
    ):Call<PengabdianResponse>

    //tingkatan daerah
    @GET("cities")
    fun getCity(
        @Header("Authorization") token:String,
        @Query("search")search:String
    ):Call<KotaResponse>

    @GET("districts")
    fun getDistricts(
        @Header("Authorization") token:String,
        @Query("search")search:String
    ):Call<KecamatanResponse>

    @GET("provinces")
    fun getProvinces(
        @Header("Authorization") token:String,
        @Query("search")search:String
    ):Call<ProvinsiResponse>

    //Ruang Obrolan Member
    @GET("members")
    fun getAllMembers(
        @Header("Authorization") token:String
    ):Call<ChatRoomMemberResponse>

    @FormUrlEncoded
    @POST("members")
    fun postMember(
        @Header("Authorization") token:String,
        @Field("user_id") userId:String,
        @Field("chat_room_id") chatRoomId:String,
        @Field("type") type:String
    ):Call<ChatMemberPostResponse>

    @DELETE("members/{id}")
    fun deleteMember(
        @Header("Authorization") token: String,
        @Path("id")id:String
    ):Call<Delete>

    @FormUrlEncoded
    @PUT("members/{id}")
    fun updateMember(
        @Header("Authorization") token:String,
        @Path("id") id:Int,
        @Field("user_id") userId:String,
        @Field("chat_room_id")chatRoomId:String,
        @Field("is_approved")isApproved:String,
        @Field("type")type:String
    ):Call<ChatMemberPostResponse>

    //User
    @GET("users")
    fun getUserAll(
        @Header("Authorization") token: String
    ):Call<Users>

    @GET("users/{id}")
    fun getUserById(
        @Header("Authorization") token: String,
        @Path("id")id:String
    ):Call<UserLoginData>

    //Message
    @GET("messages")
    fun getAllMessage(
        @Header("Authorization") token: String
    ):Call<GetChatMessages>

    @GET("rooms/{id}/messages")
    fun getAllMessagesInARoom(
        @Header("Authorization") token: String,
        @Path("id")id:String
    ):Call<GetChatMessages>

    @DELETE("messages/{id}")
    fun deleteMessage(
        @Header("Authorization") token:String,
        @Path("id") id:Int,
    ):Call<Delete>

    @FormUrlEncoded
    @PUT("messages/{id}")
    fun updateMessage(
        @Header("Authorization") token:String,
        @Path("id") id:Int,
        @Field("chat_member_id") chat_member_id:String,
        @Field("content") content:String,
        @Field("deleted_by_str") deleted_by_str:String?,
        @Field("type")type:String
    ):Call<MessageRespon>

    @Multipart
    @POST("messages")
    fun postMessageWithImage(
        @Header("Authorization")token:String,
        @Part image:MultipartBody.Part,
        @Part("chat_member_id") chat_member_id: RequestBody,
        @Part("content") content:RequestBody,
        @Part("type") type:RequestBody
    ):Call<MessageRespon>

    @GET
    fun getGambarInPesan(@Url gambar:String):Call<ResponseBody>

    @FormUrlEncoded
    @POST("messages")
    fun postMessages(
        @Header("Authorization")token:String,
        @Field("chat_member_id")chat_member_id:String,
        @Field("content")content:String,
        @Field("type")type:String
    ):Call<MessageRespon>

    //Ruang Obrolan START
    @GET("rooms")
    fun getRooms(
        @Header("Authorization") token: String
        /*@Query("select") select:String)
        */
    ):Call<ResponseChatRoom>

    @GET("rooms/{id}/members")
    fun getAllMembersInOneRoom(
        @Header("Authorization") token: String,
        @Path("id")id:String
    ):Call<ChatRoomMemberResponse>

    @DELETE("rooms/{id}")
    fun deleteRoom(
        @Header("Authorization") token: String,
        @Path("id")id:String
    ):Call<Delete>

    @FormUrlEncoded
    @PUT("rooms/{id}")
    fun updateRoom(
        @Header("Authorization") token:String,
        @Path("id") id:Int,
        @Field("name") name:String,
        @Field("description") description:String,
        @Field("type")type:String
    ):Call<ResponseChatRoomPost>

    @FormUrlEncoded
    @POST("rooms")
    fun createRoom(
        @Header("Authorization") token: String,
        @Field("name") name:String,
        @Field("type") type:String,
        @Field("description") description:String
        ):Call<ResponseChatRoomPost>

    //API CYBEX - END -
}
