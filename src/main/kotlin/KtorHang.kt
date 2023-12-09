import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException

suspend fun main() {
  val connectionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
  while (true) {
    lateinit var sm: SelectorManager

    val tcpConnectionJob = connectionScope.launch(CoroutineName("Conn")) {
      // Removing coroutineContext from the current scope appears to work around the problem by avoiding
      // the hang, but it is not a complete fix for the problem because the selector *leaks* every time
      // the server is stopped. Maybe there should be a NonCancellable block somewhere in the SelectorManager
      // implementation?
      sm = SelectorManager(Dispatchers.IO + coroutineContext)

      println("Connecting to socket")
      val socket = try {
        aSocket(sm).tcp().connect("localhost", 9500)
      } catch (e: Throwable) {
        e.printStackTrace()
        sm.close()
        delay(1_000)
        null
      }

      if (socket == null) {
        println("Connection failed, retrying in 500ms...")
        delay(500)
        return@launch
      }

      val reader = launch(CoroutineName("Reader")) {
        try {
          val readChannel = socket.openReadChannel()
          while (isActive) {
            println("Waiting for read")
            val messageBytes = ByteArray(6)
            readChannel.readFully(messageBytes)
            println("Read: ${messageBytes.decodeToString()}")
          }
        } catch (e: ClosedReceiveChannelException) {
          println("Server closed socket")

          println("Closing TCP connection")
          cancel()
          socket.dispose()
          sm.close()
          println("TCP connection closed")
        }
        println("Reader done")
      }

      reader.join()
    }

    tcpConnectionJob.join()
    delay(60)
  }
}
