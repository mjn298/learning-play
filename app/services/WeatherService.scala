package services

import models.Things
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class WeatherService(wsClient: WSClient) {
  def getTemperature(cityId: Int): Future[Double] = {
    val weatherResponseF = wsClient.url(s"http://api.openweathermap.org/data/2.5/weather?id=$cityId&units=metric&appid=${Things.weatherKey}").get()
    weatherResponseF.map { wr =>
      val weatherJson = wr.json
      val temperature = (weatherJson \ "main" \ "temp").as[Double]
      temperature
    }
  }
}
