package com.example.l.mobilefacenet.util;

import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.content.ContentValues.TAG;

public class FileUtil {

    public static void copyBigDataToSD(AssetManager assetManager, String strOutFileName) throws IOException {
        Log.i(TAG, "start copy file " + strOutFileName);
        File sdDir = Environment.getExternalStorageDirectory();//get directory
        File file = new File(sdDir.toString()+"/facem/");
        if (!file.exists()) {
            file.mkdir();
        }

        String tmpFile = sdDir.toString()+"/facem/" + strOutFileName;
        File f = new File(tmpFile);
        if (f.exists()) {
            Log.i(TAG, "file exists " + strOutFileName);
            return;
        }
        InputStream myInput;
        java.io.OutputStream myOutput = new FileOutputStream(sdDir.toString()+"/facem/"+ strOutFileName);
        myInput = assetManager.open(strOutFileName);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();
        Log.i(TAG, "end copy file " + strOutFileName);

    }
}
