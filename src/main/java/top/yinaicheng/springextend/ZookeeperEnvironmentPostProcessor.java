package top.yinaicheng.springextend;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.RetryOneTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Spring支持动态的读取文件，留下的扩展接口
 * @author: yinaicheng
 * @date: 2022/7/14 1:05
 */
@Slf4j
public class ZookeeperEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application){
        log.info("拿到environment对象，往里面塞东西");
        String configCenterUrl=environment.getProperty("zookeeper.url");
        String configNodeName=environment.getProperty("zookeeper.nodename");
        CuratorFramework zkClient=CuratorFrameworkFactory.newClient(configCenterUrl,new RetryOneTime(1000));
        /*启动和zookeeper的连接*/
        zkClient.start();
        /*获取节点下的子节点，下面的每一个子节点都代表一项配置*/
        Map<String,Object> configMap=new HashMap<>();
        try {
            List<String> configNames=zkClient.getChildren().forPath("/".concat(configNodeName));
            for(String configName:configNames){
                byte[] value=zkClient.getData().forPath("/".concat(configNodeName).concat("/").concat(configName));
                configMap.put(configName,new String(value));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*将zookeeper中取得的节点配置放到spring容器里面*/
        MapPropertySource propertySource=new MapPropertySource("zkPropertySource",configMap);
        environment.getPropertySources().addFirst(propertySource);
        /*获取动态更新*/
        /*zookeeper最新的内容*/
        TreeCache treeCache=new TreeCache(zkClient,"/".concat(configNodeName));
        try {
            treeCache.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        treeCache.getListenable().addListener((curatorFramework, treeCacheEvent) -> {
            switch (treeCacheEvent.getType()){
                case NODE_UPDATED:
                    logger.info("收到了数据变化的通知"+treeCacheEvent.getData());
                    String configName=treeCacheEvent.getData().getPath().replaceAll("/".concat(configNodeName).concat("/"), "");
                    Object newValue=new String(treeCacheEvent.getData().getData());
                    /*spring已经创建对象，改变@value注入的值*/
                    /*反射*/
                    SpringValueProcessor.cacheMap.get(configName).update(newValue);
                    break;
                default:
                    break;
            }
        });


    }
}
