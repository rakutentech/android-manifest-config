/*
 * Copyright 2018 Rakuten Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rakuten.tech.mobile.manifestconfig;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import java.lang.Boolean;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;

public final class CustomValuesManifestConfig implements CustomValues {
  private Bundle metaData;

  public CustomValuesManifestConfig(Context context) {
    this.metaData = new Bundle();
    try {
      PackageManager pm = context.getPackageManager();
      Bundle appMeta = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
      if (appMeta != null) {
        this.metaData = appMeta;
      }
    } catch (PackageManager.NameNotFoundException ignored) {
      // if we can't get metadata we'll use default config
    }
  }

  @Override
  public int rawInt() {
    return metaData.getInt("RawInt", 2);
  }

  @Override
  public Integer boxedInt() {
    return metaData.getInt("BoxedInt", 2);
  }

  @Override
  public boolean rawBoolean() {
    return metaData.getBoolean("RawBoolean", true);
  }

  @Override
  public Boolean boxedBoolean() {
    return metaData.getBoolean("BoxedBoolean", true);
  }

  @Override
  public String string() {
    return metaData.getString("String", "hello");
  }

  @Override
  public float rawFloat() {
    return metaData.getFloat("RawFloat", 2.5f);
  }

  @Override
  public Float boxedFloat() {
    return metaData.getFloat("BoxedFloat", 2.5f);
  }
}
