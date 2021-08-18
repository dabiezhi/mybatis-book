Mybatis中有很多模块会使用到ObjectFactory接口，该接口提供了多个create()方法的重载，通过这些create()方法可以创建指定类型的对象。

```java

/**
 * MyBatis uses an ObjectFactory to create all needed new Objects.
 * 
 * @author Clinton Begin
 */
public interface ObjectFactory {

  /**
   * 设置配置信息3
   * @param properties configuration properties
   */
  void setProperties(Properties properties);

  /**
   * 通过无参构造器创建指令类的对象
   * @param type Object type
   * @return
   */
  <T> T create(Class<T> type);

  /**
   * 根据参数列表，从指定类型中选择合适的构造器创建对象
   * @param type Object type
   * @param constructorArgTypes Constructor argument types
   * @param constructorArgs Constructor argument values
   * @return
   */
  <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);
  
  /**
   * 检测指定类型是否为集合类型，主要处理java.util.Collection及其自雷
   *
   * @since 3.1.0
   * @param type Object type
   * @return whether it is a collection or not
   */
  <T> boolean isCollection(Class<T> type);

}
```
DefaultObjectFactory是Mybatis提供的ObjectFactory接口的唯一实现，它是一个反射工厂，其create()方法通过调用instantiateClass()方法实现。
DefaultObjectFactory.instantiateClass()方法会根据传入的参数列表选择合适的构造函数实例化对象，具体实现如下:

```java

public class DefaultObjectFactory implements ObjectFactory, Serializable {

  private static final long serialVersionUID = -8855120656740914948L;

  @SuppressWarnings("unchecked")
  @Override
  public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
    Class<?> classToCreate = resolveInterface(type);
    // we know types are assignable
    return (T) instantiateClass(classToCreate, constructorArgTypes, constructorArgs);
  }

  @Override
  public void setProperties(Properties properties) {
    // no props for default
  }

  <T> T instantiateClass(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
    try {
      Constructor<T> constructor;
      //通过无参构造函数创建对象
      if (constructorArgTypes == null || constructorArgs == null) {
        constructor = type.getDeclaredConstructor();
        if (!constructor.isAccessible()) {
          constructor.setAccessible(true);
        }
        return constructor.newInstance();
      }
      // 根据指定的参数列表查找构造函数，并实例化对象
      constructor = type.getDeclaredConstructor(constructorArgTypes.toArray(new Class[constructorArgTypes.size()]));
      if (!constructor.isAccessible()) {
        constructor.setAccessible(true);
      }
      return constructor.newInstance(constructorArgs.toArray(new Object[constructorArgs.size()]));
    } catch (Exception e) {
      throw new ReflectionException("Error instantiating " + type + " with invalid types (" + argTypes + ") or values (" + argValues + "). Cause: " + e, e);
    }
  }

  @Override
  public <T> boolean isCollection(Class<T> type) {
    return Collection.class.isAssignableFrom(type);
  }

}

```
除了使用Mybatis提供的DefaultObjectFactory实现，我们还可以在mybatis-config.xml配置文件中指定自定义的ObjectFactory接口实现类，从而实现功能上的扩展。

