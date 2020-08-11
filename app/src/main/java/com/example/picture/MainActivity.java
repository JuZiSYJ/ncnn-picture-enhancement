package com.example.picture;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.graphics.Color;
import android.os.Environment;


import java.io.FileNotFoundException;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    private static final int SELECT_IMAGE = 1;

    private int style_type = 0;
    private TextView infoResult;
    private ImageView imageView;
    private ImageView imageView2;
    //    private Button buttonImage;
    private Bitmap yourSelectedImage = null;
    private Bitmap styledImage = null;

    private Waterdemo waterdemoncnn = new Waterdemo();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        boolean ret_init = waterdemoncnn.Init(getAssets());
        if (!ret_init)
        {
            Log.e("MainActivity", "picture enhancement Init failed!");
        }

        infoResult = (TextView) findViewById(R.id.infoResult);
        imageView = (ImageView) findViewById(R.id.imageView);
        imageView2 = (ImageView) findViewById(R.id.imageView2);

        Button buttonImage = (Button) findViewById(R.id.buttonImage);
        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                startActivityForResult(i, SELECT_IMAGE);
            }
        });

        Button buttonDetect = (Button) findViewById(R.id.buttonDetect);
        buttonDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (yourSelectedImage == null)
                    return;


                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                new Thread(new Runnable() {
                    public void run() {
                        long start = System.currentTimeMillis();
                        final Bitmap styledImage = runStyleTransfer(false);
                        long end = System.currentTimeMillis();
                        long time = end - start;
                        final String text = time+"";

                        imageView.post(new Runnable() {
                            public void run() {
                                imageView2.setImageBitmap(styledImage);

                                infoResult.setText("trans: "+styledImage.getWidth()+"  "+styledImage.getHeight()+"  infer cpu: "+text+"ms");
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                            }
                        });
                    }
                }).start();
            }
        });

        Button buttonDetectGPU = (Button) findViewById(R.id.buttonDetectGPU);
        buttonDetectGPU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (yourSelectedImage == null)
                    return;

//                String result = squeezencnn.Detect(yourSelectedImage, false);

                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                new Thread(new Runnable() {
                    public void run() {
                        long start = System.currentTimeMillis();
                        final Bitmap styledImage = runStyleTransfer(true);
                        long end = System.currentTimeMillis();
                        long time = end - start;
                        final String text = time+"";

                        imageView.post(new Runnable() {
                            public void run() {
                                imageView2.setImageBitmap(styledImage);

                                infoResult.setText("trans: "+styledImage.getWidth()+"  "+styledImage.getHeight()+"  infer gpu: "+text+"ms");
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                            }
                        });
                    }
                }).start();
            }
        });

        Spinner spinnerStyle = (Spinner) findViewById(R.id.spinnerStyle);
        spinnerStyle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                style_type = pos;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            //https://blog.csdn.net/pbm863521/article/details/73571777 a uri
            // data is a intent
            try
            {
                if (requestCode == SELECT_IMAGE) {
                    Bitmap bitmap = decodeUri(selectedImage);

                    Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                    // resize to 227x227
                    yourSelectedImage = Bitmap.createScaledBitmap(rgba, rgba.getWidth(), rgba.getHeight(), false);

                    rgba.recycle();  //回收

                    imageView.setImageBitmap(bitmap);
                    int color = bitmap.getPixel(100,100);
                    int r = Color.red(color);
                    int g = Color.green(color);
                    int b = Color.blue(color);
                    int a = Color.alpha(color);
                    infoResult.setText("orign pic H W: "+bitmap.getWidth()+"  "+bitmap.getHeight());

                }
            }
            catch (FileNotFoundException e)
            {
                Log.e("MainActivity", "FileNotFoundException");
                return;
            }
        }
    }

    private Bitmap runStyleTransfer(boolean use_gpu)
    {
        styledImage = yourSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
        waterdemoncnn.Detect(styledImage, style_type, use_gpu);
        return styledImage;
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException
    {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options(); // a class
        o.inJustDecodeBounds = true;  //return size only
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 400;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);
    }

}
