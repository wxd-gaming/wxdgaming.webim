package wxdgaming.webim.service;

import wxdgaming.boot2.core.CoreScan;
import wxdgaming.boot2.starter.RunApplicationMain;
import wxdgaming.boot2.starter.WxdApplication;
import wxdgaming.boot2.starter.batis.mapdb.MapDBScan;
import wxdgaming.boot2.starter.net.SocketScan;
import wxdgaming.boot2.starter.scheduled.ScheduledScan;

public class WebIMServiceApplication {

    public static void main(String[] args) {
        RunApplicationMain runApplication = WxdApplication.run(
                CoreScan.class,
                ScheduledScan.class,
                SocketScan.class,
                MapDBScan.class,
                WebIMServiceApplication.class
        );
        runApplication.start();
        runApplication.registerShutdownHook();
    }

}