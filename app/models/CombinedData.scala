package models

import play.api.libs.json.Json

case class CombinedData(sunInfo: SunInfo, temperature: Double, requests: Int)

object CombinedData {
  implicit val writes = Json.writes[CombinedData]
}

