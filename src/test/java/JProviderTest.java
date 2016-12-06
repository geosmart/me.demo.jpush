import org.junit.Test;

import me.demo.jpush.JPushProvider;

/**
 * 通知推送-单元测试
 *
 * @author geosmart
 * @date 2016/12/06
 */
public class JProviderTest {
    @Test
    public void test_Push() {
        JPushProvider jpush = new JPushProvider("https://api.jpush.cn/v3/push", "", "", 3);
        jpush.systemNotify("您有1个未审核的订单");
    }
}
