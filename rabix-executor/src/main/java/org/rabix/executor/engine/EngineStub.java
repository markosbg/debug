package org.rabix.executor.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.rabix.bindings.model.Job;
import org.rabix.common.engine.control.EngineControlFreeMessage;
import org.rabix.common.engine.control.EngineControlMessage;
import org.rabix.common.engine.control.EngineControlStopMessage;
import org.rabix.executor.service.ExecutorService;
import org.rabix.executor.service.FileService;
import org.rabix.transport.backend.Backend;
import org.rabix.transport.backend.HeartbeatInfo;
import org.rabix.transport.mechanism.TransportPlugin;
import org.rabix.transport.mechanism.TransportPlugin.ErrorCallback;
import org.rabix.transport.mechanism.TransportPlugin.ReceiveCallback;
import org.rabix.transport.mechanism.TransportPluginException;
import org.rabix.transport.mechanism.TransportQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EngineStub<Q extends TransportQueue, B extends Backend, T extends TransportPlugin<Q>> {

  protected final Logger logger = LoggerFactory.getLogger(getClass());
  
  protected B backend;
  protected T transportPlugin;

  protected ScheduledExecutorService scheduledHeartbeatService = Executors.newSingleThreadScheduledExecutor();

  protected Q sendToBackendQueue;
  protected Q sendToBackendControlQueue;
  protected Q receiveFromBackendQueue;
  protected Q receiveFromBackendHeartbeatQueue;
  
  protected FileService fileService;
  protected ExecutorService executorService;
  
  public void start() {
    transportPlugin.startReceiver(sendToBackendQueue, Job.class, new ReceiveCallback<Job>() {
      @Override
      public void handleReceive(Job job) throws TransportPluginException {
        executorService.start(job, job.getRootId());
      }
    }, new ErrorCallback() {
      @Override
      public void handleError(Exception error) {
        logger.error("Failed to receive message.", error);
      }
    });

    transportPlugin.startReceiver(sendToBackendControlQueue, EngineControlMessage.class, new ReceiveCallback<EngineControlMessage>() {
      @Override
      public void handleReceive(EngineControlMessage controlMessage) throws TransportPluginException {
        switch (controlMessage.getType()) {
        case STOP:
          List<String> ids = new ArrayList<>();
          ids.add(((EngineControlStopMessage)controlMessage).getId());
          executorService.stop(ids, controlMessage.getRootId());
          break;
        case FREE:
          executorService.free(controlMessage.getRootId(), ((EngineControlFreeMessage)controlMessage).getConfig());
          break;
        default:
          break;
        }
      }
    }, new ErrorCallback() {
      @Override
      public void handleError(Exception error) {
        logger.error("Failed to execute control message.", error);
      }
    });

    scheduledHeartbeatService.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        transportPlugin.send(receiveFromBackendHeartbeatQueue, new HeartbeatInfo(backend.getId(), System.currentTimeMillis()));
      }
    }, 0, 1, TimeUnit.SECONDS);
  }

  public void stop() {
    scheduledHeartbeatService.shutdown();
  }

  public void send(Job job) {
    transportPlugin.send(receiveFromBackendQueue, job);
  }

}
