/*
 * Copyright 2019 Timothy Logan
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.timmahh.ksoup

import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty0

inline infix fun <reified R> R.assignTo(property: KMutableProperty0<R>) = { property.set(this); property.get() }()
inline infix fun <reified R> KMutableProperty0<R>.set(value: R) = { this.set(value); get() }()
inline infix fun <reified R> KFunction<R>.set(value: R) = { this.call(value); value }()