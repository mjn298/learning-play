package controllers

import controllers.Assets.Asset
import javax.inject._
import play.api.mvc._
import models.{CombinedData, Things}
import services.{SunService, WeatherService}
import java.util.concurrent.TimeUnit

import play.api.libs.json.Json
import actors.StatsActor
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject() (components: ControllerComponents,
                             assets: Assets, sunService: SunService,
                             weatherService: WeatherService, actorSystem: ActorSystem)
    extends AbstractController(components) {

  def index = Action{
    Ok(views.html.index())
  }

    def data = Action.async {
      val lat = Things.nyLat
      val lng = Things.nyLng
      val sunInfoF = sunService.getSunInfo(lat, lng)
      val temperatureF = weatherService.getTemperature(5128581)
      implicit val timeout = Timeout(5, TimeUnit.SECONDS)
      val requestsF = (actorSystem.actorSelection(StatsActor.path) ?
        StatsActor.GetStats).mapTo[Int]
      for {
        sunInfo <- sunInfoF
        temperature <- temperatureF
        requests <- requestsF
      } yield {
        Ok(Json.toJson(CombinedData(sunInfo, temperature, requests)))
    }

  }

  def versioned(path: String, file: Asset) = assets.versioned(path, file)
}
