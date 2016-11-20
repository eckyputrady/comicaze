package services

import javax.inject.Singleton

import akka.actor.ActorSystem
import com.google.inject.Inject
import org.joda.time.DateTime
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.model.Element

import scala.util.{Failure, Success}


/**
  * Created by eckyputrady on 11/6/16.
  */
trait ComicRepo {
  def getComics(): Seq[Comic]
}

case class Comic(artist: String, title: String, date: DateTime, imgUrl: String, originalUrl: String)

@Singleton
class Comics @Inject()(system: ActorSystem)(implicit executor: ExecutionContext) extends ComicRepo {
  private var list: Seq[Comic] = List()

//  system.scheduler.schedule(0.seconds, 1.hour)(updateList)

  override def getComics(): Seq[Comic] = list

  // Dilbert

  private def getDilbertComicDates(): Seq[DateTime] = {
    val now = DateTime.now()
    (0 to 30).map(now.minusDays(_))
  }

  private def scrapDilbertComicAtDate(dateTime: DateTime): Future[Comic] = Future {
    val url = s"http://dilbert.com/strip/${dateTime.toString("yyyy-MM-dd")}"
    val browser = JsoupBrowser()
    val doc = browser.get(url)
    Comic(
      "Dilbert",
      doc >> text(".comic-title-name"),
      dateTime,
      doc >> attr("src")(".img-comic"),
      url
    )
  }

  private def getDilbertComics(): Future[Seq[Comic]] =
    Future.sequence(getDilbertComicDates().map(scrapDilbertComicAtDate))

  // XKCD

  def getXkcdUpdates(): Future[Seq[Comic]] = Future {
    // Get comic ids
    val doc = JsoupBrowser().get("http://xkcd.com/archive/")
    val items: List[Element] = doc >> elementList("#middleContainer > a")
    val dateUrlList: List[(DateTime, String)] = items.map(el => (DateTime.parse(el.attr("title")), el.attr("href")))

    // get each of them
    val comics = dateUrlList.take(30).map(x => scrapXkcdComic(x._1, x._2))

    // parse
    Future.sequence(comics)
  }.flatMap(x => x)

  private def scrapXkcdComic(date: DateTime, urlPart: String): Future[Comic] = Future {
    val url = s"http://xkcd.com$urlPart"
    val doc = JsoupBrowser().get(url)
    Comic(
      "XKCD",
      doc >> text("#ctitle"),
      date,
      "http:" + (doc >> attr("src")("#comic > img")),
      url
    )
  }

  // CommitStrip

  private def scrapCommitStripComic(link: String): Future[Comic] = Future {
    val browser = JsoupBrowser()
    val doc = browser.get(link)
    val segments = link.split("/")
    val year = segments(4)
    val month = segments(5)
    val date = segments(6)
    val ymd = year + "-" + month + "-" + date
    Comic(
      "CommitStrip",
      doc >> text(".entry-title"),
      DateTime.parse(ymd),
      doc >> attr("src")(".entry-content img"),
      link
    )
  }

  private def getCommitStripLinks(): Future[Iterable[String]] = Future {
    val browser = JsoupBrowser()
    val p1 = browser.get("http://www.commitstrip.com/en/?")
    val links1 = p1 >> attrs("href")(".excerpt a")
    val p2 = browser.get("http://www.commitstrip.com/en/page/2/")
    val links2 = p2 >> attrs("href")(".excerpt a")
    links1.toSeq ++ links2.toSeq
  }

  private def getCommitStripUpdates(): Future[Seq[Comic]] = {
    val links: Future[Iterable[String]] = getCommitStripLinks()
    links.flatMap(f => Future.sequence(f.map(scrapCommitStripComic).toSeq))
  }

  //// All

  def getUpdates(): Future[Seq[Comic]] = {
    val allComics = Future.sequence(List(getXkcdUpdates(), getCommitStripUpdates(), getDilbertComics())).map(_.flatten)
    allComics.map(_.sortBy(_.date)(JodaOrdering.descendingOrdering))
  }

  def updateList():Unit =
    getUpdates() onComplete {
      case Success(comics) => this.list = comics
      case Failure(e) => Logger.warn("Failed to update comics!", e)
    }
}

object JodaOrdering {
  def descendingOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isAfter _)
}