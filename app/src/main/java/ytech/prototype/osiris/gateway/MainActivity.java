package ytech.prototype.osiris.gateway;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import java.io.FileOutputStream;
import android.view.MotionEvent;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.android.gms.gcm.GoogleCloudMessaging;


public class MainActivity extends AppCompatActivity implements HttpPostListener {

    // [Common]
    private static final String TAG = "MainActivity";
    private TextView mMainText;
    private TextView mBleStText;
    private Handler mMainTextHandler;
    private Handler mBleStHandler;

    // [GCM]
    private GoogleCloudMessaging gcm;
    private Context context;
    private UpdateReceiver upReceiver;
    private IntentFilter intentFilter;
    private static final String RESIST_ID = "387907525646";

    // [Camera]
    private SurfaceView mySurfaceView;
    private Camera myCamera; //hardware
    private static final String IMG_FILE_NAME = "camera_test.jpg";
    private static final int quality = 75; //unit:percentage
    float resizeScaleWidth = 320;
    float resizeScaleHeight = 240;

    // [BLE]
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BleStatus mStatus = BleStatus.DISCONNECTED;
    static final int REQUEST_ENABLE_BT = 0;
    private static final long SCAN_PERIOD = 10000; /** BLE 機器検索のタイムアウト(ミリ秒) */
    private BluetoothGatt mBluetoothGatt;
    private boolean mIsBluetoothEnable = false;
    private static final String DEVICE_NAME = "BLE PushNotifAct";
    private static final String DEVICE_SENSOR_SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"; /** 対象のサービスUUID(デバイスにより変える必要がある) */
    private static final String DEVICE_RX_CHARACTERISTIC_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"; /** 対象のキャラクタリスティックUUID(デバイスにより変える必要がある) */
    private static final String DEVICE_TX_CHARACTERISTIC_UUID = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"; /** 対象のキャラクタリスティックUUID(デバイスにより変える必要がある) */
    private static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"; /** キャラクタリスティック設定UUID(これは固定のようだ) */


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

        Log.d("MainActivity", "onCreate");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // [GCM]
        context = getApplicationContext();
        gcm = GoogleCloudMessaging.getInstance(this);
        registerInBackground();

        upReceiver = new UpdateReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction("UPDATE_ACTION");
        registerReceiver(upReceiver, intentFilter);

        upReceiver.registerHandler(updateHandler);

        // [Camera]
        mySurfaceView = (SurfaceView)findViewById(R.id.surfaceView1);
        SurfaceHolder holder = mySurfaceView.getHolder();
        holder.addCallback(callback);

        // [BLE]
        mIsBluetoothEnable = false;

