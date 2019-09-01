package com.tungteen.autoclick;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

public class AutoClickService extends Service {
    Handler handler;
    Process sh = null;
    OutputStream os;
    Mat likeButton;
    public AutoClickService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        Bitmap bm=getBitmapFromAsset(getApplicationContext(),"likebtn.png");
        likeButton=new Mat();
        Utils.bitmapToMat(bm,likeButton);
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.facebook.lite");
        startActivity(launchIntent);
        handler.postDelayed(updateData,2000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler=null;
    }
    public static Bitmap getBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();
        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            // handle exception
        }
        return bitmap;
    }
    private Runnable updateData = new Runnable(){
        public void run() {
            runcmd("/system/bin/screencap -p sdcard/img.png");
            Bitmap bitmap = BitmapFactory.decodeFile("sdcard/img.png");
            Mat img=new Mat();
            Utils.bitmapToMat(bitmap,img);

            Mat result = new Mat(img.rows() - likeButton.rows() + 1, img.cols() - likeButton.cols() + 1, CvType.CV_32FC1);
            Imgproc.matchTemplate(img, likeButton, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            if(mmr.maxVal>0.99){
                runcmd("/system/bin/input tap "+mmr.maxLoc.x+likeButton.cols()/2+" "+mmr.maxLoc.y+likeButton.rows()/2);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            runcmd("/system/bin/input swipe 500 1000 500 600 100");
            if (handler != null)
                handler.postDelayed(updateData, 500);
        }
    };

    private void runcmd(String cmd){
        try {
            sh = Runtime.getRuntime().exec("su", null, null);
            os = sh.getOutputStream();
            os.write(cmd.getBytes("ASCII"));
            os.flush();
            os.close();
            sh.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
