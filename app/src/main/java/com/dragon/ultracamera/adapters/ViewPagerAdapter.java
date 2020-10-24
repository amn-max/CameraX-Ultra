package com.dragon.ultracamera.adapters;


import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;


import androidx.annotation.NonNull;

import androidx.recyclerview.widget.RecyclerView;

import androidx.viewpager2.widget.ViewPager2;


import com.dragon.ultracamera.R;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewHolder>{

    private ArrayList<File> bitmapArrayList = new ArrayList<File>();
    private LayoutInflater mInflator ;
    private ViewPager2 viewPager2;
    private ImageView viewPagerImage;
    private Context context;
    private ProgressBar progressBar;
    public int currentPos;
    public ViewPagerAdapter(Context context, ArrayList<File> bmp, ViewPager2 viewPager2) {
        this.mInflator = LayoutInflater.from(context);
        this.bitmapArrayList = bmp;
        this.viewPager2 = viewPager2;
        this.context = context;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflator.inflate(R.layout.slide,parent,false);
        viewPagerImage = view.findViewById(R.id.slideImg);
        progressBar = view.findViewById(R.id.progressBarLayout);
        return new ViewHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        currentPos = position;
        progressBar.setVisibility(View.VISIBLE);
        Picasso.get().load(bitmapArrayList.get(position)).error(R.drawable.ic_error).into(viewPagerImage, new Callback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    public int getCurrentPos(){
        return currentPos;
    }


    @Override
    public int getItemCount() {
        return bitmapArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