        Log.d(TAG, "get adapter");
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "no adapter");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else {
            Log.d(TAG, "found adapter");
        }
        mBleStText = (TextView)findViewById(R.id.blest_view);
        mBleStText.setText("BLE:Init Status");

        mBleStHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mBleStText.setText(((BleStatus) msg.obj).name());
            }
        };

        findViewById(R.id.connect_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });
        findViewById(R.id.discon_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });
        findViewById(R.id.post_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });


        // [Common]
        mMainText = (TextView)findViewById(R.id.main_text);
        mMainText.setText("");

        mMainTextHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mMainText.setText((String)msg.obj);
            }
        };

    }

    // [ble]
    /** BLE機器をスキャンした際のコールバック */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(TAG, "device found: " + device.getName());
            if (DEVICE_NAME.equals(device.getName())) {
                Log.d(TAG, "match device name");
                // 機器名が "SensorTag" であるものを探す

                // 機器が見つかればすぐにスキャンを停止する
                mBluetoothAdapter.stopLeScan(this);

                // 機器への接続を試みる
                mBluetoothGatt = device.connectGatt(getApplicationContext(), false, mBluetoothGattCallback);

                // 接続の成否は mBluetoothGattCallback で受け取る
            }
        }
    };

    // 接続完了や機器からのデータ受信はここで処理する
    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            // ①：connectGatt()の後でここに来る
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // GATTへ接続成功
                Log.d(TAG, "gatt connection success");
                // サービスを検索する
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // GATT通信から切断された
                setStatus(BleStatus.DISCONNECTED);
                mBluetoothGatt = null;
                mIsBluetoothEnable = false;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // ②：discoverServices()の後でここに来る
            Log.d(TAG, "check deiscovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "gatt success");

                /* test code
                for (BluetoothGattService service : gatt.getServices()) {
                	Log.d(TAG, "found uuid:" + service.getUuid().toString());
                }
                */

                BluetoothGattService service = gatt.getService(UUID.fromString(DEVICE_SENSOR_SERVICE_UUID));
                if (service == null) {
                    // サービスが見つからなかった
                    Log.d(TAG, "no service");
                    setStatus(BleStatus.SERVICE_NOT_FOUND);
                    mIsBluetoothEnable = false;
                } else {
                    // サービスを見つけた
                    Log.d(TAG, "service found");
                    setStatus(BleStatus.SERVICE_FOUND);

                    BluetoothGattCharacteristic characteristic =
                            service.getCharacteristic(UUID.fromString(DEVICE_RX_CHARACTERISTIC_UUID));

                    if (characteristic == null) {
                        // キャラクタリスティックが見つからなかった
                        Log.d(TAG, "characteristic not found");
                        setStatus(BleStatus.CHARACTERISTIC_NOT_FOUND);
                        mIsBluetoothEnable = false;
                    } else {
                        // キャラクタリスティックを見つけた
                        Log.d(TAG, "characteristic found");

                        // Notification を要求する
                        boolean registered = gatt.setCharacteristicNotification(characteristic, true);

                        // Characteristic の Notification 有効化
                        Log.d(TAG, "enable notification");
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                        Log.d(TAG, "set descriptor value");
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        Log.d(TAG, "write discriptor");
                        gatt.writeDescriptor(descriptor);

                        if (registered) {
                            // Characteristics通知設定完了
                            Log.d(TAG, "enable notification complete");
                            setStatus(BleStatus.NOTIFICATION_REGISTERED);
                            mIsBluetoothEnable = true;
                        } else {
                            Log.d(TAG, "enable notification incomplete");
                            setStatus(BleStatus.NOTIFICATION_REGISTER_FAILED);
                            mIsBluetoothEnable = false;
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            // ③データ到着時にここが実行される
            Log.d(TAG, "onCharacteristicRead: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // READ成功
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            // Characteristicの値更新通知
            Log.d(TAG, "onCharacteristicChanged");
            if (DEVICE_RX_CHARACTERISTIC_UUID.equals(characteristic.getUuid().toString())) {
                final byte[] data = characteristic.getValue();
                String result = new String(data);
                Log.d(TAG, "Received Data:" + result);
                setRcvDataText(result);
            }
        }
    };

    private void setRcvDataText(String rcvString) {
        Message msg = Message.obtain();
        msg.obj = "Received Ble Data:" + rcvString;
        mMainTextHandler.sendMessage(msg);
    }


    /** BLE機器を検索する */
    private void connect() {
        Log.d(TAG, "start searching");
        mBleStHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // タイムアウト
                Log.d(TAG, "timeout scan");
                mBluetoothAdapter.stopLeScan(mLeScanCallback );
            }
        }, SCAN_PERIOD);

        // スキャン開始
        Log.d(TAG, "start scan");
        mBluetoothAdapter.startLeScan(mLeScanCallback );
    }

    /** BLE 機器との接続を解除する */
    private void disconnect() {

        Log.d(TAG, "start disconnecting");
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            setStatus(BleStatus.CLOSED);
        }
        mIsBluetoothEnable = false;

    }

    private void send() {
        if (mIsBluetoothEnable) {
            Log.d(TAG, "send");
            BluetoothGattService myService = mBluetoothGatt.getService(UUID.fromString(DEVICE_SENSOR_SERVICE_UUID));
            BluetoothGattCharacteristic myChar = myService.getCharacteristic(UUID.fromString(DEVICE_TX_CHARACTERISTIC_UUID));
            myChar.setValue("ABC");
            mBluetoothGatt.writeCharacteristic(myChar);
        }
    }

    private void setStatus(BleStatus status) {
        mStatus = status;
        mBleStHandler.sendMessage(status.message());
    }



    // [GCM]
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    String regid = gcm.register(RESIST_ID);
                    msg = "Device registered, registration ID=" + regid;
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
            }
        }.execute(null, null, null);
    }

    private String send_start_pattern = "message=shutter";

    private Handler updateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            Bundle bundle = msg.getData();
            String message = bundle.getString("message");

            Log.d("MainActivity", "receive push notify message:" + message);

            if (message.indexOf(send_start_pattern) != -1) {
                Log.d("MainActivity", "get send start pattern");
                send();
            }
            else {
                if (myCamera != null) {
                    Log.d(TAG, "capture!");
                    myCamera.takePicture(null, null, mPictureListener);
                }
            }

        }
    };

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

                        try {
                            FileOutputStream out = null;
                            Bitmap tmp_bitmap  = BitmapFactory.decodeByteArray(data, 0, data.length);

                            // conversion
                            int width = tmp_bitmap.getWidth();
                            int height = tmp_bitmap.getHeight();
                            float postWidthScale = resizeScaleWidth / (float)width;
                            float postHeightScale = resizeScaleHeight / (float)height;

                            Matrix matrix = new Matrix();
                            matrix.postRotate (0);
                            matrix.postScale(postWidthScale, postHeightScale);
                            Bitmap bitmap = Bitmap.createBitmap (tmp_bitmap, 0, 0, width, height, matrix, true);

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
            ; //no event about surface view touch event
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

    private enum BleStatus {
        DISCONNECTED,
        SCANNING,
        SCAN_FAILED,
        DEVICE_FOUND,
        SERVICE_NOT_FOUND,
        SERVICE_FOUND,
        CHARACTERISTIC_NOT_FOUND,
        NOTIFICATION_REGISTERED,
        NOTIFICATION_REGISTER_FAILED,
        CLOSED
        ;
        public Message message() {
            Message message = new Message();
            message.obj = this;
            return message;
        }
    }

}
