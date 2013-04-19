package scalaguide.http.scalasessionflash {

  import play.api.mvc._
  import play.api.test._
  import play.api.test.Helpers._
  import org.specs2.mutable.Specification
  import play.api.libs.json._
  import play.api.libs.iteratee.Enumerator
  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner
  import play.api.http.HeaderNames

  @RunWith(classOf[JUnitRunner])
  class ScalaSessionFlashSpec extends Specification with Controller {
    import scalaguide.http.scalasessionflash.full._
    "A scala SessionFlash" should {

      "Reading a Session value" in {
        val index = Application.index
        assertAction(index, OK, FakeRequest().withSession("connected" -> "player"))(res => contentAsString(res) must contain("player"))
      }

      "Reading a Session value implicitly" in {
        val index = Application2.index
        assertAction(index, OK, FakeRequest().withSession("connected" -> "player"))(res => contentAsString(res) must contain("player"))
      }

      "Storing data in the Session" in {
        val storeSession = StoringSessionApplication.storeSession
        assertAction(storeSession, OK, FakeRequest())(res => testSession(res.asInstanceOf[PlainResult], "connected", Some("user@gmail.com")))
      }

      "add data in the Session" in {
        val addSession = StoringSessionApplication.addSession
        assertAction(addSession, OK, FakeRequest())(res => testSession(res.asInstanceOf[PlainResult], "saidHello", Some("yes")))
      }

      "remove data in the Session" in {
        val removeSession = StoringSessionApplication.removeSession
        assertAction(removeSession, OK, FakeRequest().withSession("theme" -> "blue"))(res => testSession(res.asInstanceOf[PlainResult], "theme", None))
      }

      "Discarding the whole session" in {
        val discardingSession = StoringSessionApplication.discardingSession
        assertAction(discardingSession, OK, FakeRequest().withSession("theme" -> "blue"))(res => testSession(res.asInstanceOf[PlainResult], "theme", None))
      }

      "get from flash" in {
        val index = FlashApplication.index
        assertAction(index, OK, FakeRequest().withFlash("success" -> "success!"))(res => contentAsString(res) must contain("success!"))
      }

      "add message to flash" in {
        val save = FlashApplication.save
        assertAction(save, SEE_OTHER, FakeRequest())(res => testFlash(res.asInstanceOf[PlainResult], "success", Some("The item has been created")))
      }

      "fix could not find implicit value for parameter flash" in {
        val index = FixFlashApplication.index
        assertAction(index, OK, FakeRequest())(res => contentAsString(res) must contain("Good"))
      }

    }

    def testFlash(results: PlainResult, key: String, value: Option[String]) = {
      val flash = Helpers.flash(results)
      flash.get(key) === value
    }

    def testSession(results: PlainResult, key: String, value: Option[String]) = {
      val session = Helpers.session(results)
      session.get(key) === value
    }

    def assertAction[A](action: Action[A], expectedResponse: Int = OK, request: => Request[A] = FakeRequest())(assertions: Result => Unit) {
      val fakeApp = FakeApplication(additionalConfiguration = Map("application.secret" -> "pass"))
      running(fakeApp) {
        val result = action(request)
        status(result) must_== expectedResponse
        assertions(result)
      }
    }
  }

  package scalaguide.http.scalasessionflash.full {

    object Application extends Controller {
      //#index-retrieve-incoming-session
      def index = Action { request =>
        request.session.get("connected").map { user =>
          Ok("Hello " + user)
        }.getOrElse {
          Unauthorized("Oops, you are not connected")
        }
      }
      //#index-retrieve-incoming-session
    }

    object Application2 extends Controller {
      //#index-retrieve-incoming-session-implicitly
      def index = Action { implicit request =>
        session.get("connected").map { user =>
          Ok("Hello " + user)
        }.getOrElse {
          Unauthorized("Oops, you are not connected")
        }
      }
      //#index-retrieve-incoming-session-implicitly
    }

    object StoringSessionApplication extends Controller {

      def storeSession = Action { implicit request =>
        //#store-session
        Ok("Welcome!").withSession(
          "connected" -> "user@gmail.com")
        //#store-session
      }

      def addSession = Action { implicit request =>
        //#add-session
        Ok("Hello World!").withSession(
          session + ("saidHello" -> "yes"))
        //#add-session
      }

      def removeSession = Action { implicit request =>
        //#remove-session
        Ok("Theme reset!").withSession(
          session - "theme")
        //#remove-session
      }

      def discardingSession = Action { implicit request =>
        //#discarding-session
        Ok("Bye").withNewSession
        //#discarding-session
      }
    }

    object FlashApplication extends Controller {
      //#using-flash
      def index = Action { implicit request =>
        Ok {
          flash.get("success").getOrElse("Welcome!")
        }
      }

      def save = Action {
        Redirect("/home").flashing(
          "success" -> "The item has been created")
      }
      //#using-flash
    }

    object FixFlashApplication extends Controller {
      //#find-noflash
      def index() = Action {
        implicit request =>
          Ok(views.html.Application.index())
      }
      //#find-noflash
    }

  }
}

// Faking a form view
package views.html {
  object Application {
    def index() = "Good Job!"
  }
}



 