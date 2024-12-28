package com.example.socialapp.Config;

import com.example.socialapp.Activity.AddStoryActivity;
import com.example.socialapp.Activity.ChatActivity;
import com.example.socialapp.Entity.Comment;
import com.example.socialapp.Entity.Message;
import com.example.socialapp.Entity.Story;
import com.example.socialapp.Entity.User;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RetrofitService {
// ----------------------------------POST---------------------------------
    @POST("tokens")     // 유저 토큰 저장
    Call<Void> sendToken(@Body Map<String, String> body);
    @POST("/join")      // 회원가입
    Call<Void> joinUser(@Body User user);
    @POST("/email")     // 이메일 인증번호 발송
    Call<ResponseBody> sendEmail(@Body Map<String, String> body);
    @POST("group")      // 그룹 채팅방 생성
    Call<ResponseBody> makeGroup(@Body Map<String, Integer> body, @Query("title") String title);
    @POST("groups")     // 채팅방 맴버 추가
    Call<Void> addGroups(@Body Map<String, Integer> body);
    @POST("storys")     // 스토리 추가
    Call<StoryResponse> makeStory(@Body Story story);
    @POST("images")     // 스토리 이미지 추가
    Call<Void> addImages(@Body Story story);
    @POST("comments")   // 댓글 추가
    Call<Void> sendComment(@Body Comment comment);
    @POST("messages")       // 메시지 보내기
    Call<Void> sendMessage(@Body Message message);
    @POST("deltoken")   // 토큰 삭제(로그아웃 시)
    Call<Void> deleteToken(@Body Map<String, String> body);
    @POST("friends")    // 친구 추가(새로운 친구)
    Call<Void> makeFriend(@Body User user);
    @POST("addchat")    // 친구 추가(날 친추한 친구)
    Call<ResponseBody> addMember(@Body Map<String, Integer> body);
    @POST("user/blocking/{userId}") // 친구 차단 추가
    Call<Void> addBlock(@Path("userId") int userId, @Query("myId") int myId);
    @POST("likes")      // 좋아요 추가
    Call<Void> postLike(@Body Story story);
    @Multipart
    @POST("upload/{description}")       // 채팅방 이미지, 동영상 로드
    Call<UploadResponse> uploadFile(
            @Part MultipartBody.Part file,
            @Path("description") RequestBody description,
            @Query("roomId") int roomId,
            @Query("userId") int userId,
            @Query("type") String type
    );
// ----------------------------------GET----------------------------------
    @GET("login/email/{email}")     // 로그인, 이메일 인증
    Call<ResponseBody> getEmail(@Path("email") String email);
    @GET("users/myfriends/{userId}")    // 친추한 유저 로드
    Call<ResponseBody> getFriends(@Path("userId") int userId);
    @GET("users/profile/{userId}")      // 모든 친구 로드
    Call<ResponseBody> getUsers(@Path("userId") int userId);
    @GET("users/blocked/{userId}")       // 차단 리스트 로드
    Call<ResponseBody> getBlockedFriends(@Path("userId") int userId);
    @GET("rooms/{userId}")      // 1대1 채팅방 로드
    Call<ResponseBody> getRooms(@Path("userId") int userId);
    @GET("grouprooms/{userId}")     // 그룹 채팅방 로드
    Call<ResponseBody> getGroupRooms(@Path("userId") int userId);
    @GET("members/{roomId}")        // 채팅방 유저 정보 로드
    Call<ResponseBody> getMembers(@Path("roomId") int roomId);
    @GET("likes/num/{userId}")     // 스토리 좋아요 로드
    Call<ResponseBody> getLikes(@Path("userId") int userId, @Query("page") int page, @Query("size") int size);
    @GET("storys/{userId}")     // 스토리 유저 + 친구들의 로드
    Call<ResponseBody> getStories(@Path("userId") int userId, @Query("page") int page, @Query("size") int size);
    @GET("images/{userId}")     // 스토리 이미지 로드
    Call<ResponseBody> getImages(@Path("userId") int userId, @Query("page") int page, @Query("size") int size);
    @GET("messages/message/{roomId}")   // 메시지 로드
    Call<ResponseBody> getMessages(@Path("roomId") int roomId);
    @GET("comments/{storyId}")     // 댓글 로드
    Call<ResponseBody> getComments(@Path("storyId") int storyId);
    @GET("comments/addition/{storyId}")     //  덧글 로드(추가 댓글)
    Call<ResponseBody> getAdditionComments(@Path("storyId") int storyId, @Query("id") int commentId);
    @GET("users/profile/one/{userId}")  // 한명 정보 로드
    Call<ResponseBody> getUser(@Path("userId") int userId);
    @GET("messages/chatroom/{userId}")  // 유저의 채팅방 로드
    Call<ResponseBody> getRoomId(@Path("userId") int roomId);
    @GET("preview/images/{userId}")     // 유저 프로필의 스토리 프리뷰 사진(스토리 첫 사진) 로드
    Call<ResponseBody> getImage(@Path("userId") int userId, @Query("page") int page, @Query("size") int size);
    @GET("preview/likes/{userId}")     // 프리뷰 스토리 좋아요 로드
    Call<ResponseBody> getLike(@Path("userId") int userId, @Query("my_id") int myId,@Query("page") int page, @Query("size") int size);
    @GET("preview/stories/{userId}")   // 프리뷰 스토리 로드
    Call<ResponseBody> getStory(@Path("userId") int userId, @Query("page") int page, @Query("size") int size);
// ----------------------------------PUT----------------------------------
    @PUT("revise/password")     // 비밀번호 수정
    Call<Void> putPass(@Body Map<String, String> body);
    @PUT("revise/story")        // 스토리 수정
    Call<Void> putStory(@Body Map<String, String> body);
    @FormUrlEncoded
    @PUT("revise/profile/{userId}")  // 프로필 업데이트
    Call<Void> putUsers(@Path("userId") int userId, @Field("name") String name, @Field("message") String message, @Field("image") String image);
// ----------------------------------PUT----------------------------------
    @DELETE("user/unblock/{userId}")    // 차단된 친구 차단 해제
    Call<Void> deleteBlock(@Path("userId") int userId, @Query("myId") int myId);
    @DELETE("/comments/{commentId}")    // 댓글 삭제
    Call<ResponseBody> deleteComments(@Path("commentId") int commentId);
    @DELETE("story/{storyId}")      // 스토리 삭제
    Call<Void> deletestory(@Path("storyId") int storyId);
    @DELETE("likes/{userId}/{storyId}")     // 좋아요 취소
    Call<Void> deleteLike(
            @Path("userId") int userId,
            @Path("storyId") int storyId
    );

    class StoryResponse {
        private int id; // 생성된 ID

        public int getId() {
            return id;
        }
    }
    class UploadResponse {
        private String fileUrl;  // 서버에서 반환한 파일 URL
        // getter와 setter 메서드
        public String getFileUrl() {
            return fileUrl;
        }

        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }
    }
}
