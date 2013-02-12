package org.lilyproject.servletregistry.api;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import java.util.EventListener;
import java.util.List;

public interface ServletRegistryEntry {
  
  List<String> getUrlPatterns();
  Servlet getServletInstance(ServletContext context);
  List<EventListener> getEventListeners();

}
