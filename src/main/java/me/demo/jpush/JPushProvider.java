package me.demo.jpush;

import com.alibaba.fastjson.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import me.demo.jpush.util.Base64;
import me.demo.jpush.util.CustomHttpClient;

/**
 * Jpush 通知/消息推送
 *
 * @author geosmart
 * @date 2016/12/06
 */
public class JPushProvider {
    private static HttpClient httpClient;
    public String v3PushUrl;
    public String masterSecret;
    public String appKey;
    public int retry = 3;

    public JPushProvider(String v3PushUrl, String masterSecret, String appKey, int retry) {
        this.v3PushUrl = v3PushUrl;
        this.masterSecret = masterSecret;
        this.appKey = appKey;
        this.retry = retry;
    }

    public static String getBasicAuthorization(String appKey, String masterSecret) {
        String encodeKey = appKey + ":" + masterSecret;
        return "Basic " + String.valueOf(Base64.encode(encodeKey.getBytes()));
    }

    /**
     * 读取流中所有数据
     *
     * @param input 输入流
     * @return 读取的字节长度
     */
    public static byte[] readStream(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] tmp = new byte[5 * 1024 * 1024];
        int nr;
        while ((nr = input.read(tmp, 0, 5 * 1024 * 1024)) > 0) {  // 读入字节，存入数组
            output.write(tmp, 0, nr);
        }
        return output.toByteArray();
    }

    public boolean systemNotify(String msg) {
        initJPushClient();
        try {
            JSONObject notifyJson = new JSONObject();
            notifyJson.put("platform", "all");
            notifyJson.put("audience", "all");
            JSONObject notification = new JSONObject();
            notification.put("alert", msg);
            notifyJson.put("notification", notification);
            //设置传入参数
            StringEntity entity = new StringEntity(notifyJson.toString(), "UTF-8");
            HttpPost httpPost = new HttpPost(v3PushUrl);
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
            httpPost.setHeader("Authorization", getBasicAuthorization(appKey, masterSecret));
            HttpResponse recvJsonObject = httpClient.execute(httpPost);
            String response = getResponseStr(recvJsonObject);
            if (response == null) {
                return false;
            }
            System.out.println("notify result : " + response);
        } catch (Exception e) {
            // Should review the error, and fix the request
            System.err.println("Should review the error, and fix the request：" + e);
            return false;
        }
        return true;
    }

    private void initJPushClient() {
        if (httpClient == null) {
            httpClient = CustomHttpClient.getHttpClient();
        }
    }

    private String getResponseStr(HttpResponse response) throws Exception {
        int ret = response.getStatusLine().getStatusCode();
        HttpEntity respEntity = response.getEntity();
        InputStream input = respEntity.getContent();
        String responseStr = null;
        BufferedReader br;
        if (ret == HttpStatus.SC_OK) {
            // 响应分析
            br = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            StringBuffer responseString = new StringBuffer();
            String result = br.readLine();
            while (result != null) {
                responseString.append(result);
                result = br.readLine();
            }
            responseStr = responseString.toString();
            System.out.println("消息推送返回码[" + ret + "],返回结果[" + responseStr + "]");
        } else {
            System.err.println("消息推送返回码[" + ret + "]，" + new String(readStream(input)));
        }
        input.close();
        return responseStr;
    }
}
