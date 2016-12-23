package me.demo.jpush;

import com.alibaba.fastjson.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;

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
    private String v3PushUrl;
    private String masterSecret;
    private String appKey;
    private int retry = 3;

    public JPushProvider(String v3PushUrl, String masterSecret, String appKey, int retry) {
        this.v3PushUrl = v3PushUrl;
        this.masterSecret = masterSecret;
        this.appKey = appKey;
        this.retry = retry;
    }

    private static String getBasicAuthorization(String appKey, String masterSecret) {
        String encodeKey = appKey + ":" + masterSecret;
        return "Basic " + String.valueOf(Base64.encode(encodeKey.getBytes()));
    }

    /**
     * 读取流中所有数据
     *
     * @param input 输入流
     * @return 读取的字节长度
     */
    private static byte[] readStream(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] tmp = new byte[5 * 1024 * 1024];
        int nr;
        while ((nr = input.read(tmp, 0, 5 * 1024 * 1024)) > 0) {  // 读入字节，存入数组
            output.write(tmp, 0, nr);
        }
        return output.toByteArray();
    }

    public boolean callService(String param) {
        int retryCount = 0;
        boolean isSuccess = false;
        //如果未推送成功，重试n次
        while (!isSuccess && retryCount < retry) {
            isSuccess = systemNotify(param);
            if (!isSuccess) {
                retryCount++;
            }
        }
        return isSuccess;
    }

    /**
     * 系统通知
     *
     * @param msg 通知内容
     * @return 是否调用成功
     */
    private boolean systemNotify(String msg) {
        initJPushClient();
        HttpPost post = new HttpPost(v3PushUrl);
        post.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
        post.setConfig(RequestConfig.custom().setConnectTimeout(1000 * 120).setSocketTimeout(1000 * 120).build());
        try {
            JSONObject notifyJson = new JSONObject();
            //全平台推送
            notifyJson.put("platform", "all");
            notifyJson.put("audience", "all");
            //IOS平台参数
            JSONObject iosOption = new JSONObject();
            iosOption.put("time_to_live", 60);
            iosOption.put("apns_production", false);
            notifyJson.put("options", iosOption);
            //消息配置
            JSONObject alertMsg = new JSONObject();
            alertMsg.put("alert", msg);
            alertMsg.put("sound", "default");
            JSONObject notification = new JSONObject();
            notification.put("android", alertMsg);
            notification.put("ios", alertMsg);
            notifyJson.put("notification", notification);
            //设置传入参数
            StringEntity entity = new StringEntity(notifyJson.toString(), "UTF-8");
            post.setEntity(entity);
            post.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
            post.setHeader("Authorization", getBasicAuthorization(appKey, masterSecret));
            HttpResponse recvJsonObject = httpClient.execute(post);
            String response = getResponseStr(recvJsonObject);
            if (response == null) {
                return false;
            }
            System.out.println("notify result : " + response);
        } catch (Exception e) {
            // Should review the error, and fix the request
            System.err.println("Should review the error, and fix the request：" + e);
            return false;
        } finally {
            try {
                if (post != null) {
                    post.abort();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("close jpush post error");
            }
        }
        return true;
    }

    private void initJPushClient() {
        if (httpClient == null) {
            httpClient = CustomHttpClient.getHttpClient();
        }
    }

    /**
     * 获取发送结果
     */
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
