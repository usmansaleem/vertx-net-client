package info.usmans.blog.vertx

import com.pi4j.io.gpio.*
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

    private val isPi = "arm".equals( System.getProperty("os.arch"), true)
    private val gpio: GpioController?
    private val pin0Out: GpioPinDigitalOutput?

    init {
        if(isPi) {
            gpio = GpioFactory.getInstance()
            pin0Out = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, // PIN NUMBER
                    "GPIO_OO", // PIN FRIENDLY NAME (optional)
                    PinState.LOW)      // PIN STARTUP STATE (optional)
            //pin shutdown behavior ...
            pin0Out.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF)
        } else {
            gpio = null
            pin0Out = null
        }
    }

    override fun start() {
        logger.info("Pi: ${isPi}" )
        logger.info("Server Host/Port: ${serverHost}:${serverPort}")
        logger.info("Password: ${connectMessage}")

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

                    sendSignalToPin0()
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

    private fun sendSignalToPin0() {
        pin0Out?.pulse(5000)
    }

}