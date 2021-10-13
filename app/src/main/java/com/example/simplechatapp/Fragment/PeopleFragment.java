package com.example.simplechatapp.Fragment;

import androidx.activity.result.contract.ActivityResultContracts;
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

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.example.simplechatapp.ChatActivity;
import com.example.simplechatapp.Common.Common;
import com.example.simplechatapp.Model.UserModel;
import com.example.simplechatapp.R;
import com.example.simplechatapp.ViewHolder.UserViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class PeopleFragment extends Fragment {

    @BindView(R.id.recycler_people)
    RecyclerView recycler_people;
    FirebaseRecyclerAdapter mAdapter;


    private Unbinder unbinder;
    private PeopleViewModel mViewModel;

    static PeopleFragment instance;

    public static PeopleFragment getInstance() {
        return instance == null ? new PeopleFragment() : instance;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View itemView = inflater.inflate(R.layout.people_fragment, container, false);
        initView(itemView);
        loadPeople();
        return itemView;
    }

    private void loadPeople() {
        Query query = FirebaseDatabase.getInstance().getReference()
                .child(Common.USER_REFERENCES);
        FirebaseRecyclerOptions<UserModel> options = new FirebaseRecyclerOptions
                .Builder<UserModel>()
                .setQuery(query, UserModel.class)
                .build();
        mAdapter = new FirebaseRecyclerAdapter<UserModel,UserViewHolder>(options) {

            @NonNull
            @Override
            public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.layout_people, parent, false);
                return new UserViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull UserViewHolder holder, int position, @NonNull UserModel model) {
                if(!mAdapter.getRef(position).getKey().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                    ColorGenerator generator = ColorGenerator.MATERIAL;

                    int color = generator.getColor(FirebaseAuth.getInstance().getCurrentUser().getUid());


                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(model.getFirstName()).append(" ").append(model.getLastName());
                    holder.txt_name.setText(stringBuilder.toString());
                    holder.txt_bio.setText(model.getBio());

                    TextDrawable.IBuilder builder = TextDrawable.builder()
                            .beginConfig()
                            .withBorder(4)
                            .endConfig()
                            .round();
                    TextDrawable drawable = builder.build(stringBuilder.toString().substring(0,1),color);

                    holder.img_avatar.setImageDrawable(drawable);

                    holder.itemView.setOnClickListener(v -> {
                        Common.chatUser = model;
                        Common.chatUser.setUid(mAdapter.getRef(position).getKey());
                        startActivity(new Intent(getContext(), ChatActivity.class));
                    });
                }
                else {
                    holder.itemView.setVisibility(View.GONE);
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0,0));
                }
            }
        };

        mAdapter.startListening();
        recycler_people.setAdapter(mAdapter);
    }

    private void initView(View itemView) {
        unbinder = ButterKnife.bind(this, itemView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_people.setLayoutManager(layoutManager);
        recycler_people.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(PeopleViewModel.class);
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