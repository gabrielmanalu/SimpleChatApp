package com.example.simplechatapp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;
import com.example.simplechatapp.Common.Common;
import com.example.simplechatapp.Listener.IFirebaseLoadFailed;
import com.example.simplechatapp.Listener.ILoadTimeFromFirebaseListener;
import com.example.simplechatapp.Model.ChatInfoModel;
import com.example.simplechatapp.Model.ChatMessageModel;
import com.example.simplechatapp.Model.FCMSendData;
import com.example.simplechatapp.Remote.IFCMService;
import com.example.simplechatapp.Remote.RetrofitFCMClient;
import com.example.simplechatapp.ViewHolder.ChatPictureHolder;
import com.example.simplechatapp.ViewHolder.ChatPictureReceiveHolder;
import com.example.simplechatapp.ViewHolder.ChatTextHolder;
import com.example.simplechatapp.ViewHolder.ChatTextReceiveHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ChatActivity extends AppCompatActivity implements ILoadTimeFromFirebaseListener, IFirebaseLoadFailed {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.img_camera)
    ImageView img_camera;
    @BindView(R.id.img_image)
    ImageView img_image;
    @BindView(R.id.edt_chat)
    AppCompatEditText edt_chat;
    @BindView(R.id.img_send)
    ImageView img_send;
    @BindView(R.id.recycler_chat)
    RecyclerView recycler_chat;
    @BindView(R.id.img_preview)
    ImageView img_preview;
    @BindView(R.id.img_avatar)
    ImageView img_avatar;
    @BindView(R.id.txt_name)
    TextView txt_name;

    FirebaseDatabase mDatabase;
    DatabaseReference chatRef, offsetRef;
    ILoadTimeFromFirebaseListener mListener;
    IFirebaseLoadFailed errorListener;

    FirebaseRecyclerAdapter<ChatMessageModel, RecyclerView.ViewHolder> mAdapter;
    FirebaseRecyclerOptions<ChatMessageModel> options;

    Uri fileUri;
    LinearLayoutManager mLayoutManager;
    StorageReference mStorageReference;
    IFCMService ifcmService;
    CompositeDisposable compositeDisposable = new CompositeDisposable();

    @OnClick(R.id.img_image)
    void onSelectImageClick() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        requestImage.launch(intent);
    }

    @OnClick(R.id.img_camera)
    void onCaptureImageClick() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        fileUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        requestCamera.launch(intent);
    }

    @OnClick(R.id.img_send)
    void onSubmitChatClick() {
//       if (!edt_chat.getText().toString().isEmpty() && fileUri == null) {
        offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long offset = snapshot.getValue(Long.class);
                long estimatedServeTimeInMs = System.currentTimeMillis() + offset;

                mListener.onLoadOnlyTimeSuccess(estimatedServeTimeInMs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                errorListener.onError(error.getMessage());
            }
        });
