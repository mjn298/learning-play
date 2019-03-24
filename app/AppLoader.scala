import CustomFilters.StatsFilter
import controllers.Application
import play.api.ApplicationLoader.Context
import play.api._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc._
import router.Routes
import play.api.routing.Router
import com.softwaremill.macwire._
import _root_.controllers.AssetsComponents
import actors.StatsActor
import actors.StatsActor.Ping
import akka.actor.Props
import play.api.db.{DBComponents, HikariCPComponents}
import play.api.db.evolutions.{DynamicEvolutions, EvolutionsComponents}
import play.components.ApplicationComponents
import play.filters.HttpFiltersComponents
import services.{SunService, WeatherService}
import scalikejdbc.config.DBs

import scala.concurrent.Future

class AppApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach { cfg =>
      cfg.configure(context.environment)
    }
    new AppComponents(context).application
  }
}

class AppComponents(context: Context) extends
  BuiltInComponentsFromContext(context) with AhcWSComponents
  with EvolutionsComponents with DBComponents
  with HikariCPComponents with AssetsComponents {
  override lazy val controllerComponents = wire[DefaultControllerComponents]
  lazy val prefix: String = "/"
  lazy val router: Router = wire[Routes]
  lazy val applicationController = wire[Application]
  lazy val sunService = wire[SunService]
  lazy val weatherService = wire[WeatherService]
  lazy val statsFilter: Filter = wire[StatsFilter]
  override lazy val httpFilters = Seq(statsFilter)
  override lazy val dynamicEvolutions = new DynamicEvolutions
  lazy val statsActor = actorSystem.actorOf(
    Props(wire[StatsActor]), StatsActor.name
  )
  private val log = Logger(this.getClass)
  applicationLifecycle.addStopHook{ () =>
    log.info("The app is about to stop")
    DBs.closeAll()
    Future.successful(Unit)
  }
  val onStart = {
    log.info("the app is starting...")
    applicationEvolutions
    DBs.setupAll()
    statsActor ! Ping
  }
}

