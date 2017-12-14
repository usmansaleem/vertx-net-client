package info.usmans.blog.vertx

import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.net.NetClient
import io.vertx.core.net.NetClientOptions
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    //hack for windows - netty cause dns resolver error
    if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
        System.setProperty("vertx.disableDnsResolver", "true")
    }

    //quick way to run the verticle in IDE.
    Vertx.vertx().deployVerticle(NetClientVerticle())
    println("Running NetClientVerticle!")
}

/**
 * A Vertx Verticle to connect to our custom server
 */
class NetClientVerticle : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger("info.usmans.blog.vertx.NetClientVerticle")
    private val serverHost = System.getProperty("serverHost", "127.0.0.1")
    private val serverPort = System.getProperty("serverPort", "8888").toIntOrNull() ?: 8888
    private val connectMessage = System.getProperty("connectMessage", "hello")

    override fun start() {
        val options = NetClientOptions().apply {
            isSsl = true //required if server is using SSL Socket as well.
            connectTimeout = 10000
        }
        val client = vertx.createNetClient(options)

        fireReconnectTimer(client)
    }

    //wait for 5 seconds before attempting to connect
    private fun fireReconnectTimer(client: NetClient) {
        vertx.setTimer(5000, {
            reconnect(client)
        })
    }

    private fun reconnect(client: NetClient) {
        logger.info("Connecting to $serverHost:$serverPort")

        client.connect(serverPort, serverHost, { event ->
            if (event.succeeded()) {
                logger.info("Connected")
                val socket = event.result()
                //send pass phrase ...
                socket.write(connectMessage)

                socket.handler({ data ->
                    logger.info("Data received: ${data}")
                    //TODO: Do the work here ...
                })

                socket.closeHandler({
                    logger.info("Socket closed")
                    fireReconnectTimer(client)
                })
            } else {
                logger.info("Connection attempt failed. ${event.cause().message}")
                fireReconnectTimer(client)
            }
        })
    }


}