//       }
//       else{
//           onStart();
//       }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initViews();
        loadChatContent();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mAdapter != null) {
            mAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        if (mAdapter != null) {
            mAdapter.stopListening();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void loadChatContent() {
        String receiverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mAdapter = new FirebaseRecyclerAdapter<ChatMessageModel, RecyclerView.ViewHolder>(options) {

            @Override
            public int getItemViewType(int position) {
                if (mAdapter.getItem(position).getSenderId().equals(receiverId)) {
                    return !mAdapter.getItem(position).isPicture() ? 0 : 1;
                } else {
                    return !mAdapter.getItem(position).isPicture() ? 2 : 3;
                }
            }

            @Override
            protected void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull ChatMessageModel model) {
                if (holder instanceof ChatTextHolder) {

                    ChatTextHolder chatTextHolder = (ChatTextHolder) holder;
                    chatTextHolder.txt_chat_message.setText(model.getContent());
                    chatTextHolder.text_time.setText(DateUtils.getRelativeTimeSpanString(model.getTimeStamp(),
                            Calendar.getInstance().getTimeInMillis(), 0).toString());

                } else if (holder instanceof ChatTextReceiveHolder) {

                    ChatTextReceiveHolder chatTextHolder = (ChatTextReceiveHolder) holder;
                    chatTextHolder.txt_chat_message.setText(model.getContent());
                    chatTextHolder.text_time.setText(DateUtils.getRelativeTimeSpanString(model.getTimeStamp(),
                            Calendar.getInstance().getTimeInMillis(), 0).toString());

                } else if (holder instanceof ChatPictureHolder) {

                    ChatPictureHolder chatPictureHolder = (ChatPictureHolder) holder;
                    chatPictureHolder.txt_chat_message.setText(model.getContent());
                    chatPictureHolder.text_time.setText(DateUtils.getRelativeTimeSpanString(model.getTimeStamp(),
                            Calendar.getInstance().getTimeInMillis(), 0).toString());
                    Glide.with(ChatActivity.this)
                            .load(model.getPictureLink())
                            .into(chatPictureHolder.img_preview);

                } else if (holder instanceof ChatPictureReceiveHolder) {

                    ChatPictureReceiveHolder chatPictureReceiveHolder = (ChatPictureReceiveHolder) holder;
                    chatPictureReceiveHolder.txt_chat_message.setText(model.getContent());
                    chatPictureReceiveHolder.text_time.setText(DateUtils.getRelativeTimeSpanString(model.getTimeStamp(),
                            Calendar.getInstance().getTimeInMillis(), 0).toString());
                    Glide.with(ChatActivity.this)
                            .load(model.getPictureLink())
                            .into(chatPictureReceiveHolder.img_preview);
                }
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view;
                //Own message
                if (viewType == 0) {
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_message_me, parent, false);
                    return new ChatTextReceiveHolder(view);
                }
                //own picture message
                else if (viewType == 1) {
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_message_picture_friend, parent, false);
                    return new ChatPictureReceiveHolder(view);
                }
                //Friend's message
                else if (viewType == 2) {
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_message_friend, parent, false);
                    return new ChatTextHolder(view);
                }
                //Friend's picture message
                else {
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_message_picture_own, parent, false);
                    return new ChatPictureHolder(view);
                }
            }

        };

        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mAdapter.getItemCount();
                int lastVisiblePosition = mLayoutManager.findLastVisibleItemPosition();
                if (lastVisiblePosition == -1 || (positionStart >= (friendlyMessageCount - 1)
                        && lastVisiblePosition == (positionStart - 1))) {
                    recycler_chat.scrollToPosition(positionStart);
                }
            }
        });
        recycler_chat.setAdapter(mAdapter);
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        Common.roomSelected = "";
        super.onDestroy();
    }

    private void initViews() {

        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        mListener = this;
        errorListener = this;
        mDatabase = FirebaseDatabase.getInstance();
        chatRef = mDatabase.getReference(Common.CHAT_REFERENCE);

        offsetRef = mDatabase.getReference(".info/serverTimeOffset");
        Query query = chatRef.child(Common.generateChatRoomId(Common.chatUser.getUid(),
                FirebaseAuth.getInstance().getCurrentUser().getUid())).child(Common.CHAT_DETAIL_REFERENCE);

        options = new FirebaseRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class).build();
        ButterKnife.bind(this);
        mLayoutManager = new LinearLayoutManager(this);
        recycler_chat.setLayoutManager(mLayoutManager);

        ColorGenerator generator = ColorGenerator.MATERIAL;
        int color = generator.getColor(Common.chatUser.getUid());

        TextDrawable.IBuilder builder = TextDrawable.builder()
                .beginConfig()
                .withBorder(4)
                .endConfig()
                .round();
        TextDrawable drawable = builder.build(Common.chatUser.getFirstName().substring(0, 1), color);
        img_avatar.setImageDrawable(drawable);
        txt_name.setText(Common.getName(Common.chatUser));

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> {
            finish();
        });
    }

    @Override
    public void onLoadOnlyTimeSuccess(long estimateTimeInMs) {
        ChatMessageModel chatMessageModel = new ChatMessageModel();
        chatMessageModel.setName(Common.getName(Common.currentUser));
        chatMessageModel.setContent(edt_chat.getText().toString());
        chatMessageModel.setTimeStamp(estimateTimeInMs);
        chatMessageModel.setSenderId(FirebaseAuth.getInstance().getCurrentUser().getUid());

        if (fileUri == null) {
            chatMessageModel.setPicture(false);
            submitChatToFirebase(chatMessageModel, chatMessageModel.isPicture(), estimateTimeInMs);
        } else {
            uploadPicture(fileUri, chatMessageModel, estimateTimeInMs);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void uploadPicture(Uri fileUri, ChatMessageModel chatMessageModel, long estimateTimeInMs) {
        AlertDialog dialog = new AlertDialog.Builder(ChatActivity.this)
                .setCancelable(false)
                .setMessage("Please wait...")
                .create();
        dialog.show();

        String fileName = Common.getFileName(getContentResolver(), fileUri);
        String path = new StringBuilder(Common.chatUser.getUid())
                .append("/")
                .append(fileName)
                .toString();
        mStorageReference = FirebaseStorage
                .getInstance()
                .getReference().child(path);

        UploadTask uploadTask = mStorageReference.putFile(fileUri);
        Task<Uri> task = uploadTask.continueWithTask(task1 -> {
            if (!task1.isSuccessful()) {
                Toast.makeText(this, "Failed to upload", Toast.LENGTH_SHORT).show();
            }
            return mStorageReference.getDownloadUrl();
        }).addOnCompleteListener(task12 -> {
            if (task12.isSuccessful()) {
                String url = task12.getResult().toString();
                dialog.dismiss();

                chatMessageModel.setPicture(true);
                chatMessageModel.setPictureLink(url);

                submitChatToFirebase(chatMessageModel, chatMessageModel.isPicture(), estimateTimeInMs);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitChatToFirebase(ChatMessageModel chatMessageModel, boolean isPicture, long estimateTimeInMs) {

        chatRef.child(Common.generateChatRoomId(Common.chatUser.getUid(),
                FirebaseAuth.getInstance().getCurrentUser().getUid()))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            appendChat(chatMessageModel, isPicture, estimateTimeInMs);
                        } else {
                            createChat(chatMessageModel, isPicture, estimateTimeInMs);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChatActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void appendChat(ChatMessageModel chatMessageModel, boolean isPicture, long estimateTimeInMs) {
        Map<String, Object> update_data = new HashMap<>();
        update_data.put("lastUpdate", estimateTimeInMs);

        if (isPicture) {
            update_data.put("lastMessage", "<Image>");
        } else {
            update_data.put("lastMessage", chatMessageModel.getContent());
        }


        //UpdateFirebase
        FirebaseDatabase.getInstance()
                .getReference(Common.CHAT_LIST_REFERENCES)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(Common.chatUser.getUid())
                .updateChildren(update_data)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnSuccessListener(unused -> {
                    FirebaseDatabase.getInstance()
                            .getReference(Common.CHAT_LIST_REFERENCES)
                            .child(Common.chatUser.getUid())
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .updateChildren(update_data)
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            })
                            .addOnSuccessListener(unused1 -> {
                                String roomId = Common.generateChatRoomId(Common.chatUser.getUid(),
                                        FirebaseAuth.getInstance().getCurrentUser().getUid());
                                chatRef.child(roomId)
                                        .child(Common.CHAT_DETAIL_REFERENCE)
                                        .push()
                                        .setValue(chatMessageModel)
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnCompleteListener(task -> {
                                            //CLEARING
                                            if (task.isSuccessful()) {
                                                edt_chat.setText("");
                                                edt_chat.requestFocus();
                                                if (mAdapter != null) {
                                                    mAdapter.notifyDataSetChanged();
                                                }
                                                if (isPicture) {
                                                    fileUri = null;
                                                    img_preview.setVisibility(View.GONE);
                                                }
                                                sendNotificationToFriend(chatMessageModel, roomId);
                                            }
                                        });
                            });
                });
    }

    private void sendNotificationToFriend(ChatMessageModel chatMessageModel, String roomId) {
        Map<String, String> notiData = new HashMap<>();
        notiData.put(Common.NOTI_TITLE, "Message From " + chatMessageModel.getName());
        notiData.put(Common.NOTI_CONTENT, chatMessageModel.getContent());
        notiData.put(Common.NOTI_SENDER, FirebaseAuth.getInstance().getCurrentUser().getUid());
        notiData.put(Common.NOTI_ROOM_ID, roomId);

        FCMSendData sendData = new FCMSendData("/topics/" +roomId, notiData);

        compositeDisposable.add(ifcmService.sendNotification(sendData)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fcmResponse -> {

                }, throwable -> {
                    Toast.makeText(this, ""+ throwable.getMessage(), Toast.LENGTH_SHORT).show();
                })
        );
    }

    private void createChat(ChatMessageModel chatMessageModel, boolean isPicture, long estimateTimeInMs) {
        ChatInfoModel chatInfoModel = new ChatInfoModel();
        chatInfoModel.setCreateId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        chatInfoModel.setFriendName(Common.getName(Common.chatUser));
        chatInfoModel.setFriendId(Common.chatUser.getUid());
        chatInfoModel.setCreateName(Common.getName(Common.currentUser));

        if (isPicture) {
            chatInfoModel.setLastMessage("<Image>");
        } else {
            chatInfoModel.setLastMessage(chatMessageModel.getContent());
        }
        chatInfoModel.setLastUpdate(estimateTimeInMs);
        chatInfoModel.setCreateDate(estimateTimeInMs);


        //Send to Firebase
        FirebaseDatabase.getInstance()
                .getReference(Common.CHAT_LIST_REFERENCES)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(Common.chatUser.getUid())
                .setValue(chatInfoModel)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnSuccessListener(unused -> {
                    FirebaseDatabase.getInstance()
                            .getReference(Common.CHAT_LIST_REFERENCES)
                            .child(Common.chatUser.getUid())
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .setValue(chatInfoModel)
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            })
                            .addOnSuccessListener(unused1 -> {
                                String roomId = Common.generateChatRoomId(Common.chatUser.getUid(),
                                        FirebaseAuth.getInstance().getCurrentUser().getUid());
                                chatRef.child(roomId)
                                        .child(Common.CHAT_DETAIL_REFERENCE)
                                        .push()
                                        .setValue(chatMessageModel)
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnCompleteListener(task -> {
                                            //CLEARING
                                            if (task.isSuccessful()) {
                                                edt_chat.setText("");
                                                edt_chat.requestFocus();

                                                if (mAdapter != null) {
                                                    mAdapter.notifyDataSetChanged();

                                                }
                                                if (isPicture) {
                                                    fileUri = null;
                                                    img_preview.setVisibility(View.GONE);
                                                }
                                                sendNotificationToFriend(chatMessageModel, roomId);
                                            }
                                        });
                            });
                });
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if(requestCode == MY_CAMERA_REQUEST_CODE){
//            if(requestCode == RESULT_OK){
//                try {
//                    Bitmap thumbnail = MediaStore.Images.Media
//                            .getBitmap(getContentResolver(), fileUri);
//                    img_preview.setImageBitmap(thumbnail);
//                    img_preview.setVisibility(View.VISIBLE);
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        else if(requestCode == MY_RESULT_LOAD_IMAGE){
//            if(requestCode == RESULT_OK){
//                try {
//                    final Uri imageUri = data.getData();
//                    InputStream inputStream = getContentResolver()
//                            .openInputStream(imageUri);
//                    Bitmap selectedImage = BitmapFactory.decodeStream(inputStream);
//                    img_preview.setImageBitmap(selectedImage);
//                    img_preview.setVisibility(View.VISIBLE);
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        else{
//            Toast.makeText(this, "Please Choose an Image", Toast.LENGTH_SHORT).show();
//        }
//    }

    ActivityResultLauncher<Intent> requestCamera = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        try {
                            Bitmap thumbnail = MediaStore.Images.Media
                                    .getBitmap(getContentResolver(), fileUri);
                            img_preview.setImageBitmap(thumbnail);
                            img_preview.setVisibility(View.VISIBLE);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Please Choose an Image", Toast.LENGTH_SHORT).show();
                    }
                }
            });


    ActivityResultLauncher<Intent> requestImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK ) {
                        try {
                            Intent data = result.getData();
                            Uri imageUri = data.getData();
                            InputStream inputStream = getContentResolver()
                                    .openInputStream(imageUri);
                            Bitmap selectedImage = BitmapFactory.decodeStream(inputStream);
                            img_preview.setImageBitmap(selectedImage);
                            img_preview.setVisibility(View.VISIBLE);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Please Choose an Image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

}
