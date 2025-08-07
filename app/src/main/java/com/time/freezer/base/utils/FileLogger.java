package com.time.freezer.base.utils;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileLogger {

    private static void appendLog(String cname, String method, String data){
        appendLog(cname + "\t" + method + "\t" + data);
    }

    public static void appendLog(String text)
    {
        File logFile = new File(Environment.getExternalStorageDirectory(), "time_freezer_log.txt");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
