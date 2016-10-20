import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.ZKDatabase;

import java.io.IOException;

/**
 * @Brief :  ${用途}
 * @Author: liangfei/liangfei@simpletour.com
 * @Date :  2016/10/19 18:52
 * @Since ： ${VERSION}
 * @Remark: ${Remark}
 */
public class TesDistSync {

    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {
        String zkServer1 = "127.0.0.1:2181";
        String zkServer2 = "127.0.0.1:2182";
        String zkServer3 = "127.0.0.1:2183";
        ZooKeeper zk1 = new ZooKeeper(zkServer1,6000,new Watcher(){
            public void process(WatchedEvent event) {
            }
        });
        ZooKeeper zk2 = new ZooKeeper(zkServer2,6000,new Watcher(){
            public void process(WatchedEvent event) {
            }
        });
        //模拟数据同步
        if(zk1.exists("/sync",true)== null){
            zk1.create("/sync","test".getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }else{
            zk1.setData("/sync","test".getBytes(),-1);
        }
        byte value[] = null;
//        System.out.println(String.valueOf(zk1.getData("/sync",true,null)));
        if(zk2.getData("/sync",true,null) !=null){
            value = zk2.getData("/sync",true,null);
            System.out.println(new String(value));
        }
        //模拟节点同步
        zk1.delete("/sync",-1);


        System.out.println(zk2.getChildren("/sync",true));

    }
}
