package com.example.simplechatapp.Adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.simplechatapp.Fragment.ChatFragment;
import com.example.simplechatapp.Fragment.PeopleFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {
    public ViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if(position == 0){
            return ChatFragment.getInstance();
        }else{
            return PeopleFragment.getInstance();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
