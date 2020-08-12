package xyz.ptgms.khinsiderripper

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*


object readConfig {
    fun loadConfig(): Properties {
        if (File(System.getProperty("user.home") + "/khinsider-ripper/config.properties").exists()) {
            FileInputStream(System.getProperty("user.home") + "/khinsider-ripper/config.properties").use { input ->
                val prop = Properties()
                prop.load(input)
                return prop
            }
        } else {
            //AlertThread.showAlert("Config not found, trying to revert to downloads/ but this may not work! Set it by going to Downloads/Change Download location")
            val prop = Properties()
            prop.setProperty("path", System.getProperty("user.home") + "/khinsider-ripper/")
            return prop
        }
    }

    fun getConfig(key: String): String {
        val prop = loadConfig()
        return prop.getProperty(key)
    }

    fun setConfig(key: String, setTo: String) {
        val prop = loadConfig()
        val fos = FileOutputStream(System.getProperty("user.home") + "/khinsider-ripper/config.properties")
        prop.setProperty(key, setTo)
        prop.store(fos, "")
        fos.close()
    }
}