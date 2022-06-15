package util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.nio.file.Paths
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.reflect.full.memberProperties

class Config {
    var spotifyClientId = ""
    var spotifyClientSecret = ""

    companion object {
        private val GSON = GsonBuilder().setPrettyPrinting().create()

        fun get(): Config {
            val configFile = Paths.get("config.json")

            if (!configFile.exists()) {
                val settings = Config()
                val json = GSON.toJson(settings)

                configFile.createFile()
                configFile.writeText(json)

                invalidConfigurationData()
            }

            val dataStr = configFile.readText()
            val data = GSON.fromJson(dataStr, Config::class.java)

            for (property in Config::class.memberProperties) {
                val propertyValue = property.get(data)

                if (propertyValue.toString().isBlank()) {
                    invalidConfigurationData()
                }
            }

            return data
        }

        private fun invalidConfigurationData() {
//            runLater {
//                val alertDialog = Alert(Alert.AlertType.ERROR)
//                alertDialog.title = "Fatal Error"
//                alertDialog.contentText = "Invalid 'config.json' values."
//                alertDialog.showAndWait()
//                alertDialog.setOnCloseRequest {
//                    exitProcess(-1)
//                }
//            }

            while (true) {
                Thread.sleep(1000)
            }
        }
    }
}