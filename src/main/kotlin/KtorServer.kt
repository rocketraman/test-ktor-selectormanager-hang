import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*

suspend fun main() {
  val scope = CoroutineScope(Dispatchers.Default)
  // note, SelectorManager not in scope! adding it to the cancelled scope does?
  val selectorManager = SelectorManager(Dispatchers.IO)
  val serverSocket = aSocket(selectorManager).tcp().bind("127.0.0.1", 9500) {
    reuseAddress = true
    backlogSize = 5
  }
  println("Listening at ${serverSocket.localAddress}")

  val socketJob = scope.launch {
    while (isActive) {
      val socket = serverSocket.accept().also {
        println("Accepted connection from ${it.remoteAddress}")
      }
      try {
        socket.openReadChannel()
        val writeChannnel = socket.openWriteChannel()
        while(isActive) {
          writeChannnel.writeFully("Hello!".encodeToByteArray(), 0, 6)
          writeChannnel.flush()
          println("Write complete")
          delay(5_000)
        }
      } catch (e: Exception) {
        e.printStackTrace()
        if (e is CancellationException) {
          println("Closing client socket")
          socket.close()
        }
      }
    }
  }

  Runtime.getRuntime().addShutdownHook(Thread {
    // disposing the server socket before cancelling the scope fixes the issue
    //  but presumably the client should not rely on such correct behavior from the server
//    println("Closing server socket")
//    serverSocket.dispose()
    scope.cancel()
    Thread.sleep(200)
  })

  socketJob.join()
}
