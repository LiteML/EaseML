package easeml.console.modules

import com.google.inject.AbstractModule
import easeml.console.listener.ServiceRegister

/**
  * Created by takun on 04/01/2018.
  */
class ServerEvent extends AbstractModule {

  override def configure() = {
    // We bind the implementation to the interface (trait) as an eager singleton,
    // which means it is bound immediately when the application starts.
    bind(classOf[ServiceRegister]).asEagerSingleton()
  }
}