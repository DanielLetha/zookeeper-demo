import java.util.List;

/**
 * @Brief :  ${用途}
 * @Author: liangfei/liangfei@simpletour.com
 * @Date :  2016/10/19 19:04
 * @Since ： ${VERSION}
 * @Remark: ${Remark}
 */
public class TestGroup {
    public static void main(String[] args) throws Exception {
        String hosts = "127.0.0.1";
        String groupname = "zk" ;
        String meberName = String.valueOf(System.currentTimeMillis());
        String path = "/" + groupname;

        // create
        MyZkGroup test = new MyZkGroup();
        //  连接
        test.connect(hosts);
        //
        if (null != test.isexist(path)){
            test.delete(path);
        }

        test.isexist(path);
        test.create(path);

        test.isexist(path);
        test.write(path, "test");

        test.isexist(path);
        String result = test.read(path, test.wh);
        System.out.println(path + " value = " + result);

        int sum = 0;
        for (int j = 0 ; j< 10000; j++){
            sum++;
            Thread.sleep(10);
        }
        System.out.println(sum);
        test.close();

        System.exit(2);
        // 一个本地连接的znode
        test.connect(hosts);
        test.join(groupname, meberName);

        // 遍历
        List<String> memlist = test.getChilds("/" + "zk" );
        if (memlist != null){
            for (int i = 0; i < memlist.size() - 1; i ++){
                System.out.println("mempath = " + memlist.get(i));
            }
        }
    }
}