package com.accurateaccounts.app.api;

import com.accurateaccounts.app.models.Announcement;
import com.accurateaccounts.app.models.AuthRequest;
import com.accurateaccounts.app.models.AuthResponse;
import com.accurateaccounts.app.models.ChatMessage;
import com.accurateaccounts.app.models.ClientDocumentData;
import com.accurateaccounts.app.models.ClientDocumentsResponse;
import com.accurateaccounts.app.models.CountResponse;
import com.accurateaccounts.app.models.CreateTaskRequest;
import com.accurateaccounts.app.models.DocumentDownloadResponse;
import com.accurateaccounts.app.models.Feedback;
import com.accurateaccounts.app.models.FeedbackSubmitRequest;
import com.accurateaccounts.app.models.GuestMessage;
import com.accurateaccounts.app.models.GuestMessageRequest;
import com.accurateaccounts.app.models.KycUploadRequest;
import com.accurateaccounts.app.models.SheetUploadRequest;
import com.accurateaccounts.app.models.Task;
import com.accurateaccounts.app.models.User;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // Authentication
    @POST("auth")
    Call<AuthResponse> login(@Body AuthRequest request);

    // Tasks
    @GET("tasks")
    Call<List<Task>> getAllTasks();

    @GET("tasks/user/{userId}")
    Call<List<Task>> getUserTasks(@retrofit2.http.Path("userId") int userId);

    @POST("tasks")
    Call<okhttp3.ResponseBody> createTask(@Body CreateTaskRequest request);

    @retrofit2.http.PUT("tasks/{id}/status")
    Call<okhttp3.ResponseBody> updateTaskStatus(
            @retrofit2.http.Path("id") int taskId,
            @Body java.util.Map<String, String> body
    );

    // Messages
    @GET("chats/messages")
    Call<List<ChatMessage>> getMessages(
            @Query("user_id") int userId,
            @Query("other_user_id") Integer otherUserId,
            @Query("group_id") Integer groupId
    );

    @POST("chats/messages")
    Call<Void> sendMessage(@Body ChatMessage message);

    // Dashboard Stats & Announcements
    @GET("feedback/all")
    Call<List<Feedback>> getAllFeedbacks();

    @POST("feedback/submit")
    Call<okhttp3.ResponseBody> submitFeedback(@Body FeedbackSubmitRequest request);

    @GET("feedback/count")
    Call<CountResponse> getFeedbackCount();

    @GET("feedback/count/yesterday")
    Call<CountResponse> getYesterdayFeedbackCount();

    @GET("guest-messages")
    Call<List<GuestMessage>> getAllGuestMessages();

    @GET("guest-messages/count")
    Call<CountResponse> getGuestMessageCount();

    @GET("guest-messages/count/yesterday")
    Call<CountResponse> getYesterdayGuestMessageCount();

    @GET("clientDataRoutes")
    Call<ClientDocumentData> getClientData(@Query("client_id") int clientId);

    // Client Documents (Financial Sheets View/Download & KYC PAN/Aadhar Upload)
    @GET("documents/client/{clientId}")
    Call<ClientDocumentsResponse> getClientDocuments(@retrofit2.http.Path("clientId") int clientId);

    @POST("documents/kyc/upload")
    Call<okhttp3.ResponseBody> uploadKycDocument(@Body KycUploadRequest request);

    @POST("documents/sheet/upload")
    Call<okhttp3.ResponseBody> uploadPdfSheet(@Body SheetUploadRequest request);

    @GET("documents/download/{source}/{docId}")
    Call<DocumentDownloadResponse> downloadDocument(
            @retrofit2.http.Path("source") String source,
            @retrofit2.http.Path("docId") int docId
    );

    @retrofit2.http.DELETE("documents/other/{docId}")
    Call<okhttp3.ResponseBody> deleteOtherDocument(@retrofit2.http.Path("docId") int docId);

    @retrofit2.http.DELETE("documents/important/{docId}")
    Call<okhttp3.ResponseBody> deleteImportantDocument(@retrofit2.http.Path("docId") int docId);

    @retrofit2.http.DELETE("documents/{source}/{docId}")
    Call<okhttp3.ResponseBody> deleteDocument(
            @retrofit2.http.Path("source") String source,
            @retrofit2.http.Path("docId") int docId
    );

    @GET("announcements")
    Call<List<Announcement>> getAnnouncements();

    @POST("announcements")
    Call<okhttp3.ResponseBody> createAnnouncement(@Body com.accurateaccounts.app.models.CreateAnnouncementRequest request);

    // Guest Messages (Contact Form)
    @POST("guest-messages")
    Call<okhttp3.ResponseBody> sendGuestMessage(@Body GuestMessageRequest request);

    // Users & Clients
    @GET("users")
    Call<List<User>> getAllUsers();

    @GET("clients")
    Call<List<User>> getAllClients();

    @GET("users/auth/{auth_id}")
    Call<User> getUserProfileByAuthId(@retrofit2.http.Path("auth_id") int authId);

    // User Creation & Registration
    @POST("register")
    Call<okhttp3.ResponseBody> registerUser(@Body com.accurateaccounts.app.models.RegisterRequest request);
}

