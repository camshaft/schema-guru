/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.schemaguru

// Scala
import scala.language.implicitConversions

// json4s
import org.json4s._
import org.json4s.JsonDSL._

/**
 * Trait for output a warning messages
 */
// TODO: consider output errors in the same way
trait SchemaWarning {
  /**
   * Output message designed for other app (like Web UI)
   *
   * @return JSON representation of warning
   */
  def jsonMessage: JValue

  /**
   * Output message designed for CLI output
   *
   * @return plain string representation of warning
   */
  def consoleMessage: String
}

/**
 * Class container for duplicated pairs
 *
 * @param duplicates list of two-element lists (pairs)
 *                           containing all possible duplicates
 */
case class PossibleDuplicatesWarning(duplicates: Set[(String, String)]) extends SchemaWarning {
  implicit def pairsToJArray(pairs: Set[(String, String)]): JArray =
    JArray(pairs.toList.flatMap(p => List(JString(p._1), JString(p._2))))

  def jsonMessage =
    if (duplicates.isEmpty) { JNothing }
    else { ("message", "Possibly duplicated keys found") ~ ("items", duplicates) }

  def consoleMessage =
    if (duplicates.isEmpty) {
      ""
    } else {
      "Possibly duplicated keys found:\n" + (duplicates.map { case (a, b) => a + ": " + b }).toList.mkString("\n")
    }
}

