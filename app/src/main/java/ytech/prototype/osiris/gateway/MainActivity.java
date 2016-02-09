package ytech.prototype.osiris.gateway;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.util.Log;

import android.hardware.Camera;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import android.view.MotionEvent;
import android.os.Environment;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends AppCompatActivity implements HttpPostListener {

    // [Common]
    private static final String TAG = "MainActivity";

    // [Camera]
    private SurfaceView mySurfaceView;
    private Camera myCamera; //hardware
    private static final String IMG_FILE_NAME = "camera_test.jpg";
    private static final int quality = 50; //unit:percentage

    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {

            //CameraOpen
            myCamera = Camera.open();

            try{
                myCamera.setPreviewDisplay(surfaceHolder);
            }catch(Exception e){
                e.printStackTrace();
            }

        }
        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            myCamera.startPreview();
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            myCamera.release();
            myCamera = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
            }
        });

        Button button1 = (Button)findViewById(R.id.post_button);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
            }
        });

        // [Camera]
        mySurfaceView = (SurfaceView)findViewById(R.id.surfaceView1);
        SurfaceHolder holder = mySurfaceView.getHolder();
        holder.addCallback(callback);

    }

    // [Camera]
    private Camera.PictureCallback mPictureListener =
            new Camera.PictureCallback() {
                public void onPictureTaken(byte[] data, Camera camera) {
                    // 外部ストレージのcmrフォルダに画像を保存する
                    if (data != null) {

                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/cmr/");
                        if(!file.exists()){
                            file.mkdir();
                        }
                        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/cmr/" + IMG_FILE_NAME;
                        Log.d(TAG, "creating image...:" + filePath);

                        /*
                        FileOutputStream myFOS = null;
                        try {
                            myFOS = new FileOutputStream(filePath);
                            myFOS.write(data);
                            myFOS.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        */

                        try {
                            FileOutputStream out = null;
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            out = new FileOutputStream (filePath);
                            bitmap.compress (Bitmap.CompressFormat.JPEG, quality, out);
                            out.close ();


                            // [http post]
                            Log.i(TAG, "post start!");
                            HttpHandleTask httptask = new HttpHandleTask("https://alpha-first.herokuapp.com/user/update/");
                            // リスナーをセットする
                            httptask.setListener(MainActivity.this);
                            // POST実行
                            httptask.execute();
                        } catch(Exception e) {
                            e.printStackTrace();
                        }


                        camera.startPreview(); // プレビュー再開
                    }
                }
            };

    // [camera]
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            if (myCamera != null) {
                Log.d(TAG, "flush!!!");
                myCamera.takePicture(null, null, mPictureListener);
            }
        }
        return true;
    }

    // [http post]
    @Override
    public void postCompletion(byte[] response) {
        Log.i(TAG, "post completion!");
        Log.i(TAG, new String(response));
    }

    @Override
    public void postFialure() {
        Log.i(TAG, "post failure!");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}