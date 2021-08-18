package com.bloom.book;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

public class MetaObjectTest {

    public static void main(String[] args) {
        RichType rich = new RichType();
        MetaObject meta = SystemMetaObject.forObject(rich);
        meta.setValue("richType.richMap.key", "foo");
        System.out.println(meta.getValue("richType.richMap.key"));
    }
}
