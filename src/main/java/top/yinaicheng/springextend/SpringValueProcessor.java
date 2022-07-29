package top.yinaicheng.springextend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import top.yinaicheng.utils.spring.PlaceholderHelper;
import top.yinaicheng.utils.spring.SpringValue;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
/**
 * 这个类，收集所有用到@value的地方，记录下来
 * Spring容器通过BeanProcessor给了我们一个机会对spring管理的bean进行再加工
 * @author yinaicheng
 */
@Configuration
public class SpringValueProcessor implements BeanPostProcessor {

    /**
     * 保存所有配置以及对应的bean信息
     */
    public static final ConcurrentHashMap<String, SpringValue> cacheMap=new ConcurrentHashMap();

    private final PlaceholderHelper placeholderHelper=new PlaceholderHelper();

    /**
     * bean初始化方法调用前被调用
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean,String beanName){
        Class obj=bean.getClass();
        List<Field> fields=findAllField(obj);
        for(Field field:fields){
            Value value=field.getAnnotation(Value.class);
            if(value!=null){
                Set<String> keys=placeholderHelper.extractPlaceholderKeys(value.value());
                for(String key:keys){
                    SpringValue springValue=new SpringValue(key,value.value(),bean,beanName,field,false);
                    cacheMap.putIfAbsent(key,springValue);
                }
            }
        }
        return bean;
    }

    /**
     * @return 返回指定类型上所有的字段
     */
    private List<Field> findAllField(Class<?> clazz) {
        final List<Field> res = new LinkedList<>();
        ReflectionUtils.doWithFields(clazz, res::add);
        return res;
    }

}
