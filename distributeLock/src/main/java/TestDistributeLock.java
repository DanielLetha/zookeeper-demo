import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @Brief :  ${用途}
 * @Author: liangfei/liangfei@simpletour.com
 * @Date :  2016/10/20 14:06
 * @Since ： ${VERSION}
 * @Remark: ${Remark}
 */
public class TestDistributeLock implements Watcher {
    private int threadId;
    private ZooKeeper zk = null;
    private String selfPath;
    private String waitPath;
    private String PREFIX_OF_THREAD;
    private static final int SESSION_TIMEOUT = 10000;
    private static final String GROUP_PATH = "/disLocks";
    private static final String SUB_PATH = "/disLocks/sub";
    private static final String CONNECTION_STRING = "127.0.0.1:2182";

    private static final int THREAD_NUM = 10;

    private int i = 0;
    //确保连接zk成功；
    private CountDownLatch connectedSemaphore = new CountDownLatch(1);
    //确保所有线程运行结束；
    private static final CountDownLatch threadSemaphore = new CountDownLatch(THREAD_NUM);
//    private static final Logger LOG = LoggerFactory.getLogger(TestDistributeLock.class);

    public static void main(String[] args) {
        for(int i=0; i < THREAD_NUM; i++){
            final int threadId = i+1;
            new Thread(){
                @Override
                public void run() {
                    try{
                        TestDistributeLock dc = new TestDistributeLock(threadId);
                        dc.createConnection(CONNECTION_STRING, SESSION_TIMEOUT);
                        //GROUP_PATH不存在的话，由一个线程创建即可；
                        synchronized (threadSemaphore){
                            dc.createPath(GROUP_PATH, "该节点由线程" + threadId + "创建", true);
                        }
                        dc.getLock();
                    } catch (Exception e){
                        System.out.println("[第"+threadId+"个线程] 抛出的异常：");
                        e.printStackTrace();
                    }
                }
            }.start();
        }
        try {
            threadSemaphore.await();
            System.out.println("所有线程运行结束!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public TestDistributeLock(int threadId) {
        this.threadId = threadId;
        PREFIX_OF_THREAD ="[第"+threadId+"个线程]";
    }

    /**
     * 获得锁
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void getLock() throws KeeperException,InterruptedException{
        selfPath= zk.create(SUB_PATH,null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println(PREFIX_OF_THREAD+"创建锁路径"+selfPath);
        if(checkMinPath()){
            getLockSuccess();
        }
    }

    /**
     * 创建节点
     * @param path 节点路径
     * @param data 数据
     * @param needwatch 节点是否有watch监听
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    public boolean createPath(String path,String data,boolean needwatch) throws KeeperException,InterruptedException{
        if( zk.exists(path,needwatch)==null){
            System.out.println( PREFIX_OF_THREAD + "节点创建成功, Path: "
                    + this.zk.create( path,
                    data.getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT )
                    + ", content: " + data );
        }
        return true;
    }

    /**
     * 创建连接
     * @param connStr 连接信息
     * @param sessionTimeour 超时时间
     * @throws IOException
     * @throws InterruptedException
     */
    public void createConnection(String connStr,int sessionTimeour) throws IOException,InterruptedException{
        zk = new ZooKeeper(connStr,sessionTimeour,this);
        connectedSemaphore.await();
    }

    /**
     * 获取锁成功
     * @throws KeeperException
     * @throws InterruptedException
     */
    public void getLockSuccess() throws KeeperException,InterruptedException {
        if(zk.exists(this.selfPath,false)==null){
            System.out.println(PREFIX_OF_THREAD+"本节点已经不存在了...");
            return;
        }
        System.out.println(PREFIX_OF_THREAD+"得到锁，开始执行");
        Thread.sleep(2000);
        i++;
        System.out.println(PREFIX_OF_THREAD+"删除本节点："+selfPath);
        zk.delete(this.selfPath,-1);
        releaseConnection();
        threadSemaphore.countDown();
    }

    /**
     * 关闭ZK连接
     */
    public void releaseConnection() {
        if ( this.zk !=null ) {
            try {
                this.zk.close();
            } catch ( InterruptedException e ) {}
        }
        System.out.println(PREFIX_OF_THREAD + "释放连接");
    }

    /**
     * 检查自己是否是最小节点
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    public boolean checkMinPath() throws KeeperException,InterruptedException{
        List<String> subNodes = zk.getChildren(GROUP_PATH, false);
        Collections.sort(subNodes);
        int index = subNodes.indexOf( selfPath.substring(GROUP_PATH.length()+1));
        switch (index){
            case -1:{
                System.out.println(PREFIX_OF_THREAD+"本节点已不在了..."+selfPath);
                return false;
            }
            case 0:{
                System.out.println(PREFIX_OF_THREAD+"子节点中，我果然是老大"+selfPath);
                return true;
            }
            default:{
                this.waitPath = GROUP_PATH +"/"+ subNodes.get(index - 1);
                System.out.println(PREFIX_OF_THREAD+"获取子节点中，排在我前面的"+waitPath);
                try{
                    zk.getData(waitPath, true, new Stat());
                    return false;
                }catch(KeeperException e){
                    if(zk.exists(waitPath,false) == null){
                        System.out.println(PREFIX_OF_THREAD+"子节点中，排在我前面的"+waitPath+"已失踪，幸福来得太突然?");
                        return checkMinPath();
                    }else{
                        throw e;
                    }
                }
            }

        }
    }

    public void process(WatchedEvent event) {
        if(event == null){
            return;
        }
        Event.KeeperState keeperState = event.getState();
        Event.EventType eventType = event.getType();
        if ( Event.KeeperState.SyncConnected == keeperState) {
            if ( Event.EventType.None == eventType ) {
                System.out.println( PREFIX_OF_THREAD + "成功连接上ZK服务器" );
                connectedSemaphore.countDown();
            }else if (event.getType() == Event.EventType.NodeDeleted && event.getPath().equals(waitPath)) {
                System.out.println(PREFIX_OF_THREAD + "收到情报，排我前面的家伙已挂，我是不是可以出山了？");
                try {
                    if(checkMinPath()){
                        getLockSuccess();
                    }
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }else if ( Event.KeeperState.Disconnected == keeperState ) {
            System.out.println( PREFIX_OF_THREAD + "与ZK服务器断开连接" );
        } else if ( Event.KeeperState.AuthFailed == keeperState ) {
            System.out.println( PREFIX_OF_THREAD + "权限检查失败" );
        } else if ( Event.KeeperState.Expired == keeperState ) {
            System.out.println( PREFIX_OF_THREAD + "会话失效" );
        }
    }
}

