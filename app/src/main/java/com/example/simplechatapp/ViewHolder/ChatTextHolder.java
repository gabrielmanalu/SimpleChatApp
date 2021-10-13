package com.example.simplechatapp.ViewHolder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplechatapp.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ChatTextHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.txt_chat_message)
    public TextView txt_chat_message;
    @BindView(R.id.text_time)
    public TextView text_time;

    private Unbinder mUnbinder;

    public ChatTextHolder(@NonNull View itemView) {
        super(itemView);
        mUnbinder = ButterKnife.bind(this, itemView);
    }
}
