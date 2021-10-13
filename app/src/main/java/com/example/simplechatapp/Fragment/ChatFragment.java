package com.example.simplechatapp.Fragment;

import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.example.simplechatapp.ChatActivity;
import com.example.simplechatapp.Common.Common;
import com.example.simplechatapp.Model.ChatInfoModel;
import com.example.simplechatapp.Model.UserModel;
import com.example.simplechatapp.R;
import com.example.simplechatapp.ViewHolder.ChatInfoHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;

import java.text.SimpleDateFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ChatFragment extends Fragment {

    SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy");
    FirebaseRecyclerAdapter mAdapter;
    @BindView(R.id.chat_recycler)
    RecyclerView chat_recycler;
    private Unbinder unbinder;

    private ChatViewModel mViewModel;

    static ChatFragment instance;

    public static ChatFragment getInstance() {
        return instance == null ? new ChatFragment() : instance;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View itemView = inflater.inflate(R.layout.chat_fragment, container, false);
        init(itemView);
        loadChatList();
        return itemView;
    }

    private void loadChatList() {
        Query query = FirebaseDatabase.getInstance().getReference()
                .child(Common.CHAT_LIST_REFERENCES)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        FirebaseRecyclerOptions<ChatInfoModel> options = new FirebaseRecyclerOptions
                .Builder<ChatInfoModel>()
                .setQuery(query, ChatInfoModel.class)
                .build();
        mAdapter = new FirebaseRecyclerAdapter<ChatInfoModel, ChatInfoHolder>(options){

            @NonNull
            @Override
            public ChatInfoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_chat_item, parent, false);
                return new ChatInfoHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull ChatInfoHolder holder, int position, @NonNull ChatInfoModel model) {
                if(!mAdapter.getRef(position).getKey().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                    ColorGenerator generator = ColorGenerator.MATERIAL;

                    int color = generator.getColor(FirebaseAuth.getInstance().getCurrentUser().getUid());
                    String displayName = FirebaseAuth.getInstance().getCurrentUser().getUid()
                            .equals(model.getCreateId()) ? model.getFriendName() : model.getCreateName();

                    TextDrawable.IBuilder builder = TextDrawable.builder()
                            .beginConfig()
                            .withBorder(4)
                            .endConfig()
                            .round();

                    TextDrawable drawable = builder.build(displayName.substring(0,1),color);
                    holder.img_avatar.setImageDrawable(drawable);


                    holder.txt_name.setText(displayName);
                    holder.txt_last_message.setText(model.getLastMessage());
                    holder.text_time.setText(mSimpleDateFormat.format(model.getLastUpdate()));


                    holder.itemView.setOnClickListener(v -> {
                        FirebaseDatabase.getInstance()
                                .getReference(Common.USER_REFERENCES)
                                .child(FirebaseAuth.getInstance().getCurrentUser().getUid()
                                .equals(model.getCreateId()) ? model.getFriendId() : model.getCreateId())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        UserModel userModel = snapshot.getValue(UserModel.class);
                                        Common.chatUser = userModel;
                                        Common.chatUser.setUid(snapshot.getKey());

                                        String roomId = Common.generateChatRoomId(FirebaseAuth.getInstance()
                                        .getCurrentUser().getUid(),Common.chatUser.getUid());

                                        Common.roomSelected = roomId;


                                        //Register Topic
                                        FirebaseMessaging.getInstance().subscribeToTopic(roomId)
                                                .addOnSuccessListener(unused -> {
                                                    startActivity(new Intent(getContext(), ChatActivity.class));
                                                });

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    });

                }else{
                    holder.itemView.setVisibility(View.GONE);
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0,0));
                }
            }
        };
        mAdapter.startListening();
        chat_recycler.setAdapter(mAdapter);


    }

    private void init(View itemView) {
        unbinder = ButterKnife.bind(this, itemView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        chat_recycler.setLayoutManager(layoutManager);
        chat_recycler.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        // TODO: Use the ViewModel
    }

    @Override
    public void onStart() {
        super.onStart();
        if(mAdapter != null){
            mAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        if(mAdapter != null){
            mAdapter.startListening();
        }
        super.onStop();
    }

}