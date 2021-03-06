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
package utils

// scalaz
import scalaz._
import Scalaz._

// jsonpath
import com.fasterxml.jackson.databind.ObjectMapper
import io.gatling.jsonpath.JsonPath

// json4s
import org.json4s.jackson.JsonMethods.compact

object JsonPathExtractor {

  val mapper = new ObjectMapper

  /**
   * Parse JSON string as raw JVM object to work with jackson
   *
   * @param json string containing JSON
   * @return raw JVM object representing JSON
   */
  def parseJson(json: String): Object =
    mapper.readValue(json, classOf[Object])

  /**
   * Add default values for some exceptional cases and
   * remove all special characters from the string also
   * slice it to 30 chars, so it can be used as file name
   *
   * @param content some optional content to convert
   * @return sliced string without special characters
   */
  def convertToKey(content: Option[Any]): String = content match {
    case Some(str) =>
      val key = try {
        str.toString
      } catch {
        case _: NullPointerException => "unmatched"
      }
      if (key.trim.length == 0) "unmatched"
      else key.slice(0, 30).replaceAll("[^a-zA-Z0-9.-]", "_")
    case None => "unmatched"
  }
  

  /**
   * Maps content of specified JSON Path to List of JSONs which contains same
   * content
   *
   * @param jsonPath valid JSON Path
   * @param jsonList the validated JSON list
   * @return Map from content to list of JValues
   */
  def mapByPath(jsonPath: String, jsonList: ValidJsonList): Map[String, ValidJsonList] = {
    val schemaToJsons = for {
      Success(json) <- jsonList
    } yield {
        val jsonObject = parseJson(compact(json)) // TODO: Can we avoid transforming to String?
        JsonPath.query(jsonPath, jsonObject) match {
          case Right(iter) => {
            val content = iter.toList.headOption
            val key = convertToKey(content)
            Map(key -> List(json.success))
          }
          case Left(_) => Map.empty[String, ValidJsonList]
        }
      }

    val failedJsons = for {
      Failure(err) <- jsonList
    } yield Map("$SchemaGuruFailed" -> List(err.failure))

    (failedJsons ++ schemaToJsons).reduce(_ |+| _)
  }
}
