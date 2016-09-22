package androiddev.milnaa.com.downloadfromasset;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.common.io.ByteStreams;
import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.AndServerBuild;
import com.yanzhenjie.andserver.AndServerRequestHandler;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AndServerBuild andServerBuild = AndServerBuild.create();
        andServerBuild.setPort(4477);// 指定http端口号。

        andServerBuild.add("test", new AndServerTestHandler());
        andServerBuild.add("dd.apk", new AndServerDownloadHandler());
        AndServer andServer = andServerBuild.build();
        andServer.launch();

        mWebView = (WebView) findViewById(R.id.webview);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new InJavaScriptLocalObj(), "local_obj");
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setDomStorageEnabled(true);

        mWebView.requestFocus();
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setBackgroundColor(Color.parseColor("#ffffff"));

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                if (url != null && url.startsWith("myscheme://"))
//                {
//                    String newUrl = url.replace("myscheme://", "file://android_asset/");
//                    mWebView.loadUrl(newUrl);
//                    return true;
//                }
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.loadUrl("javascript:window.local_obj.showSource('<head>'+"
                        + "document.getElementsByTagName('html')[0].innerHTML+'</head>');");

            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

        });
        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String s, String s1, String s2, String s3, long l) {
                Uri uri = Uri.parse(s);
                //Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                //startActivity(intent);
                DownloadManager.Request req = new DownloadManager.Request(uri);
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                req.setTitle("mod2.apk");
                req.setDescription("download then open");
                req.setMimeType("application/vnd.android.package-archive");
                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                long downloadId = dm.enqueue(req);
            }
        });
        mWebView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl("http://localhost:4477/test");
            }
        }, 3000);

        //mWebView.loadUrl("file:///android_asset/home.html");
    }

    final class InJavaScriptLocalObj {
        @JavascriptInterface
        public void showSource(String html) {
            System.out.println("====>html=" + html);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (mWebView.canGoBack()) {
                mWebView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void startServer() {

    }

    public class AndServerTestHandler implements AndServerRequestHandler {
        @Override
        public void handle(HttpRequest rq, HttpResponse rp, HttpContext ct) throws HttpException, IOException {
            StringEntity stringEntity = new StringEntity("<!DOCTYPE html><html><head><title></title><meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\"><meta name=\"viewport\"          content=\"width=device-width, initial-scale=1, maximum-scale=1, user-scalable=0\"/><style type=\"text/css\">        article, aside, details, figcaption, figure, footer, header, hgroup, menu, nav, section {        display: block;        }        html, body, div, span, object, iframe,        h1, h2, h3, h4, h5, h6, p, blockquote, pre,        abbr, address, cite, code,        del, dfn, em, img, ins, kbd, q, samp,        small, strong, sub, sup, var,        b, i,        dl, dt, dd, ol, ul, li,        fieldset, form, label, legend,        table, caption, tbody, tfoot, thead, tr, th, td,        article, aside, canvas, details, figcaption, figure,        footer, header, hgroup, menu, nav, section, summary,        time, mark, audio, video {        margin:0;        padding:0;        border:0;        outline:0;        vertical-align:baseline;        background:transparent;        }        ul,li {list-style:none;}        body {font:14px/20px Calibri,Microsoft YaHei,verdana,Arial,Helvetica,sans-serif;background-color:#ffffff}        a {margin:100px;font:40px;}</style></head><body><a href=\"http://localhost:4477/dd.apk\">Download</a><script type=\"text/javascript\"></script></body></html>", "utf-8");
            stringEntity.setContentType("text/html");
            rp.setEntity(stringEntity);
        }
    }

    public class AndServerDownloadHandler implements AndServerRequestHandler {
        @Override
        public void handle(HttpRequest rq, HttpResponse rp, HttpContext ct) throws HttpException, IOException {
            InputStream in = getAssets().open("mod2.apk");
            byte[] bytes = ByteStreams.toByteArray(in);
            ByteArrayEntity byteArrayEntity = new ByteArrayEntity(bytes);
            byteArrayEntity.setContentType("application/vnd.android.package-archive");
            //new Thread(new DownLoadThread("file:///android_asset/mod2.apk")).start();
//            StringEntity stringEntity = new StringEntity("<!DOCTYPE html><html>downloaded</html>", "utf-8");
//            stringEntity.setContentType("text/html");
            rp.setEntity(byteArrayEntity);
        }
    }

    public class DownLoadThread implements Runnable {

        private String dlUrl;

        public DownLoadThread(String dlUrl) {
            this.dlUrl = dlUrl;
        }

        @Override
        public void run() {
            Log.e("HEHE", "开始下载~~~~~");
            InputStream in = null;
            FileOutputStream fout = null;
            try {
                in = getAssets().open("mod2.apk");
                File downloadFile, sdFile;
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    Log.e("HEHE", "SD卡可写");
                    downloadFile = Environment.getExternalStorageDirectory();
                    sdFile = new File(downloadFile, "csdn_client.apk");
                    fout = new FileOutputStream(sdFile);
                } else {
                    Log.e("HEHE", "SD卡不存在或者不可读写");
                }
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    fout.write(buffer, 0, len);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fout != null) {
                    try {
                        fout.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.e("HEHE", "下载完毕~~~~");
        }
    }
}
