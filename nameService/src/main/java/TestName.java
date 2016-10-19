import org.apache.zookeeper.*;

import java.io.IOException;

/**
 * @Brief :  ${用途}
 * @Author: liangfei/liangfei@simpletour.com
 * @Date :  2016/10/19 18:52
 * @Since ： ${VERSION}
 * @Remark: ${Remark}
 */
public class TestName {

    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {
        String zkServer = "127.0.0.1:2181";
        ZooKeeper zk = new ZooKeeper(zkServer,6000,new Watcher(){
            public void process(WatchedEvent event) {
            }
        });
        System.out.println("所有的节点"+zk.getChildren("/",true));
        System.out.println("创建zktest节点");
        zk.create("/zktest","zktest".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        zk.create("/zktest/sub1","sub1".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        zk.create("/zktest/sub2","sub3".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        System.out.println("zktest下级节点："+zk.getChildren("/zktest",true));

        //删除节点
        System.out.println("\n删除zktest以及子节点");
        zk.delete("/zktest/sub1", -1);
        zk.delete("/zktest/sub2", -1);
        zk.delete("/zktest", -1);
    }
}
