package xyz.ptgms.khinsiderripper

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import xyz.ptgms.khinsiderripper.progressBar.printProgress
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


object downloadClass {
    private const val baseUrl = "https://downloads.khinsider.com"

    fun batchDownloadLinkGetter(links: ArrayList<String>, type: String = "mp3") : ArrayList<String> {
        val downloadQueue = ArrayList<String>()
        val total: Long = links.size.toLong()
        val startTime = System.currentTimeMillis()
        var i = 1
        for (link in links) {
            printProgress(startTime, total, i.toLong())
            val url = baseUrl + link
            val document: Document = Jsoup.connect(url).get()
            val echo: Element = document.getElementById("EchoTopic")

            for (linkTry in echo.select("a")) {
                val urlPrev = linkTry.attr("href")
                if (urlPrev.endsWith(".$type")) {
                    //print(url_prev)
                    downloadQueue.add(urlPrev)
                    continue
                }
            }
            i++
        }
        return downloadQueue
    }

    fun downloadTrack(downloadLink: String, name: String, AlbumName: String, type: String = "mp3") {
        try {
            BufferedInputStream(URL(downloadLink).openStream()).use { `in` ->
                FileOutputStream("${KhinRip.getDownloadDir()}/$AlbumName/$name.$type").use { fileOutputStream ->
                    val dataBuffer = ByteArray(1024)
                    var bytesRead: Int
                    while (`in`.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead)
                    }
                }
            }
        } catch (e: IOException) {
            println("An error occurred while trying to save $downloadLink")
            e.printStackTrace()
        }
    }

    fun getRedirectUrl(url: String?): String? {
        var urlTmp: URL? = null
        var redUrl: String? = null
        var connection: HttpURLConnection? = null
        try {
            urlTmp = URL(url)
        } catch (e1: MalformedURLException) {
            e1.printStackTrace()
        }
        try {
            connection = urlTmp!!.openConnection() as HttpURLConnection
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            connection?.getResponseCode()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        redUrl = connection?.getURL().toString()
        connection?.disconnect()
        return redUrl
    }
}