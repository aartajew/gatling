/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.json

import java.lang.{ StringBuilder => JStringBuilder }
import java.util.{ Collection => JCollection, Map => JMap }

import scala.annotation.switch
import scala.collection.JavaConverters._

import io.gatling.commons.util.HexUtils
import io.gatling.commons.util.Maps._
import io.gatling.netty.util.ahc.StringBuilderPool
import io.gatling.commons.util.Spire._

object Json {

  private val stringBuilders = new StringBuilderPool

  def stringify(value: Any, isRootObject: Boolean = true): String = {

    val sb = stringBuilders.get()

    def appendStringified(value: Any, rootLevel: Boolean): JStringBuilder = value match {
      case b: Byte                   => sb.append(b)
      case s: Short                  => sb.append(s)
      case i: Int                    => sb.append(i)
      case l: Long                   => sb.append(l)
      case f: Float                  => sb.append(f)
      case d: Double                 => sb.append(d)
      case bool: Boolean             => sb.append(bool)
      case s: String                 => appendString(s, rootLevel)
      case null                      => sb.append("null")
      case map: collection.Map[_, _] => appendMap(map)
      case jMap: JMap[_, _]          => appendMap(jMap.asScala)
      case array: Array[_]           => appendArray(array)
      case seq: Seq[_]               => appendArray(seq)
      case coll: JCollection[_]      => appendArray(coll.asScala)
      case _                         => appendString(value.toString, rootLevel)
    }

    def appendString(s: String, rootLevel: Boolean): JStringBuilder =
      if (rootLevel) {
        appendString0(s)
      } else {
        sb.append('"')
        appendString0(s).append('"')
      }

    def appendString0(s: String): JStringBuilder = {
      cfor(0)(_ < s.length, _ + 1) { i =>
        val c = s.charAt(i)
        c match {
          case '"'  => sb.append("\\\"")
          case '\\' => sb.append("\\\\")
          case '/'  => sb.append("\\/")
          case '\b' => sb.append("\\b")
          case '\f' => sb.append("\\f")
          case '\n' => sb.append("\\n")
          case '\r' => sb.append("\\r")
          case '\t' => sb.append("\\t")
          case _ =>
            if (Character.isISOControl(c)) {
              sb.append("\\u")
              var n: Int = c
              cfor(0)(_ < 4, _ + 1) { _ =>
                val digit = (n & 0xf000) >> 12
                sb.append(HexUtils.toHexChar(digit))
                n <<= 4
              }
            } else {
              sb.append(c)
            }
        }
      }
      sb
    }

    def appendArray(iterable: Traversable[_]): JStringBuilder = {
      sb.append('[')
      iterable.foreach { elem =>
        appendStringified(elem, rootLevel = false).append(',')
      }
      if (iterable.nonEmpty) {
        sb.setLength(sb.length - 1)
      }
      sb.append(']')
    }

    def appendMap(map: collection.Map[_, _]): JStringBuilder = {
      sb.append('{')
      map.foreach {
        case (k, v) =>
          sb.append('"').append(k).append("\":")
          appendStringified(v, rootLevel = false).append(',')
      }
      if (map.nonEmpty) {
        sb.setLength(sb.length - 1)
      }
      sb.append('}')
    }

    appendStringified(value, isRootObject).toString
  }

  def asScala(value: Any): Any =
    value match {
      case list: JCollection[_] => list.asScala.map(asScala)
      case map: JMap[_, _] =>
        (map.size: @switch) match {
          case 0 => Map.empty
          case 1 =>
            val entry0 = map.entrySet.iterator.next()
            new Map.Map1(entry0.getKey, asScala(entry0.getValue))
          case 2 =>
            val it = map.entrySet.iterator
            val entry0 = it.next()
            val entry1 = it.next()
            new Map.Map2(
              entry0.getKey, asScala(entry0.getValue),
              entry1.getKey, asScala(entry1.getValue)
            )
          case 3 =>
            val it = map.entrySet.iterator
            val entry0 = it.next()
            val entry1 = it.next()
            val entry2 = it.next()
            new Map.Map3(
              entry0.getKey, asScala(entry0.getValue),
              entry1.getKey, asScala(entry1.getValue),
              entry2.getKey, asScala(entry2.getValue)
            )
          case 4 =>
            val it = map.entrySet.iterator
            val entry0 = it.next()
            val entry1 = it.next()
            val entry2 = it.next()
            val entry3 = it.next()
            new Map.Map4(
              entry0.getKey, asScala(entry0.getValue),
              entry1.getKey, asScala(entry1.getValue),
              entry2.getKey, asScala(entry2.getValue),
              entry3.getKey, asScala(entry3.getValue)
            )
          case _ =>
            map.asScala.toMap.forceMapValues(asScala)
        }

      case _ => value
    }
}
