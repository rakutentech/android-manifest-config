package com.rakuten.tech.mobile.manifestconfig.annotations;

public @interface MetaData {
  String key() default "";
  String value() default "";
}
