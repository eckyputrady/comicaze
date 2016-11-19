package controllers

import javax.inject._

import play.api.mvc._
import services.{ComicRepo}
import scalatags.Text.all._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ComicsController @Inject()(comicRepo: ComicRepo) extends Controller {

  def index = Action {
    Ok(views.html.comics(comicRepo.getComics()))
  }
}
