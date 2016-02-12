package ytech.prototype.osiris.gateway;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

import android.os.AsyncTask;

/**
 * Created by XCCubeSSD on 2016/02/08.
 */
public class HttpHandleTask extends AsyncTask<Void, Void, byte[]> {

    // [Common]
    private static final String TAG = "HttpHandleTask";

    final static private String BOUNDARY = "MyBoundaryString";
    private HttpPostListener mListener;
    private String mURL;
    private HashMap<String, String> mTexts;
    private HashMap<String, byte[]> mImages;

    final String boundary =  "*****"+ UUID.randomUUID().toString()+"*****";

    public HttpHandleTask(String url)
    {
        super();

        mURL = url;
        mListener = null;
        mTexts = new HashMap<String, String>();
        mImages = new HashMap<String, byte[]>();
    }

    /**
     * リスナーをセットする。
     */
    public void setListener(HttpPostListener listener)
    {
        mListener = listener;
    }

    /**
     * 送信するテキストを追加する。
     */
    public void addText(String key, String text)
    {
        mTexts.put(key, text);
    }

    /**
     * 送信する画像を追加する。
     */
    public void addImage(String key, byte[] data)
    {
        mImages.put(key, data);
    }

    /**
     * 送信を行う。
     * @return レスポンスデータ
     */
    private byte[] send(byte[] data)
    {
        if (data == null)
            return null;

        byte[] result = null;
        HttpURLConnection connection = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = null;

        try {
            URL url = new URL(mURL);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setDoOutput(true);

            // 接続
            connection.connect();

            // 送信
            OutputStream os = connection.getOutputStream();
            os.write(data);
            Log.d(TAG, "send9");
            os.close();

            // レスポンスを取得する
            byte[] buf = new byte[10240];
            int size;
            Log.d(TAG, "send10");
            is = connection.getInputStream();
            Log.d(TAG, "send11");
            while ((size = is.read(buf)) != -1)
            {
                baos.write(buf, 0, size);
            }
            Log.d(TAG, "send12");
            result = baos.toByteArray();
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {}

            try {
                connection.disconnect();
            } catch (Exception e) {}

            try {
                baos.close();
            } catch (Exception e) {}
        }

        return result;
    }

    /**
     * POSTするデータを作成する。
     * @return
     */
    private byte[] makePostData()
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            // テキスト部分の設定
            for (Entry<String, String> entry : mTexts.entrySet())
            {
                String key = entry.getKey();
                String text = entry.getValue();

                baos.write(("--" + BOUNDARY + "\r\n").getBytes());
                baos.write(("Content-Disposition: form-data;").getBytes());
                baos.write(("name=\"" + key + "\"\r\n\r\n").getBytes());
                baos.write((text + "\r\n").getBytes());
            }

            // 画像の設定
            int count = 1;
            for (Entry<String, byte[]> entry: mImages.entrySet())
            {
                String key = entry.getKey();
                byte[] data = entry.getValue();
                String name = "upimage";

                baos.write(("--" + BOUNDARY + "\r\n").getBytes());
                baos.write(("Content-Disposition: form-data;").getBytes());
                baos.write(("name=\"" + name + "\";").getBytes());
                baos.write(("filename=\"" + key + "\"\r\n").getBytes());
                baos.write(("Content-Type: image/jpeg\r\n\r\n").getBytes());
                baos.write(data);
                baos.write(("\r\n").getBytes());
            }

            // 最後にバウンダリを付ける
            baos.write(("--" + BOUNDARY + "--\r\n").getBytes());

            return baos.toByteArray();
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                baos.close();
            } catch (Exception e) {}
        }
    }

    private void doOutput(String boundary, OutputStream out, String text, File file) throws IOException{
        String charset = "Shift_JIS";

        Log.d(TAG, "start output");

        // テキストフィールド送信
        Log.d(TAG, "send text field");
        out.write(("--" + boundary + "\r\n").getBytes(charset));
        out.write(("Content-Disposition: form-data; name=\"name\"\r\n").getBytes(charset));
        out.write(("Content-Type: text/plain; charset=Shift_JIS\r\n\r\n").getBytes(charset));
        out.write((text).getBytes(charset));
        out.write(("\r\n").getBytes(charset));

        // テキストフィールド送信2
        Log.d(TAG, "send text field2");
        Date date = new Date();
        SimpleDateFormat sdformat = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss");
        String text2 = sdformat.format(date);
        // ファイルフィールド送信
        Log.d(TAG, "date:" + text2);
        out.write(("--" + boundary + "\r\n").getBytes(charset));
        out.write(("Content-Disposition: form-data; name=\"pic_time\"\r\n").getBytes(charset));
        out.write(("Content-Type: text/plain; charset=Shift_JIS\r\n\r\n").getBytes(charset));
        out.write((text2).getBytes(charset));
        out.write(("\r\n").getBytes(charset));

        // ファイルフィールド送信
        Log.d(TAG, "send file field");
        out.write(("--" + boundary + "\r\n").getBytes(charset));
        out.write(("Content-Disposition: form-data; name=\"upimage\"; filename=\"").getBytes(charset));
        out.write((file.getName()).getBytes(charset));
        out.write(("\"\r\n").getBytes(charset));
        out.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes(charset));
        InputStream in = new FileInputStream(file);
        byte[] bytes = new byte[1024];
        Log.d(TAG, "write body data");
        for(int idx=0; ;++idx){
            int ret = in.read(bytes);
            if(ret <= 0) break;
            if((idx % 100) == 0) {
                Log.d(TAG, "idx:" + String.valueOf(idx) + "ret:" + String.valueOf(ret));
            }

            out.write(bytes, 0, ret);
            out.flush();
        }
        out.write(("\r\n").getBytes(charset));
        in.close();

        // 送信終わり
        out.write(("--" + boundary + "--").getBytes(charset));
        Log.d(TAG, "end output");
    }

    /**
     * タスク処理
     */
    @Override
    protected byte[] doInBackground(Void... params) {
        /*
        byte[] data = makePostData();
        byte[] result = send(data);
        */
        String text = "test";
        String existingFileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/cmr/" + "camera_test.jpg";
        File file = new File(existingFileName);

        try {

            Log.d(TAG, "phase1");
            // ダミーに書き込んで、データ量を調べる
            DummyOutputStream dummy = new DummyOutputStream();
            doOutput(boundary, dummy, text, file);
            int contentLength = dummy.getSize();

            Log.d(TAG, "phase2");
            // 接続
            URL url = new URL("https://alpha-first.herokuapp.com/user/update"); // 送信先
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setFixedLengthStreamingMode(contentLength);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.connect();

            // 実際にデータを送信する
            Log.d(TAG, "phase3");
            OutputStream out = conn.getOutputStream();
            doOutput(boundary, out, text, file);
            out.flush();
            out.close();

            // レスポンスを受信 (これをやらないと通信が完了しない)
            Log.d(TAG, "phase4");
            InputStream stream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String responseData = null;
            while((responseData = reader.readLine()) != null){
                Log.d(TAG, "RESPONSE:" + responseData);
            }
            stream.close();
            conn.disconnect();
            Log.d(TAG, "phase5");
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }

        return "SUCCESS!".getBytes();
    }

    @Override
    protected void onPostExecute(byte[] result)
    {
        if (mListener != null)
        {
            if (result != null)
            {
                mListener.postCompletion(result);
            }
            else
            {
                mListener.postFialure();
            }
        }
    }
}
