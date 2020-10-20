package com.dragon.ultracamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;

public class SlideAdapter extends PagerAdapter {
    Context context;
    LayoutInflater inflater;

    //list of images
    public ArrayList<Bitmap> bitmapArrayList = new ArrayList<Bitmap>();

    public SlideAdapter(Context context, ArrayList<Bitmap> images){
        this.context = context;
        bitmapArrayList = images;
    }

    @Override
    public int getCount() {
        return bitmapArrayList.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return (view==(LinearLayout)object);
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((LinearLayout)object);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.slide,container,false);
        LinearLayout layoutSlide = view.findViewById(R.id.slidelinearlayout);
        ImageView imgSlide = (ImageView) view.findViewById(R.id.slideImg);
        imgSlide.setImageBitmap(bitmapArrayList.get(position));
        container.addView(view);
        return view;
    }
}
