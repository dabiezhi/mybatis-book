ObjectWrapper提供了获取/设置对象中指定的属性值、检测getter/setter等常用功能，但是ObjectWrapper只是这些功能的最后一站，我们省略了
对属性表达式解析过程的介绍，而该解析过程是在MetaObject中实现的

```java

/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.BeanWrapper;
import org.apache.ibatis.reflection.wrapper.CollectionWrapper;
import org.apache.ibatis.reflection.wrapper.MapWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

/**
 * @author Clinton Begin
 */
public class MetaObject {

  //原始JavaBean对象
  private Object originalObject;
  //封装了originalObject对象
  private ObjectWrapper objectWrapper;
  //负责实例化originalObject的工厂对象
  private ObjectFactory objectFactory;
  //负责创建ObjectWrapper的工厂对象
  private ObjectWrapperFactory objectWrapperFactory;
  //用于创建并缓存Reflector对象的工厂对象
  private ReflectorFactory reflectorFactory;

  private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
    this.originalObject = object;
    this.objectFactory = objectFactory;
    this.objectWrapperFactory = objectWrapperFactory;
    this.reflectorFactory = reflectorFactory;

    if (object instanceof ObjectWrapper) {
      this.objectWrapper = (ObjectWrapper) object;
    } else if (objectWrapperFactory.hasWrapperFor(object)) {
      this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);
    } else if (object instanceof Map) {
      this.objectWrapper = new MapWrapper(this, (Map) object);
    } else if (object instanceof Collection) {
      this.objectWrapper = new CollectionWrapper(this, (Collection) object);
    } else {
      this.objectWrapper = new BeanWrapper(this, object);
    }
  }
}
```
MetaObject的构造方法会根据传入的原始对象的类型以及ObjectFactory工厂的实现，创建相应的ObjectWrapper对象。
```java
public class MetaObject {

  private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
    //初始化上述字段
    this.originalObject = object;
    this.objectFactory = objectFactory;
    this.objectWrapperFactory = objectWrapperFactory;
    this.reflectorFactory = reflectorFactory;

    if (object instanceof ObjectWrapper) { //若原始对象已经是ObjectWrapper对象，则直接使用
      this.objectWrapper = (ObjectWrapper) object;
    } else if (objectWrapperFactory.hasWrapperFor(object)) {
      //若ObjectWrapperFactory能够为该原始对象创建对应的ObjectWrapper对象，则由有限使用objectWrapperFactory
      //而DefaultObjectWrapperFactory.hasWrapperFor()始终返回false,用户可以自定义ObjectWrapperFactory实现进行扩展
      this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);
    } else if (object instanceof Map) {
      //若原始对象为Map类型，则创建MapWrapper对象
      this.objectWrapper = new MapWrapper(this, (Map) object);
    } else if (object instanceof Collection) {
      //若原始对象是Collection类型，则创建CollectionWrapper对象
      this.objectWrapper = new CollectionWrapper(this, (Collection) object);
    } else {
      //若原始对象是Bean类型，则创建BeanWrapper对象
      this.objectWrapper = new BeanWrapper(this, object);
    }
  }

  //MetaObject的构造方法时private修饰的，只能通过forObject()这个静态方法创建MetaObject对象
  public static MetaObject forObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
    if (object == null) {
      return SystemMetaObject.NULL_META_OBJECT;
    } else {
      return new MetaObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
    }
  }

  public Object getValue(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);//解析属性表达式
    if (prop.hasNext()) {//处理子表达式
      //根据PropertyTokenizer解析后指定的属性，创建相应的MetaObject对象
      MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return null;
      } else {
        return metaValue.getValue(prop.getChildren());//递归处理子表达式
      }
    } else {
      return objectWrapper.get(prop);//通过ObjectWrapper获取指定的属性值
    }
  }
  public MetaObject metaObjectForProperty(String name) {
    Object value = getValue(name);//获取指定的属性
    //创建该属性对象相应的MetaObject对象
    return MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
  }
  public void setValue(String name, Object value) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        if (value == null && prop.getChildren() != null) {
          // don't instantiate child path if value is null
          return;
        } else {
          metaValue = objectWrapper.instantiatePropertyValue(name, prop, objectFactory);
        }
      }
      metaValue.setValue(prop.getChildren(), value);
    } else {
      objectWrapper.set(prop, value);
    }
  }
}
```
 ![MetaObject](../../../pic/MetaObject.jpg "MetaObject")