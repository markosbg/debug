package org.rabix.executor.engine;

import org.apache.commons.configuration.Configuration;
import org.rabix.executor.service.ExecutorService;
import org.rabix.transport.backend.impl.BackendLocal;
import org.rabix.transport.mechanism.TransportPluginException;
import org.rabix.transport.mechanism.impl.local.TransportPluginLocal;
import org.rabix.transport.mechanism.impl.local.TransportQueueLocal;

public class EngineStubLocal extends EngineStub<TransportQueueLocal, BackendLocal, TransportPluginLocal> {

  public EngineStubLocal(BackendLocal backendLocal, ExecutorService executorService, Configuration configuration) throws TransportPluginException {
    this.backend = backendLocal;
    this.executorService = executorService;
    this.transportPlugin = new TransportPluginLocal(configuration);
    
    this.sendToBackendQueue = new TransportQueueLocal(BackendLocal.SEND_TO_BACKEND_QUEUE);
    this.sendToBackendControlQueue = new TransportQueueLocal(BackendLocal.SEND_TO_BACKEND_CONTROL_QUEUE);
    this.receiveFromBackendQueue = new TransportQueueLocal(BackendLocal.RECEIVE_FROM_BACKEND_QUEUE);
    this.receiveFromBackendHeartbeatQueue = new TransportQueueLocal(BackendLocal.RECEIVE_FROM_BACKEND_HEARTBEAT_QUEUE);
  }
  
}
