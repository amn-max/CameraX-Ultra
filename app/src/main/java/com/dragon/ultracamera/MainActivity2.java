package com.dragon.ultracamera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity2 extends AppCompatActivity {

    private ImageView imageView;
    private ViewPager viewPager;
    private SlideAdapter myAdapter;
    private ArrayList<Bitmap> bmp = new ArrayList<Bitmap>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        Intent myIntent = getIntent();
        ArrayList<String> bitmapArrayList = myIntent.getStringArrayListExtra("bitmapImages");
        try {
            for(int i=0;i<bitmapArrayList.size();i++){
                FileInputStream in = this.openFileInput(bitmapArrayList.get(i));
                bmp.add(BitmapFactory.decodeStream(in));
                in.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        myAdapter = new SlideAdapter(this,bmp);
        viewPager.setAdapter(myAdapter);
    }
}