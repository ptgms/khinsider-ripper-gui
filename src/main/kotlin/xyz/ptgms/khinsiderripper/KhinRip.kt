package xyz.ptgms.khinsiderripper

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.Alert
import javafx.scene.control.ChoiceDialog
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import tornadofx.App
import tornadofx.View
import xyz.ptgms.khinsiderripper.downloadClass.batchDownloadLinkGetter
import xyz.ptgms.khinsiderripper.downloadClass.downloadTrack
import xyz.ptgms.khinsiderripper.downloadClass.getRedirectUrl
import xyz.ptgms.khinsiderripper.progressBar.printProgress
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.system.exitProcess


object KhinRip {
    data class ReturnSearch(var textResults: ArrayList<String>, var linkResults: ArrayList<String>)
    data class ReturnTracks(var textResults: ArrayList<String>, var linkResults: ArrayList<String>, var available: String, var img: String, var mp3: Boolean, var flac: Boolean, var ogg: Boolean)

    @Volatile
    var busy = false

    private const val baseUrl = "https://downloads.khinsider.com/"
    private const val baseSearchUrl = "search?search="
    private const val baseSoundtrackAlbumUrl = "game-soundtracks/album/"

    fun getBusyState(): Boolean {
        return busy
    }

    fun setBusyState(setTo: Boolean) {
        busy = setTo
    }

    fun setDownloadDir(dirTo: String) {
        readConfig.setConfig("path", dirTo + "/")
    }

    fun getDownloadDir(): String {
        val downloadDir = readConfig.getConfig("path")
        File(downloadDir).mkdir()
        return downloadDir
    }

