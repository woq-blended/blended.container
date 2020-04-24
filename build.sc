import mill._
import mill.scalalib._

object blended extends Module {
  object features extends Module {
  
    def foo = {
      println("foo")
    }

  }
}

