package wxdgaming.webim.gateway;

import wxdgaming.boot2.core.CoreScan;
import wxdgaming.boot2.starter.RunApplicationMain;
import wxdgaming.boot2.starter.WxdApplication;
import wxdgaming.boot2.starter.net.SocketScan;
import wxdgaming.boot2.starter.scheduled.ScheduledScan;

public class WebIMGatewayApplication {

    public static void main(String[] args) {
        RunApplicationMain runApplication = WxdApplication.run(
                CoreScan.class,
                ScheduledScan.class,
                SocketScan.class,
                WebIMGatewayApplication.class
        );
        runApplication.start();
        runApplication.registerShutdownHook();
    }

}