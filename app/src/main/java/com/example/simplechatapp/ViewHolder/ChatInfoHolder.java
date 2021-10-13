package com.example.simplechatapp.ViewHolder;

import android.media.Image;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplechatapp.R;

import org.w3c.dom.Text;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ChatInfoHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.img_avatar)
    public ImageView img_avatar;
    @BindView(R.id.txt_name)
    public TextView txt_name;
    @BindView(R.id.txt_last_message)
    public TextView txt_last_message;
    @BindView(R.id.text_time)
    public TextView text_time;

    Unbinder mUnbinder;
    public ChatInfoHolder(@NonNull View itemView) {
        super(itemView);
        mUnbinder = ButterKnife.bind(this, itemView);
    }
}
