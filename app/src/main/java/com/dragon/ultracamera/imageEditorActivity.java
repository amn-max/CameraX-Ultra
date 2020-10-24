package com.dragon.ultracamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.viewpager2.widget.ViewPager2;

import android.content.ContextWrapper;
import android.content.Intent;

import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;


import com.dragon.ultracamera.adapters.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;

import java.util.ArrayList;

public class imageEditorActivity extends AppCompatActivity {


    private ViewPagerAdapter myAdapter;
    private ArrayList<File> bmp = new ArrayList<File>();
    private ImageView mImageView;
    private ViewPager2 viewPager2;
    private TabLayout tabLayout;
    private ArrayList<String> bitmapArrayList = new ArrayList<>();
    private Button deleteButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_editor);
        mImageView = findViewById(R.id.slideImg);
        viewPager2 = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        deleteButton = findViewById(R.id.btn_delete_img);
        try {
            getSupportActionBar().hide();
        } catch (NullPointerException e) {

        }
        Intent myIntent = getIntent();
        ArrayList<String> bitmapArrayList = myIntent.getStringArrayListExtra("bitmapImages");
        for(int i=0;i<bitmapArrayList.size();i++){

//            FileInputStream in = null;
//            try {
//                in = this.openFileInput(bitmapArrayList.get(i));
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//            bmp.add(BitmapFactory.decodeStream(in));
//            try {
//                in.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            File file = new File(bitmapArrayList.get(i));
            if(file.exists()){
                bmp.add(file);
            }
        }


        myAdapter = new ViewPagerAdapter(this,bmp,viewPager2);
        viewPager2.setAdapter(myAdapter);
        new TabLayoutMediator(tabLayout, viewPager2, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText("Tab "+(position+1));
                viewPager2.setCurrentItem(tab.getPosition(),true);
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bmp.remove(position).delete();
                        bitmapArrayList.remove(position);
                        myAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).attach();

    }

    @Override
    protected void onResume() {
        super.onResume();
        for(int i=0;i<bitmapArrayList.size();i++){

//            FileInputStream in = null;
//            try {
//                in = this.openFileInput(bitmapArrayList.get(i));
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//            bmp.add(BitmapFactory.decodeStream(in));
//            try {
//                in.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            ContextWrapper cw = new ContextWrapper(getApplicationContext());
            File file = new File(bitmapArrayList.get(i));
            if(file.exists()){
                bmp.add(file);
            }
        }
    }
}