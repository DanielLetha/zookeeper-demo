import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @Brief :  ${用途}
 * @Author: liangfei/liangfei@simpletour.com
 * @Date :  2016/10/19 19:26
 * @Since ： ${VERSION}
 * @Remark: ${Remark}
 */
public class MyZkGroup {

    private static final int SESSION_TIMEOUT = 5000;
    private CountDownLatch connectedSignal = new CountDownLatch(2);

    private ZooKeeper zk;

    private void getthreadname(String funname){
        Thread current = Thread.currentThread();
        System.out.println(funname + " is call in  " + current.getName());
    }
    // 运行在另外一个线程 main-EventThread
    private void process(WatchedEvent event) {
        getthreadname("process");
        System.out.println((event.getType()));  // 打印状态

        if (event.getState() == Watcher.Event.KeeperState.SyncConnected){
            connectedSignal.countDown();
        }
    }

    // 测试自定义监听
    public Watcher wh = new Watcher() {
        public void process(WatchedEvent event) {
            getthreadname("Watcher::process");
            System.out.println("回调watcher实例： 路径" + event.getPath() + " 类型："
                    + event.getType());
        }
    };

    public void connect(String hosts) throws IOException, InterruptedException{
        getthreadname("connect");
        zk = new ZooKeeper(hosts, SESSION_TIMEOUT, wh); // 最后一个参数用this会调用自身的监听  wh 代表？
        //connectedSignal.await(); // 主线程挂起
    }
    private Stat isExist(String groupname) throws InterruptedException, KeeperException {
        return zk.exists(groupname, true); // this
    }
    // 加入组(可以理解成一个创建本次连接的一个组)
    public void join(String groupname, String meberName) throws KeeperException, InterruptedException{
        String path = "/" + groupname + "/" + meberName ;


        // EPHEMERAL断开将被删除
        String createdpath = zk.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        System.out.println("join : " + createdpath);
    }

    public List<String> getChilds(String path) throws KeeperException, InterruptedException {
        // TODO Auto-generated method stub
        if (zk != null) {
            // return zk.getChildren(path, false); // false表示没有设置观察
            //zk.getChildren(path, true, childwatcher.processResult, null);
            zk.getChildren( path, true, new AsyncCallback.Children2Callback() {
                public void processResult(int rc, String path, Object ctx,
                                          List<String> children, Stat stat) { // stat参数代表元数据
                    // TODO Auto-generated method stub
                    System.out.println("****");
                    for (int i = 0; i < children.size() - 1; i ++){
                        System.out.println("mempath = " + children.get(i) + stat);
                    }
                }
            }, null);

        }
        return null;
    }

    public void create(String path) throws KeeperException, InterruptedException, IOException{
        // Ids.OPEN_ACL_UNSAFE 开放式ACL，允许任何客户端对znode进行读写
        // CreateMode.PERSISTENT 持久的znode，本次连接断开后还会存在，应该有持久化操作.
        // PERSISTENT_SEQUENTIAL 持久化顺序，这个由zookeeper来保证

        String createdpath = zk.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        System.out.println("crateed : " + createdpath);
    }

    public void close() throws InterruptedException{
        if  (zk != null){
            zk.close();
        }
    }

    public static class Childwatcher implements AsyncCallback.Children2Callback {
        public ChildrenCallback processResult;

        public void processResult(int rc, String path, Object ctx,
                                  List<String> children, Stat stat) {
            // TODO Auto-generated method stub
            System.out.println("**** path " + stat);
        }
    }

    public void delete(String groupname) throws InterruptedException, KeeperException{
        zk.delete(groupname, -1);
    }

    public Stat isexist(String groupname) throws InterruptedException, KeeperException{
        return zk.exists(groupname, true); // this
    }

    public void write(String path, String value)throws Exception{
        Stat stat=zk.exists(path, false);
        if(stat==null){
            zk.create(path, value.getBytes("UTF-8"), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }else{

            zk.setData(path, value.getBytes("UTF-8"), -1);
        }
    }

    public String read(String path,Watcher watch)throws Exception{
        byte[] data=zk.getData(path, watch, null);
        return new String(data, "UTF-8");
    }
}
