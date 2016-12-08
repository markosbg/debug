package org.rabix.bindings;

import java.util.Set;

import org.rabix.bindings.mapper.FilePathMapper;
import org.rabix.bindings.model.FileValue;
import org.rabix.bindings.model.Job;
import org.rabix.bindings.transformer.FileTransformer;

public interface ProtocolFileValueProcessor {

  Set<FileValue> getInputFiles(Job job, FilePathMapper fileMapper) throws BindingException;
  
  Job updateInputFiles(Job job, FileTransformer fileTransformer) throws BindingException;

  Job updateOutputFiles(Job job, FileTransformer fileTransformer) throws BindingException;

}