    fun getTrackList(albumName: String): ReturnTracks {
        var imgSrc = ""
        var flac = false
        var mp3 = false
        var ogg = false

        val tracklisturl = ArrayList<String>()
        val tracklist = ArrayList<String>()

        val tags: ArrayList<String> = ArrayList()

        val titlelength = ArrayList<String>()

        //albumName.text = albumNameInt
        try {
            val url = albumName
            val document: Document = Jsoup.connect(url).get()

            val img: Element = document.select("img").first()

            val imgSrc = img.absUrl("src")

            val echo: Element = document.getElementById("songlist")
            for (row in echo.select("tbody")) {
                for (col in row.select("tr")) {
                    for (colPre in col.select("tr")) {
                        if (colPre.id() == "songlist_header" || colPre.id() == "songlist_footer") {
                            for (tag in colPre.select("th")) {
                                tags.add(tag.text())
                            }
                            if (tags.contains("FLAC")) {
                                flac = tags.contains("FLAC")
                            }
                            if (tags.contains("MP3")){
                                mp3 = true
                            }
                            if (tags.contains("OGG")){
                                ogg = true
                            }
                        }
                        val temptag = ArrayList<String>()
                        val songname = tags.indexOf("Song Name")
                        for (titlename in colPre.select("td")) {
                            temptag.add(titlename.text())
                            val titleurl = titlename.select("a").attr("href")
                            if (titleurl != "" && !tracklisturl.contains(titleurl)) {
                                tracklisturl.add(titleurl)
                            }
                            if (temptag.size == tags.size + 1) {
                                titlelength.add(temptag[songname + 1])
                                tracklist.add(temptag[songname])
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            println("An error occured while trying to retrieve this albums information! Does it even exist?\n" +
                    "Are you using the properly formatted album name from --search? Error: ${e.localizedMessage}")
            exitProcess(-1)
        }

        var avtext = "Available Formats: "
        if (mp3) {
            avtext += "MP3 "
        }
        if (flac) {
            avtext += "FLAC "
        }
        if (ogg) {
            avtext += "OGG "
        }
        return ReturnTracks(tracklist, tracklisturl, avtext, imgSrc, mp3, flac, ogg)
    }


    fun getSearch(searchterm: String): ReturnSearch {
        val linkArray = ArrayList<String>()
        val textArray = ArrayList<String>()
        val encodedSearch = java.net.URLEncoder.encode(searchterm, "utf-8")
        val url = baseUrl + baseSearchUrl + encodedSearch
        println("Read url: $url")
        val document: Document = Jsoup.connect(url).get()
        val echo: Element = document.getElementById("EchoTopic")
        for (row in echo.select("p")) {
            for (col in row.select("a")) {
                val colContent = col.text()
                val colHref = col.attr("href")
                if (colHref.contains("game-soundtracks/browse/") or colHref.contains("/forums/")) {
                    continue
                }
                if (colContent == "Windows" && colHref == "/game-soundtracks/windows") {
                    val getRedir = getRedirectUrl(url)
                    getRedir?.replace(baseUrl + baseSoundtrackAlbumUrl, "")?.replace("-", " ")?.let { textArray.add(it) }
                    getRedir?.let { linkArray.add(it) }
                    continue
                }

                textArray.add(colContent)
                linkArray.add(colHref)
            }
        }
        return ReturnSearch(textArray, linkArray)
    }

    fun doDownloadWrapper(albumName: String, type: String = "mp3") {
        val file = File(getDownloadDir() + albumName.replace(baseUrl, "").replace(baseSoundtrackAlbumUrl, ""))
        file.mkdir()
        val (textArray, linkArray, _, _) = getTrackList(albumName)
        val downloadLinks = batchDownloadLinkGetter(linkArray, type)
        if (downloadLinks.size != linkArray.size) {
            AlertThread.showAlert("An error occured during the download! Start this program with a console window and send the logs to a GitHub issue!")
            return
        }
        val startTime = System.currentTimeMillis()
        for (i in 0 until downloadLinks.size) {
            printProgress(startTime, downloadLinks.size.toLong(), (i + 1).toLong())
            downloadTrack(downloadLinks[i], textArray[i], albumName.replace(baseUrl, "").replace(baseSoundtrackAlbumUrl, ""), type)
        }
        AlertThread.showAlert("Download finished!")
        busy = false

    }

    fun oneDownloadWrapper(trackLink: String, trackName: String, albumName: String, type: String = "mp3") {
        val file = File(getDownloadDir() + albumName.replace(baseUrl, "").replace(baseSoundtrackAlbumUrl, ""))
        file.mkdir()
        val linkArray = arrayListOf<String>(trackLink)
        val textArray = arrayListOf<String>(trackName)
        val downloadLinks = batchDownloadLinkGetter(linkArray, type)
        for (i in 0 until downloadLinks.size) {
            downloadTrack(downloadLinks[i], textArray[i], albumName.replace(baseUrl, "").replace(baseSoundtrackAlbumUrl, ""), type)
        }
        AlertThread.showAlert("Download finished!")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        MyApp()
    }
}

class layout : View() {
    var albumNameList = ArrayList<String>()
    var albumLinkList = ArrayList<String>()
    var trackNameList = ArrayList<String>()
    var trackLinkList = ArrayList<String>()

    override val root : VBox by fxml("/layout.fxml")

    val searchBox : TextField by fxid()
    //var albumList : ListView<*>? by fxid()
    //var TrackList : ListView<T> by fxid()

    //@FXML
    private val albumList: ListView<*>? by fxid()
    private val trackList: ListView<*>? by fxid()
    var albumName = ""
    var albumLink = ""
    var trackName = ""
    var trackLink = ""
    var availableFormats = ""
    var mp3av = false
    var flacav = false
    var oggav = false

    fun quitPressed() {
        System.exit(0)
    }

    fun aboutPressed() {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI("https://www.github.com/ptgms/khinsider-ripper-gui"))
        }
    }

    fun issuePressed() {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI("https://www.github.com/ptgms/khinsider-ripper-gui/issues"))
        }
    }

    fun searchPressed() {
        albumList?.items?.clear()
        val (textArray, linkArray) = KhinRip.getSearch(searchBox.getText())
        val items: ObservableList<String> = FXCollections.observableArrayList(textArray)
        albumList?.items = items
        albumNameList = textArray
        albumLinkList = linkArray
    }

    fun searchEnter() {
        albumList?.items?.clear()
        val (textArray, linkArray) = KhinRip.getSearch(searchBox.getText())
        val items: ObservableList<String> = FXCollections.observableArrayList(textArray)
        albumList?.items = items
        albumNameList = textArray
        albumLinkList = linkArray
    }

    fun downloadAlbum() {
        if (KhinRip.getBusyState()) {
            AlertThread.showAlert("Theres currently something downloading already, please wait!")
            return
        }

        if (albumName == "" || albumLink == "") {
            return
        }

        val dialogData = ArrayList<String>()
        if (mp3av) {
            dialogData.add("MP3")
        }
        if (flacav) {
            dialogData.add("FLAC")
        }
        if (oggav) {
            dialogData.add("OGG")
        }

        val dialog = ChoiceDialog(dialogData.get(0), dialogData)
        dialog.title = "Waiting..."
        dialog.setHeaderText("Select what format to download")

        val result: Optional<String>? = dialog.showAndWait()
        var selected = "null"

        if (result != null) {
            if (result.isPresent) {
                selected = result.get()
            }
        }

        when(selected) {
            "MP3", "FLAC", "OGG" -> {
                KhinRip.setBusyState(true);
                thread {
                    KhinRip.doDownloadWrapper(albumLink, selected.toLowerCase())
                }
            }
            else -> {
                return
            }
        }

    }

    fun downloadTrack() {
        if (KhinRip.getBusyState()) {
            AlertThread.showAlert("Theres currently something downloading already, please wait!")
            return
        }

        if (trackName == "" || trackLink == "") {
            return
        }
        val dialogData = ArrayList<String>()
        if (mp3av) {
            dialogData.add("MP3")
        }
        if (flacav) {
            dialogData.add("FLAC")
        }
        if (oggav) {
            dialogData.add("OGG")
        }

        val dialog = ChoiceDialog(dialogData.get(0), dialogData)
        dialog.title = "Waiting..."
        dialog.setHeaderText("Select what format to download")

        val result: Optional<String>? = dialog!!.showAndWait()
        var selected = "null"

        if (result != null) {
            if (result.isPresent) {
                selected = result.get()
            }
        }

        when(selected) {
            "MP3", "FLAC", "OGG" -> {
                KhinRip.oneDownloadWrapper(trackLink, trackName, "singletrack", selected.toLowerCase())
            }
        }
    }

    fun openDownloads() {
        val file: File = File(KhinRip.getDownloadDir())
        Desktop.getDesktop().open(file)
    }

    fun changeDownloads() {
        try {
            val directoryChooser = DirectoryChooser()
            val selectedDirectory: File = directoryChooser.showDialog(primaryStage)

            AlertThread.showAlert("Set your Download path to ${selectedDirectory.absolutePath}.")
            KhinRip.setDownloadDir(selectedDirectory.absolutePath)
        } catch (e: Exception) {}
    }

    fun albumListClick() {
        var selected = albumList?.selectionModel?.selectedIndex
        if (selected == -1) {
            return
        }
        trackName = ""
        val (textArray, linkArray, available, imgLink, mp3, flac, ogg) = KhinRip.getTrackList(albumLinkList[selected!!])
        trackList?.items?.clear()
        val items: ObservableList<String> = FXCollections.observableArrayList(textArray)
        trackList?.items = items
        trackNameList = textArray
        trackLinkList = linkArray
        albumName = albumNameList[selected]
        albumLink = albumLinkList[selected]
        availableFormats = available
        mp3av = mp3
        flacav = flac
        oggav = ogg
    }

    fun trackSelect() {
        var selected = trackList?.selectionModel?.selectedIndex
        if (selected == -1) {
            return
        }
        trackName = trackNameList[selected!!]
        trackLink = trackLinkList[selected]
        //print(trackLinkList[selected])
    }


}

object AlertThread {
    public fun showAlert(alert: String) {
        Platform.runLater {
            val alertReal = Alert(Alert.AlertType.INFORMATION, alert)
            alertReal.show()
        }
    }
}


class MyApp: App() {
    override val primaryView = layout::class

    init {
        val mainDir = File(System.getProperty("user.home") + "/khinsider-ripper")
        mainDir.mkdir()
    }
}