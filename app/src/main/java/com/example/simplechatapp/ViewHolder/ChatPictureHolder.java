package com.example.simplechatapp.ViewHolder;

import android.media.Image;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplechatapp.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ChatPictureHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.text_time)
    public TextView text_time;
    @BindView(R.id.txt_chat_message)
    public TextView txt_chat_message;
    @BindView(R.id.img_preview)
    public ImageView img_preview;

    private Unbinder mUnbinder;

    public ChatPictureHolder(@NonNull View itemView) {
        super(itemView);
        mUnbinder = ButterKnife.bind(this, itemView);
    }
}
