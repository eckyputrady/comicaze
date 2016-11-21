package controllers

import java.net.URLEncoder
import javax.inject._

import play.api.mvc._
import services.{Comic, ComicRepo}
import scalatags.Text.all._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ComicsController @Inject()(comicRepo: ComicRepo) extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.comics(comicRepo.getComics().map(ComicWithShare(_))))
  }

  def show(slug: String) = {
    comicRepo.findOneWithSlug(slug) match {
      case Some(comic) => showComic(comic)
      case None => Action { Redirect("/") }
    }
  }

  def showComic(comic: Comic) = Action { implicit request =>
    val title = s"${comic.artist} - ${comic.title}"
    val next = comicRepo.findNextComic(comic,  1).map(c => routes.ComicsController.show(c.slug)).getOrElse(routes.ComicsController.index())
    val prev = comicRepo.findNextComic(comic, -1).map(c => routes.ComicsController.show(c.slug)).getOrElse(routes.ComicsController.index())
    Ok(views.html.comic(ComicWithShare(comic), title, next, prev))
  }
}

case class ComicWithShare(comic: Comic)(implicit request: Request[AnyContent]) {
  val facebookSharer: String = {
    val url = URLEncoder.encode(routes.ComicsController.show(comic.slug).absoluteURL(), "UTF-8")
    s"https://www.facebook.com/sharer/sharer.php?u=$url"
  }

  val twitterSharer: String = {
    val text = URLEncoder.encode(s"${comic.artist} - ${comic.title}", "UTF-8")
    val url = URLEncoder.encode(routes.ComicsController.show(comic.slug).absoluteURL(), "UTF-8")
    s"https://twitter.com/intent/tweet?text=$text&url=$url"
  }
}
