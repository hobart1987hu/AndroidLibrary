package com.android.library.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import android.text.TextUtils;
import android.util.Patterns;
import android.webkit.URLUtil;

public class URLUtils {


    private URLUtils(){

    }

    public static boolean checkURL(CharSequence data) {
        if (TextUtils.isEmpty(data)) {
            return false;
        }
        Pattern URL_PATTERN = Patterns.WEB_URL;
        boolean isURL = URL_PATTERN.matcher(data).matches();
        if (!isURL) {
            String urlString = data + "";
            if (URLUtil.isNetworkUrl(urlString)) {
                try {
                    new URL(urlString);
                    isURL = true;
                } catch (Exception e) {
                }
            }
        }
        return isURL;
    }
    
    public static URL getURL(String data){
        URL url=null;
        try {
            url=new URL(data);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

}
