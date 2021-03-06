/*
 * Copyright 2017 LinkedIn Corp. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.linkedin.photon.ml.util

import org.testng.Assert.{assertEquals, assertFalse, assertTrue}
import org.testng.annotations.Test

/**
 * Unit tests for [[ClassUtils]].
 */
class ClassUtilsTest {

  /**
   * Helper class for the tests below.
   */
  private class Base {}

  @Test
  def testIsAnonClass(): Unit = {
    assertTrue(ClassUtils.isAnonClass(new Base {}.getClass))
    assertFalse(ClassUtils.isAnonClass((new Base).getClass))
  }

  @Test
  def testGetTrueClass(): Unit = {
    assertEquals(ClassUtils.getTrueClass(new Base), (new Base).getClass)
    assertEquals(ClassUtils.getTrueClass(new Base {}), (new Base).getClass)
    assertEquals(ClassUtils.getTrueClass(new Base {}), ClassUtils.getTrueClass(new Base))
  }
}
